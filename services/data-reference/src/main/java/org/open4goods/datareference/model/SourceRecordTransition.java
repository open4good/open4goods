package org.open4goods.datareference.model;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Metadata-only journal entry for a source-record mutation.
 *
 * <p>This deliberately contains neither assertions nor provider payload fields. It is enough to
 * audit ordering and ask downstream projections to recompute affected GTINs.
 *
 * @param id deterministic journal identity
 * @param key provider record identity
 * @param revision monotonically increasing accepted-head revision
 * @param schemaVersion O4G source-head contract version
 * @param providerVersion provider record version, or {@code null} when unavailable
 * @param oldHash prior payload hash, or {@code null} for creation
 * @param newHash attempted payload hash
 * @param observedAt provider observation instant
 * @param retrievedAt O4G retrieval instant
 * @param state attempted lifecycle state
 * @param outcome mutation outcome
 * @param sanitizedErrorCode safe terminal error code, or {@code null}
 * @param affectedGtins ordered union of prior and new GTIN links
 */
public record SourceRecordTransition(
        String id,
        SourceRecordKey key,
        long revision,
        String schemaVersion,
        String providerVersion,
        PayloadHash oldHash,
        PayloadHash newHash,
        Instant observedAt,
        Instant retrievedAt,
        SourceRecordState state,
        SourceRecordTransitionOutcome outcome,
        String sanitizedErrorCode,
        List<Gtin> affectedGtins) {

    /** Validates transition metadata and safe journal contents. */
    public SourceRecordTransition {
        if (id == null || !id.matches("[a-z0-9._:-]{1,256}")) {
            throw new IllegalArgumentException("transition id must be a safe deterministic identifier");
        }
        Objects.requireNonNull(key, "key must not be null");
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be positive");
        }
        if (schemaVersion == null || schemaVersion.isBlank()) {
            throw new IllegalArgumentException("schemaVersion must not be blank");
        }
        schemaVersion = schemaVersion.trim();
        if (providerVersion != null && providerVersion.isBlank()) {
            throw new IllegalArgumentException("providerVersion must be absent rather than blank");
        }
        Objects.requireNonNull(newHash, "newHash must not be null");
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        if (sanitizedErrorCode != null && !sanitizedErrorCode.matches("[A-Z0-9_:-]{1,80}")) {
            throw new IllegalArgumentException("sanitizedErrorCode must be an upper-case safe code");
        }
        affectedGtins = List.copyOf(Objects.requireNonNull(affectedGtins, "affectedGtins must not be null"));
    }

    /**
     * Derives the stable journal id from the key and accepted revision.
     *
     * @param key source record identity
     * @param revision accepted transition revision
     * @return deterministic document identity
     */
    public static String idFor(SourceRecordKey key, long revision) {
        Objects.requireNonNull(key, "key must not be null");
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be positive");
        }
        try {
            String keyDigest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(key.externalForm().getBytes(StandardCharsets.UTF_8)));
            return "source-transition:" + key.sourceId().value() + ":" + keyDigest + ":" + revision;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }
}
