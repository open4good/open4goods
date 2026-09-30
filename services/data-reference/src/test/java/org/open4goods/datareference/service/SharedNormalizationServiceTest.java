package org.open4goods.datareference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceFieldId;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.model.normalization.NormalizationStatus;
import org.open4goods.datareference.model.normalization.LocalizedValueLookup;
import org.open4goods.datareference.model.normalization.NormalizationRequest;
import org.open4goods.datareference.port.NormalizationPort;
import org.open4goods.datareference.model.registry.GitRegistryLoader;
import org.open4goods.datareference.model.registry.InMemoryCanonicalRegistry;
import org.open4goods.datareference.model.value.QuantityValue;

/** Shared-normalizer conversion, locale and failure boundaries. */
class SharedNormalizationServiceTest {

    private static final SourceId ICECAT = new SourceId("icecat");
    private SharedNormalizationService normalizer;

    @BeforeEach
    void setUp() throws Exception {
        InMemoryCanonicalRegistry registry = new GitRegistryLoader().loadDefault().registry();
        normalizer = new SharedNormalizationService(registry, Map.of());
    }

    @Test
    void convertsWidthHeightAndDepthMetresCentimetresAndMillimetresToCanonicalCentimetres() {
        assertQuantity("feature:1649", "1.25", "m", "width", new BigDecimal("125.00"));
        assertQuantity("feature:1464", "125", "cm", "height", new BigDecimal("125"));
        assertQuantity("feature:1650", "1250", "mm", "depth", new BigDecimal("125.0"));
    }

    @Test
    void usesTheDeclaredLocaleAndPreservesTheRawEvidence() {
        SourceAssertion assertion = assertion("feature:1649", "1,25", "m");

        var result = normalizer.normalizeQuantity(ICECAT, assertion, Locale.FRANCE, "fr-CA", LocalDate.of(2026, 9, 12));

        assertThat(result.status()).isEqualTo(NormalizationStatus.SUCCESS);
        assertThat(result.language().value()).isEqualTo("fr-CA");
        assertThat(((QuantityValue) result.value().value()).amount()).isEqualByComparingTo("125.00");
        assertThat(((ScalarEvidence) assertion.evidence()).lexicalValue()).isEqualTo("1,25");
    }

    @Test
    void unknownUnitsInvalidNumbersAndInvalidLanguagesAreAuditableFailures() {
        var unknown = normalizer.normalizeQuantity(ICECAT, assertion("feature:1649", "1", "furlong"),
                Locale.US, "invalid_tag_!", LocalDate.of(2026, 9, 12));
        var invalid = normalizer.normalizeQuantity(ICECAT, assertion("feature:1649", "1,2,3", "cm"),
                Locale.US, null, LocalDate.of(2026, 9, 12));

        assertThat(unknown.status()).isEqualTo(NormalizationStatus.UNKNOWN_UNIT);
        assertThat(unknown.language().value()).isEqualTo("und");
        assertThat(invalid.status()).isEqualTo(NormalizationStatus.INVALID_NUMBER);
        assertThat(invalid.value()).isNull();
    }

    @Test
    void incompatibleDimensionsOutOfRangeValuesAndRepeatedMixedLanguageAssertionsStayDeterministic() {
        var incompatible = normalizer.normalizeQuantity(ICECAT, assertion("feature:1649", "2", "g"),
                Locale.US, "en", LocalDate.of(2026, 9, 12));
        var outOfRange = normalizer.normalizeQuantity(ICECAT, assertion("feature:1649", "1001", "cm"),
                Locale.US, "fr", LocalDate.of(2026, 9, 12));
        var first = normalizer.normalizeQuantity(ICECAT, assertion("feature:1650", "250", "mm"),
                Locale.US, "fr", LocalDate.of(2026, 9, 12));
        var repeated = normalizer.normalizeQuantity(ICECAT, assertion("feature:1650", "250", "mm"),
                Locale.US, "en", LocalDate.of(2026, 9, 12));

        assertThat(incompatible.status()).isEqualTo(NormalizationStatus.INCOMPATIBLE_DIMENSION);
        assertThat(outOfRange.status()).isEqualTo(NormalizationStatus.OUT_OF_RANGE);
        assertThat(first.value().value()).isEqualTo(repeated.value().value());
        assertThat(first.language().value()).isEqualTo("fr");
        assertThat(repeated.language().value()).isEqualTo("en");
    }

