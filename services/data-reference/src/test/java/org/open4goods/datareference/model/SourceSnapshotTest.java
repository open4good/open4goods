package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class SourceSnapshotTest {

    private static final Instant RETRIEVED_AT = Instant.parse("2026-09-09T10:15:30Z");
    private static final SourceFieldId FIELD = new SourceFieldId("eprel", "dimensionHeight", "2026-06");

    @Test
    void retainsRawMultiValuesAndBuildsReplaceableKey() {
        SourceAssertion first = assertion("84,5", "cm", 0);
        SourceAssertion second = assertion("845", "mm", 1);

        SourceSnapshot snapshot = snapshot(List.of(first, second));

        assertThat(snapshot.key()).isEqualTo(new SourceSnapshotKey(
                new Gtin("00012345600012"), "eprel", "registration-42"));
        assertThat(snapshot.assertions()).containsExactly(first, second);
        assertThatThrownBy(() -> snapshot.assertions().add(first))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void permitsEmptyReplacementToMaterializeDeletion() {
        assertThat(snapshot(List.of()).assertions()).isEmpty();
    }

    @Test
    void rejectsAmbiguousRepeatedValueCoordinates() {
        SourceAssertion first = assertion("84,5", "cm", 0);
        SourceAssertion duplicate = assertion("845", "mm", 0);

        assertThatThrownBy(() -> snapshot(List.of(first, duplicate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate assertion");
    }

    private static SourceAssertion assertion(String rawValue, String rawUnit, int ordinal) {
        return new SourceAssertion(
                FIELD,
                rawValue,
                rawUnit,
                LanguageTag.UND,
                ordinal,
                GtinMatchConfidence.EXACT);
    }

    private static SourceSnapshot snapshot(List<SourceAssertion> assertions) {
        return new SourceSnapshot(
                new Gtin("00012345600012"),
                "eprel",
                "registration-42",
                "2026-06",
                Instant.parse("2026-09-08T00:00:00Z"),
                RETRIEVED_AT,
                RETRIEVED_AT.plusSeconds(86_400),
                new PayloadHash("sha-256", "0123456789abcdef0123456789abcdef"),
                new SourceUsagePolicyRef("eprel-public-api", "2024-06-03"),
                URI.create("urn:eprel:registration:42"),
                assertions);
    }
}
