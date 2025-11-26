package org.open4goods.nudgerfrontapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.nudgerfrontapi.config.properties.CacheProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.Cache;

import java.util.Arrays;
import java.time.Duration;

/**
 * Cache configuration backed by a bounded {@link Caffeine} cache to avoid
 * unbounded in-memory growth for frequently used caches such as
 * {@link CacheConstants#ONE_HOUR_LOCAL_CACHE_NAME}.
 */
@Configuration
public class CacheConfig {




    /**
     * Builds the cache manager with bounded Caffeine caches configured from properties.
     *
     * @param ticker          ticker used for cache eviction timing
     * @param cacheProperties cache sizing and TTL configuration
     * @return configured cache manager
     */
    @Bean
    CacheManager cacheManager(@Autowired final Ticker ticker, @Autowired final CacheProperties cacheProperties) {
        final CaffeineCache foreverCache = buildCache(CacheConstants.FOREVER_LOCAL_CACHE_NAME, ticker, cacheProperties.getForeverMaximumSize(), null);
        final CaffeineCache hourCache = buildCache(CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, ticker, cacheProperties.getOneHourMaximumSize(), cacheProperties.getOneHourTtl());
        final CaffeineCache minuteCache = buildCache(CacheConstants.ONE_MINUTE_LOCAL_CACHE_NAME, ticker, cacheProperties.getOneMinuteMaximumSize(), cacheProperties.getOneMinuteTtl());
        final CaffeineCache dayCache = buildCache(CacheConstants.ONE_DAY_LOCAL_CACHE_NAME, ticker, cacheProperties.getOneDayMaximumSize(), cacheProperties.getOneDayTtl());
        final SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(foreverCache, dayCache, hourCache, minuteCache));
        return manager;
    }

    /**
     * Builds a single Caffeine cache with the provided configuration.
     *
     * @param name        cache name
     * @param ticker      ticker used for time-based eviction
     * @param maximumSize maximum number of entries allowed in the cache
     * @param ttl         expiry duration, or {@code null} to keep entries indefinitely
     * @return configured {@link CaffeineCache}
     */
    private CaffeineCache buildCache(final String name, final Ticker ticker, final long maximumSize, final Duration ttl) {
        final Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .recordStats()
                .ticker(ticker)
                .maximumSize(maximumSize);
        if (ttl != null) {
            builder.expireAfterWrite(ttl);
        }
        return new CaffeineCache(name, builder.build());
    }

    /**
     * Exposes Micrometer metrics for all configured Caffeine caches to validate evictions under load.
     *
     * @param cacheManager cache manager containing the Caffeine caches
     * @return binder registering cache metrics
     */
    @Bean
    MeterBinder caffeineCacheMetricsBinder(@Autowired final CacheManager cacheManager) {
        return registry -> cacheManager.getCacheNames().forEach(cacheName -> {
            final Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                CaffeineCacheMetrics.monitor(registry, caffeineCache.getNativeCache(), cacheName);
            }
        });
    }

    /**
     * Shared ticker for all caches.
     *
     * @return system ticker
     */
    @Bean
    Ticker ticker() {
        return Ticker.systemTicker();
    }
}
