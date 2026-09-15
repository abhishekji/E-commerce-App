package com.enterprisecommerce.order;

import com.enterprisecommerce.platform.common.events.OrderEvents;
import com.enterprisecommerce.platform.common.events.EventPublisher;
import com.enterprisecommerce.platform.common.events.Topics;
import com.enterprisecommerce.platform.common.events.InventoryEvents;
import com.enterprisecommerce.platform.common.observability.UseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final Map<UUID, OrderAccepted> orders = new ConcurrentHashMap<>();
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final EventPublisher events;

    public OrderController(StringRedisTemplate redis, ObjectMapper mapper, EventPublisher events) {
        this.redis = redis;
        this.mapper = mapper;
        this.events = events;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @UseCase("order.place")
    OrderAccepted place(@RequestBody PlaceOrderRequest request,
                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
        String key = "order:idempotency:" + idempotencyKey;
        String existing = redis.opsForValue().get(key);
        if (existing != null) {
            try { return mapper.readValue(existing, OrderAccepted.class); }
            catch (JsonProcessingException exception) { throw new IllegalStateException("Invalid idempotency record", exception); }
        }
        if (request.customerId() == null || request.amountInMinorUnits() <= 0 || request.currency() == null) {
            throw new IllegalArgumentException("customerId, positive amount, and currency are required");
        }
        if (request.items() == null || request.items().isEmpty() ||
                request.items().stream().anyMatch(item -> item.productId() == null || item.quantity() <= 0 ||
                        item.unitPriceInMinorUnits() <= 0)) {
            throw new IllegalArgumentException("At least one valid product line is required");
        }
        long calculatedAmount = request.items().stream()
                .mapToLong(item -> Math.multiplyExact(item.quantity(), item.unitPriceInMinorUnits())).sum();
        if (calculatedAmount != request.amountInMinorUnits()) {
            throw new IllegalArgumentException("amountInMinorUnits must equal the sum of product lines");
        }
        UUID orderId = UUID.randomUUID();
        OrderAccepted accepted = new OrderAccepted(orderId, "PENDING_PAYMENT",
                new OrderEvents.OrderPlaced(UUID.randomUUID(), orderId, request.customerId(),
                        calculatedAmount, request.currency(), request.items(), Instant.now()));
        try {
            String json = mapper.writeValueAsString(accepted);
            Boolean stored = redis.opsForValue().setIfAbsent(key, json, Duration.ofHours(24));
            if (Boolean.FALSE.equals(stored)) return place(request, idempotencyKey);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not persist idempotency record", exception);
        }
        orders.put(orderId, accepted);
        events.publish(Topics.ORDER_EVENTS, orderId.toString(), accepted.event());
        return accepted;
    }

    @GetMapping("/{orderId}")
    OrderAccepted get(@PathVariable UUID orderId) {
        OrderAccepted order = orders.get(orderId);
        if (order == null) throw new OrderNotFoundException(orderId);
        return order;
    }

    @KafkaListener(topics = Topics.PAYMENT_EVENTS, groupId = "order-service")
    void onPayment(String payload) {
        updateStatus(extractOrderId(payload), payload.contains("\"reason\"") ? "PAYMENT_FAILED" : "PAYMENT_AUTHORIZED");
    }

    @KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "order-service")
    void onInventory(String payload) {
        updateStatus(extractOrderId(payload), payload.contains("\"reason\"") ? "INVENTORY_REJECTED" : "INVENTORY_RESERVED");
    }

    private UUID extractOrderId(String payload) {
        try { return mapper.readTree(payload).get("orderId").asText().transform(UUID::fromString); }
        catch (Exception exception) { throw new IllegalStateException("Invalid event payload", exception); }
    }

    private void updateStatus(UUID orderId, String status) {
        orders.computeIfPresent(orderId, (id, order) -> new OrderAccepted(id, status, order.event()));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class OrderNotFoundException extends RuntimeException {
        OrderNotFoundException(UUID id) { super("Order not found: " + id); }
    }

    public record PlaceOrderRequest(UUID customerId, long amountInMinorUnits, String currency,
                                    List<OrderEvents.LineItem> items) {}
    public record OrderAccepted(UUID orderId, String status, OrderEvents.OrderPlaced event) {}
}
