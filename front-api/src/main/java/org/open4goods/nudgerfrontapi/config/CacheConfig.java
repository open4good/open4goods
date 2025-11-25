package org.open4goods.nudgerfrontapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.nudgerfrontapi.config.properties.CacheProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cache configuration backed by a bounded {@link Caffeine} cache to avoid
 * unbounded in-memory growth for frequently used caches such as
 * {@link CacheConstants#ONE_HOUR_LOCAL_CACHE_NAME}.
 */
@Configuration
public class CacheConfig {

    /**
     * Configures the {@link CacheManager} to use Caffeine with explicit TTL and
     * maximum size limits driven by {@link CacheProperties}. The configuration is
     * shared by all caches, with {@link CacheConstants#ONE_HOUR_LOCAL_CACHE_NAME}
     * pre-registered for search and product reference usage.
     *
     * @param cacheProperties cache tuning properties read from {@code front.cache.*}
     * @return a cache manager backed by bounded Caffeine caches
     */
    @Bean
    public CacheManager cacheManager(CacheProperties cacheProperties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of(CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(cacheProperties.getOneHourTtl())
                .maximumSize(cacheProperties.getOneHourMaximumSize()));
        return cacheManager;
    }
}
