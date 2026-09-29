package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * The Fair Use Policy "AS IS" disclaimer is required unless an explicitly
 * reviewed policy says otherwise.
 */
class AttributionRequirementTest {

    @Test
    void anAbsentDisclaimerRequirementReadsAsRequired() {
        AttributionRequirement undeclared = new AttributionRequirement(true, "Data by Icecat",
                URI.create("https://icecat.biz"), null);

        assertThat(undeclared.asIsDisclaimerRequired()).isTrue();
    }

    @Test
    void anExplicitlyClearedDisclaimerRequirementStays() {
        AttributionRequirement cleared = new AttributionRequirement(true, "Data by Icecat",
                URI.create("https://icecat.biz"), false);

        assertThat(cleared.asIsDisclaimerRequired()).isFalse();
    }

    @Test
    void noneCarriesNoDisclaimerObligation() {
        assertThat(AttributionRequirement.NONE.asIsDisclaimerRequired()).isFalse();
    }

    @Test
    void requiredAttributionStillNeedsANoticeAndSourceUri() {
        assertThatThrownBy(() -> new AttributionRequirement(true, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required attribution needs a notice and source URI");
    }

    @Test
    void optionalAttributionMustUseTheNoneContract() {
        assertThatThrownBy(() -> new AttributionRequirement(false, "notice", URI.create("https://example.test"), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("optional attribution must use the NONE contract");
    }
}
