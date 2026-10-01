package org.open4goods.services.feedservice.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceId;

import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

/**
 * Exercises the shared CSV ingestion fixtures (GOU-111) against
 * {@link FeedColumnResolver} and {@link FeedDefinition}.
 *
 * <p>No merchant-feed {@code SourceRecordHead}/{@code SourceAssertion}/
 * {@code IngestionCheckpoint} adapter exists yet, so dedup, quarantine,
 * price-history and deletion behavior cannot be asserted end-to-end here.
 * Each such fixture is still built and resolved at the column level; see
 * {@code fixtures/csv-ingestion/README.md} for what remains once the adapter
 * lands.
 */
class CsvIngestionFixturesTest {

    private static final String FIXTURES_PATH = "/fixtures/csv-ingestion/";

    private static final CsvMapper CSV_MAPPER = CsvMapper.builder().build();

    private static final FeedDefinition DEFINITION = new FeedDefinition(
            new SourceId("merchant.fixtures.acme"),
            "2026-09-01",
            List.of("sku"),
            Locale.FRENCH,
            Map.of("weight", "weight_unit"),
            FeedSemantics.FULL,
            Map.of(
                    "title", new ColumnTarget.ReferenceField(SourceContentType.TEXT, "name"),
                    "category", new ColumnTarget.ReferenceField(SourceContentType.CLASSIFICATION, "category"),
                    "weight", new ColumnTarget.ReferenceField(SourceContentType.ATTRIBUTE, "weight"),
                    "price", new ColumnTarget.OfferField(ColumnTarget.OfferFieldKind.PRICE)),
            UnknownColumnPolicy.REPORT);

    private static final FeedDefinition DELETION_DEFINITION = new FeedDefinition(
            new SourceId("merchant.fixtures.acme"),
            "2026-09-01",
            List.of("sku"),
            Locale.FRENCH,
            Map.of(),
            FeedSemantics.DELETION,
            Map.of("title", new ColumnTarget.ReferenceField(SourceContentType.TEXT, "name")),
            UnknownColumnPolicy.REPORT);

    @Test
    void quotingEdgeCasesAreParsedWithoutColumnResolutionErrors() throws IOException {
        List<Map<String, String>> rows = readRows("quoting-edge-cases.csv");

        assertThat(FeedColumnResolver.resolve(DEFINITION, rows.get(0).keySet()).isClean()).isTrue();

        assertThat(rows.get(0).get("title")).isEqualTo("Wireless Headphones, Over-Ear");
        assertThat(rows.get(1).get("title")).isEqualTo("She said \"wow\"");
        assertThat(rows.get(2).get("title")).isEqualTo("Line one\nLine two");
        assertThat(rows.get(3).get("title")).isEqualTo("  Spaced Title  ");

        // The multi-valued cell is kept as one raw string; splitting it into
        // ordinal-distinguished assertions is adapter work (see README).
        assertThat(rows.get(0).get("category")).isEqualTo("Audio|Headphones");
    }

    @Test
    void localizedDecimalsArePreservedVerbatimAndRepresentTheSamePrice() throws IOException, ParseException {
        List<Map<String, String>> rows = readRows("localized-decimals.csv");

        assertThat(FeedColumnResolver.resolve(DEFINITION, rows.get(0).keySet()).isClean()).isTrue();

        String frenchPrice = rows.get(0).get("price");
        String usPrice = rows.get(1).get("price");
        assertThat(frenchPrice).isEqualTo("99,90");
        assertThat(usPrice).isEqualTo("99.90");

        // FeedColumnResolver never normalizes values (same contract as unit columns);
        // both literals resolve to the same number once parsed with their own locale.
        double parsedFrench = NumberFormat.getInstance(Locale.FRENCH).parse(frenchPrice).doubleValue();
        double parsedUs = NumberFormat.getInstance(Locale.US).parse(usPrice).doubleValue();
        assertThat(parsedFrench).isEqualTo(parsedUs);
    }

