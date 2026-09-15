package com.enterprisecommerce.inventory;

import com.enterprisecommerce.platform.common.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SpringBootApplication(scanBasePackages = "com.enterprisecommerce")
public class InventoryServiceApplication {
    public static void main(String[] args) { SpringApplication.run(InventoryServiceApplication.class, args); }

    @Component
    static class InventoryConsumer {
        private final ObjectMapper mapper;
        private final EventPublisher publisher;
        private final ConcurrentMap<UUID, Integer> stock = new ConcurrentHashMap<>();

        InventoryConsumer(ObjectMapper mapper, EventPublisher publisher) {
            this.mapper = mapper;
            this.publisher = publisher;
            stock.put(UUID.fromString("00000000-0000-0000-0000-000000000001"), 100);
            stock.put(UUID.fromString("00000000-0000-0000-0000-000000000002"), 25);
        }

        @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "inventory-service")
        synchronized void reserve(String payload) throws Exception {
            OrderEvents.OrderPlaced order = mapper.readValue(payload, OrderEvents.OrderPlaced.class);
            // A real implementation atomically decrements all order lines in its database.
            boolean available = order.items().stream()
                    .allMatch(item -> stock.getOrDefault(item.productId(), 0) >= item.quantity());
            if (available) {
                order.items().forEach(item -> stock.computeIfPresent(item.productId(),
                        (id, quantity) -> quantity - item.quantity()));
                publisher.publish(Topics.INVENTORY_EVENTS, order.orderId().toString(),
                        new InventoryEvents.InventoryReserved(UUID.randomUUID(), order.orderId(), Instant.now()));
            } else {
                publisher.publish(Topics.INVENTORY_EVENTS, order.orderId().toString(),
                        new InventoryEvents.InventoryRejected(UUID.randomUUID(), order.orderId(),
                                "Insufficient stock", Instant.now()));
            }
        }
    }
}
