package com.enterprisecommerce.inventory;

import com.enterprisecommerce.platform.common.events.EventPublisher;
import com.enterprisecommerce.platform.common.events.InventoryEvents;
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

class InventoryServiceApplicationTest {
    @Test
    void reservesInventoryWhenStockIsSufficient() throws Exception {
        EventPublisher publisher = mock(EventPublisher.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        InventoryServiceApplication.InventoryConsumer consumer = new InventoryServiceApplication.InventoryConsumer(mapper, publisher);
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID orderId = UUID.randomUUID();
        OrderEvents.OrderPlaced order = new OrderEvents.OrderPlaced(
                UUID.randomUUID(), orderId, UUID.randomUUID(), 2000, "INR",
                List.of(new OrderEvents.LineItem(productId, 2, 1000)),
                Instant.now());

        consumer.reserve(mapper.writeValueAsString(order));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publish(eq(Topics.INVENTORY_EVENTS), eq(order.orderId().toString()), eventCaptor.capture());
        assertInstanceOf(InventoryEvents.InventoryReserved.class, eventCaptor.getValue());
    }

    @Test
    void rejectsInventoryWhenStockIsInsufficient() throws Exception {
        EventPublisher publisher = mock(EventPublisher.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        InventoryServiceApplication.InventoryConsumer consumer = new InventoryServiceApplication.InventoryConsumer(mapper, publisher);
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID orderId = UUID.randomUUID();
        OrderEvents.OrderPlaced order = new OrderEvents.OrderPlaced(
                UUID.randomUUID(), orderId, UUID.randomUUID(), 5000, "INR",
                List.of(new OrderEvents.LineItem(productId, 100, 50)),
                Instant.now());

        consumer.reserve(mapper.writeValueAsString(order));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publish(eq(Topics.INVENTORY_EVENTS), eq(order.orderId().toString()), eventCaptor.capture());
        assertInstanceOf(InventoryEvents.InventoryRejected.class, eventCaptor.getValue());
    }
}
