package com.enterprisecommerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.enterprisecommerce")
public class OrderServiceApplication {
    public static void main(String[] args) { SpringApplication.run(OrderServiceApplication.class, args); }
}
