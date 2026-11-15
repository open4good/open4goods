package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.vertical.VerticalConfig;
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
            description = "Runs /aggregate/verticals/{vertical} for every active vertical, one after the other. "
                    + "Same behaviour and same precautions as the single-vertical run. A vertical that fails is "
                    + "logged (ERROR) and the next ones still run.")
    @ApiResponse(responseCode = "200", description = "Run finished; products of the verticals that succeeded are queued for indexation (a failed vertical is only logged)")
    public void aggregateAllVerticals() throws InvalidParameterException, IOException {
        aggregationFacadeService.sanitizeAllVerticals();
    }

    @PostMapping("/aggregate/verticals/{vertical}")
    @Operation(
            summary = "Aggregate a specific vertical",
            description = "Reloads the products whose datasourceCategories match the vertical's matchingCategories, "
                    + "or whose vertical is already this one, and runs the standard aggregation chain on them "
                    + "(identity, taxonomy, attributes, usage cost, names, price, media): the taxonomy re-assigns the "
                    + "vertical from the categories. No completion and no scoring: scores are rebuilt by the 13:00 batch or by "
                    + "POST /batch/{vertical}. Products are queued for full indexation by pages of 500; each page first "
                    + "gets the newest realtime offers (price, offersCount, lastChange) from the index and the "
                    + "indexation queues, so offers updated during the run are not overwritten. Returns once the "
                    + "products are queued, not once they are indexed. Run it outside the 13:00 batch.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products aggregated and queued for indexation"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public void aggregateVertical(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop', 'air-conditioner')", required = true)
            @PathVariable String vertical) throws InvalidParameterException, IOException, ResourceNotFoundException {
        aggregationFacadeService.sanitizeVertical(knownVertical(vertical));
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

    /**
     * @param vertical vertical identifier from the path
     * @return the configuration of that vertical
     * @throws ResourceNotFoundException when no vertical has this identifier (404, GOU-2280)
     */
    private VerticalConfig knownVertical(String vertical) throws ResourceNotFoundException {
        VerticalConfig config = verticalConfigService.getConfigById(vertical);
        if (config == null) {
            throw new ResourceNotFoundException("Unknown vertical: " + vertical);
        }
        return config;
    }
}
