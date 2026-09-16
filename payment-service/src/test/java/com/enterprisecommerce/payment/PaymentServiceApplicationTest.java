package com.enterprisecommerce.payment;

import com.enterprisecommerce.platform.common.events.EventPublisher;
import com.enterprisecommerce.platform.common.events.OrderEvents;
import com.enterprisecommerce.platform.common.events.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceApplicationTest {
    @Test
    void authorizesPaymentWhenOrderIsBelowThreshold() throws Exception {
        EventPublisher publisher = mock(EventPublisher.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        PaymentServiceApplication.PaymentConsumer consumer = new PaymentServiceApplication.PaymentConsumer(mapper, publisher);
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderEvents.OrderPlaced order = new OrderEvents.OrderPlaced(
                UUID.randomUUID(), orderId, customerId, 500, "INR",
                List.of(new OrderEvents.LineItem(UUID.randomUUID(), 1, 500)),
                Instant.now());

        consumer.authorize(mapper.writeValueAsString(order));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publish(eq(Topics.PAYMENT_EVENTS), eq(order.orderId().toString()), eventCaptor.capture());
        assertInstanceOf(OrderEvents.PaymentAuthorized.class, eventCaptor.getValue());
    }

    @Test
    void rejectsPaymentWhenOrderExceedsThreshold() throws Exception {
        EventPublisher publisher = mock(EventPublisher.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        PaymentServiceApplication.PaymentConsumer consumer = new PaymentServiceApplication.PaymentConsumer(mapper, publisher);
        UUID orderId = UUID.randomUUID();
        OrderEvents.OrderPlaced order = new OrderEvents.OrderPlaced(
                UUID.randomUUID(), orderId, UUID.randomUUID(), 1_000_001, "INR",
                List.of(new OrderEvents.LineItem(UUID.randomUUID(), 1, 1_000_001)),
                Instant.now());

        consumer.authorize(mapper.writeValueAsString(order));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publish(eq(Topics.PAYMENT_EVENTS), eq(order.orderId().toString()), eventCaptor.capture());
        assertInstanceOf(OrderEvents.PaymentFailed.class, eventCaptor.getValue());
    }
}
