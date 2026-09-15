package com.enterprisecommerce.platform.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;

public class EventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize event for topic " + topic, exception);
        }
    }
}
