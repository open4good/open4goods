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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin endpoints for brand resolution, statistics and AI-generated suggestions.
 */
@RestController
@Tag(name = "Brands", description = "Brand resolution, missing-company statistics, and AI-generated brand suggestions.")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping("/brands/resolve")
    @PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
    @Operation(
            summary = "Resolve a brand by raw name",
            description = "Looks up the internal Brand record that matches the supplied raw name string. "
                    + "Resolution uses fuzzy normalization: casing, accents and common abbreviations are handled. "
                    + "Returns an UNKNOWN placeholder when no match is found rather than a 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand resolved — may be an UNKNOWN placeholder when no match is found"),
            @ApiResponse(responseCode = "403", description = "Insufficient authority")
    })
    public Brand resolve(
            @Parameter(description = "Raw brand name as it appears in a feed or product title", required = true)
            @RequestParam String name) {
        return brandService.resolve(name);
    }

    @GetMapping("/brands/stats/companies/missing")
    @PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
    @Operation(
            summary = "Unresolved brand names sorted by miss count",
            description = "Returns the list of raw brand strings that were looked up but did not match any known brand, "
                    + "ordered by the number of times each miss was encountered (highest first). "
                    + "Use this to prioritise which brands to add to the brand database.")
    @ApiResponse(responseCode = "200", description = "Ordered list of unresolved brand name strings")
    public List<String> statsMissingCompanies() {
        return brandService.getMissCounter().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    @GetMapping("/brands/stats/companies/missing/counts")
    @PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
    @Operation(
            summary = "Unresolved brand names with occurrence counts",
            description = "Returns a map of unresolved raw brand name to miss count. "
                    + "Same data as /brands/stats/companies/missing but keyed for programmatic consumption "
                    + "rather than sorted display.")
    @ApiResponse(responseCode = "200", description = "Map of raw brand name to miss count")
    public Map<String, Long> statsMissingCompanyCounts() {
        return brandService.getMissCounter();
    }

    @GetMapping("/brands/suggestions")
    @PreAuthorize("hasAuthority('" + RolesConstants.ROLE_XWIKI_ALL + "')")
    @Operation(
            summary = "AI-generated brand creation suggestions",
            description = "Inspects the current miss counter and other heuristics to propose new Brand records "
                    + "that could be created to improve resolution coverage. "
                    + "Each suggestion includes a confidence score and the raw strings that prompted it.")
    @ApiResponse(responseCode = "200", description = "List of brand creation suggestions")
    public List<BrandSuggestion> suggestions() {
        return brandService.generateSuggestions();
    }
}
