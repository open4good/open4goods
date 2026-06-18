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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin endpoints for triggering on-demand data completion runs (resource, Amazon, Icecat, EPREL).
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Completion", description = "Trigger on-demand data completion runs from external sources: "
        + "resource/image fetching, Amazon product data, Icecat specs and EPREL energy labels. "
        + "Each source has endpoints scoped to all verticals, a single vertical, or a single product by GTIN.")
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
    @Operation(
            summary = "Run resource completion on all verticals",
            description = "Fetches missing resources (product images, PDF datasheets, cached web pages) "
                    + "for every product in every active vertical. "
                    + "Use /completion/resources/{vertical} for a scoped run.")
    @ApiResponse(responseCode = "200", description = "Resource completion run started for all verticals")
    public void resourceCompletionAll() throws InvalidParameterException, IOException {
        resourceCompletionService.completeAll(false);
    }

    @PostMapping("/completion/resources/{vertical}")
    @Operation(
            summary = "Run resource completion on a specific vertical",
            description = "Fetches missing resources (product images, PDF datasheets, cached web pages) "
                    + "for every product in the named vertical. "
                    + "Use /completion/resources to run across all verticals.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource completion run started for the vertical"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public void resourceCompletionVertical(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable @NotBlank final String vertical)
            throws InvalidParameterException, IOException {
        resourceCompletionService.complete(verticalConfigService.getConfigById(vertical), false);
    }

    @PostMapping("/completion/resources/gtin/{gtin}")
    @Operation(
            summary = "Run resource completion on a single product",
            description = "Fetches missing resources for the product with the given GTIN and re-indexes it. "
                    + "The product's vertical is resolved automatically from its existing document.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource completion run for the product"),
            @ApiResponse(responseCode = "404", description = "No product with the given GTIN found in the index")
    })
    public void resourceCompletionProduct(
            @Parameter(description = "GTIN (EAN-13) of the product to process", required = true)
            @PathVariable final Long gtin) {
        Product data = loadProduct(gtin);
        resourceCompletionService.completeAndIndexProduct(
                verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
    }

    ///////////////////////////////////
    // Amazon completion
    ///////////////////////////////////

    @PostMapping("/completion/amazon")
    @Operation(
            summary = "Run Amazon completion on all verticals",
            description = "Enriches products across all active verticals with data fetched from the Amazon Product "
                    + "Advertising API (title, description, specs, images). The batch size is capped by the "
                    + "configured maxCallsPerBatch limit to stay within API quotas.")
    @ApiResponse(responseCode = "200", description = "Amazon completion run started for all verticals")
    public void amazonCompletionAll() throws InvalidParameterException, IOException {
        amazonCompletionService.completeAll(amazonCompletionService.getAmazonConfig().getMaxCallsPerBatch(), false);
    }

    @PostMapping("/completion/amazon/{vertical}")
    @Operation(
            summary = "Run Amazon completion on a specific vertical",
            description = "Enriches products in the named vertical with Amazon Product Advertising API data. "
                    + "An optional max parameter overrides the default per-batch API call limit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Amazon completion run started for the vertical"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public void amazonCompletionVertical(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable @NotBlank final String vertical,
            @Parameter(description = "Maximum number of Amazon API calls to make; defaults to the configured maxCallsPerBatch")
            @RequestParam(required = false) Integer max)
            throws InvalidParameterException, IOException {
        int limit = max == null ? amazonCompletionService.getAmazonConfig().getMaxCallsPerBatch() : max;
        amazonCompletionService.complete(verticalConfigService.getConfigById(vertical), limit, false);
    }

    @PostMapping("/completion/amazon/gtin/{gtin}")
    @Operation(
            summary = "Run Amazon completion on a single product",
            description = "Queries the Amazon Product Advertising API for the product with the given GTIN "
                    + "and re-indexes it with the enriched data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Amazon completion run for the product"),
            @ApiResponse(responseCode = "404", description = "No product with the given GTIN found in the index")
    })
    public void amazonCompletionProduct(
            @Parameter(description = "GTIN (EAN-13) of the product to enrich via Amazon", required = true)
            @PathVariable final Long gtin) {
        Product data = loadProduct(gtin);
        amazonCompletionService.completeAndIndexProduct(
                verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
    }

    ///////////////////////////////////
    // Icecat completion
    ///////////////////////////////////

    @PostMapping("/completion/icecat")
    @Operation(
            summary = "Run Icecat completion on all verticals",
            description = "Enriches products across all active verticals with structured specification data "
                    + "from the Icecat product catalogue (dimensions, weight, technical attributes). "
                    + "Runs with force=true to re-fetch even already-enriched products.")
    @ApiResponse(responseCode = "200", description = "Icecat completion run started for all verticals")
    public void icecatCompletionAll() throws InvalidParameterException, IOException {
        iceCatService.completeAll(true);
    }

    @PostMapping("/completion/icecat/{vertical}")
    @Operation(
            summary = "Run Icecat completion on a specific vertical",
            description = "Enriches products in the named vertical with Icecat specification data. "
                    + "The max parameter caps the number of products processed in this run.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Icecat completion run started for the vertical"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public void icecatCompletionVertical(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable @NotBlank final String vertical,
            @Parameter(description = "Maximum number of products to process in this run", required = true)
            @RequestParam Integer max)
            throws InvalidParameterException, IOException {
        iceCatService.complete(verticalConfigService.getConfigById(vertical), max, true);
    }

    @PostMapping("/completion/icecat/gtin/{gtin}")
    @Operation(
            summary = "Run Icecat completion on a single product",
            description = "Fetches Icecat specification data for the product with the given GTIN and re-indexes it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Icecat completion run for the product"),
            @ApiResponse(responseCode = "404", description = "No product with the given GTIN found in the index")
    })
    public void icecatCompletionProduct(
            @Parameter(description = "GTIN (EAN-13) of the product to enrich via Icecat", required = true)
            @PathVariable final Long gtin) {
        Product data = loadProduct(gtin);
        iceCatService.completeAndIndexProduct(
                verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
    }

    ///////////////////////////////////
    // EPREL completion
    ///////////////////////////////////

    @PostMapping("/completion/eprel")
    @Operation(
            summary = "Run EPREL completion on all verticals",
            description = "Matches products across all active verticals against the EPREL catalogue and enriches "
                    + "them with EU energy label data (energy class, efficiency ratings). "
                    + "Runs with force=true to re-process already-enriched products.")
    @ApiResponse(responseCode = "200", description = "EPREL completion run started for all verticals")
    public void eprelCompletionAll() throws InvalidParameterException, IOException {
        eprelCompletionService.completeAll(true);
    }

    @PostMapping("/completion/eprel/{vertical}")
    @Operation(
            summary = "Run EPREL completion on a specific vertical",
            description = "Matches products in the named vertical against the EPREL catalogue and enriches them "
                    + "with EU energy label data. The max parameter caps the number of products processed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "EPREL completion run started for the vertical"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public void eprelCompletionVertical(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'washing-machine')", required = true)
            @PathVariable @NotBlank final String vertical,
            @Parameter(description = "Maximum number of products to process in this run", required = true)
            @RequestParam Integer max) throws InvalidParameterException, IOException {
        eprelCompletionService.complete(verticalConfigService.getConfigById(vertical), max, true);
    }

    @PostMapping("/completion/eprel/gtin/{gtin}")
    @Operation(
            summary = "Run EPREL completion on a single product",
            description = "Matches the product with the given GTIN against the EPREL catalogue and enriches it "
                    + "with EU energy label data, then re-indexes it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "EPREL completion run for the product"),
            @ApiResponse(responseCode = "404", description = "No product with the given GTIN found in the index")
    })
    public void eprelCompletionProduct(
            @Parameter(description = "GTIN (EAN-13) of the product to enrich via EPREL", required = true)
            @PathVariable final Long gtin) {
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
