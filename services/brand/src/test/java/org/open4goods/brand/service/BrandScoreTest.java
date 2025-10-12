package org.open4goods.brand.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.brand.model.BrandScore;

class BrandScoreTest {

    @Test
    void shouldNormalizeScoreWhenInvertBaseProvided() {
        BrandScore score = new BrandScore("datasource", 100.0, "Brand", "90", "http://example.com");
        assertThat(score.getNormalized()).isEqualTo(10.0);
    }

    @Test
    void shouldNormalizeScoreWithoutInvertBase() {
        BrandScore score = new BrandScore("datasource", null, "Brand", "42", null);
        assertThat(score.getNormalized()).isEqualTo(42.0);
    }

    @Test
    void shouldGenerateDeterministicId() {
        String id = BrandScore.id("data", "Brand");
        assertThat(id).isEqualTo("data-brand");
    }
}
