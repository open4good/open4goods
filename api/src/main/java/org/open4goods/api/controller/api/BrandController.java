package org.open4goods.api.controller.api;

import java.util.List;
import java.util.Map;

import org.open4goods.brand.model.Brand;
import org.open4goods.brand.model.BrandSuggestion;
import org.open4goods.brand.service.BrandService;
import org.open4goods.model.RolesConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoints for brand resolution, statistics and AI-generated suggestions.
 */
@RestController
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping("/brands/resolve")
    @PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
    public Brand resolve(@RequestParam String name) {
        return brandService.resolve(name);
    }

    @GetMapping("/brands/stats/companies/missing")
    @PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
    public List<String> statsMissingCompanies() {
        return brandService.getMissCounter().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    @GetMapping("/brands/stats/companies/missing/counts")
    @PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
    public Map<String, Long> statsMissingCompanyCounts() {
        return brandService.getMissCounter();
    }

    @GetMapping("/brands/suggestions")
    @PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
    public List<BrandSuggestion> suggestions() {
        return brandService.generateSuggestions();
    }
}
