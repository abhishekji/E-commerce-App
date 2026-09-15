package com.enterprisecommerce.platform.common.config;

import com.enterprisecommerce.platform.common.web.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlatformWebConfiguration {
    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}
