
package org.open4goods.api;

import java.io.IOException;

import org.open4goods.commons.config.CacheKeyGenerator;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.commons.store.repository.elastic.BrandScoresRepository;
import org.open4goods.commons.store.repository.elastic.ElasticProductRepository;
import org.open4goods.commons.store.repository.redis.RedisProductRepository;
import org.open4goods.crawler.controller.CrawlController;
import org.open4goods.crawler.repository.IndexationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.Unirest;

import jakarta.annotation.PostConstruct;



@SpringBootApplication (scanBasePackageClasses = { Api.class, CrawlController.class, CacheKeyGenerator.class})

@EnableScheduling
@EnableElasticsearchRepositories(basePackageClasses = {ElasticProductRepository.class, IndexationRepository.class, BrandScoresRepository.class})
@EnableRedisRepositories(basePackageClasses = RedisProductRepository.class)
@EnableCaching

public abstract class Api {

	/** The Constant logger. */
	protected static final Logger logger = LoggerFactory.getLogger(Api.class);

	/** The serialisation service. */
	private @Autowired SerialisationService serialisationService;



	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(final String[] args) {
		SpringApplication.run(Api.class, args);
	}

	/**
	 * Inits the.
	 */
	@PostConstruct
	private void init() {
		Unirest.setObjectMapper(new  com.mashape.unirest.http.ObjectMapper() {


			@Override
			public <T> T readValue(final String value, final Class<T> valueType) {
				try {
					return serialisationService.getJsonMapper().readValue(value, valueType);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public String writeValue(final Object value) {
				try {
					return serialisationService.getJsonMapper().writeValueAsString(value);
				} catch (final JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

}
