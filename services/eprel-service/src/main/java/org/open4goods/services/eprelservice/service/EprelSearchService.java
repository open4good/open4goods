package org.open4goods.services.eprelservice.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.open4goods.services.eprelservice.model.EprelProduct;
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
 */
@Service
public class EprelSearchService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EprelSearchService.class);

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Creates the search service.
     *
     * @param elasticsearchOperations Elasticsearch template
     */
    public EprelSearchService(ElasticsearchOperations elasticsearchOperations)
    {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * Finds products matching the provided GTIN.
     *
     * @param gtin textual representation of the GTIN
     * @return matching products ordered by natural Elasticsearch score
     */
    public List<EprelProduct> searchByGtin(String gtin)
    {
        Optional<Long> numericGtin = GtinHelper.toNumeric(gtin);
        if (numericGtin.isEmpty())
        {
            LOGGER.debug("Provided GTIN [{}] is not numeric", gtin);
            return List.of();
        }
        Query query = Query.of(q -> q.term(t -> t.field("numericGtin").value(numericGtin.get())));
        return execute(query);
    }

    /**
     * Finds products whose model identifier matches the provided value exactly (case insensitive).
     *
     * @param model model identifier to search for
     * @return matching products
     */
    public List<EprelProduct> searchByExactModel(String model)
    {
        String normalized = normalize(model);
        if (normalized == null)
        {
            return List.of();
        }
        Query query = Query.of(q -> q.match(m -> m.field("modelIdentifier").query(normalized)));
        return execute(query);
    }

    public List<EprelProduct> search(String gtin, String model)
    {

    	LOGGER.info("Searching by GTIN : {}", gtin);
    	List<EprelProduct> results = searchByGtin(gtin);

    	if (null != results && results.size() > 0) {
        	LOGGER.info("Found {} result for GTIN : {}", results.size(), gtin);
    		return results;
    	}

    	LOGGER.info("Searching by exact model : {}", model);
    	results = searchByExactModel(model);

    	if (null != results && results.size() > 0) {
    		LOGGER.info("Found {} result by exact model : {}", results.size(), model);
    		return results;
    	}

    	LOGGER.info("Searching by model prefix : {}", model);
    	results = searchByModelPrefix(model);

    	if (null != results && results.size() > 0) {
    		LOGGER.info("Found {} result by model prefix : {}", results.size(), model);
    		return results;
    	}
       	LOGGER.info("No eprel match for GTIN : {} , model : {} ", gtin, model);
    	return List.of();

    }


    public List<EprelProduct> search(String gtin, List<String> models)
    {

    	   List<String> bySizeModels = models.stream()
    		        .sorted((a, b) -> Integer.compare(b.length(), a.length()))
    		        .collect(Collectors.toList());

    	LOGGER.info("Searching by GTIN : {}", gtin);
    	List<EprelProduct> results = searchByGtin(gtin);

    	if (null != results && results.size() > 0) {
        	LOGGER.info("Found {} result for GTIN : {}", results.size(), gtin);
    		return results;
    	}


    	for (String model : bySizeModels) {
	    	LOGGER.info("Searching by exact model : {}", model);
	    	results = searchByExactModel(model);

	    	if (null != results && results.size() > 0) {
	    		LOGGER.info("Found {} result by exact model : {}", results.size(), model);
	    		return results;
	    	}

	    	LOGGER.info("Searching by model prefix : {}", model);
	    	results = searchByModelPrefix(model);

	    	if (null != results && results.size() > 0) {
	    		LOGGER.info("Found {} result by model prefix : {}", results.size(), model);
	    		return results;
	    	}
    	}
    	LOGGER.info("No eprel match for GTIN : {} , models : {} ", gtin, bySizeModels);
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
        String normalized = normalize(modelPrefix);
        if (normalized == null)
        {
            return List.of();
        }
        Query query = Query.of(q -> q.prefix(p -> p.field("modelIdentifier").value(normalized)));
        return execute(query);
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
        return searchByModelPrefix(model);
    }

    private List<EprelProduct> execute(Query query)
    {
        NativeQuery nativeQuery = NativeQuery.builder().withQuery(query).build();
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
