package org.open4goods.pricehistory.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable, separately shaped legacy minimum backfill excluded from public queries by default. */
public record LegacyPriceBackfill(String id, LegacyMinimumPricePoint point) {

    /** Validates the deterministic legacy identifier. */
    public LegacyPriceBackfill {
        if (id == null || !id.matches("legacy-price:[a-f0-9]{64}")) {
            throw new IllegalArgumentException("id must be a deterministic legacy-price digest");
        }
        Objects.requireNonNull(point, "point must not be null");
    }

    /** Creates a repeat-safe backfill from a legacy source coordinate. */
    public static LegacyPriceBackfill of(LegacyMinimumPricePoint point) {
        Objects.requireNonNull(point, "point must not be null");
        String material = point.gtin().value() + "\u0000" + point.condition() + "\u0000" + point.currency().getCurrencyCode()
                + "\u0000" + point.amount().toPlainString() + "\u0000" + point.recordedAt() + "\u0000"
                + point.sourceCoordinate();
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8)));
            return new LegacyPriceBackfill("legacy-price:" + digest, point);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }
}
