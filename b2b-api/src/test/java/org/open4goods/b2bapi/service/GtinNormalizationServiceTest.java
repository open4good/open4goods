package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.exception.InvalidGtinException;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.model.product.BarcodeType;

/**
 * Verifies B2B GTIN normalization against the canonical barcode validator.
 */
class GtinNormalizationServiceTest {

    private final GtinNormalizationService service = new GtinNormalizationService(new BarcodeValidationService());

    @Test
    void normalizesLeadingZeroVariantsToSameProductId() {
        NormalizedGtin gtin13 = service.normalize("0088381694858");
        NormalizedGtin gtin12 = service.normalize("088381694858");

        assertThat(gtin13.value()).isEqualTo("0088381694858");
        assertThat(gtin12.value()).isEqualTo("0088381694858");
        assertThat(gtin13.productId()).isEqualTo(88381694858L);
        assertThat(gtin12.productId()).isEqualTo(gtin13.productId());
        assertThat(gtin13.barcodeType()).isEqualTo(BarcodeType.GTIN_13);
    }

    @Test
    void normalizesGtin8ThroughValidatorPaddedResult() {
        NormalizedGtin normalized = service.normalize("40170725");

        assertThat(normalized.value()).isEqualTo("0000040170725");
        assertThat(normalized.productId()).isEqualTo(40170725L);
        assertThat(normalized.barcodeType()).isEqualTo(BarcodeType.GTIN_13);
    }

    @Test
    void keepsTrueGtin14WhenValidatorReportsIt() {
        NormalizedGtin normalized = service.normalize("70753800008156");

        assertThat(normalized.value()).isEqualTo("70753800008156");
        assertThat(normalized.productId()).isEqualTo(70753800008156L);
        assertThat(normalized.barcodeType()).isEqualTo(BarcodeType.GTIN_14);
    }

    @Test
    void rejectsInvalidChecksumBeforeProductLookup() {
        assertThatThrownBy(() -> service.normalize("8436542859045"))
                .isInstanceOf(InvalidGtinException.class)
                .hasMessage("GTIN checksum is invalid.");
    }

    @Test
    void rejectsUnsupportedPathFormatsEvenIfValidatorCouldParseThem() {
        assertThatThrownBy(() -> service.normalize("88381694858"))
                .isInstanceOf(InvalidGtinException.class)
                .hasMessage("GTIN must contain 8, 12, 13, or 14 digits.");
        assertThatThrownBy(() -> service.normalize("978-614-404-018-8"))
                .isInstanceOf(InvalidGtinException.class)
                .hasMessage("GTIN must contain 8, 12, 13, or 14 digits.");
    }
}
