package org.open4goods.nudgerfrontapi.controller;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.service.ProductViewService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * REST endpoints for products.
 */
@RestController
public class ProductController {

    private final ProductRepository repository;
    private final ProductViewService renderingService;

    public ProductController(ProductRepository repository, ProductViewService renderingService) {
        this.repository = repository;
        this.renderingService = renderingService;
    }

    @GetMapping("/product/{gtin}")
    @Operation(summary = "Get a product")
    public ProductViewResponse product(@PathVariable("gtin") Long gtin) {
        return renderingService.render(new ProductViewRequest(gtin));
    }
}
