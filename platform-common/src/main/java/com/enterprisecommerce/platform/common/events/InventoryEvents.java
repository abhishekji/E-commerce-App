package com.enterprisecommerce.platform.common.events;

import java.time.Instant;
import java.util.UUID;

public final class InventoryEvents {
    private InventoryEvents() {}

    public record InventoryReserved(UUID eventId, UUID orderId, Instant occurredAt) {}
    public record InventoryRejected(UUID eventId, UUID orderId, String reason, Instant occurredAt) {}
    public record InventoryReleaseRequested(UUID eventId, UUID orderId, Instant occurredAt) {}
}
