package org.open4goods.api.services;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.referential.EtimReferential;
import org.open4goods.model.vertical.referential.GoogleTaxonomyReferential;
import org.open4goods.model.vertical.referential.IcecatReferential;
import org.open4goods.model.vertical.referential.TaxonomyReferentials;
import org.open4goods.model.vertical.referential.WikidataReferential;
import org.open4goods.services.wikidataservice.service.WikidataSearchService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Cross-referential taxonomy bridge service.
 * <p>
 * Provides helpers to interrogate the {@link TaxonomyReferentials} block of a vertical
 * configuration and, where possible, resolves additional mappings via Wikidata SPARQL.
 * <p>
 * Intended for use by batch jobs and AI-agent resolution loops that enrich vertical
 * YAML configurations with taxonomy identifiers.
 */
@Service
public class TaxonomyMappingService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomyMappingService.class);

    private final VerticalsConfigService verticalsConfigService;
    private final WikidataSearchService wikidataSearchService;

    public TaxonomyMappingService(
            VerticalsConfigService verticalsConfigService,
            WikidataSearchService wikidataSearchService)
    {
        this.verticalsConfigService = verticalsConfigService;
        this.wikidataSearchService = wikidataSearchService;
    }

    /**
     * Returns all ETIM class IDs configured for the given vertical.
     *
     * @param verticalId vertical identifier
     * @return list of ETIM class ID strings (e.g. ["EC011604", "EC011573"]), never null
     */
    public List<String> getEtimClassIds(String verticalId)
    {
        VerticalConfig vc = verticalsConfigService.getConfigById(verticalId);
        if (vc == null || vc.getReferentials() == null)
        {
            return Collections.emptyList();
        }
        return vc.getReferentials().getEtim().stream()
                .map(EtimReferential::getClassId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * Returns all Wikidata Q-identifiers configured for the given vertical.
     *
     * @param verticalId vertical identifier
     * @return list of Q-id strings (e.g. ["Q174488"]), never null
     */
    public List<String> getWikidataQids(String verticalId)
    {
        VerticalConfig vc = verticalsConfigService.getConfigById(verticalId);
        if (vc == null || vc.getReferentials() == null)
        {
            return Collections.emptyList();
        }
        return vc.getReferentials().getWikidata().stream()
                .map(WikidataReferential::getQid)
                .filter(qid -> qid != null && !qid.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * Returns all Icecat category IDs configured for the given vertical.
     *
     * @param verticalId vertical identifier
     * @return list of category IDs, never null
     */
    public List<Integer> getIcecatCategoryIds(String verticalId)
    {
        VerticalConfig vc = verticalsConfigService.getConfigById(verticalId);
        if (vc == null || vc.getReferentials() == null)
        {
            return Collections.emptyList();
        }
        return vc.getReferentials().getIcecat().stream()
                .map(IcecatReferential::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
    }

    /**
     * Returns all Google Product Taxonomy IDs configured for the given vertical.
     *
     * @param verticalId vertical identifier
     * @return list of Google taxonomy IDs, never null
     */
    public List<Integer> getGoogleTaxonomyIds(String verticalId)
    {
        VerticalConfig vc = verticalsConfigService.getConfigById(verticalId);
        if (vc == null || vc.getReferentials() == null)
        {
            return Collections.emptyList();
        }
        return vc.getReferentials().getGoogleTaxonomy().stream()
                .map(GoogleTaxonomyReferential::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
    }

    /**
     * Resolves candidate ETIM class IDs for the given Icecat category ID via
     * Wikidata SPARQL.
     * <p>
     * Uses Wikidata property P4175 (Icecat category ID) and P11207 (ETIM code)
     * to traverse the graph. Returns an empty map when no mappings are found or
     * when the SPARQL endpoint is unavailable.
     *
     * @param icecatCategoryId the Icecat category ID to resolve
     * @return map of ETIM class ID → English label, possibly empty
     */
    public Map<String, String> resolveEtimFromIcecat(Integer icecatCategoryId)
    {
        if (icecatCategoryId == null || icecatCategoryId <= 0)
        {
            return Collections.emptyMap();
        }

        String sparql = """
                SELECT ?etimCode ?itemLabel WHERE {
                  ?item wdt:P4175 "%d" .
                  ?item wdt:P11207 ?etimCode .
                  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
                } LIMIT 10
                """.formatted(icecatCategoryId);

        try
        {
            List<Map<String, String>> rows = wikidataSearchService.executeSparql(sparql);
            return rows.stream()
                    .filter(row -> row.containsKey("etimCode"))
                    .collect(Collectors.toMap(
                            row -> row.get("etimCode"),
                            row -> row.getOrDefault("itemLabel", ""),
                            (a, b) -> a));
        }
        catch (Exception e)
        {
            LOGGER.warn("ETIM resolution via SPARQL failed for icecatId={}: {}", icecatCategoryId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Summarizes the referential completeness of all enabled verticals.
     * <p>
     * Returns a map of vertical ID → completeness flags for quick inspection by
     * the AI agent or admin tooling.
     *
     * @return map of vertical ID → set of taxonomies that have at least one mapping
     */
    public Map<String, List<String>> referentialCoverage()
    {
        return verticalsConfigService.getConfigs().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> coveredTaxonomies(e.getValue())));
    }

    private List<String> coveredTaxonomies(VerticalConfig vc)
    {
        if (vc.getReferentials() == null)
        {
            return Collections.emptyList();
        }
        TaxonomyReferentials ref = vc.getReferentials();
        List<String> covered = new java.util.ArrayList<>();
        if (!ref.getWikidata().isEmpty())
        {
            covered.add("wikidata");
        }
        if (!ref.getGoogleTaxonomy().isEmpty())
        {
            covered.add("google");
        }
        if (!ref.getIcecat().isEmpty())
        {
            covered.add("icecat");
        }
        if (!ref.getEtim().isEmpty())
        {
            covered.add("etim");
        }
        if (!ref.getEprel().isEmpty())
        {
            covered.add("eprel");
        }
        return covered;
    }
}
