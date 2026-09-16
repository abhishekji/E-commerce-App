package com.enterprisecommerce.platform.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventPublisherTest {
    @Test
    void publishesSerializedEventToKafka() throws Exception {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPublisher publisher = new EventPublisher(kafkaTemplate, objectMapper);
        OrderEvents.OrderPlaced event = new OrderEvents.OrderPlaced(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                2500,
                "INR",
                java.util.List.of(new OrderEvents.LineItem(java.util.UUID.randomUUID(), 2, 1250)),
                java.time.Instant.now());

        publisher.publish("commerce.order.events.v1", "customer-1", event);

        verify(kafkaTemplate).send(eq("commerce.order.events.v1"), eq("customer-1"), anyString());
    }

    @Test
    void throwsWhenEventCannotBeSerialized() throws Exception {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        EventPublisher publisher = new EventPublisher(kafkaTemplate, mapper);
        Object event = new Object();
        when(mapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("bad") {});

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> publisher.publish("topic", "key", event));

        assertTrue(exception.getMessage().contains("Could not serialize event for topic topic"));
    }
}
