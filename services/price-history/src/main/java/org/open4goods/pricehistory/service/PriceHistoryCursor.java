package org.open4goods.pricehistory.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/** Opaque, URL-safe cursor encoding for ascending time then stable-id pagination. */
public final class PriceHistoryCursor {

    private static final String SEPARATOR = "\u0000";

    private PriceHistoryCursor() { }

    /** Encodes an exclusive position after the supplied stable sort tuple. */
    public static String after(Instant timestamp, String stableId) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        validateStableId(stableId);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (timestamp + SEPARATOR + stableId).getBytes(StandardCharsets.UTF_8));
    }

    /** Decodes a cursor created by {@link #after(Instant, String)}. */
    public static Position decode(String opaqueCursor) {
        if (opaqueCursor == null || opaqueCursor.isBlank()) {
            throw new IllegalArgumentException("cursor must be nonblank");
        }
        try {
            String value = new String(Base64.getUrlDecoder().decode(opaqueCursor), StandardCharsets.UTF_8);
            int separator = value.indexOf(SEPARATOR);
            if (separator < 1 || separator != value.lastIndexOf(SEPARATOR)) {
                throw new IllegalArgumentException("cursor is malformed");
            }
            String stableId = value.substring(separator + 1);
            validateStableId(stableId);
            return new Position(Instant.parse(value.substring(0, separator)), stableId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("cursor is malformed", exception);
        }
    }

    private static void validateStableId(String stableId) {
        if (stableId == null || stableId.isBlank() || stableId.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("stableId must be nonblank and contain no cursor separator");
        }
    }

    /** Decoded exclusive pagination position. */
    public record Position(Instant timestamp, String stableId) {
        /** Validates a decoded immutable sort tuple. */
        public Position {
            Objects.requireNonNull(timestamp, "timestamp must not be null");
            validateStableId(stableId);
        }
    }
}
