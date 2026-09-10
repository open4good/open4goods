package org.open4goods.model.attribute;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Regression tests for the provenance a datasource contribution carries.
 *
 * <p>The language a datasource states is evidence: it says which language the value
 * is written in, not which language a page will be served in. Losing it, or
 * replacing it with a serving-domain language, makes a value impossible to reuse
 * for another locale.
 */
class SourcedAttributeTest {

    @Test
    void keepsTheLanguageStatedByTheDatasource() {
        Attribute attribute = new Attribute("SCREEN_TYPE", "Écran plat", "fr");

        SourcedAttribute sourced = new SourcedAttribute(attribute, "some-merchant.com");

        assertThat(sourced.getLanguage()).isEqualTo("fr");
    }

    @Test
    void marksTheLanguageUndetermined_whenTheDatasourceStatesNone() {
        Attribute attribute = new Attribute("WIDTH", "55", null);

        SourcedAttribute sourced = new SourcedAttribute(attribute, "some-merchant.com");

        assertThat(sourced.getLanguage()).isEqualTo(SourcedAttribute.UNDETERMINED_LANGUAGE);
    }

    @Test
    void marksTheLanguageUndetermined_whenTheDatasourceStatesABlankOne() {
        Attribute attribute = new Attribute("WIDTH", "55", "   ");

        SourcedAttribute sourced = new SourcedAttribute(attribute, "some-merchant.com");

        assertThat(sourced.getLanguage()).isEqualTo(SourcedAttribute.UNDETERMINED_LANGUAGE);
    }

    @Test
    void keepsEachSourcesOwnLanguageThroughMergeIntoAnIndexedAttribute() {
        ProductAttribute raw = new ProductAttribute();
        raw.setName("SCREEN_TYPE");
        raw.addSourceAttribute(new SourcedAttribute(new Attribute("SCREEN_TYPE", "Écran plat", "fr"), "fr-merchant.com"));
        raw.addSourceAttribute(new SourcedAttribute(new Attribute("SCREEN_TYPE", "Flat screen", "en"), "en-merchant.com"));
        raw.addSourceAttribute(new SourcedAttribute(new Attribute("SCREEN_TYPE", "55", null), "silent-merchant.com"));

        IndexedAttribute indexed = new IndexedAttribute("SCREEN_TYPE", "Écran plat");
        indexed.getSource().addAll(raw.getSource());

        assertThat(indexed.getSource())
                .extracting(SourcedAttribute::getDataSourcename, SourcedAttribute::getLanguage)
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("fr-merchant.com", "fr"),
                        org.assertj.core.api.Assertions.tuple("en-merchant.com", "en"),
                        org.assertj.core.api.Assertions.tuple("silent-merchant.com",
                                SourcedAttribute.UNDETERMINED_LANGUAGE));
    }
}
