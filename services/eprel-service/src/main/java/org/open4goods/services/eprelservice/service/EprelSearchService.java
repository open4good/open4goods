package org.open4goods.services.eprelservice.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.services.eprelservice.config.EprelServiceProperties;
import org.open4goods.services.eprelservice.model.GtinHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Provides read access to the EPREL Elasticsearch index.
 * TODO : PErf : restict search on product defined vertical (against eprel associated categorie)if any
 */
@Service
public class EprelSearchService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EprelSearchService.class);

    private static final int MIN_PREFIX_LENGTH = 5;

    private final ElasticsearchOperations elasticsearchOperations;
    private final EprelServiceProperties properties;

    /**
     * Creates the search service.
     *
     * @param elasticsearchOperations Elasticsearch template
     */
    public EprelSearchService(ElasticsearchOperations elasticsearchOperations, EprelServiceProperties properties)
    {
        this.elasticsearchOperations = elasticsearchOperations;
        this.properties = properties;
    }

    /**
     * Finds products matching the provided GTIN.
     *
     * @param gtin textual representation of the GTIN
     * @return matching products ordered by natural Elasticsearch score
     */
    public List<EprelProduct> searchByGtin(String gtin)
    {
        return searchByGtin(gtin, null);
    }

    private List<EprelProduct> searchByGtin(String gtin, String eprelCategory)
    {
        Optional<Long> numericGtin = GtinHelper.toNumeric(gtin);
        if (numericGtin.isEmpty())
        {
            LOGGER.debug("Provided GTIN [{}] is not numeric", gtin);
            return List.of();
        }
        Query query = Query.of(q -> q.term(t -> t.field("numericGtin").value(numericGtin.get())));
        return execute(query, eprelCategory);
    }

    /**
     * Finds products whose model identifier matches the provided value exactly (case insensitive).
     *
     * @param model model identifier to search for
     * @return matching products
     */
    public List<EprelProduct> searchByExactModel(String model)
    {
        return searchByExactModel(model, null);
    }

    private List<EprelProduct> searchByExactModel(String model, String eprelCategory)
    {
        String normalized = normalize(model);
        if (normalized == null)
        {
            return List.of();
        }
        Query query = Query.of(q -> q.term(t -> t.field("modelIdentifier").value(normalized)));
        return execute(query, eprelCategory);
    }

    public List<EprelProduct> search(String gtin, String model)
    {
        return search(gtin, model, null);
    }

    public List<EprelProduct> search(String gtin, String model, String eprelCategory)
    {
        LOGGER.info("Searching by GTIN : {}", gtin);
        List<EprelProduct> results = searchByGtin(gtin, eprelCategory);
        if (!results.isEmpty())
        {
            LOGGER.info("Found {} result for GTIN : {}", results.size(), gtin);
            return results;
        }

        LOGGER.info("Searching by exact model : {}", model);
        results = searchByExactModel(model, eprelCategory);
        if (!results.isEmpty())
        {
            LOGGER.info("Found {} result by exact model : {}", results.size(), model);
            return results;
        }

        LOGGER.info("Searching by model prefix : {}", model);
        results = searchByModelPrefix(model, eprelCategory);
        if (!results.isEmpty())
        {
            LOGGER.info("Found {} result by model prefix : {}", results.size(), model);
            return results;
        }

        LOGGER.info("Searching by model best match : {}", model);
        results = searchByModelBestMatch(model, eprelCategory);
        if (!results.isEmpty())
        {
            LOGGER.warn("Found {} result by model best match : {}", results.size(), model);
            return results;
        }

        LOGGER.info("No eprel match for GTIN : {} , model : {}", gtin, model);
        return List.of();
    }


    public List<EprelProduct> search(String gtin, List<String> models)
    {
        return search(gtin, models, null);
    }

    public List<EprelProduct> search(String gtin, List<String> models, String eprelCategory)
    {
        List<String> candidates = sanitiseModelCandidates(models);

        LOGGER.info("Searching by GTIN : {}", gtin);
        List<EprelProduct> results = searchByGtin(gtin, eprelCategory);
        if (!results.isEmpty())
        {
            LOGGER.info("Found {} result for GTIN : {}", results.size(), gtin);
            return results;
        }

        for (String model : candidates)
        {
            LOGGER.info("Searching by exact model : {}", model);
            results = searchByExactModel(model, eprelCategory);
            if (!results.isEmpty())
            {
                LOGGER.info("Found {} result by exact model : {}", results.size(), model);
                return results;
            }
        }

        for (String model : candidates)
        {
            LOGGER.info("Searching by model prefix : {}", model);
            results = searchByModelPrefix(model, eprelCategory);
            if (!results.isEmpty())
            {
                LOGGER.info("Found {} result by model prefix : {}", results.size(), model);
                return results;
            }
        }

        for (String model : candidates)
        {
            LOGGER.info("Searching by model best match : {}", model);
            results = searchByModelBestMatch(model, eprelCategory);
            if (!results.isEmpty())
            {
                LOGGER.info("Found {} result by model best match : {}", results.size(), model);
                return results;
            }
        }

        LOGGER.warn("No eprel match for GTIN : {} , models : {}", gtin, candidates);
        return List.of();
    }


    /**
     * Finds products whose model identifier starts with the provided prefix (case insensitive).
     *
     * @param modelPrefix prefix to use for the search
     * @return matching products
     */
    public List<EprelProduct> searchByModelPrefix(String modelPrefix)
    {
        return searchByModelPrefix(modelPrefix, null);
    }

    private List<EprelProduct> searchByModelPrefix(String modelPrefix, String eprelCategory)
    {
        String normalized = normalize(modelPrefix);
        if (normalized == null)
        {
            return List.of();
        }
        Query query = Query.of(q -> q.prefix(p -> p.field("modelIdentifier").value(normalized)));
        return execute(query, eprelCategory);
    }



    /**
     * Performs an exact match, falling back to a prefix search.
     *
     * @param model model identifier to search for
     * @return matching products
     */
    public List<EprelProduct> searchByModel(String model)
    {
        List<EprelProduct> exactMatches = searchByExactModel(model);
        if (!exactMatches.isEmpty())
        {
            return exactMatches;
        }
        List<EprelProduct> prefixMatches = searchByModelPrefix(model);
        if (!prefixMatches.isEmpty())
        {
            return prefixMatches;
        }
        return searchByModelBestMatch(model);
    }

    private List<EprelProduct> searchByModelBestMatch(String model)
    {
        return searchByModelBestMatch(model, null);
    }

    private List<EprelProduct> searchByModelBestMatch(String model, String eprelCategory)
    {
        String normalized = normalize(model);
        if (normalized == null)
        {
            return List.of();
        }

        List<String> prefixes = buildPrefixes(normalized);
        if (prefixes.isEmpty())
        {
            return List.of();
        }

        Query query = Query.of(q -> q.bool(b ->
        {
            prefixes.stream()
                .map(prefix -> Query.of(inner -> inner.term(t -> t.field("modelIdentifier").value(prefix))))
                .forEach(b::should);
            b.minimumShouldMatch("1");
            return b;
        }));
        return execute(query, eprelCategory);
    }

    private List<String> buildPrefixes(String normalized)
    {
        List<String> prefixes = new ArrayList<>();
        if (normalized == null || normalized.isEmpty())
        {
            return prefixes;
        }

        int lowerBound = Math.min(normalized.length(), Math.max(MIN_PREFIX_LENGTH, 1));
        for (int length = normalized.length(); length >= lowerBound; length--)
        {
            prefixes.add(normalized.substring(0, length));
        }

        if (prefixes.isEmpty())
        {
            prefixes.add(normalized);
        }

        return prefixes;
    }

    private List<String> sanitiseModelCandidates(List<String> models)
    {
        if (models == null)
        {
            return List.of();
        }

        return models.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(candidate -> !candidate.isEmpty())
            .filter(candidate -> !shouldExcludeCandidate(candidate))
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();
    }

    private boolean shouldExcludeCandidate(String candidate)
    {
        long spaceCount = candidate.chars().filter(Character::isWhitespace).count();
        if (spaceCount > properties.getExcludeIfSpaces())
        {
            LOGGER.debug("Skipping model candidate [{}] because it contains {} spaces (limit = {})", candidate, spaceCount,
                properties.getExcludeIfSpaces());
            return true;
        }
        return false;
    }

    private List<EprelProduct> execute(Query query)
    {
        return execute(query, null);
    }

    private List<EprelProduct> execute(Query query, String eprelCategory)
    {
        Query finalQuery = query;
        if (hasCategory(eprelCategory))
        {
            finalQuery = Query.of(q -> q.bool(b ->
            {
                b.must(query);
                b.filter(f -> f.term(t -> t.field("eprelCategory").value(eprelCategory)));
                return b;
            }));
        }
        NativeQuery nativeQuery = NativeQuery.builder().withQuery(finalQuery).build();
        SearchHits<EprelProduct> hits = elasticsearchOperations.search(nativeQuery, EprelProduct.class);
        return hits.stream().map(SearchHit::getContent).toList();
    }

    private boolean hasCategory(String eprelCategory)
    {
        return eprelCategory != null && !eprelCategory.isBlank();
    }

    private String normalize(String value)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty())
        {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
