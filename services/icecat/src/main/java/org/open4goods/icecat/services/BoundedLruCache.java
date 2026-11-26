package org.open4goods.icecat.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Bounded, least-recently-used cache of Elasticsearch lookups, safe between threads.
 *
 * <p>The lock is held only to read or write the entries, never while a value is loaded
 * ({@link #getOrLoad(Object, Function)}): a slow Elasticsearch query does not hold back the
 * other lookups, unlike {@code Collections.synchronizedMap(...).computeIfAbsent}, which kept
 * the lock of the whole map for the time of the query (GOU-2280). Two threads that miss the
 * same key at the same time both load it; the first value stored wins, and both return it.
 *
 * @param <K> key type
 * @param <V> value type
 */
final class BoundedLruCache<K, V> {

    /** Entries in access order, the least recently used one first. Guarded by {@code this}. */
    private final LinkedHashMap<K, V> entries;

    /** Incremented by {@link #clear()}. Guarded by {@code this}. */
    private long generation;

    /**
     * @param maxSize number of entries above which the least recently used one is evicted
     */
    BoundedLruCache(final int maxSize) {
        this.entries = new LinkedHashMap<>(256, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        };
    }

    /**
     * Returns the cached value of {@code key}, or loads it outside the lock and caches it.
     * A {@code null} value is returned but never cached. A value whose load started before a
     * {@link #clear()} is returned but not cached, since it may come from the data the clear
     * discards.
     *
     * @param key    key to look up
     * @param loader called on a miss, without holding the lock
     * @return the cached or loaded value, possibly {@code null}
     */
    V getOrLoad(final K key, final Function<? super K, ? extends V> loader) {
        final long loadGeneration;
        synchronized (this) {
            V cached = entries.get(key);
            if (cached != null) {
                return cached;
            }
            loadGeneration = generation;
        }

        V loaded = loader.apply(key);
        if (loaded == null) {
            return null;
        }

        synchronized (this) {
            if (loadGeneration != generation) {
                return loaded;
            }
            V stored = entries.putIfAbsent(key, loaded);
            return stored != null ? stored : loaded;
        }
    }

    /**
     * @param key key to look up
     * @return the cached value, or {@code null}
     */
    synchronized V get(final K key) {
        return entries.get(key);
    }

    /**
     * Caches a value, evicting the least recently used entry above the maximum size.
     *
     * @param key   key
     * @param value value to cache
     */
    synchronized void put(final K key, final V value) {
        entries.put(key, value);
    }

    /** Removes every entry; a load in progress is then not cached. */
    synchronized void clear() {
        entries.clear();
        generation++;
    }

    /**
     * @return current number of entries
     */
    synchronized int size() {
        return entries.size();
    }
}
