package org.open4goods.icecat.repository;

import org.open4goods.icecat.model.IcecatSupplierDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for Icecat suppliers (brands/manufacturers).
 *
 * <p>Suppliers are indexed from SuppliersList.xml and store brand logo URLs
 * for use in product enrichment.
 */
public interface IcecatSupplierRepository extends ElasticsearchRepository<IcecatSupplierDocument, Integer> {

    /**
     * Full-text search on the supplier/brand name.
     *
     * @param query    search terms
     * @param pageable pagination
     * @return matching supplier documents
     */
    Page<IcecatSupplierDocument> findByNameContaining(String query, Pageable pageable);
}
