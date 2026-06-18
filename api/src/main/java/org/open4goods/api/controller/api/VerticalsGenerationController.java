

package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.List;

import org.open4goods.api.dto.AttributeSuggestionDto;
import org.open4goods.api.dto.CategorySuggestionsDto;
import org.open4goods.api.dto.DatasourceCoverageDto;
import org.open4goods.api.dto.LeakageWarningDto;
import org.open4goods.api.dto.SignificantCategoryDto;
import org.open4goods.api.dto.UnmappedCategoryDto;
import org.open4goods.api.model.VerticalAttributesStats;
import org.open4goods.api.services.VerticalsGenerationService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Read-only vertical generation and suggestion endpoints.
 * All write operations (YAML mutations) are performed by agents via their file tools,
 * using the data returned by these endpoints.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Verticals", description = "Generate category mappings, attribute suggestions, scoring config and impact-score "
        + "prompts for verticals. All endpoints are read-only; YAML writes are performed by AI agents using their file tools.")
public class VerticalsGenerationController {

    private final VerticalsGenerationService verticalsGenService;
    private final VerticalsConfigService verticalsConfigService;

    public VerticalsGenerationController(VerticalsGenerationService verticalsGenService,
            VerticalsConfigService verticalsConfigService) {
        this.verticalsGenService = verticalsGenService;
        this.verticalsConfigService = verticalsConfigService;
    }

    // -----------------------------------------------------------------------
    // Legacy read-only fragments (YAML strings)
    // -----------------------------------------------------------------------

