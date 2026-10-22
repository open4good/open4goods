package org.open4goods.nudgerfrontapi.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EmailMaskUtils}.
 */
class EmailMaskUtilsTest {

    @Test
    void keepsFirstCharacterAndDomain() {
        assertThat(EmailMaskUtils.mask("john.doe@example.com")).isEqualTo("j***@example.com");
    }

    @Test
    void masksValueWithoutAtSign() {
        assertThat(EmailMaskUtils.mask("john.doeexample.com")).isEqualTo("j***");
    }

    @Test
    void handlesEmptyLocalPartAndBlankValues() {
        assertThat(EmailMaskUtils.mask("@example.com")).isEqualTo("***@example.com");
        assertThat(EmailMaskUtils.mask(null)).isEqualTo("<empty>");
        assertThat(EmailMaskUtils.mask("  ")).isEqualTo("<empty>");
    }
}
