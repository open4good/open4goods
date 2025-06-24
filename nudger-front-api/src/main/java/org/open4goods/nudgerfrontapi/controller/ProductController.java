package org.open4goods.nudgerfrontapi.controller;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.service.ProductViewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * REST controller exposing product endpoints for the frontend.
 */
@RestController
public class ProductController {

    private final ProductViewService renderingService;

    public ProductController(ProductViewService renderingService) {
        this.renderingService = renderingService;
    }

    @GetMapping("/product/{gtin}")
    @Operation(summary = "Get a product")
    // TODO : Add spring doc maximum documentation
    public ProductViewResponse product(ProductViewRequest productViewRequest) {
        return renderingService.render(productViewRequest);
    }
}
