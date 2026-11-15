package org.open4goods.model.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SourcableAttributeTest {


        @Test
        void normalizesValuesBeforeCounting() {
                ProductAttribute attribute = buildAttribute("COLOR",
                                sourced("COLOR", "Noir", "sourceA"),
                                sourced("COLOR", "  noir  ", "sourceB"));

                assertThat(attribute.getValue()).isEqualTo("Noir");
        }

        @Test
        void resolvesTiesLexicographically() {
                ProductAttribute attribute = buildAttribute("TYPE",
                                sourced("TYPE", "Beta", "sourceA"),
                                sourced("TYPE", "Alpha", "sourceB"));

                assertThat(attribute.getValue()).isEqualTo("Alpha");
        }

        @Test
        void rejectsMismatchedAttributeNames() {
                ProductAttribute attribute = new ProductAttribute();
                attribute.setName("COLOR");

                assertThatThrownBy(() -> attribute.addSourceAttribute(sourced("MODEL", "123", "sourceA")))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("expected COLOR");
        }

        @Test
        void preferredSourcesOverrideTheVoteCount() {
                ProductAttribute attribute = buildAttribute("INDEX",
                                sourced("INDEX", "5", "shopA"),
                                sourced("INDEX", "5", "shopB"),
                                sourced("INDEX", "5", "shopC"),
                                sourced("INDEX", "8", "regulator"));

                assertThat(attribute.bestValue()).isEqualTo("5");
                assertThat(attribute.bestValue(List.of("regulator"))).isEqualTo("8");
                assertThat(attribute.bestValue(List.of())).isEqualTo("5");
                assertThat(SourcableAttribute.trustedSourcePriority("regulator", List.of("x", "regulator"))).isEqualTo(1);
        }

        @Test
        void defaultOrderKeepsEprelThenIcecat() {
                ProductAttribute attribute = buildAttribute("INDEX",
                                sourced("INDEX", "5", "shopA"),
                                sourced("INDEX", "5", "shopB"),
                                sourced("INDEX", "7", "icecat.biz"),
                                sourced("INDEX", "9", "eprel"));

                assertThat(attribute.bestValue()).isEqualTo("9");
                assertThat(SourcableAttribute.trustedSourcePriority("eprel")).isZero();
                assertThat(SourcableAttribute.trustedSourcePriority("icecat.biz")).isEqualTo(1);
        }

        private ProductAttribute buildAttribute(String name, SourcedAttribute... sourcedAttributes) {
                ProductAttribute attribute = new ProductAttribute();
                attribute.setName(name);
                for (SourcedAttribute sourcedAttribute : sourcedAttributes) {
                        attribute.addSourceAttribute(sourcedAttribute);
                }
                return attribute;
        }

        private SourcedAttribute sourced(String name, String value, String datasource) {
                SourcedAttribute attribute = new SourcedAttribute();
                attribute.setName(name);
                attribute.setValue(value);
                attribute.setDataSourcename(datasource);
                return attribute;
        }
}
