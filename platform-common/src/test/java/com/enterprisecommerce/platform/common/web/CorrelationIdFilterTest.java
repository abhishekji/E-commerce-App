package com.enterprisecommerce.platform.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {
    @Test
    void usesExistingHeaderAndCleansUpMdc() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> insideMdc = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            insideMdc.set(MDC.get(CorrelationIdFilter.HEADER));
            assertEquals("req-123", insideMdc.get());
        };

        filter.doFilter(request, response, chain);

        assertEquals("req-123", response.getHeader(CorrelationIdFilter.HEADER));
        assertEquals("req-123", insideMdc.get());
        assertNull(MDC.get(CorrelationIdFilter.HEADER));
    }

    @Test
    void generatesCorrelationIdWhenHeaderMissing() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> insideMdc = new AtomicReference<>();
        FilterChain chain = (req, res) -> insideMdc.set(MDC.get(CorrelationIdFilter.HEADER));

        filter.doFilter(request, response, chain);

        String id = response.getHeader(CorrelationIdFilter.HEADER);
        assertNotNull(id);
        assertFalse(id.isBlank());
        assertTrue(UUID.fromString(id).toString().equals(id));
        assertNotNull(insideMdc.get());
        assertNull(MDC.get(CorrelationIdFilter.HEADER));
    }
}