    @Test
    void localizedOutputUsesExactThenParentThenEnglishThenCanonicalId() {
        Map<org.open4goods.datareference.model.LanguageTag, String> values = Map.of(
                new org.open4goods.datareference.model.LanguageTag("fr"), "Largeur",
                new org.open4goods.datareference.model.LanguageTag("fr-CA"), "Largeur CA",
                new org.open4goods.datareference.model.LanguageTag("en"), "Width");

        assertThat(LocalizedValueLookup.resolve(values, new org.open4goods.datareference.model.LanguageTag("fr-CA"), "width"))
                .isEqualTo("Largeur CA");
        assertThat(LocalizedValueLookup.resolve(values, new org.open4goods.datareference.model.LanguageTag("fr-BE"), "width"))
                .isEqualTo("Largeur");
        assertThat(LocalizedValueLookup.resolve(Map.of(new org.open4goods.datareference.model.LanguageTag("en"), "Width"),
                new org.open4goods.datareference.model.LanguageTag("de"), "width")).isEqualTo("Width");
        assertThat(LocalizedValueLookup.resolve(Map.of(), new org.open4goods.datareference.model.LanguageTag("de"), "width"))
                .isEqualTo("width");
    }

    @Test
    void adapterFacingPortRequiresExplicitLocaleAndMappingCoordinates() {
        NormalizationPort port = normalizer;

        var result = port.normalize(new NormalizationRequest(ICECAT, assertion("feature:1649", "2", "m"),
                Locale.US, "en", LocalDate.of(2026, 9, 12)));

        assertThat(result.status()).isEqualTo(NormalizationStatus.SUCCESS);
        assertThat(((QuantityValue) result.value().value()).amount()).isEqualByComparingTo("200");
    }

    @Test
    void conversionPropertyPreservesEveryExactMillimetreValueAcrossLengthAliases() {
        for (int millimetres = 1; millimetres <= 1000; millimetres++) {
            var fromMillimetres = normalizer.normalizeQuantity(ICECAT,
                    assertion("feature:1649", Integer.toString(millimetres), "mm"), Locale.US, "und",
                    LocalDate.of(2026, 9, 12));
            var fromMetres = normalizer.normalizeQuantity(ICECAT,
                    assertion("feature:1649", new BigDecimal(millimetres).movePointLeft(3).toPlainString(), "m"),
                    Locale.US, "und", LocalDate.of(2026, 9, 12));

            assertThat(fromMillimetres.status()).isEqualTo(NormalizationStatus.SUCCESS);
            assertThat(fromMetres.status()).isEqualTo(NormalizationStatus.SUCCESS);
            assertThat(((QuantityValue) fromMillimetres.value().value()).amount())
                    .isEqualByComparingTo(((QuantityValue) fromMetres.value().value()).amount());
        }
    }

    private void assertQuantity(String field, String amount, String unit, String attribute, BigDecimal expected) {
        var result = normalizer.normalizeQuantity(ICECAT, assertion(field, amount, unit), Locale.US, "en",
                LocalDate.of(2026, 9, 12));

        assertThat(result.status()).isEqualTo(NormalizationStatus.SUCCESS);
        assertThat(result.value().attribute().slug()).isEqualTo(attribute);
        assertThat(((QuantityValue) result.value().value()).amount()).isEqualByComparingTo(expected);
        assertThat(((QuantityValue) result.value().value()).unit().value()).isEqualTo("cm");
    }

    private static SourceAssertion assertion(String field, String amount, String unit) {
        SourceRecordKey key = SourceRecordKey.of("icecat", "fixture-record");
        SourceFieldId sourceField = new SourceFieldId("icecat", field, "v1");
        return SourceAssertion.of(key, sourceField, 0, SourceContentType.ATTRIBUTE,
                new ScalarEvidence(amount, unit, org.open4goods.datareference.model.LanguageTag.UND));
    }
}
