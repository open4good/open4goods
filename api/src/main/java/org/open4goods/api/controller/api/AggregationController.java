package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin endpoints for triggering product aggregation (sanitize) runs.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Aggregation", description = "Trigger on-demand product aggregation (sanitization) runs — "
        + "normalise, merge and re-index product data for one or all verticals.")
public class AggregationController {

    private final VerticalsConfigService verticalConfigService;
    private final AggregationFacadeService aggregationFacadeService;
    private final ProductRepository repository;

    public AggregationController(AggregationFacadeService aggregationFacadeService,
                                 VerticalsConfigService verticalsConfigService,
                                 ProductRepository repository) {
        this.verticalConfigService = verticalsConfigService;
        this.aggregationFacadeService = aggregationFacadeService;
        this.repository = repository;
    }

    @PostMapping("/aggregate/verticals")
    @Operation(
            summary = "Aggregate all verticals",
            description = "Runs the sanitization pipeline for every active vertical in sequence. "
                    + "Sanitization normalises and merges product attributes, recomputes derived fields "
                    + "and re-indexes products in Elasticsearch. "
                    + "Use /aggregate/verticals/{vertical} to restrict the run to a single category.")
    @ApiResponse(responseCode = "200", description = "Aggregation completed for all verticals")
    public void aggregateAllVerticals() throws InvalidParameterException, IOException {
        aggregationFacadeService.sanitizeAllVerticals();
    }

    @PostMapping("/aggregate/verticals/{vertical}")
    @Operation(
            summary = "Aggregate a specific vertical",
            description = "Runs the sanitization pipeline for the named vertical only. "
                    + "Sanitization normalises and merges product attributes, recomputes derived fields "
                    + "and re-indexes the affected products in Elasticsearch.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aggregation completed for the vertical"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public void aggregateVertical(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop', 'air-conditioner')", required = true)
            @PathVariable String vertical) throws InvalidParameterException, IOException {
        aggregationFacadeService.sanitizeVertical(verticalConfigService.getConfigById(vertical));
    }

    @PostMapping("/aggregate/products")
    @Operation(
            summary = "Aggregate all products",
            description = "Runs the sanitization pipeline across every product in the index regardless of vertical. "
                    + "This is a heavier operation than /aggregate/verticals because it also processes products "
                    + "that are not yet assigned to any vertical.")
    @ApiResponse(responseCode = "200", description = "Aggregation completed for all products")
    public void aggregateAllProducts() throws InvalidParameterException, IOException {
        aggregationFacadeService.sanitizeAll();
    }

    @PostMapping("/aggregate/gtin/{gtin}")
    @Operation(
            summary = "Aggregate a single product by GTIN",
            description = "Fetches the product with the given GTIN from Elasticsearch, runs the full sanitization "
                    + "pipeline on it and persists the updated document. "
                    + "Useful for re-processing a single product after a configuration change without triggering "
                    + "a full vertical or global run.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product aggregated and re-indexed successfully"),
            @ApiResponse(responseCode = "404", description = "No product with the given GTIN found in the index")
    })
    public void aggregateProduct(
            @Parameter(description = "GTIN (EAN-13) of the product to aggregate", required = true)
            @PathVariable Long gtin)
            throws InvalidParameterException, IOException, ResourceNotFoundException, AggregationSkipException {
        aggregationFacadeService.sanitizeAndSave(repository.getById(gtin));
    }
}
