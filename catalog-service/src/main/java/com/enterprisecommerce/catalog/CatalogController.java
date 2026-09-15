package com.enterprisecommerce.catalog;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/products")
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    List<Product> search(@RequestParam(required = false) String q) {
        return catalog.search(q);
    }

    @GetMapping("/{id}")
    Product get(@PathVariable UUID id) {
        return catalog.get(id);
    }

    @PutMapping("/{id}")
    Product upsert(@PathVariable UUID id, @RequestBody ProductRequest request) {
        if (request.name() == null || request.name().isBlank() || request.priceInMinorUnits() <= 0 ||
                request.currency() == null || request.currency().isBlank()) {
            throw new IllegalArgumentException("name, positive price, and currency are required");
        }
        return catalog.upsert(id, request);
    }

    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    static class ProductNotFoundException extends RuntimeException {
        ProductNotFoundException(UUID id) { super("Product not found: " + id); }
    }

    public record ProductRequest(String name, long priceInMinorUnits, String currency) {}
    public record Product(UUID id, String name, long priceInMinorUnits, String currency) {}
}
