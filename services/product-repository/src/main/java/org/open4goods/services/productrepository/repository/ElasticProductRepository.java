package org.open4goods.services.productrepository.repository;

import java.util.List;

import org.open4goods.model.product.Product;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.annotations.SourceFilters;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 
 * @author goulven
 *
 */

public interface  ElasticProductRepository extends ElasticsearchRepository<Product, Long> {

    @SourceFilters(excludes = {"embedding", "resources.imageInfo.embedding"})
    List<Product> findByVerticalAndExcludedFalse(String vertical);

    long countByVertical(String vertical);

    long countByExcluded(boolean excluded);

    @Query(count = true, value = """
            {
              "bool": {
                "must": [
                  { "range": { "lastChange": { "gt": ?0 } } },
                  { "range": { "offersCount": { "gt": 0 } } }
                ]
              }
            }
            """)
    long countWithRecentPrices(long expirationEpochMs);


}
