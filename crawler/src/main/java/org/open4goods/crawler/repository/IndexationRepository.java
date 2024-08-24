package org.open4goods.crawler.repository;

import org.open4goods.commons.model.crawlers.WebIndexationStats;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * This "blank" repository is useless, but it is the model trhat allows allows "easy" schema recreation for verticals (see VerticalGeneration.createIndex)
 * @author goulven
 *
 */

public interface  IndexationRepository extends ElasticsearchRepository<WebIndexationStats, String> {



}
