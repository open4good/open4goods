package org.open4goods.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.services.BatchService;
import org.open4goods.api.services.DatasourcePurgeService;
import org.open4goods.api.services.ProductIndexMappingRunner;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 * The beta and prod config set {@code org.open4goods} to warn, which hid every INFO line of the datasource purge
 * (GOU-1815). Boots Spring with the same config locations and profiles as {@code publish-jars.sh}
 * ({@code classpath:/application.yml}, then the environment file) and checks that the purge logger still logs INFO
 * while the rest of {@code org.open4goods} stays at warn. Same for the products index mapping result line (GOU-1835).
 */
class DatasourcePurgeLoggingConfigTest {

	/** Levels set on the shared logback context before the test, restored after it. */
	private final Map<String, Level> levels = new HashMap<>();

	@BeforeEach
	void saveLevels() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLoggerList().forEach(logger -> levels.put(logger.getName(), logger.getLevel()));
	}

	@AfterEach
	void restoreLevels() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLoggerList().forEach(logger -> logger.setLevel(levels.get(logger.getName())));
	}

	@Test
	void purgeLoggerLogsInfoDespiteTheProdWarnLevel() {
		SpringApplication application = new SpringApplication(EmptyConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setBannerMode(Banner.Mode.OFF);

		try (ConfigurableApplicationContext ignored = application.run(
				"--spring.config.location=classpath:/application.yml,classpath:/logging/prod-log-levels.yml",
				"--spring.profiles.active=nudger,prod",
				"--spring.docker.compose.enabled=false",
				"--logging.register-shutdown-hook=false")) {
			Logger purge = (Logger) LoggerFactory.getLogger(DatasourcePurgeService.class);
			Logger mapping = (Logger) LoggerFactory.getLogger(ProductIndexMappingRunner.class);
			Logger batch = (Logger) LoggerFactory.getLogger(BatchService.class);

			assertThat(purge.getEffectiveLevel()).isEqualTo(Level.INFO);
			assertThat(purge.isInfoEnabled()).isTrue();
			// GOU-1835: the products index mapping result line too
			assertThat(mapping.getEffectiveLevel()).isEqualTo(Level.INFO);
			// The prod override is really applied: other services of the package stay at warn
			assertThat(batch.getEffectiveLevel()).isEqualTo(Level.WARN);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {
	}
}
