package org.open4goods.model.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SourcableAttributeTest {

        @AfterEach
        void resetTrustedSources() {
                SourcableAttribute.resetTrustedSourcePriority();
        }

        @Test
        void selectsTrustedSourceBeforeHigherVoteCount() {
                SourcableAttribute.setDEFAULT_TRUSTED_SOURCE_PRIORITY(List.of("priority-source", "secondary"));

                ProductAttribute attribute = buildAttribute("COLOR",
                                sourced("COLOR", "Blue", "other"),
                                sourced("COLOR", "Blue", "another"),
                                sourced("COLOR", "Crimson", "priority-source"));

                assertThat(attribute.getValue()).isEqualTo("Crimson");
        }

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
