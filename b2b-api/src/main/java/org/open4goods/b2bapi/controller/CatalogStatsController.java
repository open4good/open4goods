package org.open4goods.b2bapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public endpoint returning catalog-level statistics (no auth required).
 */
@Tag(name = "Catalog", description = "Public catalog statistics")
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogStatsController {

    private final ProductRepository productRepository;

    public CatalogStatsController(final ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Operation(summary = "Get catalog statistics", description = "Returns the total number of indexed products. Cached for 10 minutes.")
    @Cacheable("catalogStats")
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Long> getCatalogStats() {
        return Map.of("indexedProducts", productRepository.countMainIndex());
    }
}
