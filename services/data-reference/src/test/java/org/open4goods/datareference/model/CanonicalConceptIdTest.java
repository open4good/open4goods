package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CanonicalConceptIdTest {

    @Test
    void parsesStableClassAndAttributeIdentifiers() {
        CanonicalConceptId classId = CanonicalConceptId.parse("o4g:class:washing-machine");
        CanonicalConceptId attributeId = CanonicalConceptId.parse("o4g:attribute:energy-class");

        assertThat(classId).isEqualTo(new CanonicalClassId("washing-machine"));
        assertThat(classId.toString()).isEqualTo("o4g:class:washing-machine");
        assertThat(attributeId).isEqualTo(new CanonicalAttributeId("energy-class"));
        assertThat(attributeId.toString()).isEqualTo("o4g:attribute:energy-class");
    }

    @Test
    void rejectsProviderIdsAndNonCanonicalSlugs() {
        assertThatThrownBy(() -> CanonicalConceptId.parse("icecat:feature:42"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CanonicalAttributeId("ENERGY_CLASS"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canonicalizesValidBcp47TagsAndRequiresUnknownToBeExplicit() {
        assertThat(new LanguageTag("fr-fr").value()).isEqualTo("fr-FR");
        assertThat(LanguageTag.UND.value()).isEqualTo("und");
        assertThatThrownBy(() -> new LanguageTag("fr_FR"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LanguageTag(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
