package org.open4goods.services.eprelservice.service;

import java.util.ArrayList;
import java.util.Collection;
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

import co.elastic.clients.elasticsearch._types.FieldValue;
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

    public List<EprelProduct> searchByGtin(String gtin, Collection<String> eprelCategories)
    {
        Optional<Long> numericGtin = GtinHelper.toNumeric(gtin);
        if (numericGtin.isEmpty())
        {
            LOGGER.debug("Provided GTIN [{}] is not numeric", gtin);
            return List.of();
        }
        Query query = Query.of(q -> q.term(t -> t.field("numericGtin").value(numericGtin.get())));
        return execute(query, eprelCategories);
    }

    /**
     * Finds all EPREL products for a shared model core identifier.
     *
     * @param modelCoreId model core identifier
     * @param eprelCategories optional EPREL categories to restrict the search
     * @return matching products
     */
    public List<EprelProduct> searchByProductModelCoreId(Long modelCoreId, Collection<String> eprelCategories)
    {
        if (modelCoreId == null)
        {
            return List.of();
        }
        Query query = Query.of(q -> q.term(t -> t.field("productModelCoreId").value(modelCoreId)));
        return execute(query, eprelCategories);
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

    private List<EprelProduct> searchByExactModel(String model, Collection<String> eprelCategories)
    {
        String normalized = normalize(model);
        if (normalized == null)
        {
            return List.of();
        }
        Query query = Query.of(q -> q.term(t -> t.field("modelIdentifier").value(normalized)));
        return execute(query, eprelCategories);
    }

    public List<EprelProduct> search(String gtin, String model)
    {
        return search(gtin, model, (Collection<String>) null);
    }

    public List<EprelProduct> search(String gtin, String model, String eprelCategory)
    {
        return search(gtin, model, eprelCategory == null ? null : List.of(eprelCategory));
    }

    public List<EprelProduct> search(String gtin, String model, Collection<String> eprelCategories)
    {
        LOGGER.info("Searching by GTIN : {}", gtin);
        List<EprelProduct> results = searchByGtin(gtin, eprelCategories);
        if (!results.isEmpty())
        {
            LOGGER.info("Found {} result for GTIN : {}", results.size(), gtin);
            return results;
        }

        LOGGER.info("Searching by exact model : {}", model);
        results = searchByExactModel(model, eprelCategories);
        if (!results.isEmpty())
        {
            LOGGER.info("Found {} result by exact model : {}", results.size(), model);
            return results;
        }

        LOGGER.info("Searching by model prefix : {}", model);
        results = searchByModelPrefix(model, eprelCategories);
        if (!results.isEmpty())
        {
            LOGGER.info("Found {} result by model prefix : {}", results.size(), model);
            return results;
        }

        LOGGER.info("Searching by model best match : {}", model);
        results = searchByModelBestMatch(model, eprelCategories);
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
        return search(gtin, models, (Collection<String>) null);
    }

    public List<EprelProduct> search(String gtin, List<String> models, String eprelCategory)
    {
        return search(gtin, models, eprelCategory == null ? null : List.of(eprelCategory));
    }

    public List<EprelProduct> search(String gtin, List<String> models, Collection<String> eprelCategories)
    {
        List<String> candidates = sanitiseModelCandidates(models);

        LOGGER.info("Searching by GTIN : {}", gtin);
        List<EprelProduct> results = searchByGtin(gtin, eprelCategories);
        if (!results.isEmpty())
        {
            LOGGER.info("Found {} result for GTIN : {}", results.size(), gtin);
            return results;
        }

        for (String model : candidates)
        {
            LOGGER.info("Searching by exact model : {}", model);
            results = searchByExactModel(model, eprelCategories);
            if (!results.isEmpty())
            {
                LOGGER.info("Found {} result by exact model : {}", results.size(), model);
                return results;
            }
        }

        for (String model : candidates)
        {
            LOGGER.info("Searching by model prefix : {}", model);
            results = searchByModelPrefix(model, eprelCategories);
            if (!results.isEmpty())
            {
                LOGGER.info("Found {} result by model prefix : {}", results.size(), model);
                return results;
            }
        }

        for (String model : candidates)
        {
            LOGGER.info("Searching by model best match : {}", model);
            results = searchByModelBestMatch(model, eprelCategories);
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

    private List<EprelProduct> searchByModelPrefix(String modelPrefix, Collection<String> eprelCategories)
    {
        String normalized = normalize(modelPrefix);
        if (normalized == null)
        {
            return List.of();
        }
        Query query = Query.of(q -> q.prefix(p -> p.field("modelIdentifier").value(normalized)));
        return execute(query, eprelCategories);
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

    private List<EprelProduct> searchByModelBestMatch(String model, Collection<String> eprelCategories)
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
        return execute(query, eprelCategories);
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

    private List<String> sanitiseCategories(Collection<String> categories)
    {
        if (categories == null)
        {
            return List.of();
        }

        return categories.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
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

    private List<EprelProduct> execute(Query query, Collection<String> eprelCategories)
    {
        List<String> categories = sanitiseCategories(eprelCategories);
        Query finalQuery = query;
        if (!categories.isEmpty())
        {
            List<FieldValue> values = categories.stream()
                .map(FieldValue::of)
                .toList();

            finalQuery = Query.of(q -> q.bool(b ->
            {
                b.must(query);
                b.filter(f -> f.terms(t -> t.field("productGroup").terms(v -> v.value(values))));
                return b;
            }));
        }
        NativeQuery nativeQuery = NativeQuery.builder().withQuery(finalQuery).build();
        SearchHits<EprelProduct> hits = elasticsearchOperations.search(nativeQuery, EprelProduct.class);
        return hits.stream().map(SearchHit::getContent).toList();
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
