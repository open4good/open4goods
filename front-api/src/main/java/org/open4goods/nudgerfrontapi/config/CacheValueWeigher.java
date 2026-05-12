package org.open4goods.nudgerfrontapi.config;

import com.github.benmanes.caffeine.cache.Weigher;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

/**
 * Weigher used by the Caffeine caches to estimate the retained byte cost of a
 * cached value. The byte size is approximated from the reachable object graph
 * via {@link GraphLayout#parseInstance(Object)}.
 *
 * <p>The weight is approximate: shared sub-graphs (e.g. interned strings,
 * singleton configuration referenced by a DTO) are counted on each cache entry
 * that reaches them, which slightly over-estimates retention. That is the safe
 * direction for an OOM defence.</p>
 *
 * <p>If {@code jol} fails to walk a value (e.g. JVM-internal types behind
 * modules) the weigher falls back to {@link #FALLBACK_WEIGHT_BYTES} to avoid
 * polluting the cache with a zero weight.</p>
 */
public final class CacheValueWeigher implements Weigher<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheValueWeigher.class);

    /** Approximate byte weight used when reflection-based sizing fails. */
    static final int FALLBACK_WEIGHT_BYTES = 4_096;

    /** Minimum weight, so a null/empty payload still costs one unit. */
    static final int MIN_WEIGHT_BYTES = 1;

    @Override
    public int weigh(final Object key, final Object value) {
        if (value == null) {
            return MIN_WEIGHT_BYTES;
        }
        final Object payload = unwrap(value);
        if (payload == null) {
            return MIN_WEIGHT_BYTES;
        }
        try {
            final long totalSize = GraphLayout.parseInstance(payload).totalSize();
            if (totalSize <= 0) {
                return MIN_WEIGHT_BYTES;
            }
            return (int) Math.min(totalSize, Integer.MAX_VALUE);
        } catch (final Throwable t) {
            LOGGER.debug("Weigher could not size {} ({}); falling back to {} bytes",
                    payload.getClass().getName(), t.getMessage(), FALLBACK_WEIGHT_BYTES);
            return FALLBACK_WEIGHT_BYTES;
        }
    }

    private static Object unwrap(final Object value) {
        if (value instanceof ResponseEntity<?> entity) {
            return entity.getBody();
        }
        return value;
    }
}
