package org.open4goods.nudgerfrontapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.benmanes.caffeine.cache.Weigher;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.nudgerfrontapi.config.properties.CacheProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

/**
 * Cache configuration backed by byte-budgeted {@link Caffeine} caches.
 *
 * <p>Caches use {@link Caffeine#maximumWeight(long)} with a {@link CacheValueWeigher}
 * rather than count-based {@code maximumSize}: a single cached entry (e.g. a
 * search result holding 100 enriched product DTOs) can be megabytes, so capping
 * by entry count gives no OOM guarantee. Each cache is therefore sized in bytes
 * via {@link CacheProperties}.</p>
 */
@Configuration
public class CacheConfig {

    @Bean
    CacheManager cacheManager(@Autowired final Ticker ticker,
                              @Autowired final CacheProperties cacheProperties,
                              @Autowired final Weigher<Object, Object> cacheValueWeigher) {
        final CaffeineCache foreverCache = buildCache(CacheConstants.FOREVER_LOCAL_CACHE_NAME, ticker,
                cacheProperties.getForeverMaxBytes().toBytes(), null, cacheValueWeigher);
        final CaffeineCache hourCache = buildCache(CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, ticker,
                cacheProperties.getOneHourMaxBytes().toBytes(), cacheProperties.getOneHourTtl(), cacheValueWeigher);
        final CaffeineCache minuteCache = buildCache(CacheConstants.ONE_MINUTE_LOCAL_CACHE_NAME, ticker,
                cacheProperties.getOneMinuteMaxBytes().toBytes(), cacheProperties.getOneMinuteTtl(), cacheValueWeigher);
        final CaffeineCache dayCache = buildCache(CacheConstants.ONE_DAY_LOCAL_CACHE_NAME, ticker,
                cacheProperties.getOneDayMaxBytes().toBytes(), cacheProperties.getOneDayTtl(), cacheValueWeigher);
        final SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(foreverCache, dayCache, hourCache, minuteCache));
        return manager;
    }

    private CaffeineCache buildCache(final String name, final Ticker ticker, final long maxBytes,
                                     final Duration ttl, final Weigher<Object, Object> weigher) {
        final Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .recordStats()
                .ticker(ticker)
                .maximumWeight(maxBytes)
                .weigher(weigher);
        if (ttl != null) {
            builder.expireAfterWrite(ttl);
        }
        return new CaffeineCache(name, builder.build());
    }

    @Bean
    Weigher<Object, Object> cacheValueWeigher() {
        return new CacheValueWeigher();
    }

    @Bean
    MeterBinder caffeineCacheMetricsBinder(@Autowired final CacheManager cacheManager) {
        return registry -> cacheManager.getCacheNames().forEach(cacheName -> {
            final Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                CaffeineCacheMetrics.monitor(registry, caffeineCache.getNativeCache(), cacheName);
            }
        });
    }

    @Bean
    Ticker ticker() {
        return Ticker.systemTicker();
    }
}
