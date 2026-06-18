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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin endpoints for inspecting and retrieving individual products.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Products", description = "Inspect individual product documents by GTIN or retrieve random samples.")
public class ProductAdminController {

    private final ProductRepository repository;
    private final ProductNameSelectionService productNameSelectionService;

    public ProductAdminController(ProductRepository repository,
                                  ProductNameSelectionService productNameSelectionService) {
        this.repository = repository;
        this.productNameSelectionService = productNameSelectionService;
    }

    @GetMapping(path = "/product", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get a product by GTIN",
            description = "Fetches the full aggregated product document from Elasticsearch for the given GTIN (EAN-13). "
                    + "The document includes all offer data, aggregated attributes, scores and AI review metadata.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product document found and returned"),
            @ApiResponse(responseCode = "404", description = "No product with the given GTIN exists in the index")
    })
    public Product get(
            @Parameter(description = "GTIN (EAN-13) of the product to retrieve", required = true)
            @RequestParam final Long gtin) throws ResourceNotFoundException {
        return repository.getById(gtin);
    }

    @GetMapping(path = "/product/bestname", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get the best display name for a product",
            description = "Collects all offer title strings available for the given GTIN and runs them through the "
                    + "ProductNameSelectionService industrial heuristic to pick the most human-readable candidate. "
                    + "Returns null when no name passes the quality threshold.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Best name selected, or null when none passes the threshold"),
            @ApiResponse(responseCode = "404", description = "No product with the given GTIN exists in the index")
    })
    public String getBestName(
            @Parameter(description = "GTIN (EAN-13) of the product", required = true)
            @RequestParam final Long gtin) throws ResourceNotFoundException {
        List<String> names = new ArrayList<>(repository.getById(gtin).getOfferNames());
        return productNameSelectionService.selectBestNameIndustrial(names).orElse(null);
    }

    @GetMapping(path = "/product/random", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get random products from a vertical",
            description = "Returns a random sample of product documents from the given vertical. "
                    + "Useful for manual QA, UI testing and checking the output of batch pipelines "
                    + "without having to know specific GTINs.")
    @ApiResponse(responseCode = "200", description = "List of randomly sampled product documents")
    public List<Product> getRandom(
            @Parameter(description = "Number of products to return", required = false)
            @RequestParam(defaultValue = "5") final int number,
            @Parameter(description = "Vertical identifier to sample from (e.g. 'tv', 'laptop')", required = false)
            @RequestParam(defaultValue = "tv") final String vertical) {
        return repository.getRandomProducts(vertical, number);
    }
}
