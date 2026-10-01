package org.open4goods.api.services.migration.legacybackup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LegacyBackupFieldDispositionRegistryTest {

    @Test
    void classifiesGtinFieldsAsUnattributedIdentity() {
        assertThat(LegacyBackupFieldDispositionRegistry.classify("gtin")).isEqualTo(LegacyFieldDisposition.UNATTRIBUTED_IDENTITY);
        assertThat(LegacyBackupFieldDispositionRegistry.classify("id")).isEqualTo(LegacyFieldDisposition.UNATTRIBUTED_IDENTITY);
    }

    @Test
    void classifiesAmazonFieldsAsQuarantine() {
        assertThat(LegacyBackupFieldDispositionRegistry.classify("amazonAsin")).isEqualTo(LegacyFieldDisposition.QUARANTINE);
        assertThat(LegacyBackupFieldDispositionRegistry.classify("datasourceAmazonFr")).isEqualTo(LegacyFieldDisposition.QUARANTINE);
    }

    @Test
    void classifiesPriceFieldsAsLegacyMinimumPrice() {
        assertThat(LegacyBackupFieldDispositionRegistry.classify("price")).isEqualTo(LegacyFieldDisposition.LEGACY_MINIMUM_PRICE);
        assertThat(LegacyBackupFieldDispositionRegistry.classify("minPrice")).isEqualTo(LegacyFieldDisposition.LEGACY_MINIMUM_PRICE);
        assertThat(LegacyBackupFieldDispositionRegistry.classify("offers")).isEqualTo(LegacyFieldDisposition.LEGACY_MINIMUM_PRICE);
    }

    @Test
    void classifiesDerivedFieldsAsRecomputed() {
        assertThat(LegacyBackupFieldDispositionRegistry.classify("score")).isEqualTo(LegacyFieldDisposition.RECOMPUTED);
        assertThat(LegacyBackupFieldDispositionRegistry.classify("availability")).isEqualTo(LegacyFieldDisposition.RECOMPUTED);
    }

    @Test
    void classifiesNativeScalarFieldsAsNativeEvidencedConversion() {
        assertThat(LegacyBackupFieldDispositionRegistry.classify("brand")).isEqualTo(LegacyFieldDisposition.NATIVE_EVIDENCED_CONVERSION);
        assertThat(LegacyBackupFieldDispositionRegistry.classify("name")).isEqualTo(LegacyFieldDisposition.NATIVE_EVIDENCED_CONVERSION);
        assertThat(LegacyBackupFieldDispositionRegistry.isDirectlyConvertibleNativeField("brand")).isTrue();
    }

    @Test
    void classifiesAttributeFieldsAsNativeEvidencedConversionButNotDirectlyConvertible() {
        assertThat(LegacyBackupFieldDispositionRegistry.classify("attributesByDatasource"))
                .isEqualTo(LegacyFieldDisposition.NATIVE_EVIDENCED_CONVERSION);
        assertThat(LegacyBackupFieldDispositionRegistry.isDirectlyConvertibleNativeField("attributesByDatasource")).isFalse();
    }

    @Test
    void classifiesUnknownFieldsAsQuarantine() {
        assertThat(LegacyBackupFieldDispositionRegistry.classify("someUnreviewedField")).isEqualTo(LegacyFieldDisposition.QUARANTINE);
    }
}
