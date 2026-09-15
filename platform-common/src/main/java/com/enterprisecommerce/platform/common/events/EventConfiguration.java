package com.enterprisecommerce.platform.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class EventConfiguration {
    @Bean
    EventPublisher eventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        return new EventPublisher(kafkaTemplate, objectMapper);
    }
}
