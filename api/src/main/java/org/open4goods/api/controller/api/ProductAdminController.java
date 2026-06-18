package org.open4goods.api.controller.api;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.commons.services.ProductNameSelectionService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Admin endpoints for inspecting and retrieving individual products.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class ProductAdminController {

    private final ProductRepository repository;
    private final ProductNameSelectionService productNameSelectionService;

    public ProductAdminController(ProductRepository repository,
                                  ProductNameSelectionService productNameSelectionService) {
        this.repository = repository;
        this.productNameSelectionService = productNameSelectionService;
    }

    @GetMapping(path = "/product", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a product by GTIN")
    public Product get(@RequestParam final Long gtin) throws ResourceNotFoundException {
        return repository.getById(gtin);
    }

    @GetMapping(path = "/product/bestname", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a product's best display name")
    public String getBestName(@RequestParam final Long gtin) throws ResourceNotFoundException {
        List<String> names = new ArrayList<>(repository.getById(gtin).getOfferNames());
        return productNameSelectionService.selectBestNameIndustrial(names).orElse(null);
    }

    @GetMapping(path = "/product/random", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get random products from a vertical")
    public List<Product> getRandom(
            @RequestParam(defaultValue = "5") final int number,
            @RequestParam(defaultValue = "tv") final String vertical) {
        return repository.getRandomProducts(vertical, number);
    }
}
