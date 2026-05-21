package org.open4goods.api.controller.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.api.dto.EtimCandidateDto;
import org.open4goods.api.dto.GoogleCandidateDto;
import org.open4goods.api.dto.WikidataCandidateDto;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
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

    public ReferentialHelperController(
            VerticalsConfigService verticalsService,
            GoogleTaxonomyService googleTaxonomyService,
            WikidataSearchService wikidataSearchService)
    {
        this.verticalsService = verticalsService;
        this.googleTaxonomyService = googleTaxonomyService;
        this.wikidataSearchService = wikidataSearchService;
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
}
