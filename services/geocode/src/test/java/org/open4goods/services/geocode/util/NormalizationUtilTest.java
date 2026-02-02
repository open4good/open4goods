package org.open4goods.services.geocode.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NormalizationUtil}.
 */
class NormalizationUtilTest
{
    @Test
    void normalizeRemovesDiacriticsAndPunctuation()
    {
        String result = NormalizationUtil.normalize("São-Paulo!!!");
        assertThat(result).isEqualTo("sao-paulo");
    }

    @Test
    void normalizeCollapsesWhitespace()
    {
        String result = NormalizationUtil.normalize("  New   York  City ");
        assertThat(result).isEqualTo("new york city");
    }
}
