package org.open4goods.pricehistory.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Opaque, URL-safe cursor encoding for ascending time then stable sort-key-tuple pagination.
 *
 * <p>The number of sort keys must match the arity of the Elasticsearch {@code sort}/{@code search_after}
 * clause it was captured from - {@code queryChanges} sorts on one stable key ({@code event_id}) while
 * {@code queryDaily} sorts on three ({@code provider_id}, {@code condition}, {@code currency}), so a
 * fixed single-value cursor cannot represent both without Elasticsearch rejecting the mismatched
 * {@code search_after} arity.
 */
public final class PriceHistoryCursor {

    private static final String SEPARATOR = "\u0000";

    private PriceHistoryCursor() { }

    /** Encodes an exclusive position after the supplied stable sort tuple. */
    public static String after(final Instant timestamp, final String... sortKeys) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        if (sortKeys == null || sortKeys.length == 0) {
            throw new IllegalArgumentException("at least one sort key is required");
        }
        final StringBuilder material = new StringBuilder(timestamp.toString());
        for (final String sortKey : sortKeys) {
            validateSortKey(sortKey);
            material.append(SEPARATOR).append(sortKey);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                material.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Decodes a cursor created by {@link #after(Instant, String...)}. */
    public static Position decode(final String opaqueCursor) {
        if (opaqueCursor == null || opaqueCursor.isBlank()) {
            throw new IllegalArgumentException("cursor must be nonblank");
        }
        try {
            final String value = new String(Base64.getUrlDecoder().decode(opaqueCursor), StandardCharsets.UTF_8);
            final String[] parts = value.split(SEPARATOR, -1);
            if (parts.length < 2) {
                throw new IllegalArgumentException("cursor is malformed");
            }
            final Instant timestamp = Instant.parse(parts[0]);
            final List<String> sortKeys = Arrays.asList(parts).subList(1, parts.length);
            sortKeys.forEach(PriceHistoryCursor::validateSortKey);
            return new Position(timestamp, List.copyOf(sortKeys));
        } catch (final IllegalArgumentException | java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("cursor is malformed", exception);
        }
    }

    private static void validateSortKey(final String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            throw new IllegalArgumentException("sort keys must be nonblank");
        }
    }

    /** Decoded exclusive pagination position: a timestamp followed by one or more stable sort keys. */
    public record Position(Instant timestamp, List<String> sortKeys) {
        /** Validates a decoded immutable sort tuple. */
        public Position {
            Objects.requireNonNull(timestamp, "timestamp must not be null");
            if (sortKeys == null || sortKeys.isEmpty()) {
                throw new IllegalArgumentException("at least one sort key is required");
            }
            sortKeys.forEach(PriceHistoryCursor::validateSortKey);
        }
    }
}
