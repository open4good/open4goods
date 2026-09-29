package org.open4goods.services.feedservice.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceId;

class FeedColumnResolverTest {

    private static final FeedDefinition DEFINITION = new FeedDefinition(
            new SourceId("merchant.awin.acme"),
            "2026-09-01",
            List.of("sku"),
            Locale.FRENCH,
            Map.of("weight", "weight_unit"),
            FeedSemantics.FULL,
            Map.of(
                    "title", new ColumnTarget.ReferenceField(SourceContentType.TEXT, "name"),
                    "price", new ColumnTarget.OfferField(ColumnTarget.OfferFieldKind.PRICE)),
            UnknownColumnPolicy.REPORT);

    @Test
    void resolvesDeclaredColumnsAndReportsUnknownOnes() {
        ColumnResolution resolution = FeedColumnResolver.resolve(
                DEFINITION, Set.of("sku", "title", "price", "weight_unit", "some_unmapped_header"));

        assertThat(resolution.matchedTargets()).containsOnlyKeys("title", "price");
        assertThat(resolution.matchedUnitColumns()).containsEntry("weight_unit", "weight");
        assertThat(resolution.unknownColumns()).containsExactly("some_unmapped_header");
        assertThat(resolution.missingUnitColumns()).isEmpty();
        assertThat(resolution.isClean()).isFalse();
    }

    @Test
    void neverFallsBackToACandidateListForUnmappedHeaders() {
        // A header named exactly like a common label ("Price") is NOT the mapped "price"
        // column and must be reported unknown, never guessed by label (AC2).
        ColumnResolution resolution = FeedColumnResolver.resolve(DEFINITION, Set.of("sku", "Price"));

        assertThat(resolution.unknownColumns()).containsExactly("Price");
        assertThat(resolution.matchedTargets()).isEmpty();
    }

    @Test
    void reportsMissingUnitColumn() {
        ColumnResolution resolution = FeedColumnResolver.resolve(DEFINITION, Set.of("sku", "title", "price"));

        assertThat(resolution.missingUnitColumns()).containsExactly("weight");
        assertThat(resolution.isClean()).isFalse();
    }

    @Test
    void isCleanWhenEveryHeaderIsAccountedFor() {
        ColumnResolution resolution = FeedColumnResolver.resolve(
                DEFINITION, Set.of("sku", "title", "price", "weight_unit"));

        assertThat(resolution.isClean()).isTrue();
    }
}
