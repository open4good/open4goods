
package org.open4goods.ui;

import java.io.IOException;

import org.open4goods.dao.ProductRepository;
import org.open4goods.services.SerialisationService;
import org.open4goods.store.repository.elastic.BrandScoresRepository;
import org.open4goods.store.repository.elastic.ElasticProductRepository;
import org.open4goods.store.repository.redis.RedisProductRepository;
import org.open4goods.ui.repository.ContributionVoteRepository;
import org.open4goods.ui.repository.UserSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@SpringBootApplication(scanBasePackages = "org.open4goods")
@EnableScheduling
@EnableCaching
@Configuration
@EnableElasticsearchRepositories(basePackageClasses = {ContributionVoteRepository.class, UserSearchRepository.class, ElasticProductRepository.class, BrandScoresRepository.class})
@EnableRedisRepositories(basePackageClasses = RedisProductRepository.class)
public class Ui {

	private static final Logger logger = LoggerFactory.getLogger(Ui.class);

	@Autowired
	private SerialisationService serialisationService;

	public static void main(final String[] args) {
		System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

		SpringApplication.run(Ui.class, args);
	}

	// TODO (p3,design,1) : remove all unirests call and dependencies, in flavor of spring "nativs" libraries
	@PostConstruct
	private void init() {
		Unirest.setObjectMapper(new ObjectMapper() {
			@Override
			public <T> T readValue(final String value, final Class<T> valueType) {
				try {
					return serialisationService.fromJson(value, valueType);
				} catch (final Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public String writeValue(final Object value) {
				try {
					return serialisationService.toJson(value);
				} catch (final Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
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

}
