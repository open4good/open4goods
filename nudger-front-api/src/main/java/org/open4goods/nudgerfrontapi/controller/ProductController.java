package org.open4goods.nudgerfrontapi.controller;

import java.util.Map;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.services.ProductRenderingService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class ProductController {


	private ProductRepository repository;
	private ProductRenderingService renderingService;


    @GetMapping("/product/{gtin}")
    @Operation(summary = "Get a ptoduct")
	// TODO : Add spring doc maximum documentation

    public ProductViewResponse product(ProductViewRequest productViewRequest) {

    	ProductViewResponse ret = renderingService.render(productViewRequest);

        return ret;
    }
}
