package org.open4goods.b2bapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import org.open4goods.b2bapi.dto.ProductSimpleDto;
import org.open4goods.b2bapi.service.ProductMappingService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductsController {

    private final ProductMappingService service;

    public ProductsController(ProductMappingService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List products", description = "Returns paginated product list with filters")
    public Page<ProductSimpleDto> getProducts(
            @ParameterObject @PageableDefault(size = 20, sort = "offersCount", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "requireVertical", defaultValue = "false") boolean requireVertical,
            @RequestParam(name = "minOffers", required = false) @Min(0) Integer minOffers
    ) {
        return service.getProducts(pageable, requireVertical, minOffers);
    }
}
