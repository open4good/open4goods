package org.open4goods.crawler.repository;

import org.open4goods.model.crawlers.FetchCsvStats;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * This "blank" repository is useless, but it is the model trhat allows allows "easy" schema recreation for verticals (see VerticalGeneration.createIndex)
 * @author goulven
 *
 */

public interface  CsvIndexationRepository extends ElasticsearchRepository<FetchCsvStats, String> {



}
