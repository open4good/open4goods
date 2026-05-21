

package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.List;

import org.open4goods.api.dto.AttributeSuggestionDto;
import org.open4goods.api.dto.CategorySuggestionsDto;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Read-only vertical generation and suggestion endpoints.
 * All write operations (YAML mutations) are performed by agents via their file tools,
 * using the data returned by these endpoints.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
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

    @GetMapping(path = "/{vertical}/attributes/")
    @Operation(summary = "Attribute coverage stats for a vertical (JSON)")
    @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
    public VerticalAttributesStats generateAttributesCoverage(@PathVariable String vertical)
            throws ResourceNotFoundException, IOException {
        return verticalsGenService.attributesStats(vertical);
    }

    @GetMapping(path = "/{vertical}/categories/")
    @Operation(summary = "Suggested matchingCategories YAML fragment for a vertical")
    @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
    public String generateCategoryMappingsFragment(@PathVariable String vertical,
            @RequestParam(defaultValue = "5") Integer minOffersCount)
            throws ResourceNotFoundException, IOException {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        return verticalsGenService.generateMapping(vc, minOffersCount);
    }

    @GetMapping(path = "/{vertical}/ecoscore/")
    @Operation(
        summary = "Run the ecoscore LLM prompt and return YAML content for impactscores/{vertical}.yml",
        description = "Cached for 1h per vertical. Pass force=true to evict the entry and re-run the LLM "
                + "(useful when iterating during Phase E of the category playbook)."
    )
    @Caching(
        cacheable = @Cacheable(
            cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME,
            key = "'ecoscore:' + #vertical",
            condition = "!#force"
        ),
        evict = @CacheEvict(
            cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME,
            key = "'ecoscore:' + #vertical",
            condition = "#force",
            beforeInvocation = true
        )
    )
    public String generateEcoscoreMappings(@PathVariable String vertical,
            @RequestParam(name = "force", defaultValue = "false") boolean force)
            throws ResourceNotFoundException, IOException {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        return verticalsGenService.generateEcoscoreYamlConfig(vc);
    }

    @GetMapping(path = "/{vertical}/impactscore-criterias/")
    @Operation(summary = "Available impact-score criterias YAML fragment for a vertical")
    @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
    public String generateImpactScoreCriterias(@PathVariable String vertical,
            @RequestParam(defaultValue = "10") Integer minCoveragePercent)
            throws ResourceNotFoundException, IOException {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        return verticalsGenService.generateAvailableImpactScoreCriteriasFragment(vc, minCoveragePercent);
    }

    @GetMapping(path = "/{vertical}/nudgetool/compute")
    @Operation(summary = "Compute nudge tool score thresholds and impact subsets without writing to disk")
    public VerticalConfig computeNudgeTool(@PathVariable String vertical)
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
    public CategorySuggestionsDto suggestCategories(@PathVariable String vertical,
            @RequestParam(defaultValue = "5") int minOffersCount) throws ResourceNotFoundException {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResourceNotFoundException("Vertical not found: " + vertical);
        }
        return verticalsGenService.suggestCategories(vc, minOffersCount);
    }

    @GetMapping(path = "/verticals/{vertical}/suggestions/attributes")
    @Operation(
        summary = "Attribute suggestions for a vertical",
        description = "Returns attribute candidates with coverage stats and a ymlExists flag. "
                + "Use this to assemble attributesConfig.configs of a VerticalConfig YAML "
                + "without server-side file mutations."
    )
    public List<AttributeSuggestionDto> suggestAttributes(@PathVariable String vertical,
            @RequestParam(defaultValue = "10") int minCoverage,
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
    public org.open4goods.services.prompt.config.PromptConfig getImpactScorePrompt(
            @PathVariable String vertical) throws Exception {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResourceNotFoundException("Vertical not found: " + vertical);
        }
        return verticalsGenService.generateEcoscoreDryRun(vc);
    }

    @PostMapping(path = "/verticals/{vertical}/suggestions/impact-config")
    @Operation(
        summary = "Compose ImpactScoreConfig from LLM JSON answer",
        description = "Accepts the raw JSON answer produced by an LLM from the prompt returned "
                + "by GET /verticals/{vertical}/suggestions/impact-prompt and returns a fully "
                + "populated ImpactScoreConfig. The caller serialises the result to "
                + "impactscores/{vertical}.yml (no server-side file write)."
    )
    public ImpactScoreConfig generateImpactScoreConfig(@PathVariable String vertical,
            @RequestBody String aiJsonResponse) throws Exception {
        VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
        if (vc == null) {
            throw new ResourceNotFoundException("Vertical not found: " + vertical);
        }
        return verticalsGenService.generateEcoscoreConfigFromJson(vc, aiJsonResponse);
    }
}
