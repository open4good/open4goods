package org.open4goods.services.wikidataservice.repository;

import java.util.List;

import org.open4goods.services.wikidataservice.model.WikidataEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for {@link WikidataEntity} documents.
 */
public interface WikidataEntityRepository extends ElasticsearchRepository<WikidataEntity, String> {

    /**
     * Finds entities that contain the specified GTIN in their {@code gtins} list.
     *
     * @param gtin the GTIN to search for
     * @return matching entities
     */
    List<WikidataEntity> findByGtins(String gtin);
}
