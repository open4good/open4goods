package org.open4goods.api.controller.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.dto.AttributeEtimCandidateDto;
import org.open4goods.api.dto.AttributeIcecatCandidateDto;
import org.open4goods.api.dto.AttributeReferentialCoverageDto;
import org.open4goods.api.dto.AttributeWikidataPropertyCandidateDto;
import org.open4goods.api.dto.EtimCandidateDto;
import org.open4goods.api.dto.GoogleCandidateDto;
import org.open4goods.api.dto.WikidataCandidateDto;
import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatCategoryFeatureDocument;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.services.IcecatIndexService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.referential.AttributeReferentials;
import org.open4goods.model.vertical.referential.EprelFeatureReferential;
import org.open4goods.model.vertical.referential.EtimFeatureReferential;
import org.open4goods.model.vertical.referential.IcecatFeatureReferential;
import org.open4goods.model.vertical.referential.IcecatReferential;
import org.open4goods.model.vertical.referential.WikidataPropertyReferential;
import org.open4goods.services.wikidataservice.service.WikidataSearchService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Resolution assistance endpoints that help an AI agent discover candidate taxonomy
 * mappings for a vertical.
 * <p>
 * Each endpoint returns scored candidates from a specific taxonomy (Google Product
 * Taxonomy, ETIM, or Wikidata). The agent queries these endpoints, selects the best
 * matches, and writes them back to the vertical YAML configuration under the
 * {@code referentials:} block.
 */
@RestController
@RequestMapping("/api/referentials")
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Profile("!beta")
@Tag(name = "Referentials", description = "Taxonomy candidate discovery endpoints consumed by AI agents during vertical configuration. "
        + "Returns scored candidates from Google Product Taxonomy, ETIM and Wikidata at both vertical and attribute level. "
        + "Not active in the beta profile.")