    @Test
    void missingLanguageContentIsReadAsBlankNotFabricated() throws IOException {
        List<Map<String, String>> rows = readRows("missing-language.csv");

        assertThat(FeedColumnResolver.resolve(DEFINITION, rows.get(0).keySet()).isClean()).isTrue();
        assertThat(rows.get(0).get("title")).isEmpty();
        assertThat(rows.get(1).get("title")).isEqualTo("Casque Filaire");
    }

    @Test
    void missingUnitColumnIsReportedNotGuessed() throws IOException {
        List<Map<String, String>> rows = readRows("missing-unit-column.csv");

        ColumnResolution resolution = FeedColumnResolver.resolve(DEFINITION, rows.get(0).keySet());

        assertThat(resolution.missingUnitColumns()).containsExactly("weight");
        assertThat(resolution.unknownColumns()).isEmpty();
        assertThat(resolution.isClean()).isFalse();
    }

    @Test
    void duplicateIdenticalRowsAreByteForByteEqual() throws IOException {
        List<Map<String, String>> rows = readRows("duplicate-identical-rows.csv");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).isEqualTo(rows.get(1));
        assertThat(FeedColumnResolver.resolve(DEFINITION, rows.get(0).keySet()).isClean()).isTrue();
        // Deduplicating the two rows into a single mutation is adapter work (see README).
    }

    @Test
    void conflictingRowsShareTheKeyButDisagreeOnValue() throws IOException {
        List<Map<String, String>> rows = readRows("conflicting-rows-same-key.csv");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("sku")).isEqualTo(rows.get(1).get("sku"));
        assertThat(rows.get(0).get("price")).isNotEqualTo(rows.get(1).get("price"));
        assertThat(FeedColumnResolver.resolve(DEFINITION, rows.get(0).keySet()).isClean()).isTrue();
        // Quarantining both rows instead of last-row-wins is adapter work (see README).
    }

    @Test
    void multiValuedColumnIsKeptAsOneRawCellUntilTheAdapterSplitsIt() throws IOException {
        List<Map<String, String>> rows = readRows("multi-valued-columns.csv");

        ColumnResolution resolution = FeedColumnResolver.resolve(DEFINITION, rows.get(0).keySet());

        assertThat(rows.get(0).get("category")).isEqualTo("Audio|Headphones|Wireless");
        assertThat(resolution.matchedTargets())
                .containsEntry("category", new ColumnTarget.ReferenceField(SourceContentType.CLASSIFICATION, "category"));
    }

    @Test
    void unchangedOfferAcrossTwoFeedVersionsHasIdenticalPrice() throws IOException {
        Map<String, String> poll1 = readRows("unchanged-offer-poll-1.csv").get(0);
        Map<String, String> poll2 = readRows("unchanged-offer-poll-2.csv").get(0);

        assertThat(poll1.get("sku")).isEqualTo(poll2.get("sku"));
        assertThat(poll1.get("price")).isEqualTo(poll2.get("price"));
        assertThat(FeedColumnResolver.resolve(DEFINITION, poll1.keySet()).isClean()).isTrue();
        assertThat(FeedColumnResolver.resolve(DEFINITION, poll2.keySet()).isClean()).isTrue();
        // No new price-history point for an unchanged offer is adapter work (see README).
    }

    @Test
    void priceChangeAcrossTwoFeedVersionsIsVisibleAtTheRawLevel() throws IOException {
        Map<String, String> poll1 = readRows("price-change-poll-1.csv").get(0);
        Map<String, String> poll2 = readRows("price-change-poll-2.csv").get(0);

        assertThat(poll1.get("sku")).isEqualTo(poll2.get("sku"));
        assertThat(poll1.get("price")).isNotEqualTo(poll2.get("price"));
        assertThat(FeedColumnResolver.resolve(DEFINITION, poll1.keySet()).isClean()).isTrue();
        assertThat(FeedColumnResolver.resolve(DEFINITION, poll2.keySet()).isClean()).isTrue();
        // Recording the new price-history point is adapter work (see README).
    }

    @Test
    void explicitDeletionFeedDeclaresDeletionSemanticsAndCarriesOnlyIdentity() throws IOException {
        Map<String, String> row = readRows("explicit-deletion.csv").get(0);

        ColumnResolution resolution = FeedColumnResolver.resolve(DELETION_DEFINITION, row.keySet());

        assertThat(DELETION_DEFINITION.feedSemantics()).isEqualTo(FeedSemantics.DELETION);
        assertThat(resolution.matchedTargets()).isEmpty();
        assertThat(resolution.isClean()).isTrue();
        // Removing exactly the named SourceRecordHead is adapter work (see README).
    }

    @Test
    void interruptedFullFeedSeesFewerRecordsThanThePreviousCompleteRun() throws IOException {
        List<Map<String, String>> previousComplete = readRows("interrupted-full-feed-previous-complete.csv");
        List<Map<String, String>> truncated = readRows("interrupted-full-feed-truncated.csv");

        assertThat(previousComplete).hasSize(3);
        assertThat(truncated).hasSize(1);
        assertThat(FeedColumnResolver.resolve(DEFINITION, truncated.get(0).keySet()).isClean()).isTrue();
        // SKU-902 and SKU-903 are absent from the truncated run only because the read was
        // cut short, not because the source deleted them; not marking them deleted when the
        // IngestionCheckpoint shows an incomplete FULL read is adapter work (see README).
    }

    @Test
    void sourceUsagePolicyChangeOnlyAffectsContentTypeNotCanonicalField() throws IOException {
        Map<String, String> row = readRows("usage-policy-change.csv").get(0);

        FeedDefinition beforePolicyChange = policyDefinition(SourceContentType.TEXT);
        FeedDefinition afterPolicyChange = policyDefinition(SourceContentType.ATTRIBUTE);

        ColumnTarget.ReferenceField before =
                (ColumnTarget.ReferenceField) FeedColumnResolver.resolve(beforePolicyChange, row.keySet())
                        .matchedTargets()
                        .get("warranty");
        ColumnTarget.ReferenceField after =
                (ColumnTarget.ReferenceField) FeedColumnResolver.resolve(afterPolicyChange, row.keySet())
                        .matchedTargets()
                        .get("warranty");

        assertThat(before.contentType()).isEqualTo(SourceContentType.TEXT);
        assertThat(after.contentType()).isEqualTo(SourceContentType.ATTRIBUTE);
        assertThat(before.canonicalFieldId()).isEqualTo(after.canonicalFieldId());
        // FeedColumnResolver/FeedDefinition never reference Product: a usage-policy
        // reclassification only changes how the resulting SourceAssertion is filtered
        // downstream, never a direct Product field write.
    }

    private static FeedDefinition policyDefinition(SourceContentType warrantyContentType) {
        return new FeedDefinition(
                new SourceId("merchant.fixtures.acme"),
                "2026-09-01",
                List.of("sku"),
                Locale.FRENCH,
                Map.of("weight", "weight_unit"),
                FeedSemantics.FULL,
                Map.of(
                        "title", new ColumnTarget.ReferenceField(SourceContentType.TEXT, "name"),
                        "warranty", new ColumnTarget.ReferenceField(warrantyContentType, "warrantyText"),
                        "weight", new ColumnTarget.ReferenceField(SourceContentType.ATTRIBUTE, "weight"),
                        "price", new ColumnTarget.OfferField(ColumnTarget.OfferFieldKind.PRICE)),
                UnknownColumnPolicy.REPORT);
    }

    private static List<Map<String, String>> readRows(String fixtureName) throws IOException {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader reader = CSV_MAPPER.readerFor(HashMap.class).with(schema);
        try (InputStream in = CsvIngestionFixturesTest.class.getResourceAsStream(FIXTURES_PATH + fixtureName)) {
            MappingIterator<Map<String, String>> it = reader.readValues(in);
            return it.readAll();
        }
    }
}
