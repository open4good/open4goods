package org.open4goods.pricehistory.model;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Currency;
import java.util.HexFormat;
import java.util.Objects;

import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceUsagePolicyRef;

/**
 * Immutable event emitted only when an offer changes semantic state.
 *
 * @param id deterministic delivery identity
 * @param key offer identity
 * @param kind transition kind
 * @param condition advertised condition after the transition
 * @param currency advertised currency after the transition
 * @param amount advertised amount after the transition
 * @param availability advertised availability after the transition
 * @param observedAt original O4G observation instant
 * @param persistenceTimestamp distinct TSDS timestamp derived from the observation
 * @param contentHash observation digest
 * @param policyRef usage policy applied to the event
 */
public record PriceChangeEvent(
        String id,
        OfferKey key,
        PriceChangeKind kind,
        OfferCondition condition,
        Currency currency,
        BigDecimal amount,
        OfferAvailability availability,
        Instant observedAt,
        Instant persistenceTimestamp,
        PayloadHash contentHash,
        SourceUsagePolicyRef policyRef) {

    /** Validates the sparse provider price event. */
    public PriceChangeEvent {
        if (id == null || !id.matches("price-event:[a-f0-9]{64}")) {
            throw new IllegalArgumentException("id must be a deterministic price-event digest");
        }
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(availability, "availability must not be null");
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        Objects.requireNonNull(persistenceTimestamp, "persistenceTimestamp must not be null");
        Objects.requireNonNull(contentHash, "contentHash must not be null");
        Objects.requireNonNull(policyRef, "policyRef must not be null");
    }

    /**
     * Creates a deterministic event and a distinct TSDS timestamp when necessary.
     *
     * <p>Elasticsearch derives a time-series identity from dimensions and timestamp. The digest
     * offset makes distinct same-observation changes persistable without relying on custom ids.
     *
     * @param observation accepted observation
     * @param kind transition kind
     * @return immutable event
     */
    public static PriceChangeEvent of(OfferObservation observation, PriceChangeKind kind) {
        Objects.requireNonNull(observation, "observation must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        String material = observation.key().externalForm() + "\u0000" + kind + "\u0000"
                + observation.contentHash().algorithm() + ":" + observation.contentHash().hexadecimalValue();
        String digest = sha256(material);
        long offsetNanos = Long.parseUnsignedLong(digest.substring(0, 8), 16) % 1_000_000_000L;
        Instant timestamp = observation.observedAt().plusNanos(offsetNanos);
        return new PriceChangeEvent("price-event:" + digest, observation.key(), kind, observation.condition(),
                observation.currency(), observation.amount(), observation.availability(), observation.observedAt(), timestamp,
                observation.contentHash(), observation.policyRef());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }
}
