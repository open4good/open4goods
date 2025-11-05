package org.open4goods.services.eprelservice.repository;

import org.open4goods.services.eprelservice.model.EprelProduct;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data repository for {@link EprelProduct} documents.
 */
public interface EprelProductRepository extends ElasticsearchRepository<EprelProduct, String>
{
}
