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

/**
 * Admin endpoints for EPREL catalogue browsing and re-indexation.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class EprelController {

    private final EprelSearchService eprelSearchService;
    private final EprelCatalogueService eprelCatalogueService;

    public EprelController(EprelSearchService eprelSearchService, EprelCatalogueService eprelCatalogueService) {
        this.eprelSearchService = eprelSearchService;
        this.eprelCatalogueService = eprelCatalogueService;
    }

    @PostMapping("/eprel/index")
    @Operation(summary = "Launch the EPREL catalogue re-indexation")
    public void eprelIndex() throws IOException {
        eprelCatalogueService.refreshCatalogue();
    }

    @GetMapping("/eprel/search")
    @Operation(summary = "Search EPREL by GTIN or model")
    public List<EprelProduct> eprelSearch(@RequestParam String gtin, @RequestParam String model) {
        return eprelSearchService.search(gtin, model);
    }

    @GetMapping("/eprel")
    @Operation(summary = "Get the full EPREL catalogue")
    public List<EprelProductGroup> eprelCatalogue() {
        return eprelCatalogueService.getCatalog();
    }
}
