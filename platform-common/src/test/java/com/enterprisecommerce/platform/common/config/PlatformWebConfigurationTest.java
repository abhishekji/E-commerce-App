package com.enterprisecommerce.platform.common.config;

import com.enterprisecommerce.platform.common.web.CorrelationIdFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformWebConfigurationTest {
    @Test
    void createsCorrelationIdFilterBean() {
        PlatformWebConfiguration configuration = new PlatformWebConfiguration();

        CorrelationIdFilter filter = configuration.correlationIdFilter();

        assertNotNull(filter);
        assertTrue(filter instanceof CorrelationIdFilter);
    }
}
