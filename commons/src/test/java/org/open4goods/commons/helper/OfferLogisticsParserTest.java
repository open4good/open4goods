package org.open4goods.commons.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.open4goods.model.exceptions.InvalidParameterException;

/**
 * Tests merchant logistics parser aliases and unknown-value handling.
 */
class OfferLogisticsParserTest {

    /**
     * Verifies common shipping cost formats.
     *
     * @throws InvalidParameterException when parsing fails
     */
    @Test
    void parsesShippingCosts() throws InvalidParameterException {
        assertThat(ShippingCostParser.parse("4,99 EUR")).isEqualTo(4.99);
        assertThat(ShippingCostParser.parse("€ 0")).isEqualTo(0.0);
        assertThat(ShippingCostParser.parse("Livraison offerte")).isEqualTo(0.0);
    }

    /**
     * Verifies common shipping time formats.
     *
     * @throws InvalidParameterException when parsing fails
     */
    @Test
    void parsesShippingTimes() throws InvalidParameterException {
        assertThat(ShippingTimeParser.parse("Livraison sous 3 à 5 jours")).isEqualTo(5);
        assertThat(ShippingTimeParser.parse("48h")).isEqualTo(2);
        assertThat(ShippingTimeParser.parse("2 semaines")).isEqualTo(14);
    }

    /**
     * Verifies common stock quantity formats.
     *
     * @throws InvalidParameterException when parsing fails
     */
    @Test
    void parsesStockQuantities() throws InvalidParameterException {
        assertThat(StockQuantityParser.parse("12")).isEqualTo(12);
        assertThat(StockQuantityParser.parse("12+")).isEqualTo(12);
        assertThat(StockQuantityParser.parse("En stock (7)")).isEqualTo(7);
    }

    /**
     * Verifies that unknown logistics values are rejected after being logged.
     *
     * @throws InvalidParameterException when parsing fails
     */
    @Test
    void rejectsUnknownLogisticsValues() throws InvalidParameterException {
        assertThatThrownBy(() -> ShippingCostParser.parse("contact us"))
                .isInstanceOf(InvalidParameterException.class);
        assertThat(ShippingTimeParser.parse("soonish")).isNull();
        assertThatThrownBy(() -> StockQuantityParser.parse("many"))
                .isInstanceOf(InvalidParameterException.class);
    }
}
