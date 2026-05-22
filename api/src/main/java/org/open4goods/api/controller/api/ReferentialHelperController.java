package org.open4goods.api.controller.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import org.open4goods.model.vertical.ProductI18nElements;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

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
    public ResponseEntity<List<GoogleCandidateDto>> googleCandidates(
            @RequestParam String vertical,
            @RequestParam(defaultValue = "" + DEFAULT_MAX_CANDIDATES) int maxResults)
    {
        VerticalConfig vc = verticalsService.getConfigById(vertical);
        if (vc == null)
        {
            return ResponseEntity.notFound().build();
        }

        Set<String> searchTerms = verticalSearchTerms(vc);
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
    public ResponseEntity<List<EtimCandidateDto>> etimCandidates(
            @RequestParam String vertical,
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
            for (String term : verticalSearchTerms(vc))
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
    public ResponseEntity<List<WikidataCandidateDto>> wikidataCandidates(
            @RequestParam String vertical,
            @RequestParam(defaultValue = "" + DEFAULT_MAX_CANDIDATES) int maxResults)
    {
        VerticalConfig vc = verticalsService.getConfigById(vertical);
        if (vc == null)
        {
            return ResponseEntity.notFound().build();
        }

        List<WikidataCandidateDto> candidates = new ArrayList<>();
        Set<String> searchTerms = verticalSearchTerms(vc);

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
                    + "categories and scores them by name overlap with the attribute's "
                    + "synonyms and localized names.")
    public ResponseEntity<List<AttributeIcecatCandidateDto>> attributeIcecatCandidates(
            @RequestParam String vertical,
            @RequestParam String attribute,
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
                if (featureId == null)
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
                if (existingFeatureIds.contains(featureId))
                {
                    score = Math.min(1.0, score + 0.10);
                }
                String matchSource = (existingFeatureIds.contains(featureId))
                        ? "already-bound"
                        : (englishName != null && attributeTerms.stream()
                                .map(t -> IdHelper.azCharAndDigits(t).toLowerCase())
                                .anyMatch(t -> IdHelper.azCharAndDigits(englishName).toLowerCase().equals(t))
                                ? "exact-name"
                                : "name-overlap");
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
    public ResponseEntity<List<AttributeEtimCandidateDto>> attributeEtimCandidates(
            @RequestParam String vertical,
            @RequestParam String attribute,
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
            summary = "Candidate Wikidata property IDs (P-xxxx) for a Nudger attribute key",
            description = "Searches Wikidata properties by the attribute's name and synonyms.")
    public ResponseEntity<List<AttributeWikidataPropertyCandidateDto>> attributeWikidataPropertyCandidates(
            @RequestParam String vertical,
            @RequestParam String attribute,
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
    public ResponseEntity<List<AttributeReferentialCoverageDto>> attributeCoverage(
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

    /** Collects localized names and the vertical ID as search terms. */
    private Set<String> verticalSearchTerms(VerticalConfig vc)
    {
        Set<String> terms = new LinkedHashSet<>();
        addTerm(terms, vc.getId());
        if (vc.getId() != null)
        {
            addTerm(terms, vc.getId().replace('-', ' ').replace('_', ' '));
        }
        for (ProductI18nElements i18n : vc.getI18n().values())
        {
            addTerm(terms, i18n.getVerticalHomeTitle());
            addTerm(terms, i18n.getCardTitle());
            addTerm(terms, i18n.getShortName());
            if (i18n.getSingular() != null)
            {
                addTerm(terms, i18n.getSingular().getPrefix());
            }
        }
        return terms;
    }

    private void addTerm(Set<String> terms, String term)
    {
        if (term != null && !term.isBlank())
        {
            terms.add(term.trim());
        }
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
        addTerm(terms, attrConfig.getKey());
        if (attrConfig.getName() != null && attrConfig.getName().values() != null)
        {
            for (String name : attrConfig.getName().values())
            {
                addTerm(terms, name);
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
                        addTerm(terms, synonym);
                    }
                }
            }
        }
        return terms;
    }

    /**
     * Token-overlap score between an Icecat feature and an attribute. Returns
     * a value in [0, 1]. Uses English feature name and normalized variants.
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
            best = keywordOverlapScore(englishName, attributeTerms);
        }
        if (doc.getNormalizedNames() != null)
        {
            for (String normalized : doc.getNormalizedNames())
            {
                double score = keywordOverlapScore(normalized, attributeTerms);
                if (score > best)
                {
                    best = score;
                }
            }
        }
        return best;
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
