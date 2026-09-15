package com.enterprisecommerce.catalog;

import com.enterprisecommerce.platform.common.observability.UseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class CatalogService {
    private static final String CACHE_PREFIX = "catalog:product:";
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final ConcurrentMap<UUID, CatalogController.Product> products = new ConcurrentHashMap<>();

    public CatalogService(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
        products.put(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                new CatalogController.Product(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Platform Starter", 4999, "INR"));
        products.put(UUID.fromString("00000000-0000-0000-0000-000000000002"),
                new CatalogController.Product(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Enterprise Bundle", 14999, "INR"));
    }

    @UseCase("catalog.search")
    public List<CatalogController.Product> search(String query) {
        return products.values().stream()
                .filter(product -> query == null || query.isBlank() ||
                        product.name().toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    @UseCase("catalog.get-product")
    public CatalogController.Product get(UUID id) {
        String key = CACHE_PREFIX + id;
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                return mapper.readValue(cached, CatalogController.Product.class);
            } catch (JsonProcessingException exception) {
                redis.delete(key);
            }
        }
        CatalogController.Product product = products.get(id);
        if (product == null) throw new CatalogController.ProductNotFoundException(id);
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(product), Duration.ofMinutes(10));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not cache product " + id, exception);
        }
        return product;
    }

    @UseCase("catalog.upsert-product")
    public CatalogController.Product upsert(UUID id, CatalogController.ProductRequest request) {
        CatalogController.Product product = new CatalogController.Product(id, request.name(),
                request.priceInMinorUnits(), request.currency());
        products.put(id, product);
        evict(id);
        return product;
    }

    public void evict(UUID id) {
        redis.delete(CACHE_PREFIX + id);
    }
}
