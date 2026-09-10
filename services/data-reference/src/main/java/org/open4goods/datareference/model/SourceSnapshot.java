package org.open4goods.datareference.model;

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Atomic, replayable observation of one provider record for one GTIN.
 *
 * <p>An empty assertion list is valid and materializes removal of all assertions
 * from the prior snapshot with the same {@link #key()}.
 *
 * @param gtin canonical product identity
 * @param source source identifier
 * @param sourceRecordId provider record identifier
 * @param schemaVersion provider schema version
 * @param observedAt instant represented by the provider data
 * @param retrievedAt instant at which O4G retrieved the data
 * @param expiresAt last instant at which the snapshot may be used, or {@code null}
 * @param payloadHash digest of the retrieved payload
 * @param usagePolicyRef policy governing storage and projection
 * @param evidenceReference durable proof pointer without an embedded source payload
 * @param assertions complete assertions for this provider record
 */
public record SourceSnapshot(
        Gtin gtin,
        String source,
        String sourceRecordId,
        String schemaVersion,
        Instant observedAt,
        Instant retrievedAt,
        Instant expiresAt,
        PayloadHash payloadHash,
        SourceUsagePolicyRef usagePolicyRef,
        URI evidenceReference,
        List<SourceAssertion> assertions) {

    /**
     * Validates snapshot identity, chronology, and multi-value coordinates.
     */
    public SourceSnapshot {
        Objects.requireNonNull(gtin, "gtin must not be null");
        source = requireText(source, "source");
        sourceRecordId = requireText(sourceRecordId, "sourceRecordId");
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");
        Objects.requireNonNull(payloadHash, "payloadHash must not be null");
        Objects.requireNonNull(usagePolicyRef, "usagePolicyRef must not be null");
        Objects.requireNonNull(evidenceReference, "evidenceReference must not be null");
        if (expiresAt != null && expiresAt.isBefore(retrievedAt)) {
            throw new IllegalArgumentException("expiresAt must not precede retrievedAt");
        }
        assertions = List.copyOf(Objects.requireNonNull(assertions, "assertions must not be null"));
        Set<AssertionCoordinate> coordinates = new HashSet<>();
        for (SourceAssertion assertion : assertions) {
            Objects.requireNonNull(assertion, "assertions must not contain null");
            if (!coordinates.add(new AssertionCoordinate(assertion.field(), assertion.ordinal()))) {
                throw new IllegalArgumentException("duplicate assertion field and ordinal: " + assertion.field());
            }
        }
    }

    /**
     * Returns the replaceable identity used by the assertion store.
     *
     * @return snapshot key
     */
    public SourceSnapshotKey key() {
        return new SourceSnapshotKey(gtin, source, sourceRecordId);
    }

    /**
     * Validates a required textual component.
     *
     * @param value component value
     * @param name component name
     * @return trimmed value
     */
    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    /** Coordinate that makes repeated values explicit and deterministic. */
    private record AssertionCoordinate(SourceFieldId field, int ordinal) {
    }
}
