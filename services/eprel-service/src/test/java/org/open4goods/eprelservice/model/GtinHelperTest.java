package org.open4goods.eprelservice.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GtinHelper}.
 */
class GtinHelperTest
{
    @Test
    @DisplayName("toNumeric should return an Optional containing the parsed value when input is valid")
    void toNumericShouldParseDigits()
    {
        Optional<Long> result = GtinHelper.toNumeric("01234567890123");
        assertThat(result).contains(1234567890123L);
    }

    @Test
    @DisplayName("toNumeric should return empty when the value contains non digits")
    void toNumericShouldRejectNonDigits()
    {
        assertThat(GtinHelper.toNumeric("123ABC")).isEmpty();
    }

    @Test
    @DisplayName("toNumeric should return empty when the value is null or blank")
    void toNumericShouldHandleNullAndBlank()
    {
        assertThat(GtinHelper.toNumeric(null)).isEmpty();
        assertThat(GtinHelper.toNumeric("   ")).isEmpty();
    }
}
