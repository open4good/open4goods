package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.evidence.ScalarEvidence;

/** Unit coverage for mutation intent that cannot be inferred from a source head alone. */
class SourceRecordMutationTest {

    @Test
    void rejectsAnAmbiguousEmptyPartialUpdate() {
        SourceRecordHead partial = head(SourceRecordCompleteness.PARTIAL, SourceRecordState.ACTIVE, List.of(), List.of());

        assertThatThrownBy(() -> new SourceRecordMutation(partial, List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ambiguous");
    }

    @Test
    void requiresASafeErrorCodeForTerminalAttempts() {
        SourceRecordHead unavailable = head(SourceRecordCompleteness.FULL, SourceRecordState.UNAVAILABLE, List.of(), List.of());

        assertThatThrownBy(() -> new SourceRecordMutation(unavailable, List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sanitized error code");
        assertThat(new SourceRecordMutation(unavailable, List.of(), List.of(), "HTTP_503").sanitizedErrorCode())
                .isEqualTo("HTTP_503");
    }

    @Test
    void rejectsEvidenceOnATerminalAttempt() {
        SourceRecordKey key = SourceRecordKey.of("fixture", "record-1");
        SourceFieldId field = new SourceFieldId("fixture", "brand", "1");
        SourceAssertion assertion = SourceAssertion.of(key, field, 0, SourceContentType.IDENTITY, ScalarEvidence.of("Acme"));
        SourceRecordHead unavailable = head(SourceRecordCompleteness.FULL, SourceRecordState.UNAVAILABLE, List.of(),
                List.of(assertion));

        assertThatThrownBy(() -> new SourceRecordMutation(unavailable, List.of(), List.of(), "HTTP_503"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not carry assertions");
    }

    @Test
    void transitionIdentityIsStableAndKeySpecific() {
        SourceRecordKey key = SourceRecordKey.of("fixture", "record-1");

        assertThat(SourceRecordTransition.idFor(key, 2)).isEqualTo(SourceRecordTransition.idFor(key, 2));
        assertThat(SourceRecordTransition.idFor(key, 2)).isNotEqualTo(SourceRecordTransition.idFor(key, 3));
        assertThat(SourceRecordTransition.idFor(key, 2)).doesNotContain("record-1");
    }

    private static SourceRecordHead head(SourceRecordCompleteness completeness, SourceRecordState state,
            List<GtinLink> links, List<SourceAssertion> assertions) {
        Instant instant = Instant.parse("2026-09-15T00:00:00Z");
        return new SourceRecordHead(SourceRecordKey.of("fixture", "record-1"), "1", null, instant, instant, null,
                completeness, state, new PayloadHash("SHA-256", "aa"), URI.create("urn:test:record-1"),
                new SourceUsagePolicyRef("fixture", "1"), links, assertions);
    }
}
