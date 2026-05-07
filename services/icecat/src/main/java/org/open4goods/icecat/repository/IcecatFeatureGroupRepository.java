package org.open4goods.icecat.repository;

import org.open4goods.icecat.model.IcecatFeatureGroupDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for Icecat feature groups.
 *
 * <p>Feature groups are indexed from FeatureGroupsList.xml and link
 * related features (e.g. "Display", "Connectivity") within a category.
 */
public interface IcecatFeatureGroupRepository extends ElasticsearchRepository<IcecatFeatureGroupDocument, Integer> {

    /**
     * Full-text search on the English feature group name.
     *
     * @param query    search terms
     * @param pageable pagination
     * @return matching feature group documents
     */
    Page<IcecatFeatureGroupDocument> findByEnglishNameContaining(String query, Pageable pageable);
}
