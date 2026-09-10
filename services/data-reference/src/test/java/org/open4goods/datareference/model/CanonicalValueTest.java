package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.value.BooleanValue;
import org.open4goods.datareference.model.value.CanonicalValue;
import org.open4goods.datareference.model.value.CanonicalValueType;
import org.open4goods.datareference.model.value.CodeValue;
import org.open4goods.datareference.model.value.DateValue;
import org.open4goods.datareference.model.value.DecimalValue;
import org.open4goods.datareference.model.value.IntegerValue;
import org.open4goods.datareference.model.value.LocalizedTextValue;
import org.open4goods.datareference.model.value.QuantityValue;
import org.open4goods.datareference.model.value.UriValue;

/**
 * Each canonical value type reports its own frozen discriminator and refuses to
 * be constructed without the parts that give it meaning.
 */
class CanonicalValueTest {

    @Test
    void everyTypeReportsItsOwnDiscriminator() {
        assertThat(new LocalizedTextValue("x", LanguageTag.UND).type())
                .isEqualTo(CanonicalValueType.LOCALIZED_TEXT);
        assertThat(new BooleanValue(true).type()).isEqualTo(CanonicalValueType.BOOLEAN);
        assertThat(new IntegerValue(BigInteger.ONE).type()).isEqualTo(CanonicalValueType.INTEGER);
        assertThat(new DecimalValue(BigDecimal.ONE).type()).isEqualTo(CanonicalValueType.DECIMAL);
        assertThat(QuantityValue.of(BigDecimal.ONE, "LENGTH", "m").type())
                .isEqualTo(CanonicalValueType.QUANTITY);
        assertThat(new CodeValue("eu-energy-label", "A").type()).isEqualTo(CanonicalValueType.CODE);
        assertThat(new DateValue(LocalDate.of(2026, 1, 1)).type()).isEqualTo(CanonicalValueType.DATE);
        assertThat(new UriValue(URI.create("https://example.invalid")).type())
                .isEqualTo(CanonicalValueType.URI);
    }

    @Test
    void theUnionIsClosedAndCoveredByTheDiscriminatorEnum() {
        // A new subtype without a matching constant would break every stored document.
        assertThat(CanonicalValue.class.getPermittedSubclasses())
                .hasSize(CanonicalValueType.values().length);
    }

    @Test
    void aTextValueStatesItsLanguage() {
        assertThatThrownBy(() -> new LocalizedTextValue("x", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aCodeValueStatesItsCodeSystem() {
        assertThatThrownBy(() -> new CodeValue(null, "A")).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> new CodeValue("eu-energy-label", null)).isInstanceOf(Exception.class);
    }

    @Test
    void aQuantityStatesADimensionAndAUnit() {
        assertThatThrownBy(() -> QuantityValue.of(BigDecimal.ONE, " ", "m"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuantityValue.of(BigDecimal.ONE, "LENGTH", "centimetres"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aQuantityKeepsTheScaleItWasGiven() {
        assertThat(QuantityValue.of(new BigDecimal("0.5500"), "LENGTH", "m").amount().scale()).isEqualTo(4);
    }

    @Test
    void requiredValuesAreNeverNull() {
        assertThatThrownBy(() -> new IntegerValue(null)).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> new DecimalValue(null)).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> new DateValue(null)).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> new UriValue(null)).isInstanceOf(Exception.class);
    }
}
