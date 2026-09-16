package com.enterprisecommerce.platform.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EventConfigurationTest {
    @Test
    void createsEventPublisherBean() {
        EventConfiguration configuration = new EventConfiguration();
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        EventPublisher publisher = configuration.eventPublisher(kafkaTemplate, objectMapper);

        assertNotNull(publisher);
    }
}
