package org.open4goods.nudgerfrontapi.controller.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link AdminController}.
 */
class AdminControllerTest {

    private Cache cacheOne;
    private Cache cacheTwo;
    private CacheManager cacheManager;
    private AdminController controller;

    @BeforeEach
    void setUp() {
        cacheOne = new ConcurrentMapCache("one");
        cacheTwo = new ConcurrentMapCache("two");
        cacheOne.put("key", "value");
        cacheTwo.put("another", "entry");

        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        simpleCacheManager.setCaches(List.of(cacheOne, cacheTwo));
        simpleCacheManager.initializeCaches();
        cacheManager = simpleCacheManager;

        controller = new AdminController(cacheManager);
    }

    @Test
    void resetCacheShouldClearEveryCacheEntry() {
        ResponseEntity<Void> response = controller.resetCache();

        assertThat(cacheOne.get("key")).isNull();
        assertThat(cacheTwo.get("another")).isNull();
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
