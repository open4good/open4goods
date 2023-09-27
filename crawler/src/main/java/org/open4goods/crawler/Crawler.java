
package org.open4goods.crawler;

import java.io.IOException;

import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.services.SerialisationService;
import org.open4goods.store.repository.ProductSpringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.Unirest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@SpringBootApplication
@EnableScheduling
@EnableElasticsearchRepositories(basePackageClasses = IndexationRepository.class)
public class Crawler {

	protected static final Logger logger = LoggerFactory.getLogger(Crawler.class);

//	private @Autowired SerialisationService serialisationService;

	public static void main(final String[] args) {
		SpringApplication.run(Crawler.class, args);
	}

	@PreDestroy
	public void shutdown() {
		try {
			logger.info("Shutdown Unirest Client");
			Unirest.shutdown();
		} catch (final IOException e) {
			logger.error("Cannot shutdown Unirest Client", e);
		}
	}

//	@PostConstruct
//	private void init() {
//		Unirest.setObjectMapper(new  com.mashape.unirest.http.ObjectMapper() {
//
//
//			@Override
//			public <T> T readValue(final String value, final Class<T> valueType) {
//				try {
//					return serialisationService.getJsonMapper().readValue(value, valueType);
//				} catch (final IOException e) {
//					throw new RuntimeException(e);
//				}
//			}
//
//			@Override
//			public String writeValue(final Object value) {
//				try {
//					return serialisationService.getJsonMapper().writeValueAsString(value);
//				} catch (final JsonProcessingException e) {
//					throw new RuntimeException(e);
//				}
//			}
//		});
//
//
//	}



}
