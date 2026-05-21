
package org.open4goods.api;

import org.open4goods.brand.repository.BrandScoresRepository;
import org.open4goods.crawler.controller.CrawlController;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.model.CacheKeyGenerator;
import org.open4goods.services.eprelservice.repository.EprelProductRepository;
import org.open4goods.services.productrepository.repository.ElasticProductRepository;
import org.open4goods.services.wikidataservice.repository.WikidataEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Main entry point for the open4goods API application. */
@SpringBootApplication(scanBasePackages = {"org.open4goods.services"}, scanBasePackageClasses = { Api.class, CrawlController.class, CacheKeyGenerator.class})
@EnableScheduling
@EnableElasticsearchRepositories(basePackageClasses = {ElasticProductRepository.class, IndexationRepository.class, BrandScoresRepository.class, EprelProductRepository.class, IcecatFeatureRepository.class, WikidataEntityRepository.class})
@EnableCaching
public class Api {

    protected static final Logger logger = LoggerFactory.getLogger(Api.class);

    public static void main(final String[] args) {
        SpringApplication.run(Api.class, args);
    }

}
