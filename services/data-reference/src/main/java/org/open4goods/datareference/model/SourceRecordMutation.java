package org.open4goods.datareference.model;

import java.util.List;
import java.util.Objects;

/**
 * One attempted replacement of a source-record head.
 *
 * <p>A mutation keeps partial-update intent separate from {@link SourceRecordHead}: a stored
 * head is always a complete representation of current truth, whereas a partial input only names
 * coordinates to add or remove. This prevents an adapter from accidentally treating a missing
 * assertion as a deletion.
 *
 * @param candidate provider observation being applied
 * @param tombstones assertion coordinates explicitly removed by a partial update
 * @param withdrawnGtins GTIN attachments explicitly removed by a partial update
 * @param sanitizedErrorCode stable, non-sensitive reason for an unavailable or rejected attempt
 */
public record SourceRecordMutation(
        SourceRecordHead candidate,
        List<SourceAssertion.Coordinate> tombstones,
        List<Gtin> withdrawnGtins,
        String sanitizedErrorCode) {

    /**
     * Validates partial-update intent and terminal-state shape.
     */
    public SourceRecordMutation {
        Objects.requireNonNull(candidate, "candidate must not be null");
        tombstones = List.copyOf(Objects.requireNonNull(tombstones, "tombstones must not be null"));
        withdrawnGtins = List.copyOf(Objects.requireNonNull(withdrawnGtins, "withdrawnGtins must not be null"));
        if (sanitizedErrorCode != null && !sanitizedErrorCode.matches("[A-Z0-9_:-]{1,80}")) {
            throw new IllegalArgumentException("sanitizedErrorCode must be an upper-case safe code");
        }
        if (candidate.completeness() == SourceRecordCompleteness.FULL
                && (!tombstones.isEmpty() || !withdrawnGtins.isEmpty())) {
            throw new IllegalArgumentException("a FULL replacement must not carry removals");
        }
        if (candidate.completeness() == SourceRecordCompleteness.PARTIAL
                && candidate.assertions().isEmpty() && candidate.gtinLinks().isEmpty()
                && tombstones.isEmpty() && withdrawnGtins.isEmpty()) {
            throw new IllegalArgumentException("an empty PARTIAL update is ambiguous");
        }
        if (candidate.state() == SourceRecordState.DELETED
                && (!candidate.assertions().isEmpty() || !candidate.gtinLinks().isEmpty())) {
            throw new IllegalArgumentException("a DELETED head must not carry assertions or GTIN links");
        }
        if (candidate.state() == SourceRecordState.UNAVAILABLE || candidate.state() == SourceRecordState.REJECTED) {
            if (sanitizedErrorCode == null) {
                throw new IllegalArgumentException("terminal attempt requires a sanitized error code");
            }
            if (!candidate.assertions().isEmpty() || !candidate.gtinLinks().isEmpty()) {
                throw new IllegalArgumentException("terminal attempt must not carry assertions or GTIN links");
            }
        } else if (sanitizedErrorCode != null) {
            throw new IllegalArgumentException("only terminal attempts carry a sanitized error code");
        }
    }

    /**
     * Creates a full replacement.
     *
     * @param candidate replacement head
     * @return validated mutation
     */
    public static SourceRecordMutation full(SourceRecordHead candidate) {
        return new SourceRecordMutation(candidate, List.of(), List.of(), null);
    }
}
