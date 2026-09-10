package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.evidence.ScalarEvidence;

/**
 * Validation and replacement rules of the replaceable record head.
 */
class SourceRecordHeadTest {

    private static final SourceRecordKey KEY = SourceRecordKey.of("icecat", "REC-1");
    private static final Instant OBSERVED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant RETRIEVED = Instant.parse("2026-01-02T00:00:00Z");
    private static final long RETRIEVAL_DELAY_SECONDS = 86_400L;
    private static final SourceFieldId FIELD = new SourceFieldId("icecat", "1234", "v2");

    private static SourceRecordHead head(Instant observedAt, String hash, List<SourceAssertion> assertions) {
        return new SourceRecordHead(
                KEY,
                "1",
                "icecat-2026-01",
                observedAt,
                observedAt.plusSeconds(RETRIEVAL_DELAY_SECONDS),
                null,
                SourceRecordCompleteness.FULL,
                SourceRecordState.ACTIVE,
                new PayloadHash("SHA-256", hash),
                URI.create("urn:o4g:evidence:1"),
                new SourceUsagePolicyRef("icecat-standard", "3"),
                List.of(new GtinLink(new Gtin("4006381333931"), GtinMatchConfidence.EXACT,
                        GtinMatchMethod.DECLARED_IDENTIFIER, null)),
                assertions);
    }

    private static SourceAssertion assertion(int ordinal, String value) {
        return SourceAssertion.of(KEY, FIELD, ordinal, SourceContentType.ATTRIBUTE, ScalarEvidence.of(value));
    }

    @Test
    void acceptsAFullHeadWithNoAssertion() {
        SourceRecordHead empty = head(OBSERVED, "aa", List.of());

        // A FULL head with no assertion states that the source now asserts nothing.
        assertThat(empty.assertions()).isEmpty();
        assertThat(empty.completeness()).isEqualTo(SourceRecordCompleteness.FULL);
    }