public class ReferentialHelperController
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferentialHelperController.class);

    private static final int DEFAULT_MAX_CANDIDATES = 5;

    private final VerticalsConfigService verticalsService;
    private final GoogleTaxonomyService googleTaxonomyService;
    private final WikidataSearchService wikidataSearchService;
    private final IcecatIndexService icecatIndexService;

    public ReferentialHelperController(
            VerticalsConfigService verticalsService,
            GoogleTaxonomyService googleTaxonomyService,
            WikidataSearchService wikidataSearchService,
            IcecatIndexService icecatIndexService)
    {
        this.verticalsService = verticalsService;
        this.googleTaxonomyService = googleTaxonomyService;
        this.wikidataSearchService = wikidataSearchService;
        this.icecatIndexService = icecatIndexService;
    }

    /**
     * Returns candidate Google Product Taxonomy entries for a vertical.
     * <p>
     * Scans the in-memory taxonomy index and scores each entry by keyword overlap
     * with the vertical's localized names and ID.
     *
     * @param verticalId vertical identifier (e.g. "air-conditioner")
     * @param maxResults maximum number of candidates to return (default 5)
     * @return ranked list of {@link GoogleCandidateDto}
     */
    @GetMapping("/google/candidates")
    @Operation(
            summary = "Candidate Google Product Taxonomy entries for a vertical",
            description = "Scores taxonomy entries by keyword overlap with vertical names. "
                    + "Use to choose the best googleTaxonomy referential mapping.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranked list of Google Product Taxonomy candidates"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public ResponseEntity<List<GoogleCandidateDto>> googleCandidates(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop', 'air-conditioner')", required = true)
            @RequestParam String vertical,
            @Parameter(description = "Maximum number of candidates to return")
            @RequestParam(defaultValue = "" + DEFAULT_MAX_CANDIDATES) int maxResults)
    {
        VerticalConfig vc = verticalsService.getConfigById(vertical);
        if (vc == null)
        {
            return ResponseEntity.notFound().build();
        }

        Set<String> searchTerms = VerticalSearchTerms.of(vc);
        Map<String, Integer> lastCategories = googleTaxonomyService.getLastCategoriesId();
        Map<String, Integer> fullCategories = googleTaxonomyService.getFullCategoriesId();

        List<GoogleCandidateDto> candidates = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : fullCategories.entrySet())
        {
            String categoryKey = entry.getKey();
            Integer categoryId = entry.getValue();
            double score = keywordOverlapScore(categoryKey, searchTerms);
            if (score > 0)
            {
                List<String> pathParts = googleTaxonomyService.getTaxonomyName(categoryId);
                String path = pathParts == null ? categoryKey : String.join(" > ", pathParts);
                candidates.add(new GoogleCandidateDto(categoryId, path, score, "full-path-match"));
            }
        }

        for (Map.Entry<String, Integer> entry : lastCategories.entrySet())
        {
            String leafKey = entry.getKey();
            Integer categoryId = entry.getValue();
            double score = keywordOverlapScore(leafKey, searchTerms);
            if (score > 0)
            {
                boolean alreadyFound = candidates.stream().anyMatch(c -> c.id().equals(categoryId));
                if (!alreadyFound)
                {
                    List<String> pathParts = googleTaxonomyService.getTaxonomyName(categoryId);
                    String path = pathParts == null ? leafKey : String.join(" > ", pathParts);
                    candidates.add(new GoogleCandidateDto(categoryId, path, score * 0.9, "leaf-match"));
                }
            }
        }

        candidates.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        return ResponseEntity.ok(candidates.stream().limit(maxResults).toList());
    }

    /**
     * Returns candidate ETIM class entries for a vertical.
     * <p>
     * Uses the Wikidata SPARQL endpoint to resolve ETIM class identifiers linked
     * to the vertical's Icecat category via Wikidata equivalence properties.
     * Falls back to a label-text search when no Icecat category is configured.
     *
     * @param verticalId vertical identifier
     * @param maxResults maximum number of candidates to return (default 5)
     * @return ranked list of {@link EtimCandidateDto}
     */
    @GetMapping("/etim/candidates")
    @Operation(
            summary = "Candidate ETIM class entries for a vertical",
            description = "Resolves ETIM class IDs via Wikidata SPARQL. "
                    + "Queries by Icecat category ID first, then falls back to label search.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranked list of ETIM class candidates"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public ResponseEntity<List<EtimCandidateDto>> etimCandidates(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop', 'air-conditioner')", required = true)
            @RequestParam String vertical,
            @Parameter(description = "Maximum number of candidates to return")
            @RequestParam(defaultValue = "" + DEFAULT_MAX_CANDIDATES) int maxResults)
    {
        VerticalConfig vc = verticalsService.getConfigById(vertical);
        if (vc == null)
        {
            return ResponseEntity.notFound().build();
        }

        List<EtimCandidateDto> candidates = new ArrayList<>();

        Integer icecatId = vc.getIcecatTaxonomyId();
        if (icecatId != null && icecatId > 0)
        {
            candidates.addAll(resolveEtimViaIcecatSparql(icecatId, maxResults));
        }

        if (candidates.size() < maxResults)
        {
            for (String term : VerticalSearchTerms.of(vc))
            {
                if (candidates.size() >= maxResults)
                {
                    break;
                }
                candidates.addAll(resolveEtimViaLabelSearch(term, maxResults - candidates.size()));
            }
        }

        candidates.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        return ResponseEntity.ok(candidates.stream().limit(maxResults).toList());
    }

    /**
     * Returns candidate Wikidata entities for a vertical.
     * <p>
     * Performs a Wikidata text search using the vertical's localized names and
     * synonyms via the {@code wikibase:mwapi EntitySearch} SPARQL service.
     *
     * @param verticalId vertical identifier
     * @param maxResults maximum number of candidates to return (default 5)
     * @return ranked list of {@link WikidataCandidateDto}
     */
    @GetMapping("/wikidata/candidates")
    @Operation(
            summary = "Candidate Wikidata entities for a vertical",
            description = "Searches Wikidata by vertical names using the EntitySearch SPARQL service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranked list of Wikidata entity candidates"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public ResponseEntity<List<WikidataCandidateDto>> wikidataCandidates(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop', 'air-conditioner')", required = true)
            @RequestParam String vertical,
            @Parameter(description = "Maximum number of candidates to return")
            @RequestParam(defaultValue = "" + DEFAULT_MAX_CANDIDATES) int maxResults)
    {
        VerticalConfig vc = verticalsService.getConfigById(vertical);
        if (vc == null)
        {
            return ResponseEntity.notFound().build();
        }

        List<WikidataCandidateDto> candidates = new ArrayList<>();
        Set<String> searchTerms = VerticalSearchTerms.of(vc);

        for (String term : searchTerms)
        {
            if (candidates.size() >= maxResults)
            {
                break;
            }
            candidates.addAll(wikidataEntitySearch(term, maxResults - candidates.size()));
        }

        return ResponseEntity.ok(candidates.stream().limit(maxResults).toList());
    }

    // -------------------------------------------------------------------------
    // Attribute-level reconciliation endpoints
    // -------------------------------------------------------------------------

    /**
     * Returns Icecat feature candidates for a single Nudger attribute, scoped to the
     * Icecat categories already declared for the vertical (either through the legacy
     * {@code icecatTaxonomyId} or through the new {@code referentials.icecat} block).
     * <p>
     * Each candidate carries the Icecat feature ID, English name, feature type and
     * the originating category to ease manual review.
     *
     * @param vertical    vertical identifier (e.g. "tv")
     * @param attribute   Nudger attribute key (e.g. "HEIGHT")
     * @param maxResults  maximum number of candidates to return (default 5)
     */
    @GetMapping("/attribute/icecat/candidates")
    @Operation(
            summary = "Candidate Icecat feature IDs for a Nudger attribute key",
            description = "Iterates Icecat features bound to the vertical's Icecat "
                    + "categories and scores them by token overlap against the attribute's "
                    + "key, localized names and synonyms (all Icecat languages). "
                    + "If no candidates can be found within the declared categories - for "
                    + "instance because the category-feature mapping has not been "
                    + "synchronised - falls back to a global Icecat-features index search "
                    + "with matchSource=global-fallback.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranked list of Icecat feature candidates for the attribute"),
            @ApiResponse(responseCode = "404", description = "Vertical or attribute not found")
    })
    public ResponseEntity<List<AttributeIcecatCandidateDto>> attributeIcecatCandidates(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @RequestParam String vertical,
            @Parameter(description = "Nudger attribute key to find Icecat feature mappings for (e.g. 'HEIGHT', 'WEIGHT')", required = true)
            @RequestParam String attribute,
            @Parameter(description = "Maximum number of candidates to return")
            @RequestParam(defaultValue = "" + DEFAULT_MAX_CANDIDATES) int maxResults)
    {
        VerticalConfig vc = verticalsService.getConfigById(vertical);
        if (vc == null)
        {
            return ResponseEntity.notFound().build();
        }

        AttributeConfig attrConfig = vc.getAttributesConfig() == null ? null
                : vc.getAttributesConfig().getAttributeConfigByKey(attribute);
        if (attrConfig == null)
        {
            return ResponseEntity.notFound().build();
        }

        Set<String> attributeTerms = attributeSearchTerms(attrConfig);
        Set<Integer> existingFeatureIds = attrConfig.icecatFeatureIds();
        List<AttributeIcecatCandidateDto> candidates = new ArrayList<>();
        Set<Integer> seenFeatureIds = new HashSet<>();

        for (Integer categoryId : icecatCategoryIds(vc))
        {
            Optional<IcecatCategoryDocument> category = icecatIndexService.findCategory(categoryId);
            if (category.isEmpty())
            {
                continue;
            }
            IcecatCategoryDocument cat = category.get();
            Map<Integer, IcecatFeatureDocument> docs = icecatIndexService.findCategoryFeatureDocuments(cat);
            List<IcecatCategoryFeatureDocument> categoryFeatures = cat.getFeatures() == null
                    ? List.<IcecatCategoryFeatureDocument>of()
                    : cat.getFeatures();
            for (IcecatCategoryFeatureDocument catFeature : categoryFeatures)
            {
                Integer featureId = catFeature.getId();
                if (featureId == null || !seenFeatureIds.add(featureId))
                {
                    continue;
                }
                IcecatFeatureDocument doc = docs.get(featureId);
                String englishName = doc == null ? null : doc.getEnglishName();
                double score = scoreFeatureAgainstAttribute(doc, attributeTerms);
                if (score <= 0)
                {
                    continue;
                }
                boolean alreadyBound = existingFeatureIds.contains(featureId);
                if (alreadyBound)
                {
                    score = Math.min(1.0, score + 0.10);
                }
                String matchSource = alreadyBound
                        ? "already-bound"
                        : (score >= 1.0 ? "exact-name" : "token-overlap");
                candidates.add(new AttributeIcecatCandidateDto(
                        featureId,
                        englishName,
                        doc == null ? null : doc.getType(),
                        cat.getId(),
                        cat.getEnglishName(),
                        score,
                        matchSource));
            }
        }

        if (candidates.isEmpty())
        {
            candidates.addAll(globalIcecatFeatureFallback(
                    attributeTerms, existingFeatureIds, seenFeatureIds, maxResults));
        }

        candidates.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        return ResponseEntity.ok(candidates.stream().limit(maxResults).toList());
    }

    /**
     * Returns ETIM feature candidates for a single Nudger attribute via Wikidata SPARQL.
     * <p>
     * For every ETIM class declared at vertical level, resolves features (ETIM
     * {@code EFxxxxxx}) linked to the vertical's Icecat features through Wikidata
     * properties. Falls back to a Wikidata text search against the attribute name when
     * no ETIM class is configured.
     */
    @GetMapping("/attribute/etim/candidates")
    @Operation(
            summary = "Candidate ETIM feature IDs for a Nudger attribute key",
            description = "Resolves ETIM features via Wikidata SPARQL bridges (Icecat "
                    + "feature ↔ ETIM feature) and Wikidata label search.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranked list of ETIM feature candidates for the attribute"),
            @ApiResponse(responseCode = "404", description = "Vertical or attribute not found")
    })
    public ResponseEntity<List<AttributeEtimCandidateDto>> attributeEtimCandidates(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @RequestParam String vertical,
            @Parameter(description = "Nudger attribute key to find ETIM feature mappings for (e.g. 'HEIGHT', 'WEIGHT')", required = true)
            @RequestParam String attribute,
            @Parameter(description = "Maximum number of candidates to return")
            @RequestParam(defaultValue = "" + DEFAULT_MAX_CANDIDATES) int maxResults)
    {
        VerticalConfig vc = verticalsService.getConfigById(vertical);
        if (vc == null)
        {
            return ResponseEntity.notFound().build();
        }
        AttributeConfig attrConfig = vc.getAttributesConfig() == null ? null
                : vc.getAttributesConfig().getAttributeConfigByKey(attribute);
        if (attrConfig == null)
        {
            return ResponseEntity.notFound().build();
        }

        List<AttributeEtimCandidateDto> candidates = new ArrayList<>();

        for (Integer icecatFeatureId : attrConfig.icecatFeatureIds())
        {
            candidates.addAll(resolveEtimFeatureViaIcecatFeature(icecatFeatureId, maxResults));
            if (candidates.size() >= maxResults)
            {
                break;
            }
        }

        if (candidates.size() < maxResults)
        {
            for (String term : attributeSearchTerms(attrConfig))
            {
                if (candidates.size() >= maxResults)
                {
                    break;
                }
                candidates.addAll(resolveEtimFeatureViaLabelSearch(term, maxResults - candidates.size()));
            }
        }

        candidates.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        return ResponseEntity.ok(candidates.stream().limit(maxResults).toList());
    }

    /**
     * Returns Wikidata property candidates (e.g. {@code P2048} for {@code height})
     * for a Nudger attribute. Uses the Wikidata EntitySearch service constrained to
     * properties.
     */
    @GetMapping("/attribute/wikidata/candidates")
    @Operation(
            summary = "Candidate Wikidata property IDs for a Nudger attribute key",
            description = "Searches Wikidata properties by the attribute's name and synonyms.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranked list of Wikidata property (P-id) candidates for the attribute"),
            @ApiResponse(responseCode = "404", description = "Vertical or attribute not found")
    })
    public ResponseEntity<List<AttributeWikidataPropertyCandidateDto>> attributeWikidataPropertyCandidates(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @RequestParam String vertical,
            @Parameter(description = "Nudger attribute key to find Wikidata property mappings for (e.g. 'HEIGHT', 'WEIGHT')", required = true)
            @RequestParam String attribute,
            @Parameter(description = "Maximum number of candidates to return")
            @RequestParam(defaultValue = "" + DEFAULT_MAX_CANDIDATES) int maxResults)
    {
        VerticalConfig vc = verticalsService.getConfigById(vertical);
        if (vc == null)
        {
            return ResponseEntity.notFound().build();
        }
        AttributeConfig attrConfig = vc.getAttributesConfig() == null ? null
                : vc.getAttributesConfig().getAttributeConfigByKey(attribute);
        if (attrConfig == null)
        {
            return ResponseEntity.notFound().build();
        }

        List<AttributeWikidataPropertyCandidateDto> candidates = new ArrayList<>();
        for (String term : attributeSearchTerms(attrConfig))
        {
            if (candidates.size() >= maxResults)
            {
                break;
            }
            candidates.addAll(wikidataPropertySearch(term, maxResults - candidates.size()));
        }

        candidates.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        return ResponseEntity.ok(candidates.stream().limit(maxResults).toList());
    }

    /**
     * Per-attribute referential coverage report for a vertical.
     * <p>
     * Lists which taxonomies each attribute is wired to (Icecat, EPREL, ETIM, Wikidata)
     * so an AI agent can target the attributes still missing mappings.
     */
    @GetMapping("/attribute/coverage")
    @Operation(
            summary = "Per-attribute cross-referential coverage of a vertical",
            description = "Lists for every attribute of the vertical which taxonomies "
                    + "are wired (icecat / eprel / etim / wikidata) and which are still missing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coverage report listing wired and missing taxonomies per attribute"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public ResponseEntity<List<AttributeReferentialCoverageDto>> attributeCoverage(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop')", required = true)
            @RequestParam String vertical)
    {
        VerticalConfig vc = verticalsService.getConfigById(vertical);
        if (vc == null)
        {
            return ResponseEntity.notFound().build();
        }
        if (vc.getAttributesConfig() == null)
        {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<AttributeReferentialCoverageDto> coverage = new ArrayList<>();
        for (AttributeConfig ac : vc.getAttributesConfig().getConfigs())
        {
            coverage.add(toCoverage(ac));
        }
        return ResponseEntity.ok(coverage);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves ETIM classes linked to an Icecat category ID via Wikidata SPARQL.
     * Uses Wikidata property P4175 (Icecat category ID) and P11207 (ETIM code).
     */
    private List<EtimCandidateDto> resolveEtimViaIcecatSparql(Integer icecatId, int limit)
    {
        List<EtimCandidateDto> result = new ArrayList<>();
        String sparql = """
                SELECT ?item ?etimCode ?itemLabel WHERE {
                  ?item wdt:P4175 "%d" .
                  ?item wdt:P11207 ?etimCode .
                  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
                } LIMIT %d
                """.formatted(icecatId, limit);

        try
        {
            List<Map<String, String>> rows = wikidataSearchService.executeSparql(sparql);
            for (Map<String, String> row : rows)
            {
                String code = row.get("etimCode");
                String label = row.getOrDefault("itemLabel", "");
                if (code != null && !code.isBlank())
                {
                    result.add(new EtimCandidateDto(code, label, 0.90));
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("ETIM/Icecat SPARQL resolution failed for icecatId={}: {}", icecatId, e.getMessage());
        }
        return result;
    }

    /**
     * Searches for ETIM class entries on Wikidata by label text match.
     */
    private List<EtimCandidateDto> resolveEtimViaLabelSearch(String term, int limit)
    {
        List<EtimCandidateDto> result = new ArrayList<>();
        String safeTerm = escapeSparql(term);
        String sparql = """
                SELECT ?item ?etimCode ?itemLabel WHERE {
                  SERVICE wikibase:mwapi {
                    bd:serviceParam wikibase:api "EntitySearch" .
                    bd:serviceParam wikibase:endpoint "www.wikidata.org" .
                    bd:serviceParam mwapi:search "%s" .
                    bd:serviceParam mwapi:language "en" .
                    ?item wikibase:apiOutputItem mwapi:item .
                  }
                  ?item wdt:P11207 ?etimCode .
                  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
                } LIMIT %d
                """.formatted(safeTerm, limit);

        try
        {
            List<Map<String, String>> rows = wikidataSearchService.executeSparql(sparql);
            for (Map<String, String> row : rows)
            {
                String code = row.get("etimCode");
                String label = row.getOrDefault("itemLabel", "");
                if (code != null && !code.isBlank())
                {
                    result.add(new EtimCandidateDto(code, label, 0.70));
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("ETIM label-search SPARQL failed for term='{}': {}", term, e.getMessage());
        }
        return result;
    }

    /**
     * Performs a Wikidata EntitySearch and returns matching entities with labels/descriptions.
     */
    private List<WikidataCandidateDto> wikidataEntitySearch(String term, int limit)
    {
        List<WikidataCandidateDto> result = new ArrayList<>();
        String safeTerm = escapeSparql(term);
        String sparql = """
                SELECT ?item ?itemLabel ?itemDescription WHERE {
                  SERVICE wikibase:mwapi {
                    bd:serviceParam wikibase:api "EntitySearch" .
                    bd:serviceParam wikibase:endpoint "www.wikidata.org" .
                    bd:serviceParam mwapi:search "%s" .
                    bd:serviceParam mwapi:language "en" .
                    ?item wikibase:apiOutputItem mwapi:item .
                  }
                  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
                } LIMIT %d
                """.formatted(safeTerm, limit);

        try
        {
            List<Map<String, String>> rows = wikidataSearchService.executeSparql(sparql);
            for (Map<String, String> row : rows)
            {
                String itemUri = row.get("item");
                String label = row.getOrDefault("itemLabel", "");
                String description = row.getOrDefault("itemDescription", "");
                if (itemUri != null)
                {
                    String qid = extractQid(itemUri);
                    if (qid != null)
                    {
                        double confidence = labelConfidence(term, label);
                        result.add(new WikidataCandidateDto(qid, label, description, confidence));
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("Wikidata entity search SPARQL failed for term='{}': {}", term, e.getMessage());
        }
        return result;
    }

    /**
     * Scores a taxonomy key against the vertical search terms using token overlap.
     * Returns a value in (0, 1]; 0 means no overlap.
     */
    private double keywordOverlapScore(String categoryKey, Set<String> searchTerms)
    {
        String normalizedKey = IdHelper.azCharAndDigits(categoryKey).toLowerCase();
        int matches = 0;
        for (String term : searchTerms)
        {
            String normalizedTerm = IdHelper.azCharAndDigits(term).toLowerCase();
            if (normalizedKey.contains(normalizedTerm) || normalizedTerm.contains(normalizedKey))
            {
                matches++;
            }
        }
        return matches == 0 ? 0.0 : Math.min(1.0, matches / (double) searchTerms.size());
    }

    /**
     * Simple confidence from Levenshtein-inspired overlap between the search term and the
     * Wikidata label. Returns a value in [0, 1].
     */
    private double labelConfidence(String term, String label)
    {
        if (label == null || label.isBlank())
        {
            return 0.5;
        }
        String t = term.toLowerCase().trim();
        String l = label.toLowerCase().trim();
        if (l.equals(t))
        {
            return 1.0;
        }
        if (l.contains(t) || t.contains(l))
        {
            return 0.85;
        }
        return 0.65;
    }

    private String extractQid(String uri)
    {
        if (uri == null)
        {
            return null;
        }
        int lastSlash = uri.lastIndexOf('/');
        return (lastSlash >= 0 && lastSlash < uri.length() - 1)
                ? uri.substring(lastSlash + 1)
                : null;
    }

    private String escapeSparql(String value)
    {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // -------------------------------------------------------------------------
    // Attribute-scoped helpers
    // -------------------------------------------------------------------------

    /**
     * Collects all Icecat category IDs declared for a vertical, both via the legacy
     * {@code icecatTaxonomyId} scalar and the new {@code referentials.icecat} block.
     */
    private List<Integer> icecatCategoryIds(VerticalConfig vc)
    {
        List<Integer> result = new ArrayList<>();
        if (vc.getIcecatTaxonomyId() != null && vc.getIcecatTaxonomyId() > 0)
        {
            result.add(vc.getIcecatTaxonomyId());
        }
        if (vc.getReferentials() != null)
        {
            for (IcecatReferential ref : vc.getReferentials().getIcecat())
            {
                Integer id = ref == null ? null : ref.getCategoryId();
                if (id != null && id > 0 && !result.contains(id))
                {
                    result.add(id);
                }
            }
        }
        return result;
    }

    /**
     * Returns the search terms used to score taxonomy candidates against an attribute:
     * the attribute key, every localized name and every synonym across datasources.
     */
    private Set<String> attributeSearchTerms(AttributeConfig attrConfig)
    {
        Set<String> terms = new LinkedHashSet<>();
        VerticalSearchTerms.addTerm(terms, attrConfig.getKey());
        if (attrConfig.getName() != null && attrConfig.getName().values() != null)
        {
            for (String name : attrConfig.getName().values())
            {
                VerticalSearchTerms.addTerm(terms, name);
            }
        }
        if (attrConfig.getSynonyms() != null)
        {
            for (Set<String> synonymSet : attrConfig.getSynonyms().values())
            {
                if (synonymSet != null)
                {
                    for (String synonym : synonymSet)
                    {
                        VerticalSearchTerms.addTerm(terms, synonym);
                    }
                }
            }
        }
        return terms;
    }

    /**
     * Token-overlap score between an Icecat feature and an attribute. Returns
     * a value in [0, 1]. Scores against the English name plus every localized name
     * carried in {@link IcecatFeatureDocument#getLangNames()} (encoded as
     * {@code "langId:name"}), so French attribute keys can match French Icecat names.
     */
    private double scoreFeatureAgainstAttribute(IcecatFeatureDocument doc, Set<String> attributeTerms)
    {
        if (doc == null)
        {
            return 0.0;
        }
        double best = 0.0;
        String englishName = doc.getEnglishName();
        if (englishName != null && !englishName.isBlank())
        {
            best = tokenOverlapScore(englishName, attributeTerms);
        }
        if (doc.getLangNames() != null)
        {
            for (String entry : doc.getLangNames())
            {
                String localized = parseLangNameValue(entry);
                if (localized == null || localized.isBlank())
                {
                    continue;
                }
                double score = tokenOverlapScore(localized, attributeTerms);
                if (score > best)
                {
                    best = score;
                }
            }
        }
        return best;
    }

    /**
     * Token-level overlap score between a candidate name and a set of attribute search terms.
     * <p>
     * Each input string is tokenized on any non-alphanumeric character (after accent
     * stripping and lowercasing). Tokens shorter than 3 characters are dropped. For each
     * attribute term, computes a Jaccard-like similarity where two tokens match either
     * exactly or when one is a {@code >=4}-character prefix of the other (so
     * {@code "diagonale"} matches {@code "diagonal"}, but unrelated short tokens don't
     * spuriously match). Returns the best similarity across all attribute terms in
     * {@code [0, 1]}; 1.0 means every candidate token is matched and the candidate has
     * at least as many tokens as the term (treated as an exact-name match).
     */
    private double tokenOverlapScore(String candidate, Set<String> attributeTerms)
    {
        List<String> candidateTokens = tokenize(candidate);
        if (candidateTokens.isEmpty())
        {
            return 0.0;
        }
        double best = 0.0;
        for (String term : attributeTerms)
        {
            List<String> termTokens = tokenize(term);
            if (termTokens.isEmpty())
            {
                continue;
            }
            int matches = 0;
            Set<Integer> consumed = new HashSet<>();
            for (String tt : termTokens)
            {
                for (int i = 0; i < candidateTokens.size(); i++)
                {
                    if (consumed.contains(i))
                    {
                        continue;
                    }
                    if (tokensMatch(tt, candidateTokens.get(i)))
                    {
                        matches++;
                        consumed.add(i);
                        break;
                    }
                }
            }
            if (matches == 0)
            {
                continue;
            }
            int union = candidateTokens.size() + termTokens.size() - matches;
            double jaccard = union == 0 ? 0.0 : (double) matches / union;
            if (matches == termTokens.size() && matches == candidateTokens.size())
            {
                jaccard = 1.0;
            }
            if (jaccard > best)
            {
                best = jaccard;
            }
        }
        return best;
    }

    /**
     * Splits a string on every non-alphanumeric boundary after accent stripping and
     * lowercasing, dropping tokens shorter than 3 characters.
     */
    private List<String> tokenize(String value)
    {
        if (value == null || value.isBlank())
        {
            return List.of();
        }
        String stripped = StringUtils.stripAccents(value).toLowerCase();
        String[] parts = stripped.split("[^a-z0-9]+");
        List<String> tokens = new ArrayList<>(parts.length);
        for (String p : parts)
        {
            if (p.length() >= 3)
            {
                tokens.add(p);
            }
        }
        return tokens;
    }

    /**
     * Two tokens match if they are equal or if one is a prefix of the other and the
     * shorter token has at least 4 characters. Catches Fr/En near-cognates like
     * {@code "diagonale"}/{@code "diagonal"} without producing spurious matches on
     * short prefixes.
     */
    private boolean tokensMatch(String a, String b)
    {
        if (a.equals(b))
        {
            return true;
        }
        String shorter = a.length() <= b.length() ? a : b;
        String longer = a.length() <= b.length() ? b : a;
        return shorter.length() >= 4 && longer.startsWith(shorter);
    }

    /**
     * Parses a {@code "langId:name"} entry as encoded by
     * {@link org.open4goods.icecat.services.IcecatIndexService}. Returns the name part,
     * or {@code null} when the encoding is invalid.
     */
    private String parseLangNameValue(String entry)
    {
        if (entry == null)
        {
            return null;
        }
        int colon = entry.indexOf(':');
        return colon < 0 ? null : entry.substring(colon + 1);
    }

    /**
     * Global fallback that searches the Icecat features index directly when the
     * vertical's category-feature mapping is empty (e.g. not synced) or when no
     * scoped candidate scored above zero.
     * <p>
     * Issues an Elasticsearch {@code englishName} full-text query for every attribute
     * search term, deduplicates against {@code seenFeatureIds}, then re-scores each hit
     * with {@link #tokenOverlapScore(String, Set)}. Returned candidates carry
     * {@code categoryId=null} and {@code matchSource=global-fallback}.
     */
    private List<AttributeIcecatCandidateDto> globalIcecatFeatureFallback(
            Set<String> attributeTerms,
            Set<Integer> existingFeatureIds,
            Set<Integer> seenFeatureIds,
            int maxResults)
    {
        if (attributeTerms.isEmpty())
        {
            return List.of();
        }
        Set<String> queryTokens = new LinkedHashSet<>();
        for (String term : attributeTerms)
        {
            queryTokens.addAll(tokenize(term));
        }
        if (queryTokens.isEmpty())
        {
            return List.of();
        }
        List<AttributeIcecatCandidateDto> result = new ArrayList<>();
        int perQueryLimit = Math.max(maxResults, DEFAULT_MAX_CANDIDATES);
        for (String token : queryTokens)
        {
            Page<IcecatFeatureDocument> page;
            try
            {
                page = icecatIndexService.searchFeatures(token, PageRequest.of(0, perQueryLimit));
            }
            catch (Exception e)
            {
                LOGGER.warn("Icecat global feature search failed for token='{}': {}", token, e.getMessage());
                continue;
            }
            for (IcecatFeatureDocument doc : page.getContent())
            {
                Integer featureId = doc.getId();
                if (featureId == null || !seenFeatureIds.add(featureId))
                {
                    continue;
                }
                double score = scoreFeatureAgainstAttribute(doc, attributeTerms);
                if (score <= 0)
                {
                    continue;
                }
                if (existingFeatureIds.contains(featureId))
                {
                    score = Math.min(1.0, score + 0.10);
                }
                String matchSource = existingFeatureIds.contains(featureId)
                        ? "already-bound"
                        : "global-fallback";
                result.add(new AttributeIcecatCandidateDto(
                        featureId,
                        doc.getEnglishName(),
                        doc.getType(),
                        null,
                        null,
                        score * 0.85,
                        matchSource));
            }
        }
        return result;
    }

    /**
     * Resolves ETIM feature codes linked to an Icecat feature ID via Wikidata SPARQL.
     * Uses Wikidata property P2702 (Icecat feature ID) and P11207 (ETIM code).
     */
    private List<AttributeEtimCandidateDto> resolveEtimFeatureViaIcecatFeature(Integer icecatFeatureId, int limit)
    {
        List<AttributeEtimCandidateDto> result = new ArrayList<>();
        String sparql = """
                SELECT ?etimFeature ?itemLabel ?etimClass ?classLabel WHERE {
                  ?item wdt:P2702 "%d" .
                  ?item wdt:P11207 ?etimFeature .
                  OPTIONAL { ?item wdt:P5009 ?etimClass . }
                  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
                } LIMIT %d
                """.formatted(icecatFeatureId, limit);

        try
        {
            List<Map<String, String>> rows = wikidataSearchService.executeSparql(sparql);
            for (Map<String, String> row : rows)
            {
                String code = row.get("etimFeature");
                String label = row.getOrDefault("itemLabel", "");
                String classId = row.get("etimClass");
                String className = row.getOrDefault("classLabel", "");
                if (code != null && !code.isBlank())
                {
                    result.add(new AttributeEtimCandidateDto(code, label, classId, className, 0.90, "icecat-feature-bridge"));
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("ETIM/Icecat-feature SPARQL resolution failed for icecatFeatureId={}: {}", icecatFeatureId, e.getMessage());
        }
        return result;
    }

    /**
     * Searches for ETIM feature codes on Wikidata by label match.
     */
    private List<AttributeEtimCandidateDto> resolveEtimFeatureViaLabelSearch(String term, int limit)
    {
        List<AttributeEtimCandidateDto> result = new ArrayList<>();
        String safeTerm = escapeSparql(term);
        String sparql = """
                SELECT ?etimFeature ?itemLabel WHERE {
                  SERVICE wikibase:mwapi {
                    bd:serviceParam wikibase:api "EntitySearch" .
                    bd:serviceParam wikibase:endpoint "www.wikidata.org" .
                    bd:serviceParam mwapi:search "%s" .
                    bd:serviceParam mwapi:language "en" .
                    ?item wikibase:apiOutputItem mwapi:item .
                  }
                  ?item wdt:P11207 ?etimFeature .
                  FILTER(STRSTARTS(?etimFeature, "EF"))
                  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
                } LIMIT %d
                """.formatted(safeTerm, limit);

        try
        {
            List<Map<String, String>> rows = wikidataSearchService.executeSparql(sparql);
            for (Map<String, String> row : rows)
            {
                String code = row.get("etimFeature");
                String label = row.getOrDefault("itemLabel", "");
                if (code != null && !code.isBlank())
                {
                    result.add(new AttributeEtimCandidateDto(code, label, null, null, 0.65, "label-search"));
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("ETIM feature label-search SPARQL failed for term='{}': {}", term, e.getMessage());
        }
        return result;
    }

    /**
     * Performs a Wikidata EntitySearch restricted to properties and returns matching
     * P-IDs with labels and descriptions.
     */
    private List<AttributeWikidataPropertyCandidateDto> wikidataPropertySearch(String term, int limit)
    {
        List<AttributeWikidataPropertyCandidateDto> result = new ArrayList<>();
        String safeTerm = escapeSparql(term);
        String sparql = """
                SELECT ?prop ?propLabel ?propDescription WHERE {
                  SERVICE wikibase:mwapi {
                    bd:serviceParam wikibase:api "EntitySearch" .
                    bd:serviceParam wikibase:endpoint "www.wikidata.org" .
                    bd:serviceParam mwapi:search "%s" .
                    bd:serviceParam mwapi:language "en" .
                    bd:serviceParam mwapi:srnamespace "120" .
                    ?prop wikibase:apiOutputItem mwapi:item .
                  }
                  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
                } LIMIT %d
                """.formatted(safeTerm, limit);

        try
        {
            List<Map<String, String>> rows = wikidataSearchService.executeSparql(sparql);
            for (Map<String, String> row : rows)
            {
                String uri = row.get("prop");
                String label = row.getOrDefault("propLabel", "");
                String description = row.getOrDefault("propDescription", "");
                String pid = extractPid(uri);
                if (pid != null)
                {
                    double confidence = labelConfidence(term, label);
                    result.add(new AttributeWikidataPropertyCandidateDto(pid, label, description, confidence));
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("Wikidata property search SPARQL failed for term='{}': {}", term, e.getMessage());
        }
        return result;
    }

    private String extractPid(String uri)
    {
        if (uri == null)
        {
            return null;
        }
        int lastSlash = uri.lastIndexOf('/');
        String tail = (lastSlash >= 0 && lastSlash < uri.length() - 1)
                ? uri.substring(lastSlash + 1)
                : uri;
        return tail.startsWith("P") ? tail : null;
    }

    /**
     * Builds the per-attribute coverage report for the {@code /attribute/coverage} endpoint.
     */
    private AttributeReferentialCoverageDto toCoverage(AttributeConfig ac)
    {
        AttributeReferentials ref = ac.getReferentials();
        List<Integer> icecatIds = new ArrayList<>();
        List<String> eprelNames = new ArrayList<>();
        List<String> etimIds = new ArrayList<>();
        List<String> wikidataPids = new ArrayList<>();
        if (ref != null)
        {
            for (IcecatFeatureReferential entry : ref.getIcecat())
            {
                if (entry != null && entry.getFeatureId() != null)
                {
                    icecatIds.add(entry.getFeatureId());
                }
            }
            for (EprelFeatureReferential entry : ref.getEprel())
            {
                if (entry != null && entry.getFeatureName() != null && !entry.getFeatureName().isBlank())
                {
                    eprelNames.add(entry.getFeatureName());
                }
            }
            for (EtimFeatureReferential entry : ref.getEtim())
            {
                if (entry != null && entry.getFeatureId() != null && !entry.getFeatureId().isBlank())
                {
                    etimIds.add(entry.getFeatureId());
                }
            }
            for (WikidataPropertyReferential entry : ref.getWikidata())
            {
                if (entry != null && entry.getPid() != null && !entry.getPid().isBlank())
                {
                    wikidataPids.add(entry.getPid());
                }
            }
        }
        List<String> covered = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        addCoverage(covered, missing, "icecat", !icecatIds.isEmpty());
        addCoverage(covered, missing, "eprel", !eprelNames.isEmpty());
        addCoverage(covered, missing, "etim", !etimIds.isEmpty());
        addCoverage(covered, missing, "wikidata", !wikidataPids.isEmpty());
        return new AttributeReferentialCoverageDto(
                ac.getKey(), covered, missing, icecatIds, eprelNames, etimIds, wikidataPids);
    }

    private void addCoverage(List<String> covered, List<String> missing, String taxonomy, boolean present)
    {
        if (present)
        {
            covered.add(taxonomy);
        }
        else
        {
            missing.add(taxonomy);
        }
    }
}
