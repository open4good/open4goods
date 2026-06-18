package org.open4goods.services.eprelservice.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.util.ProductModelCandidateHelper;
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
 *
 * <p>Search strategy (in order, stopping at first non-empty result):
 * <ol>
 *   <li>Exact GTIN match</li>
 *   <li>Exact model identifier match</li>
 *   <li>Prefix model match</li>
 *   <li>Best-match (progressive prefix terms)</li>
 *   <li>Contains match (wildcard {@code *model*}) — catches cases where EPREL
 *       prepends a category prefix, e.g. our {@code N3B3HTX} vs EPREL {@code CA6 N3B3HTX}</li>
 * </ol>
 */
@Service
public class EprelSearchService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EprelSearchService.class);

    // Aligned with EprelCompletionService.MIN_COMPACT_MODEL_CONTAINMENT_LENGTH to avoid
    // sending broad prefix queries whose results would always score 0 and be rejected.
    private static final int MIN_PREFIX_LENGTH = 7;

    /** Minimum number of alphanumeric characters required for a contains/wildcard search. */
    private static final int MIN_CONTAINS_ALNUM_LENGTH = 5;

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

    /**
     * Finds products matching the provided GTIN, optionally restricted to specific EPREL categories.
     *
     * @param gtin textual representation of the GTIN
     * @param eprelCategories optional category restriction
     * @return matching products
     */
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

    /**
     * Finds products whose model identifier matches the provided value case-insensitively,
     * ignoring spaces and typical punctuation separators.
     *
     * @param model model identifier to search for
     * @return matching products
     */
    public List<EprelProduct> searchBySpaceInsensitiveModel(String model)
    {
        return searchBySpaceInsensitiveModel(model, null);
    }

    private List<EprelProduct> searchBySpaceInsensitiveModel(String model, Collection<String> eprelCategories)
    {
        String normalized = normalize(model);
        if (normalized == null || normalized.length() < 4)
        {
            return List.of();
        }
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++)
        {
            char c = normalized.charAt(i);
            if (Character.isLetterOrDigit(c))
            {
                if (regex.length() > 0)
                {
                    regex.append("[-_/ ]*");
                }
                regex.append(c);
            }
        }
        if (regex.length() == 0)
        {
            return List.of();
        }
        Query query = Query.of(q -> q.regexp(r -> r.field("modelIdentifier").value(regex.toString())));
        return execute(query, eprelCategories);
    }

    /**
     * Filters the provided EPREL products by brand, matching {@code supplierOrTrademark}
     * against the supplied brand string (case-insensitive containment check).
     *
     * <p>If no results survive the filter the original list is returned unchanged so that
     * callers always have something to work with.
     *
     * @param results EPREL products to filter
     * @param brand   product brand from the catalogue (may be {@code null})
     * @return brand-filtered list, or the original list when the filter yields nothing
     */
    public List<EprelProduct> filterByBrand(List<EprelProduct> results, String brand)
    {
        if (brand == null || brand.isBlank() || results.isEmpty())
        {
            return results;
        }
        String normalizedBrand = brand.toLowerCase(Locale.ROOT).trim();
        List<EprelProduct> brandMatches = results.stream()
            .filter(p -> {
                String supplier = p.getSupplierOrTrademark();
                if (supplier == null)
                {
                    return false;
                }
                String normalizedSupplier = supplier.toLowerCase(Locale.ROOT);
                return normalizedSupplier.contains(normalizedBrand) || normalizedBrand.contains(normalizedSupplier);
            })
            .toList();
        if (brandMatches.isEmpty())
        {
            LOGGER.debug("Brand filter [{}] matched nothing out of {} results, keeping all", brand, results.size());
            return results;
        }
        return brandMatches;
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

        LOGGER.info("Searching by space-insensitive model : {}", model);
        results = searchBySpaceInsensitiveModel(model, eprelCategories);
        if (!results.isEmpty())
        {
            LOGGER.info("Found {} result by space-insensitive model : {}", results.size(), model);
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

        LOGGER.info("Searching by model contains : {}", model);
        results = searchByModelContains(model, eprelCategories);
        if (!results.isEmpty())
        {
            LOGGER.info("Found {} result by model contains : {}", results.size(), model);
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

    /**
     * Searches EPREL by GTIN then by model candidates, trying each strategy in turn
     * and returning on the first non-empty result set.
     *
     * @param gtin GTIN of the product
     * @param models all known model identifiers for the product
     * @param eprelCategories EPREL product-group names to restrict the search
     * @return matching EPREL products, or an empty list when nothing is found
     */
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
            LOGGER.info("Searching by space-insensitive model : {}", model);
            results = searchBySpaceInsensitiveModel(model, eprelCategories);
            if (!results.isEmpty())
            {
                LOGGER.info("Found {} result by space-insensitive model : {}", results.size(), model);
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

        for (String model : candidates)
        {
            LOGGER.info("Searching by model contains : {}", model);
            results = searchByModelContains(model, eprelCategories);
            if (!results.isEmpty())
            {
                LOGGER.info("Found {} result by model contains : {}", results.size(), model);
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
     * Finds products whose model identifier contains the provided string (case insensitive wildcard
     * search: {@code *model*}).
     *
     * <p>This catches cases where EPREL stores an additional prefix or suffix around the model code,
     * for example our {@code N3B3HTX} matching EPREL's {@code CA6 N3B3HTX}.
     *
     * <p>Only executed when the alphanumeric content of the model is at least
     * {@value #MIN_CONTAINS_ALNUM_LENGTH} characters to avoid overly broad matches.
     *
     * @param model model identifier fragment to search for
     * @return matching products
     */
    public List<EprelProduct> searchByModelContains(String model)
    {
        return searchByModelContains(model, null);
    }

    private List<EprelProduct> searchByModelContains(String model, Collection<String> eprelCategories)
    {
        String normalized = normalize(model);
        if (normalized == null)
        {
            return List.of();
        }
        long alnumCount = normalized.chars().filter(Character::isLetterOrDigit).count();
        if (alnumCount < MIN_CONTAINS_ALNUM_LENGTH)
        {
            LOGGER.debug("Skipping contains search for [{}]: only {} alphanumeric chars (min={})",
                model, alnumCount, MIN_CONTAINS_ALNUM_LENGTH);
            return List.of();
        }
        Query query = Query.of(q -> q.wildcard(w -> w.field("modelIdentifier").value("*" + normalized + "*")));
        return execute(query, eprelCategories);
    }

    /**
     * Performs an exact match, falling back to a prefix search and then a contains search.
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
        List<EprelProduct> spaceInsensitiveMatches = searchBySpaceInsensitiveModel(model);
        if (!spaceInsensitiveMatches.isEmpty())
        {
            return spaceInsensitiveMatches;
        }
        List<EprelProduct> prefixMatches = searchByModelPrefix(model);
        if (!prefixMatches.isEmpty())
        {
            return prefixMatches;
        }
        List<EprelProduct> bestMatches = searchByModelBestMatch(model);
        if (!bestMatches.isEmpty())
        {
            return bestMatches;
        }
        return searchByModelContains(model);
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

    /**
     * Sanitises and ranks model candidates before searching.
     *
     * <p>In addition to the existing space-count filter, this method now also discards:
     * <ul>
     *   <li>Purely numeric strings — these are typically order reference IDs, not model codes</li>
     *   <li>Strings with fewer than 3 alphanumeric characters — too generic to yield useful matches</li>
     *   <li>Dimension codes such as {@code 568X500X430MM} or {@code L400XP700XH850}</li>
     * </ul>
     */
    private List<String> sanitiseModelCandidates(List<String> models)
    {
        return ProductModelCandidateHelper.sanitise(models, properties.getExcludeIfSpaces(),
                properties.getMinAlnumLength());
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
        NativeQuery nativeQuery = NativeQuery.builder().withQuery(finalQuery).withMaxResults(properties.getMaxSearchResults()).build();
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
