package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A unit slot holds a UCUM code, never a localizable label.
 */
class UcumCodeTest {

    @ParameterizedTest
    @ValueSource(strings = {"m", "cm", "mm", "kg", "g", "L", "kW.h", "W", "Cel", "%", "m2", "km/h"})
    void acceptsUcumCodes(String code) {
        assertThat(new UcumCode(code).value()).isEqualTo(code);
    }

    @Test
    void preservesCaseBecauseUcumCaseCarriesMeaning() {
        // mm is millimetre and Mm is megametre; folding either way changes the unit.
        assertThat(new UcumCode("mm").value()).isEqualTo("mm");
        assertThat(new UcumCode("Mm").value()).isEqualTo("Mm");
        assertThat(new UcumCode("mm")).isNotEqualTo(new UcumCode("Mm"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"centimetres", "pouces par seconde", "square metres"})
    void rejectsLocalizableLabels(String label) {
        assertThatThrownBy(() -> new UcumCode(label))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankAndOverlongCodes() {
        assertThatThrownBy(() -> new UcumCode("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UcumCode("m".repeat(UcumCode.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
