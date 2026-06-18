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

/**
 * Admin endpoints for triggering product aggregation (sanitize) runs.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
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
    @Operation(summary = "Launch aggregation of all verticals")
    public void aggregateAllVerticals() throws InvalidParameterException, IOException {
        aggregationFacadeService.sanitizeAllVerticals();
    }

    @PostMapping("/aggregate/verticals/{vertical}")
    @Operation(summary = "Launch aggregation of a specific vertical")
    public void aggregateVertical(@PathVariable String vertical) throws InvalidParameterException, IOException {
        aggregationFacadeService.sanitizeVertical(verticalConfigService.getConfigById(vertical));
    }

    @PostMapping("/aggregate/products")
    @Operation(summary = "Launch aggregation of all products")
    public void aggregateAllProducts() throws InvalidParameterException, IOException {
        aggregationFacadeService.sanitizeAll();
    }

    @PostMapping("/aggregate/gtin/{gtin}")
    @Operation(summary = "Launch aggregation of a single product by GTIN")
    public void aggregateProduct(@PathVariable Long gtin)
            throws InvalidParameterException, IOException, ResourceNotFoundException, AggregationSkipException {
        aggregationFacadeService.sanitizeAndSave(repository.getById(gtin));
    }
}
