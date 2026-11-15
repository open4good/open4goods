package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * {@link BoundedLruCache} (GOU-2280): the bound and the eviction order of the former synchronized
 * {@code LinkedHashMap}, with the value loaded outside the lock.
 */
class BoundedLruCacheTest {

    @Test
    void missIsLoadedOnceThenServedFromTheCache() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(10);
        AtomicInteger loads = new AtomicInteger();

        assertThat(cache.getOrLoad("A", key -> key + loads.incrementAndGet())).isEqualTo("A1");
        assertThat(cache.getOrLoad("A", key -> key + loads.incrementAndGet())).isEqualTo("A1");

        assertThat(loads).hasValue(1);
    }

    @Test
    void leastRecentlyUsedEntryIsEvictedAboveTheMaximumSize() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);
        cache.put("A", "a");
        cache.put("B", "b");
        cache.get("A");

        cache.getOrLoad("C", key -> "c");

        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.get("B")).isNull();
        assertThat(cache.get("A")).isEqualTo("a");
        assertThat(cache.get("C")).isEqualTo("c");
    }

    @Test
    void nullIsReturnedButNotCached() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(10);
        AtomicInteger loads = new AtomicInteger();

        assertThat(cache.getOrLoad("A", key -> {
            loads.incrementAndGet();
            return null;
        })).isNull();
        cache.getOrLoad("A", key -> {
            loads.incrementAndGet();
            return null;
        });

        assertThat(loads).hasValue(2);
        assertThat(cache.size()).isZero();
    }

    @Test
    void valueStoredByAnotherThreadDuringTheLoadWins() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(10);

        String returned = cache.getOrLoad("A", key -> {
            cache.put("A", "first");
            return "second";
        });

        assertThat(returned).isEqualTo("first");
        assertThat(cache.get("A")).isEqualTo("first");
    }

    @Test
    void valueLoadedBeforeAClearIsReturnedButNotCached() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(10);

        String returned = cache.getOrLoad("A", key -> {
            cache.clear();
            return "read before the reimport";
        });

        assertThat(returned).isEqualTo("read before the reimport");
        assertThat(cache.size()).isZero();
        assertThat(cache.getOrLoad("A", key -> "read after the reimport")).isEqualTo("read after the reimport");
    }
}
