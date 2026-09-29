package org.open4goods.b2bapi.service.pricehistory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.open4goods.pricehistory.model.PriceHistoryGranularity;

/**
 * Opaque, client-facing price-history cursor that binds the internal Elasticsearch cursor to a
 * fingerprint of every query filter, so a cursor replayed against a different query fails fast as a
 * validation error (GOU-28 AC2 "cursor/query mismatch") instead of silently resuming a different
 * result set.
 */
public final class PriceHistoryPublicCursor {

    private static final char SEPARATOR = '\u0000';

    private PriceHistoryPublicCursor() {
    }

    /**
     * Encodes the opaque internal cursor together with a fingerprint of the query it was issued for.
     *
     * @param innerCursor opaque cursor returned by {@link org.open4goods.pricehistory.port.PriceHistoryQueryPort}
     * @param fingerprint query fingerprint from {@link #fingerprint}
     * @return opaque, URL-safe public cursor
     */
    public static String encode(final String innerCursor, final String fingerprint) {
        Objects.requireNonNull(innerCursor, "innerCursor must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        final String material = fingerprint + SEPARATOR + innerCursor;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(material.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a public cursor, verifying it was issued for the exact same query.
     *
     * @param opaqueCursor client-supplied cursor
     * @param expectedFingerprint fingerprint of the request currently being served
     * @return decoded internal cursor, when the fingerprint matches
     */
    public static Optional<String> decode(final String opaqueCursor, final String expectedFingerprint) {
        if (opaqueCursor == null || opaqueCursor.isBlank()) {
            return Optional.empty();
        }
        final String value;
        try {
            value = new String(Base64.getUrlDecoder().decode(opaqueCursor), StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException ex) {
            return Optional.empty();
        }
        final int separator = value.indexOf(SEPARATOR);
        if (separator < 1) {
            return Optional.empty();
        }
        final String fingerprint = value.substring(0, separator);
        final String innerCursor = value.substring(separator + 1);
        if (!fingerprint.equals(expectedFingerprint) || innerCursor.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(innerCursor);
    }

    /**
     * Computes a stable fingerprint over every filter that changes the served result set.
     *
     * @param gtin normalized GTIN
     * @param granularity effective granularity
     * @param providerId optional provider filter, or {@code null}
     * @param condition optional condition filter, or {@code null}
     * @param currency optional currency filter, or {@code null}
     * @param from inclusive UTC start
     * @param to exclusive UTC end
     * @param limit requested page size
     * @return lower-case hexadecimal SHA-256 fingerprint
     */
    public static String fingerprint(
            final String gtin,
            final PriceHistoryGranularity granularity,
            final String providerId,
            final String condition,
            final String currency,
            final Instant from,
            final Instant to,
            final int limit) {
        final String material = String.join("|",
                nullToEmpty(gtin),
                String.valueOf(granularity),
                nullToEmpty(providerId),
                nullToEmpty(condition),
                nullToEmpty(currency),
                String.valueOf(from),
                String.valueOf(to),
                String.valueOf(limit));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("the JDK must provide SHA-256", ex);
        }
    }

    private static String nullToEmpty(final String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
