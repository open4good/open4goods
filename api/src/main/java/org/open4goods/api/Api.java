
package org.open4goods.api;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.open4goods.config.ESConfig;
import org.open4goods.services.SerialisationService;
import org.open4goods.store.repository.DataFragmentRepository;
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

import springfox.documentation.swagger2.annotations.EnableSwagger2;



@SpringBootApplication (scanBasePackageClasses = {Api.class, ESConfig.class})

@EnableAspectJAutoProxy
@EnableScheduling
@EnableElasticsearchRepositories(basePackageClasses = DataFragmentRepository.class)
@EnableCaching
@EnableSwagger2

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