    @GetMapping(path = "/verticals/{vertical}/attributes")
    @Operation(
            summary = "Attribute coverage stats for a vertical",
            description = "Returns a VerticalAttributesStats object showing, for each product attribute in the vertical, "
                    + "how many products carry a value and the top values observed. "
                    + "Cached for one hour.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attribute coverage statistics for the vertical"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
    public VerticalAttributesStats generateAttributesCoverage(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical)
            throws ResourceNotFoundException, IOException {
        return verticalsGenService.attributesStats(vertical);
    }

    @GetMapping(path = "/verticals/{vertical}/categories")
    @Operation(
            summary = "Suggested matchingCategories YAML fragment for a vertical",
            description = "Generates a YAML snippet that can be pasted into the vertical's matchingCategories block. "
                    + "Each entry lists a datasource category string with its observed offer count. "
                    + "Only categories with at least minOffersCount offers are included. Cached for one hour.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "YAML fragment string listing candidate matchingCategories entries"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
    public String generateCategoryMappingsFragment(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical,
            @Parameter(description = "Minimum number of offers a datasource category must have to be included")
            @RequestParam(defaultValue = "5") Integer minOffersCount)
            throws ResourceNotFoundException, IOException {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        return verticalsGenService.generateMapping(vc, minOffersCount);
    }

    @GetMapping(path = "/verticals/{vertical}/ecoscore")
    @Operation(
        summary = "Run the ecoscore LLM prompt and return YAML content for impactscores/{vertical}.yml",
        description = "Cached for 1h per vertical. Pass force=true to evict the entry and re-run the LLM "
                + "(useful when iterating during Phase E of the category playbook)."
    )
    @Caching(
        cacheable = @Cacheable(
            cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME,
            key = "'ecoscore-' + #vertical",
            condition = "!#force"
        ),
        evict = @CacheEvict(
            cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME,
            key = "'ecoscore-' + #vertical",
            condition = "#force",
            beforeInvocation = true
        )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "YAML content for impactscores/{vertical}.yml"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public String generateEcoscoreMappings(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical,
            @Parameter(description = "Pass true to evict the cache entry and re-run the LLM call")
            @RequestParam(name = "force", defaultValue = "false") boolean force)
            throws ResourceNotFoundException, IOException {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        return verticalsGenService.generateEcoscoreYamlConfig(vc);
    }

    @GetMapping(path = "/verticals/{vertical}/impactscore-criterias")
    @Operation(
            summary = "Available impact-score criteria YAML fragment for a vertical",
            description = "Lists every product attribute that is sufficiently covered in the vertical's product set "
                    + "to be used as an impact-score criterion. Returns a YAML fragment that can be used as input "
                    + "when configuring the impactScore block. Cached for one hour.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "YAML fragment listing available impact-score criteria"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
    public String generateImpactScoreCriterias(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical,
            @Parameter(description = "Minimum percentage of products in the vertical that must carry a value for an attribute to be included")
            @RequestParam(defaultValue = "10") Integer minCoveragePercent)
            throws ResourceNotFoundException, IOException {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        return verticalsGenService.generateAvailableImpactScoreCriteriasFragment(vc, minCoveragePercent);
    }

    @GetMapping(path = "/verticals/{vertical}/nudgetool/compute")
    @Operation(
            summary = "Compute nudge tool score thresholds for a vertical",
            description = "Analyses the current product distribution in the vertical and computes score thresholds "
                    + "and impact subsets for the nudge tool, returning the updated VerticalConfig. "
                    + "No file is written; the caller persists the result to YAML.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "VerticalConfig with computed nudge tool thresholds"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public VerticalConfig computeNudgeTool(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical)
            throws ResourceNotFoundException, IOException {
        return verticalsGenService.computeNudgeToolThresholds(vertical);
    }

    // -----------------------------------------------------------------------
    // Structured suggestion endpoints (JSON, no disk writes)
    // -----------------------------------------------------------------------

    @GetMapping(path = "/verticals/{vertical}/suggestions/categories")
    @Operation(
            summary = "Category suggestions for a vertical",
            description = "Returns per-datasource category lists derived from indexed products. "
                    + "Use this to assemble the matchingCategories block of a VerticalConfig YAML "
                    + "without server-side file mutations."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category suggestions grouped by datasource"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public CategorySuggestionsDto suggestCategories(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical,
            @Parameter(description = "Minimum number of offers a datasource category must have to be included")
            @RequestParam(defaultValue = "5") int minOffersCount) throws ResourceNotFoundException {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResourceNotFoundException("Vertical not found: " + vertical);
        }
        return verticalsGenService.suggestCategories(vc, minOffersCount);
    }

    @GetMapping(path = "/verticals/{vertical}/datasources/stats/coverage")
    @Operation(
            summary = "Per-datasource category coverage for a vertical",
            description = "Returns how many products each datasource contributes to the vertical above the minVolume threshold, "
                    + "and which datasource categories are already mapped vs. unmapped.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Per-datasource coverage statistics"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public List<DatasourceCoverageDto> datasourceCoverage(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical,
            @Parameter(description = "Minimum offer volume threshold for a datasource category to be included")
            @RequestParam(defaultValue = "50") int minVolume) {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vertical not found: " + vertical);
        }
        return verticalsGenService.datasourceCoverage(vc, minVolume);
    }

    @GetMapping(path = "/verticals/{vertical}/datasources/stats/unmapped")
    @Operation(
            summary = "Unmapped datasource category strings observed in a vertical",
            description = "Lists datasource category strings that appear in the vertical's products but are not "
                    + "yet mapped in matchingCategories, sorted by volume. "
                    + "Use this to discover which categories should be added to the vertical YAML.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of unmapped datasource category strings with volumes"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public List<UnmappedCategoryDto> unmappedCategories(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical,
            @Parameter(description = "Minimum offer volume threshold to include a category string")
            @RequestParam(defaultValue = "50") int minVolume,
            @Parameter(description = "Maximum number of unmapped category strings to return")
            @RequestParam(defaultValue = "200") int limit) {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vertical not found: " + vertical);
        }
        return verticalsGenService.unmappedCategories(vc, minVolume, limit);
    }

    @GetMapping(path = "/verticals/{vertical}/datasources/stats/leakage")
    @Operation(
            summary = "Cross-vertical datasource category leakage warnings",
            description = "Detects datasource categories whose products are split across multiple verticals above "
                    + "the leakageThreshold ratio. Leaking categories can cause products to appear in the wrong vertical "
                    + "and should be reviewed or de-duplicated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of leakage warnings for datasource categories shared across verticals"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public List<LeakageWarningDto> categoryLeakage(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical,
            @Parameter(description = "Minimum offer volume for a category to be included in leakage analysis")
            @RequestParam(defaultValue = "50") int minVolume,
            @Parameter(description = "Minimum fraction of a category's products that must appear in another vertical to flag a leakage warning")
            @RequestParam(defaultValue = "0.2") double leakageThreshold) {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vertical not found: " + vertical);
        }
        return verticalsGenService.categoryLeakage(vertical, minVolume, leakageThreshold);
    }

    @GetMapping(path = "/verticals/{vertical}/datasources/stats/significant")
    @Operation(
            summary = "Most significant datasource category strings for a vertical",
            description = "Returns the datasource category strings with the highest offer volume in the vertical, "
                    + "regardless of whether they are already mapped. Useful for understanding the dominant "
                    + "categories in the current product set.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of significant datasource category strings sorted by volume"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public List<SignificantCategoryDto> significantCategories(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical,
            @Parameter(description = "Minimum offer volume for a category string to be included")
            @RequestParam(defaultValue = "50") int minVolume,
            @Parameter(description = "Maximum number of entries to return")
            @RequestParam(defaultValue = "50") int limit) {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vertical not found: " + vertical);
        }
        return verticalsGenService.significantCategories(vertical, minVolume, limit);
    }

    @GetMapping(path = "/verticals/{vertical}/suggestions/attributes")
    @Operation(
            summary = "Attribute suggestions for a vertical",
            description = "Returns attribute candidates with coverage stats and a ymlExists flag. "
                    + "Use this to assemble the attributesConfig.configs block of a VerticalConfig YAML "
                    + "without server-side file mutations."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of attribute suggestions with coverage and YAML status"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public List<AttributeSuggestionDto> suggestAttributes(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical,
            @Parameter(description = "Minimum percentage of products that must carry this attribute for it to be suggested")
            @RequestParam(defaultValue = "10") int minCoverage,
            @Parameter(description = "Optional substring filter on attribute key name")
            @RequestParam(defaultValue = "") String containing) throws ResourceNotFoundException {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResourceNotFoundException("Vertical not found: " + vertical);
        }
        return verticalsGenService.suggestAttributes(vc, minCoverage, containing);
    }

    // -----------------------------------------------------------------------
    // Impact score generation helpers (compose only, no disk writes)
    // -----------------------------------------------------------------------

    @GetMapping(path = "/verticals/{vertical}/suggestions/impact-prompt")
    @Operation(
            summary = "Resolved impact-score generation prompt (dry run)",
            description = "Returns the fully resolved PromptConfig for the given vertical "
                    + "without executing the AI call. Feed the returned prompt to an LLM, "
                    + "then POST the JSON answer to /verticals/{vertical}/suggestions/impact-config."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fully resolved PromptConfig ready to send to an LLM"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public org.open4goods.services.prompt.config.PromptConfig getImpactScorePrompt(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical) throws Exception {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResourceNotFoundException("Vertical not found: " + vertical);
        }
        return verticalsGenService.generateEcoscoreDryRun(vc);
    }

    @PostMapping(path = "/verticals/{vertical}/suggestions/impact-config")
    @Operation(
            summary = "Compose ImpactScoreConfig from an LLM JSON answer",
            description = "Accepts the raw JSON answer produced by an LLM from the prompt returned "
                    + "by GET /verticals/{vertical}/suggestions/impact-prompt and returns a fully "
                    + "populated ImpactScoreConfig. The caller serialises the result to "
                    + "impactscores/{vertical}.yml (no server-side file write)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ImpactScoreConfig deserialized from the LLM answer"),
            @ApiResponse(responseCode = "404", description = "Vertical not found"),
            @ApiResponse(responseCode = "400", description = "LLM JSON answer could not be parsed into an ImpactScoreConfig")
    })
    public ImpactScoreConfig generateImpactScoreConfig(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @PathVariable String vertical,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Raw JSON string produced by the LLM in response to the impact-prompt",
                    required = true)
            @RequestBody String aiJsonResponse) throws Exception {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResourceNotFoundException("Vertical not found: " + vertical);
        }
        return verticalsGenService.generateEcoscoreConfigFromJson(vc, aiJsonResponse);
    }
}
