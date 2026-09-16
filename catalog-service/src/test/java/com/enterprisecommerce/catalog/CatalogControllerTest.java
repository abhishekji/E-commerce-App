package com.enterprisecommerce.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogControllerTest {
    @Test
    void searchDelegatesToService() {
        CatalogService service = mock(CatalogService.class);
        CatalogController controller = new CatalogController(service);
        CatalogController.Product product = new CatalogController.Product(UUID.randomUUID(), "Starter", 4999, "INR");
        when(service.search("starter")).thenReturn(List.of(product));

        List<CatalogController.Product> result = controller.search("starter");

        assertEquals(List.of(product), result);
        verify(service).search("starter");
    }

    @Test
    void getDelegatesToService() {
        CatalogService service = mock(CatalogService.class);
        CatalogController controller = new CatalogController(service);
        UUID id = UUID.randomUUID();
        CatalogController.Product product = new CatalogController.Product(id, "Starter", 4999, "INR");
        when(service.get(id)).thenReturn(product);

        CatalogController.Product result = controller.get(id);

        assertEquals(product, result);
        verify(service).get(id);
    }

    @Test
    void upsertValidatesInput() {
        CatalogController controller = new CatalogController(mock(CatalogService.class));
        UUID id = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.upsert(id, new CatalogController.ProductRequest("", 100, "INR")));

        assertTrue(exception.getMessage().contains("name, positive price, and currency are required"));
    }

    @Test
    void upsertDelegatesToService() {
        CatalogService service = mock(CatalogService.class);
        CatalogController controller = new CatalogController(service);
        UUID id = UUID.randomUUID();
        CatalogController.ProductRequest request = new CatalogController.ProductRequest("Starter", 4999, "INR");
        CatalogController.Product product = new CatalogController.Product(id, "Starter", 4999, "INR");
        when(service.upsert(id, request)).thenReturn(product);

        CatalogController.Product result = controller.upsert(id, request);

        assertEquals(product, result);
        verify(service).upsert(id, request);
    }
}
