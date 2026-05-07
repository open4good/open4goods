package org.open4goods.icecat.repository;

import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for Icecat product categories.
 *
 * <p>Categories are indexed from CategoriesList.xml and used to power
 * admin category search and fuzzy vertical-to-category matching.
 */
public interface IcecatCategoryRepository extends ElasticsearchRepository<IcecatCategoryDocument, Integer> {

    /**
     * Full-text search on the English category name.
     * Used for fuzzy matching when linking verticals to Icecat categories.
     *
     * @param query    search terms
     * @param pageable pagination
     * @return matching category documents
     */
    Page<IcecatCategoryDocument> findByEnglishNameContaining(String query, Pageable pageable);
}
