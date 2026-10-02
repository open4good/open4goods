package org.open4goods.sbadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

import de.codecentric.boot.admin.server.config.EnableAdminServer;

@SpringBootApplication
@EnableAdminServer
@EnableCaching
public class SbAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(SbAdminApplication.class, args);
	}

	/**
	 * Plain in-memory cache manager required by {@code @EnableCaching}. Previously supplied
	 * transitively by the now-removed xwiki-spring-boot-starter.
	 */
	@Bean
	CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}
}
