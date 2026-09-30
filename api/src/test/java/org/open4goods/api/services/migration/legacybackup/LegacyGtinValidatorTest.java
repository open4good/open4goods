package org.open4goods.api.services.migration.legacybackup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LegacyGtinValidatorTest {

    @Test
    void acceptsValidGtin13() {
        assertThat(LegacyGtinValidator.validate("4006381333931")).isPresent();
    }

    @Test
    void rejectsBadCheckDigit() {
        assertThat(LegacyGtinValidator.validate("4006381333930")).isEmpty();
    }

    @Test
    void acceptsValidGtin8() {
        assertThat(LegacyGtinValidator.validate("96385074")).isPresent();
    }

    @Test
    void rejectsWrongLength() {
        assertThat(LegacyGtinValidator.validate("123456")).isEmpty();
    }

    @Test
    void rejectsNonDigits() {
        assertThat(LegacyGtinValidator.validate("400638133393X")).isEmpty();
    }

    @Test
    void rejectsNull() {
        assertThat(LegacyGtinValidator.validate(null)).isEmpty();
    }

    @Test
    void rejectsBlank() {
        assertThat(LegacyGtinValidator.validate("   ")).isEmpty();
    }

    @Test
    void trimsWhitespaceBeforeValidating() {
        assertThat(LegacyGtinValidator.validate(" 4006381333931 ")).isPresent();
    }
}
