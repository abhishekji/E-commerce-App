package com.enterprisecommerce.order;

import com.enterprisecommerce.platform.common.events.EventPublisher;
import com.enterprisecommerce.platform.common.events.OrderEvents;
import com.enterprisecommerce.platform.common.events.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderControllerTest {
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private EventPublisher events;
    private OrderController controller;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        events = mock(EventPublisher.class);
        when(redis.opsForValue()).thenReturn(valueOperations);
        controller = new OrderController(redis, objectMapper, events);
    }

    @Test
    void placeCreatesOrderAndPublishesEvent() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "idem-123";
        when(valueOperations.get("order:idempotency:" + idempotencyKey)).thenReturn(null);
        when(valueOperations.setIfAbsent(eq("order:idempotency:" + idempotencyKey), anyString(), eq(Duration.ofHours(24))))
                .thenReturn(true);

        OrderController.OrderAccepted accepted = controller.place(
                new OrderController.PlaceOrderRequest(
                        customerId,
                        2500,
                        "INR",
                        List.of(new OrderEvents.LineItem(productId, 2, 1250))),
                idempotencyKey);

        assertEquals("PENDING_PAYMENT", accepted.status());
        assertNotNull(accepted.orderId());
        assertEquals(customerId, accepted.event().customerId());
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(events).publish(eq(Topics.ORDER_EVENTS), eq(accepted.orderId().toString()), eventCaptor.capture());
        assertTrue(eventCaptor.getValue() instanceof OrderEvents.OrderPlaced);
    }

    @Test
    void placeRequiresIdempotencyKey() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.place(new OrderController.PlaceOrderRequest(
                        UUID.randomUUID(), 1000, "INR",
                        List.of(new OrderEvents.LineItem(UUID.randomUUID(), 1, 1000))), null));

        assertTrue(exception.getMessage().contains("Idempotency-Key is required"));
    }

    @Test
    void getReturnsOrderWhenPresent() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "idem-456";
        when(valueOperations.get("order:idempotency:" + idempotencyKey)).thenReturn(null);
        when(valueOperations.setIfAbsent(eq("order:idempotency:" + idempotencyKey), anyString(), eq(Duration.ofHours(24))))
                .thenReturn(true);

        OrderController.OrderAccepted accepted = controller.place(
                new OrderController.PlaceOrderRequest(customerId, 500, "INR",
                        List.of(new OrderEvents.LineItem(productId, 1, 500))),
                idempotencyKey);

        OrderController.OrderAccepted stored = controller.get(accepted.orderId());

        assertEquals(accepted, stored);
    }

    @Test
    void paymentListenerUpdatesStatus() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "idem-789";
        when(valueOperations.get("order:idempotency:" + idempotencyKey)).thenReturn(null);
        when(valueOperations.setIfAbsent(eq("order:idempotency:" + idempotencyKey), anyString(), eq(Duration.ofHours(24))))
                .thenReturn(true);

        OrderController.OrderAccepted accepted = controller.place(
                new OrderController.PlaceOrderRequest(customerId, 1500, "INR",
                        List.of(new OrderEvents.LineItem(productId, 1, 1500))),
                idempotencyKey);
        String payload = objectMapper.writeValueAsString(new OrderEvents.PaymentAuthorized(
                UUID.randomUUID(), accepted.orderId(), "pay_123", Instant.now()));

        controller.onPayment(payload);

        assertEquals("PAYMENT_AUTHORIZED", controller.get(accepted.orderId()).status());
    }
}
