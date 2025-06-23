package org.open4goods.nudgerfrontapi.controller;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.service.ProductViewUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class ProductController {

    private final ProductViewUseCase productView;

    public ProductController(ProductViewUseCase productView) {
        this.productView = productView;
    }

    @GetMapping("/product/{gtin}")
    @Operation(summary = "Get a product")
    public ProductViewResponse product(@PathVariable long gtin) {
        ProductViewRequest request = new ProductViewRequest(gtin);
        return productView.render(request);
    }
}
