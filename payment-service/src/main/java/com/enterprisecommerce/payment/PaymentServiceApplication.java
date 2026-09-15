package com.enterprisecommerce.payment;

import com.enterprisecommerce.platform.common.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.time.Instant;
import java.util.UUID;

@SpringBootApplication(scanBasePackages = "com.enterprisecommerce")
public class PaymentServiceApplication {
    public static void main(String[] args) { SpringApplication.run(PaymentServiceApplication.class, args); }

    @Component
    static class PaymentConsumer {
        private final ObjectMapper mapper;
        private final EventPublisher publisher;

        PaymentConsumer(ObjectMapper mapper, EventPublisher publisher) {
            this.mapper = mapper;
            this.publisher = publisher;
        }

        @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "payment-service")
        void authorize(String payload) throws Exception {
            OrderEvents.OrderPlaced order = mapper.readValue(payload, OrderEvents.OrderPlaced.class);
            boolean accepted = order.amountInMinorUnits() < 1_000_000;
            if (accepted) {
                publisher.publish(Topics.PAYMENT_EVENTS, order.orderId().toString(),
                        new OrderEvents.PaymentAuthorized(UUID.randomUUID(), order.orderId(),
                                "pay_" + order.orderId(), Instant.now()));
            } else {
                publisher.publish(Topics.PAYMENT_EVENTS, order.orderId().toString(),
                        new OrderEvents.PaymentFailed(UUID.randomUUID(), order.orderId(),
                                "Payment requires manual review", Instant.now()));
            }
        }
    }
}
