package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.List;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.services.eprelservice.client.EprelProductGroup;
import org.open4goods.services.eprelservice.service.EprelCatalogueService;
import org.open4goods.services.eprelservice.service.EprelSearchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin endpoints for EPREL catalogue browsing and re-indexation.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "EPREL", description = "Browse and re-index the European Product Registry for Energy Labelling (EPREL) catalogue. "
        + "EPREL data enriches products with official energy labels and efficiency ratings.")
public class EprelController {

    private final EprelSearchService eprelSearchService;
    private final EprelCatalogueService eprelCatalogueService;

    public EprelController(EprelSearchService eprelSearchService, EprelCatalogueService eprelCatalogueService) {
        this.eprelSearchService = eprelSearchService;
        this.eprelCatalogueService = eprelCatalogueService;
    }

    @PostMapping("/eprel/index")
    @Operation(
            summary = "Re-index the EPREL catalogue",
            description = "Downloads the latest product groups from the European EPREL API and refreshes the local "
                    + "Elasticsearch EPREL index. This is a potentially long operation depending on the number of "
                    + "registered product groups. Run after EPREL publishes a new data release.")
    @ApiResponse(responseCode = "200", description = "EPREL catalogue refresh started")
    public void eprelIndex() throws IOException {
        eprelCatalogueService.refreshCatalogue();
    }

    @GetMapping("/eprel/search")
    @Operation(
            summary = "Search EPREL by GTIN or model number",
            description = "Queries the local EPREL Elasticsearch index for products matching the supplied GTIN and/or "
                    + "model string. Both parameters are used together to disambiguate products from the same brand "
                    + "that share similar model numbers. "
                    + "Returns a list of matching EprelProduct records with energy label details.")
    @ApiResponse(responseCode = "200", description = "List of EPREL product records matching the query")
    public List<EprelProduct> eprelSearch(
            @Parameter(description = "GTIN (EAN-13) to match against the EPREL catalogue", required = true)
            @RequestParam String gtin,
            @Parameter(description = "Model number or identifier string to match against the EPREL catalogue", required = true)
            @RequestParam String model) {
        return eprelSearchService.search(gtin, model);
    }

    @GetMapping("/eprel")
    @Operation(
            summary = "Get the full EPREL catalogue",
            description = "Returns every product group currently stored in the local EPREL index. "
                    + "Each EprelProductGroup corresponds to one product family registered in the EU EPREL system. "
                    + "Use /eprel/search for targeted lookups; this endpoint is intended for bulk inspection only.")
    @ApiResponse(responseCode = "200", description = "Complete list of EPREL product groups")
    public List<EprelProductGroup> eprelCatalogue() {
        return eprelCatalogueService.getCatalog();
    }
}
