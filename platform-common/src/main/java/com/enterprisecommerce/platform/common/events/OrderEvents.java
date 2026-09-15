package com.enterprisecommerce.platform.common.events;

import java.time.Instant;
import java.util.UUID;

public final class OrderEvents {
    private OrderEvents() {}

    public record OrderPlaced(
            UUID eventId,
            UUID orderId,
            UUID customerId,
            long amountInMinorUnits,
            String currency,
            java.util.List<LineItem> items,
            Instant occurredAt) {}

    public record LineItem(UUID productId, int quantity, long unitPriceInMinorUnits) {}

    public record PaymentAuthorized(
            UUID eventId,
            UUID orderId,
            String paymentReference,
            Instant occurredAt) {}

    public record PaymentFailed(
            UUID eventId,
            UUID orderId,
            String reason,
            Instant occurredAt) {}

    public record OrderConfirmed(UUID eventId, UUID orderId, Instant occurredAt) {}
}
