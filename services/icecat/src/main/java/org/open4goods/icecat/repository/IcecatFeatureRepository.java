package org.open4goods.icecat.repository;

import java.util.List;

import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for Icecat feature definitions.
 *
 * <p>Features are indexed from FeaturesList.xml and used to power
 * admin feature-search and attribute-name resolution.
 */
public interface IcecatFeatureRepository extends ElasticsearchRepository<IcecatFeatureDocument, Integer> {

    /**
     * Full-text search on the English feature name.
     *
     * @param query    search terms
     * @param pageable pagination
     * @return matching feature documents
     */
    Page<IcecatFeatureDocument> findByEnglishNameContaining(String query, Pageable pageable);

    /**
     * Finds features whose normalizedNames set contains the given value.
     * Used to rebuild the featuresByNames reverse-lookup from the index.
     *
     * @param normalizedName normalized attribute name (see IdHelper.normalizeAttributeName)
     * @return matching feature documents
     */
    @Query("""
            {
              "term": {
                "normalizedNames": "#{#normalizedName}"
              }
            }
            """)
    List<IcecatFeatureDocument> findByNormalizedName(String normalizedName);
}
