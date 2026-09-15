package com.enterprisecommerce.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.enterprisecommerce.platform.common.events.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Service;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@SpringBootApplication(scanBasePackages = "com.enterprisecommerce")
public class NotificationServiceApplication {
    public static void main(String[] args) { SpringApplication.run(NotificationServiceApplication.class, args); }

    @RestController
    static class NotificationController {
        private final NotificationDeliveryService delivery;
        NotificationController(NotificationDeliveryService delivery) { this.delivery = delivery; }
        @GetMapping("/api/notifications")
        List<NotificationDelivery> deliveries() { return delivery.deliveries(); }
    }

    @Service
    static class NotificationDeliveryService {
        private final ObjectMapper mapper;
        private final CopyOnWriteArrayList<NotificationDelivery> deliveries = new CopyOnWriteArrayList<>();

        NotificationDeliveryService(ObjectMapper mapper) { this.mapper = mapper; }

        @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "notification-service")
        void orderPlaced(String payload) { create(payload, "ORDER_PLACED", "Your order was received"); }

        @KafkaListener(topics = Topics.PAYMENT_EVENTS, groupId = "notification-service")
        void paymentChanged(String payload) {
            create(payload, payload.contains("\"reason\"") ? "PAYMENT_FAILED" : "PAYMENT_AUTHORIZED",
                    payload.contains("\"reason\"") ? "Payment needs attention" : "Payment authorized");
        }

        @KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "notification-service")
        void inventoryChanged(String payload) {
            create(payload, payload.contains("\"reason\"") ? "INVENTORY_REJECTED" : "INVENTORY_RESERVED",
                    payload.contains("\"reason\"") ? "An item is unavailable" : "Items reserved");
        }

        private void create(String payload, String type, String message) {
            try {
                JsonNode event = mapper.readTree(payload);
                deliveries.add(new NotificationDelivery(UUID.randomUUID(), event.get("eventId").asText(),
                        event.get("orderId").asText(), type, message, Instant.now(), "ACCEPTED"));
            } catch (Exception exception) {
                throw new IllegalStateException("Invalid event received by notification service", exception);
            }
        }

        List<NotificationDelivery> deliveries() { return List.copyOf(deliveries); }
    }

    record NotificationDelivery(UUID notificationId, String eventId, String orderId, String type,
                                String message, Instant acceptedAt, String status) {}
}
