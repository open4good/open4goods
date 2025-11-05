package org.open4goods.eprelservice.repository;

import org.open4goods.eprelservice.model.EprelProduct;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data repository for {@link EprelProduct} documents.
 */
public interface EprelProductRepository extends ElasticsearchRepository<EprelProduct, String>
{
}
