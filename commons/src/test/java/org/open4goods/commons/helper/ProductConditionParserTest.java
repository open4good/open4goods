package org.open4goods.commons.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.product.ProductCondition;

/**
 * Tests merchant condition parsing aliases.
 */
class ProductConditionParserTest {

    /**
     * Verifies that marketplace enum spellings are normalized before matching.
     *
     * @throws InvalidParameterException when parsing fails
     */
    @Test
    void parsesMarketplaceUsedConditionAliases() throws InvalidParameterException {
        assertThat(ProductConditionParser.parse("VERY_GOOD")).isEqualTo(ProductCondition.OCCASION);
        assertThat(ProductConditionParser.parse("Etat correct")).isEqualTo(ProductCondition.OCCASION);
        assertThat(ProductConditionParser.parse("Très bon état")).isEqualTo(ProductCondition.OCCASION);
    }

    /**
     * Verifies that new condition variants are accepted.
     *
     * @throws InvalidParameterException when parsing fails
     */
    @Test
    void parsesNewConditionAliases() throws InvalidParameterException {
        assertThat(ProductConditionParser.parse("http://schema.org/NewCondition")).isEqualTo(ProductCondition.NEW);
        assertThat(ProductConditionParser.parse("comme-neuf")).isEqualTo(ProductCondition.NEW);
    }

    /**
     * Verifies that unrelated feed values are still rejected.
     */
    @Test
    void rejectsUnknownValues() {
        assertThatThrownBy(() -> ProductConditionParser.parse("NON"))
                .isInstanceOf(InvalidParameterException.class);
        assertThatThrownBy(() -> ProductConditionParser.parse("2500"))
                .isInstanceOf(InvalidParameterException.class);
    }
}
