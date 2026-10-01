package org.open4goods.api.services.migration.legacybackup;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.migration.legacybackup.LegacyBackupInputManifest.ConversionRules;
import org.open4goods.datareference.model.SourceContentType;

class LegacyBackupRecordConverterTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-09-01T00:00:00Z");
    private static final ConversionRules RULES = new ConversionRules(ConversionRules.MAPPING_VERSION, null, Map.of());

    private final LegacyBackupRecordConverter converter = new LegacyBackupRecordConverter(RULES);

    @Test
    void dropsALineWithoutAnyValidGtin() {
        LegacyBackupRecordConversion conversion = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"name\":\"Kettle\"}", OBSERVED_AT);

        assertThat(conversion.mutation()).isEmpty();
        assertThat(conversion.deadLetters()).extracting(LegacyBackupDeadLetter::reason)
                .containsExactly(LegacyBackupDeadLetterReason.MISSING_OR_INVALID_GTIN);
    }

    @Test
    void dropsMalformedJson() {
        LegacyBackupRecordConversion conversion = converter.convert("dataset", "products-backup-0.gz", 1,
                "not json", OBSERVED_AT);

        assertThat(conversion.mutation()).isEmpty();
        assertThat(conversion.deadLetters()).extracting(LegacyBackupDeadLetter::reason)
                .containsExactly(LegacyBackupDeadLetterReason.MALFORMED_JSON_LINE);
    }

    @Test
    void convertsGtinIdentityAndNativeScalarFields() {
        LegacyBackupRecordConversion conversion = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"gtin\":\"4006381333931\",\"brand\":\"Acme\",\"name\":\"Kettle\"}", OBSERVED_AT);

        assertThat(conversion.mutation()).isPresent();
        var head = conversion.mutation().orElseThrow().candidate();
        assertThat(head.key().sourceRecordId().value()).isEqualTo("gtin:4006381333931");
        assertThat(head.gtinLinks()).hasSize(1);
        assertThat(head.gtinLinks().get(0).gtin().value()).isEqualTo("4006381333931");
        assertThat(head.assertions()).hasSize(2);
        assertThat(head.assertions()).allSatisfy(assertion -> assertThat(assertion.contentType())
                .isEqualTo(SourceContentType.TEXT));
    }

    @Test
    void extractsALegacyMinimumPricePoint() {
        LegacyBackupRecordConversion conversion = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"gtin\":\"4006381333931\",\"price\":42.5}", OBSERVED_AT);

        assertThat(conversion.price()).isPresent();
        assertThat(conversion.price().orElseThrow().amount()).isEqualByComparingTo("42.5");
        assertThat(conversion.price().orElseThrow().gtin().value()).isEqualTo("4006381333931");
    }

    @Test
    void flagsAmazonNamedFieldsAsForbiddenSourceContentWithoutBlockingTheRecord() {
        LegacyBackupRecordConversion conversion = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"gtin\":\"4006381333931\",\"amazonAsin\":\"B000TEST00\"}", OBSERVED_AT);

        assertThat(conversion.mutation()).isPresent();
        assertThat(conversion.deadLetters()).extracting(LegacyBackupDeadLetter::reason)
                .containsExactly(LegacyBackupDeadLetterReason.FORBIDDEN_SOURCE_CONTENT);
    }

    @Test
    void dropsUnknownFieldsWithoutAnyConversion() {
        LegacyBackupRecordConversion conversion = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"gtin\":\"4006381333931\",\"someUnreviewedField\":\"x\"}", OBSERVED_AT);

        assertThat(conversion.mutation()).isPresent();
        assertThat(conversion.mutation().orElseThrow().candidate().assertions()).isEmpty();
        assertThat(conversion.deadLetters()).isEmpty();
    }

    @Test
    void reportsDeferredAttributeMappingWithoutBlockingTheRecord() {
        LegacyBackupRecordConversion conversion = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"gtin\":\"4006381333931\",\"attributesByDatasource\":{\"web\":{\"COLOR\":\"red\"}}}", OBSERVED_AT);

        assertThat(conversion.mutation()).isPresent();
        assertThat(conversion.deadLetters()).extracting(LegacyBackupDeadLetter::reason)
                .containsExactly(LegacyBackupDeadLetterReason.ATTRIBUTE_MAPPING_DEFERRED);
    }

    @Test
    void producesTheSameDeterministicRecordIdRegardlessOfLinePosition() {
        LegacyBackupRecordConversion first = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"gtin\":\"4006381333931\"}", OBSERVED_AT);
        LegacyBackupRecordConversion second = converter.convert("dataset", "products-backup-1.gz", 99,
                "{\"gtin\":\"4006381333931\"}", OBSERVED_AT);

        assertThat(first.mutation().orElseThrow().candidate().key())
                .isEqualTo(second.mutation().orElseThrow().candidate().key());
    }
}