    @Test
    void rejectsRetrievalBeforeObservation() {
        assertThatThrownBy(() -> new SourceRecordHead(KEY, "1", null,
                Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"), null,
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, new PayloadHash("SHA-256", "aa"),
                URI.create("urn:o4g:evidence:1"), new SourceUsagePolicyRef("p", "1"), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retrievedAt must not precede observedAt");
    }

    @Test
    void rejectsExpiryBeforeRetrieval() {
        assertThatThrownBy(() -> new SourceRecordHead(KEY, "1", null, OBSERVED, RETRIEVED,
                Instant.parse("2026-01-01T12:00:00Z"),
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, new PayloadHash("SHA-256", "aa"),
                URI.create("urn:o4g:evidence:1"), new SourceUsagePolicyRef("p", "1"), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt must not precede retrievedAt");
    }

    @Test
    void rejectsTwoAssertionsAtTheSameCoordinate() {
        assertThatThrownBy(() -> head(OBSERVED, "aa", List.of(assertion(0, "black"), assertion(0, "noir"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate assertion coordinate");
    }

    @Test
    void acceptsRepeatedValuesOfTheSameFieldAtDistinctOrdinals() {
        SourceRecordHead multi = head(OBSERVED, "aa", List.of(assertion(0, "black"), assertion(1, "white")));

        assertThat(multi.assertions()).hasSize(2);
        // Provider order is preserved: the ordinals are positions, not a ranking.
        assertThat(multi.assertions()).extracting(SourceAssertion::ordinal).containsExactly(0, 1);
    }

    @Test
    void rejectsTheSameGtinLinkedTwice() {
        assertThatThrownBy(() -> new SourceRecordHead(KEY, "1", null, OBSERVED, RETRIEVED, null,
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, new PayloadHash("SHA-256", "aa"),
                URI.create("urn:o4g:evidence:1"), new SourceUsagePolicyRef("p", "1"),
                List.of(new GtinLink(new Gtin("4006381333931"), GtinMatchConfidence.EXACT,
                                GtinMatchMethod.DECLARED_IDENTIFIER, null),
                        new GtinLink(new Gtin("4006381333931"), GtinMatchConfidence.WEAK,
                                GtinMatchMethod.TEXT_EVIDENCE, null)),
                List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate GTIN link");
    }

    @Test
    void rejectsAnAssertionClaimingAnotherRecordsIdentity() {
        SourceAssertion foreign = new SourceAssertion(
                AssertionId.of(SourceRecordKey.of("eprel", "OTHER"), FIELD, 0),
                FIELD, 0, SourceContentType.ATTRIBUTE, ScalarEvidence.of("black"));

        assertThatThrownBy(() -> head(OBSERVED, "aa", List.of(foreign)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match its coordinate");
    }

    @Test
    void rejectsAnAssertionClaimingAnotherCoordinate() {
        SourceAssertion mislabelled = new SourceAssertion(
                AssertionId.of(KEY, FIELD, 7), FIELD, 0, SourceContentType.ATTRIBUTE,
                ScalarEvidence.of("black"));

        assertThatThrownBy(() -> head(OBSERVED, "aa", List.of(mislabelled)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match its coordinate");
    }

    @Test
    void aNewerObservationSupersedesTheStoredHead() {
        SourceRecordHead stored = head(OBSERVED, "aa", List.of());
        SourceRecordHead newer = head(Instant.parse("2026-03-01T00:00:00Z"), "bb", List.of());

        assertThat(newer.supersedes(stored)).isTrue();
        assertThat(stored.supersedes(null)).isTrue();
    }

    @Test
    void anOlderObservationIsNotReinstated() {
        SourceRecordHead stored = head(Instant.parse("2026-03-01T00:00:00Z"), "bb", List.of());
        SourceRecordHead older = head(OBSERVED, "aa", List.of());

        assertThat(older.supersedes(stored)).isFalse();
    }

    @Test
    void arepeatedPayloadHashIsIdempotent() {
        SourceRecordHead stored = head(OBSERVED, "aa", List.of());
        SourceRecordHead sameContentObservedLater = head(Instant.parse("2026-03-01T00:00:00Z"), "aa", List.of());

        assertThat(sameContentObservedLater.supersedes(stored)).isFalse();
    }

    @Test
    void refusesToCompareHeadsOfDifferentRecords() {
        SourceRecordHead stored = head(OBSERVED, "aa", List.of());
        SourceRecordHead other = new SourceRecordHead(SourceRecordKey.of("eprel", "OTHER"), "1", null,
                OBSERVED, RETRIEVED, null, SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                new PayloadHash("SHA-256", "cc"), URI.create("urn:o4g:evidence:1"),
                new SourceUsagePolicyRef("p", "1"), List.of(), List.of());

        assertThatThrownBy(() -> other.supersedes(stored)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anExpiredOrWithdrawnHeadIsNotUsable() {
        SourceRecordHead expired = new SourceRecordHead(KEY, "1", null, OBSERVED, RETRIEVED,
                Instant.parse("2026-01-10T00:00:00Z"), SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                new PayloadHash("SHA-256", "aa"), URI.create("urn:o4g:evidence:1"),
                new SourceUsagePolicyRef("p", "1"), List.of(), List.of());
        SourceRecordHead withdrawn = new SourceRecordHead(KEY, "1", null, OBSERVED, RETRIEVED, null,
                SourceRecordCompleteness.FULL, SourceRecordState.DELETED,
                new PayloadHash("SHA-256", "aa"), URI.create("urn:o4g:evidence:1"),
                new SourceUsagePolicyRef("p", "1"), List.of(), List.of());

        assertThat(expired.isUsableAt(Instant.parse("2026-01-05T00:00:00Z"))).isTrue();
        assertThat(expired.isUsableAt(Instant.parse("2026-02-05T00:00:00Z"))).isFalse();
        assertThat(withdrawn.isUsableAt(Instant.parse("2026-01-05T00:00:00Z"))).isFalse();
    }
}
