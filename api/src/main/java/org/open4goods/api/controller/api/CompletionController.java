package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.completion.AmazonCompletionService;
import org.open4goods.api.services.completion.EprelCompletionService;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin endpoints for triggering on-demand data completion runs (resource, Amazon, Icecat, EPREL).
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class CompletionController {

    private final VerticalsConfigService verticalConfigService;
    private final ResourceCompletionService resourceCompletionService;
    private final AmazonCompletionService amazonCompletionService;
    private final IcecatCompletionService iceCatService;
    private final EprelCompletionService eprelCompletionService;
    private final ProductRepository repository;

    public CompletionController(VerticalsConfigService verticalsConfigService,
            ResourceCompletionService resourceCompletionService,
            AmazonCompletionService amazonCompletionService,
            IcecatCompletionService iceCatService,
            EprelCompletionService eprelCompletionService,
            ProductRepository repository) {
        this.verticalConfigService = verticalsConfigService;
        this.resourceCompletionService = resourceCompletionService;
        this.amazonCompletionService = amazonCompletionService;
        this.iceCatService = iceCatService;
        this.eprelCompletionService = eprelCompletionService;
        this.repository = repository;
    }

    ///////////////////////////////////
    // Resource completion
    ///////////////////////////////////

    @PostMapping("/completion/resources")
    @Operation(summary = "Launch resource completion on all verticals")
    public void resourceCompletionAll() throws InvalidParameterException, IOException {
        resourceCompletionService.completeAll(false);
    }

    @PostMapping("/completion/resources/{vertical}")
    @Operation(summary = "Launch resource completion on the specified vertical")
    public void resourceCompletionVertical(@PathVariable @NotBlank final String vertical)
            throws InvalidParameterException, IOException {
        resourceCompletionService.complete(verticalConfigService.getConfigById(vertical), false);
    }

    @PostMapping("/completion/resources/gtin/{gtin}")
    @Operation(summary = "Launch resource completion on a specific product")
    public void resourceCompletionProduct(@PathVariable final Long gtin) {
        Product data = loadProduct(gtin);
        resourceCompletionService.completeAndIndexProduct(
                verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
    }

    ///////////////////////////////////
    // Amazon completion
    ///////////////////////////////////

    @PostMapping("/completion/amazon")
    @Operation(summary = "Launch Amazon completion on all verticals")
    public void amazonCompletionAll() throws InvalidParameterException, IOException {
        amazonCompletionService.completeAll(amazonCompletionService.getAmazonConfig().getMaxCallsPerBatch(), false);
    }

    @PostMapping("/completion/amazon/{vertical}")
    @Operation(summary = "Launch Amazon completion on the specified vertical")
    public void amazonCompletionVertical(@PathVariable @NotBlank final String vertical,
            @RequestParam(required = false) Integer max)
            throws InvalidParameterException, IOException {
        int limit = max == null ? amazonCompletionService.getAmazonConfig().getMaxCallsPerBatch() : max;
        amazonCompletionService.complete(verticalConfigService.getConfigById(vertical), limit, false);
    }

    @PostMapping("/completion/amazon/gtin/{gtin}")
    @Operation(summary = "Launch Amazon completion on a specific product")
    public void amazonCompletionProduct(@PathVariable final Long gtin) {
        Product data = loadProduct(gtin);
        amazonCompletionService.completeAndIndexProduct(
                verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
    }

    ///////////////////////////////////
    // Icecat completion
    ///////////////////////////////////

    @PostMapping("/completion/icecat")
    @Operation(summary = "Launch Icecat completion on all verticals")
    public void icecatCompletionAll() throws InvalidParameterException, IOException {
        iceCatService.completeAll(true);
    }

    @PostMapping("/completion/icecat/{vertical}")
    @Operation(summary = "Launch Icecat completion on the specified vertical")
    public void icecatCompletionVertical(@PathVariable @NotBlank final String vertical,
            @RequestParam Integer max)
            throws InvalidParameterException, IOException {
        iceCatService.complete(verticalConfigService.getConfigById(vertical), max, true);
    }

    @PostMapping("/completion/icecat/gtin/{gtin}")
    @Operation(summary = "Launch Icecat completion on a specific product")
    public void icecatCompletionProduct(@PathVariable final Long gtin) {
        Product data = loadProduct(gtin);
        iceCatService.completeAndIndexProduct(
                verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
    }

    ///////////////////////////////////
    // EPREL completion
    ///////////////////////////////////

    @PostMapping("/completion/eprel")
    @Operation(summary = "Launch EPREL completion on all verticals")
    public void eprelCompletionAll() throws InvalidParameterException, IOException {
        eprelCompletionService.completeAll(true);
    }

    @PostMapping("/completion/eprel/{vertical}")
    @Operation(summary = "Launch EPREL completion on the specified vertical")
    public void eprelCompletionVertical(@PathVariable @NotBlank final String vertical,
            @RequestParam Integer max) throws InvalidParameterException, IOException {
        eprelCompletionService.complete(verticalConfigService.getConfigById(vertical), max, true);
    }

    @PostMapping("/completion/eprel/gtin/{gtin}")
    @Operation(summary = "Launch EPREL completion on a specific product")
    public void eprelCompletionProduct(@PathVariable final Long gtin) {
        Product data = loadProduct(gtin);
        eprelCompletionService.completeAndIndexProduct(
                verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
    }

    private Product loadProduct(Long gtin) {
        try {
            return repository.getById(gtin);
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
