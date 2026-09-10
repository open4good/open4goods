package org.open4goods.nudgerfrontapi.config;

import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Configuration enabling the Elasticsearch repositories that back {@code IcecatIndexService}
 * (feature, category, feature-group and supplier reference data), required since
 * {@code IcecatService} reads feature names through them rather than an in-memory map.
 */
@Configuration
@EnableElasticsearchRepositories(basePackageClasses = IcecatFeatureRepository.class)
public class IcecatRepositoryConfig {
}
