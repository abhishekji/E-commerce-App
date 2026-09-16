package com.enterprisecommerce.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CatalogServiceTest {
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private CatalogService service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper();
        when(redis.opsForValue()).thenReturn(valueOperations);
        service = new CatalogService(redis, objectMapper);
    }

    @Test
    void searchReturnsMatchesIgnoringCase() {
        List<CatalogController.Product> products = service.search("starter");

        assertEquals(1, products.size());
        assertEquals("Platform Starter", products.getFirst().name());
    }

    @Test
    void getReadsFromRedisCacheWhenPresent() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        CatalogController.Product product = new CatalogController.Product(id, "Platform Starter", 4999, "INR");
        when(valueOperations.get("catalog:product:" + id)).thenReturn(objectMapper.writeValueAsString(product));

        CatalogController.Product result = service.get(id);

        assertEquals(product, result);
    }

    @Test
    void getThrowsNotFoundWhenProductMissing() {
        UUID id = UUID.randomUUID();

        CatalogController.ProductNotFoundException exception = assertThrows(CatalogController.ProductNotFoundException.class,
                () -> service.get(id));

        assertTrue(exception.getMessage().contains(id.toString()));
    }

    @Test
    void upsertStoresProductAndEvictsCache() {
        UUID id = UUID.randomUUID();
        CatalogController.ProductRequest request = new CatalogController.ProductRequest("New Product", 3000, "USD");

        CatalogController.Product result = service.upsert(id, request);

        assertEquals("New Product", result.name());
        assertEquals(id, result.id());
        verify(redis).delete("catalog:product:" + id);
    }
}
