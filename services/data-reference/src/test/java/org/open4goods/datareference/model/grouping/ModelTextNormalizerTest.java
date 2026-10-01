package org.open4goods.datareference.model.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Tests normalization used by exact-tuple model matching and search tokens. */
class ModelTextNormalizerTest {

    @Test
    void foldsCaseAndSeparatorRunsButKeepsSeparatorPresence() {
        assertThat(ModelTextNormalizer.normalize("XR-500")).isEqualTo("xr-500");
        assertThat(ModelTextNormalizer.normalize("xr 500")).isEqualTo("xr-500");
        // Separator presence is part of the exact tuple: "XR500" stays its own group.
        assertThat(ModelTextNormalizer.normalize("XR500")).isEqualTo("xr500");
    }

    @Test
    void returnsEmptyForNullOrBlankInput() {
        assertThat(ModelTextNormalizer.normalize(null)).isEmpty();
        assertThat(ModelTextNormalizer.normalize("   ")).isEmpty();
    }

    @Test
    void tokenizeSplitsOnNonAlphanumericRuns() {
        assertThat(ModelTextNormalizer.tokenize("XR-500 Plus!")).containsExactly("xr", "500", "plus");
    }

    @Test
    void tokenizeOfNullOrBlankIsEmpty() {
        assertThat(ModelTextNormalizer.tokenize(null)).isEmpty();
        assertThat(ModelTextNormalizer.tokenize("   ")).isEmpty();
    }

    @Test
    void joinSlugRejectsABlankFragment() {
        assertThatThrownBy(() -> ModelTextNormalizer.joinSlug("acme", "", "xr500"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }
}
