package org.open4goods.datareference.model;

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The current, replaceable truth O4G holds about one provider record.
 *
 * <p>Only the head is needed to replay current truth. Superseded provider values
 * are not kept: the journal records that a transition happened, with hashes and
 * outcomes, not the payloads it replaced.
 *
 * <p>Two chronologies are tracked because they answer different questions.
 * {@code observedAt} is the instant the provider's data describes and decides
 * which of two heads is newer; {@code retrievedAt} is when O4G fetched it and
 * decides staleness. A provider that republishes yesterday's data today is newer
 * by retrieval and older by observation, and ordering by the wrong one silently
 * reinstates stale values.
 *
 * <p>An empty assertion list is meaningful, not degenerate: a {@code FULL} head
 * with no assertions states that the source now asserts nothing for this record.
 *
 * @param key identity of the provider record
 * @param schemaVersion O4G contract version this head was written against
 * @param providerVersion provider's own version of the record, or {@code null}
 * @param observedAt instant the provider data describes
 * @param retrievedAt instant O4G retrieved it
 * @param expiresAt last instant the head may be used, or {@code null} when open-ended
 * @param completeness whether the head replaces all assertions or only those it names
 * @param state lifecycle state of the record
 * @param payloadHash digest of the retrieved payload, used for idempotence
 * @param evidenceReference durable proof pointer holding no provider payload
 * @param usagePolicyRef policy in force when the head was retrieved
 * @param gtinLinks ordered attachments to product identities
 * @param assertions ordered assertions carried by this head
 */
public record SourceRecordHead(
        SourceRecordKey key,
        String schemaVersion,
        String providerVersion,
        Instant observedAt,
        Instant retrievedAt,
        Instant expiresAt,
        SourceRecordCompleteness completeness,
        SourceRecordState state,
        PayloadHash payloadHash,
        URI evidenceReference,
        SourceUsagePolicyRef usagePolicyRef,
        List<GtinLink> gtinLinks,
        List<SourceAssertion> assertions) {

    /**
     * Validates identity, chronology, ordering and coordinate uniqueness.
     */
    public SourceRecordHead {
        Objects.requireNonNull(key, "key must not be null");
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        if (providerVersion != null && providerVersion.isBlank()) {
            throw new IllegalArgumentException("providerVersion must be absent rather than blank");
        }
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");
        Objects.requireNonNull(completeness, "completeness must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(payloadHash, "payloadHash must not be null");
        Objects.requireNonNull(evidenceReference, "evidenceReference must not be null");
        Objects.requireNonNull(usagePolicyRef, "usagePolicyRef must not be null");
        if (retrievedAt.isBefore(observedAt)) {
            throw new IllegalArgumentException("retrievedAt must not precede observedAt");
        }
        if (expiresAt != null && expiresAt.isBefore(retrievedAt)) {
            throw new IllegalArgumentException("expiresAt must not precede retrievedAt");
        }

        gtinLinks = List.copyOf(Objects.requireNonNull(gtinLinks, "gtinLinks must not be null"));
        Set<Gtin> linkedGtins = new HashSet<>();
        for (GtinLink link : gtinLinks) {
            Objects.requireNonNull(link, "gtinLinks must not contain null");
            if (!linkedGtins.add(link.gtin())) {
                throw new IllegalArgumentException("duplicate GTIN link: " + link.gtin());
            }
        }

        assertions = List.copyOf(Objects.requireNonNull(assertions, "assertions must not be null"));
        Set<SourceAssertion.Coordinate> coordinates = new HashSet<>();
        for (SourceAssertion assertion : assertions) {
            Objects.requireNonNull(assertion, "assertions must not contain null");
            if (!coordinates.add(assertion.coordinate())) {
                throw new IllegalArgumentException(
                        "duplicate assertion coordinate: " + assertion.field() + "#" + assertion.ordinal());
            }
            // An assertion id is derived, not allocated, and a projection uses it as
            // the audit trail back to this head. A well-formed id belonging to another
            // record would make that trail point at a record that never said this.
            AssertionId expected = AssertionId.of(key, assertion.field(), assertion.ordinal());
            if (!expected.equals(assertion.assertionId())) {
                throw new IllegalArgumentException("assertion id " + assertion.assertionId()
                        + " does not match its coordinate, expected " + expected);
            }
        }
    }

    /**
     * Reports whether this head can supersede another head of the same record.
     *
     * <p>Ordering is by observation, so a re-retrieval of older provider data is
     * journalled as ignored rather than reinstated. A repeated payload hash is
     * idempotent and supersedes nothing.
     *
     * @param current head currently stored, or {@code null} when the record is new
     * @return {@code true} when this head should replace {@code current}
     */
    public boolean supersedes(SourceRecordHead current) {
        if (current == null) {
            return true;
        }
        if (!current.key().equals(key)) {
            throw new IllegalArgumentException("cannot compare heads of different records");
        }
        if (current.payloadHash().equals(payloadHash)) {
            return false;
        }
        return observedAt.isAfter(current.observedAt());
    }

    /**
     * Reports whether the head is usable at an instant.
     *
     * @param instant instant to test
     * @return {@code true} when the record is active and not expired
     */
    public boolean isUsableAt(Instant instant) {
        Objects.requireNonNull(instant, "instant must not be null");
        return state == SourceRecordState.ACTIVE && (expiresAt == null || !instant.isAfter(expiresAt));
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
}
