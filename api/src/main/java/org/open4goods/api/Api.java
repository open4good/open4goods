
package org.open4goods.api;

import java.io.IOException;

import org.open4goods.crawler.controller.CrawlController;
import org.open4goods.dao.ProductRepository;
import org.open4goods.services.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.Unirest;

import jakarta.annotation.PostConstruct;



@SpringBootApplication (scanBasePackageClasses = {Api.class, CrawlController.class})

@EnableAspectJAutoProxy
@EnableScheduling
@EnableElasticsearchRepositories(basePackageClasses = ProductRepository.class)
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
