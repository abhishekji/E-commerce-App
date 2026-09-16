package com.enterprisecommerce.notification;

import com.enterprisecommerce.platform.common.events.OrderEvents;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceApplicationTest {
    @Test
    void orderPlacedCreatesNotification() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        NotificationServiceApplication.NotificationDeliveryService service =
                new NotificationServiceApplication.NotificationDeliveryService(mapper);
        UUID orderId = UUID.randomUUID();
        OrderEvents.OrderPlaced event = new OrderEvents.OrderPlaced(
                UUID.randomUUID(), orderId, UUID.randomUUID(), 2500, "INR",
                List.of(new OrderEvents.LineItem(UUID.randomUUID(), 2, 1250)),
                Instant.now());

        service.orderPlaced(mapper.writeValueAsString(event));

        assertEquals(1, service.deliveries().size());
        assertEquals("ORDER_PLACED", service.deliveries().getFirst().type());
    }

    @Test
    void paymentFailureCreatesNotification() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        NotificationServiceApplication.NotificationDeliveryService service =
                new NotificationServiceApplication.NotificationDeliveryService(mapper);
        UUID orderId = UUID.randomUUID();
        OrderEvents.PaymentFailed event = new OrderEvents.PaymentFailed(
                UUID.randomUUID(), orderId, "Payment requires manual review", Instant.now());

        service.paymentChanged(mapper.writeValueAsString(event));

        assertEquals("PAYMENT_FAILED", service.deliveries().getFirst().type());
    }

    @Test
    void inventoryReservedCreatesNotification() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        NotificationServiceApplication.NotificationDeliveryService service =
                new NotificationServiceApplication.NotificationDeliveryService(mapper);
        UUID orderId = UUID.randomUUID();
        OrderEvents.OrderPlaced event = new OrderEvents.OrderPlaced(
                UUID.randomUUID(), orderId, UUID.randomUUID(), 2500, "INR",
                List.of(new OrderEvents.LineItem(UUID.randomUUID(), 1, 2500)),
                Instant.now());

        service.inventoryChanged(mapper.writeValueAsString(event));

        assertEquals("INVENTORY_RESERVED", service.deliveries().getFirst().type());
    }
}
