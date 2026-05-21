package org.open4goods.services.wikidataservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.services.wikidataservice.model.WikidataEntity;
import org.open4goods.services.wikidataservice.util.WikidataConstants;

/**
 * Unit tests for {@link WikidataParser} using canned maps (no network calls).
 */
class WikidataParserTest {

    private WikidataParser parser;

    @BeforeEach
    void setUp() {
        parser = new WikidataParser(List.of("en", "fr"));
    }

    @Test
    void parsesLabels() {
        Map<String, Object> entity = Map.of(
                "labels", Map.of(
                        "en", Map.of("language", "en", "value", "Test Product"),
                        "fr", Map.of("language", "fr", "value", "Produit Test")));

        WikidataEntity result = parser.parse("Q1", entity);

        assertThat(result.getLabels()).containsExactlyInAnyOrder("en:Test Product", "fr:Produit Test");
    }

    @Test
    void parsesAliases() {
        Map<String, Object> entity = Map.of(
                "aliases", Map.of(
                        "en", List.of(
                                Map.of("language", "en", "value", "Model X"),
                                Map.of("language", "en", "value", "X Model"))));

        WikidataEntity result = parser.parse("Q2", entity);

        assertThat(result.getAliases()).containsExactlyInAnyOrder("en:Model X", "en:X Model");
    }

    @Test
    void parsesSitelinks() {
        Map<String, Object> entity = Map.of(
                "sitelinks", Map.of(
                        "enwiki", Map.of("title", "Test Product", "url", "https://en.wikipedia.org/wiki/Test_Product"),
                        "frwiki", Map.of("title", "Produit Test", "url", "https://fr.wikipedia.org/wiki/Produit_Test")));

        WikidataEntity result = parser.parse("Q3", entity);

        assertThat(result.getWikipediaUrls())
                .contains("en:https://en.wikipedia.org/wiki/Test_Product")
                .contains("fr:https://fr.wikipedia.org/wiki/Produit_Test");
    }

    @Test
    void parsesGtinClaim() {
        Map<String, Object> entity = Map.of(
                "claims", Map.of(
                        WikidataConstants.P_GTIN, List.of(
                                Map.of("mainsnak", Map.of(
                                        "snaktype", "value",
                                        "property", WikidataConstants.P_GTIN,
                                        "datavalue", Map.of("value", "1234567890123", "type", "string"))))));

        WikidataEntity result = parser.parse("Q4", entity);

        assertThat(result.getGtins()).containsExactly("1234567890123");
    }

    @Test
    void parsesReleaseDateYear() {
        Map<String, Object> entity = Map.of(
                "claims", Map.of(
                        WikidataConstants.P_RELEASE_DATE, List.of(
                                Map.of("mainsnak", Map.of(
                                        "snaktype", "value",
                                        "property", WikidataConstants.P_RELEASE_DATE,
                                        "datavalue", Map.of(
                                                "type", "time",
                                                "value", Map.of(
                                                        "time", "+2022-03-15T00:00:00Z",
                                                        "precision", 11)))))));

        WikidataEntity result = parser.parse("Q5", entity);

        assertThat(result.getReleaseYear()).isEqualTo("2022");
    }

    @Test
    void parsesQuantityClaim() {
        Map<String, Object> entity = Map.of(
                "claims", Map.of(
                        WikidataConstants.P_WIDTH, List.of(
                                Map.of("mainsnak", Map.of(
                                        "snaktype", "value",
                                        "property", WikidataConstants.P_WIDTH,
                                        "datavalue", Map.of(
                                                "type", "quantity",
                                                "value", Map.of(
                                                        "amount", "+35.5",
                                                        "unit", "http://www.wikidata.org/entity/Q174789")))))));

        WikidataEntity result = parser.parse("Q6", entity);

        assertThat(result.getNumericClaims()).containsKey(WikidataConstants.P_WIDTH);
        assertThat(result.getNumericClaims().get(WikidataConstants.P_WIDTH)).startsWith("35.5");
    }

    @Test
    void skipsNovalueAndSomevalue() {
        Map<String, Object> entity = Map.of(
                "claims", Map.of(
                        WikidataConstants.P_GTIN, List.of(
                                Map.of("mainsnak", Map.of(
                                        "snaktype", "novalue",
                                        "property", WikidataConstants.P_GTIN)))));

        WikidataEntity result = parser.parse("Q7", entity);

        assertThat(result.getGtins()).isEmpty();
    }

    @Test
    void handlesEmptyEntityMap() {
        WikidataEntity result = parser.parse("Q8", Map.of());

        assertThat(result.getQId()).isEqualTo("Q8");
        assertThat(result.getLabels()).isEmpty();
        assertThat(result.getGtins()).isEmpty();
    }
}
