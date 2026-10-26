package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * Unit tests for {@link ProductSeoMetaService}: SERP-safe lengths, manufacturer
 * reference removal and fallbacks without score, category or offers.
 */
class ProductSeoMetaServiceTest {

    private final ProductSeoMetaService service = new ProductSeoMetaService();

    @Test
    void dishwasherTitleDropsReferenceAndFitsSixtyCharacters() {
        Product product = product("ELECTROLUX", "E62LB200S 911572124");
        index(product, "PLACE_SETTINGS", "14");
        index(product, "NOISE_LEVEL", "44");
        index(product, "INSTALLATION_TYPE", "ENCASTRABLE");

        String title = service.buildMetaTitle(product, null, dishwasherConfig(), DomainLanguage.fr);

        assertThat(title).isEqualTo("Electrolux E62LB200S : lave-vaisselle – avis, prix et impact");
        assertThat(title).hasSizeLessThanOrEqualTo(ProductSeoMetaService.MAX_TITLE_LENGTH);
        assertThat(title).doesNotContain("911572124").doesNotContain("…");
    }

    @Test
    void titleKeepsKeyFeatureWithShortSuffixWhenLongSuffixDoesNotFit() {
        Product product = product("HISENSE", "WD3I8043BBF2");
        index(product, "WASHING_CAPACITY", "8");
        index(product, "SPIN_SPEED", "1400");

        String title = service.buildMetaTitle(product, null, washingMachineConfig(), DomainLanguage.fr);

        assertThat(title).isEqualTo("Hisense WD3I8043BBF2 : lave-linge 8 kg – avis et prix");
    }

    @Test
    void titleUsesTwoFeaturesAndLongSuffixWhenTheyFit() {
        Product product = product("AEG", "FS6");
        index(product, "PLACE_SETTINGS", "13");
        index(product, "NOISE_LEVEL", "44");

        String title = service.buildMetaTitle(product, null, dishwasherConfig(), DomainLanguage.fr);

        // Two features would need 66 characters: the second one is dropped first.
        assertThat(title).isEqualTo("AEG FS6 : lave-vaisselle 13 couverts – avis, prix et impact");
        assertThat(title).hasSizeLessThanOrEqualTo(ProductSeoMetaService.MAX_TITLE_LENGTH);
    }

    @Test
    void titleStaysWithinLimitForVeryLongNames() {
        Product product = product("THOMSON", "THBI1444BFULL INTEGRABLE SILENCE PLUS EDITION LIMITEE 2026 NOIRE");
        index(product, "PLACE_SETTINGS", "14");

        String title = service.buildMetaTitle(product, null, dishwasherConfig(), DomainLanguage.fr);

        assertThat(title).hasSizeLessThanOrEqualTo(ProductSeoMetaService.MAX_TITLE_LENGTH);
        assertThat(title).startsWith("Thomson THBI1444BFULL").doesNotContain("…");
    }

    @Test
    void titleFallsBackWithoutCategory() {
        Product product = product("LG", "OLED55C4 1234567");

        String title = service.buildMetaTitle(product, null, null, DomainLanguage.fr);

        assertThat(title).isEqualTo("LG OLED55C4 – avis, prix et impact");
    }

    @Test
    void titleUsesFallbackNameWhenBrandOrModelIsMissing() {
        Product product = new Product(1L);

        assertThat(service.buildMetaTitle(product, "Rouleau couvre livre", null, DomainLanguage.fr))
                .isEqualTo("Rouleau couvre livre – avis, prix et impact");
        assertThat(service.buildMetaTitle(product, null, null, DomainLanguage.fr)).isNull();
    }

    @Test
    void englishTitleUsesEnglishCopy() {
        Product product = product("HISENSE", "WD3I8043BBF2");
        index(product, "WASHING_CAPACITY", "8");

        String title = service.buildMetaTitle(product, null, washingMachineConfig(), DomainLanguage.en);

        // "… washing machine 8 kg – reviews and prices" would need 63 characters.
        assertThat(title).isEqualTo("Hisense WD3I8043BBF2: washing machine – reviews and prices");
    }

    @Test
    void descriptionCarriesScoreFeaturesAndPriceRange() {
        Product product = product("ELECTROLUX", "E62LB200S 911572124");
        index(product, "PLACE_SETTINGS", "14");
        index(product, "NOISE_LEVEL", "44");
        index(product, "INSTALLATION_TYPE", "ENCASTRABLE");
        product.setScores(Map.of("ECOSCORE", new Score("ECOSCORE", 13.04)));
        offers(product, 699.99, 454.99, 524.99);

        String description = service.buildMetaDescription(product, null, dishwasherConfig(), DomainLanguage.fr);

        assertThat(description).isEqualTo("Electrolux E62LB200S, lave-vaisselle : score impact 13/20, 14 couverts, "
                + "44 dB, prix de 454,99 à 699,99 €. Comparez les offres sur Nudger.");
        assertThat(description).hasSizeLessThanOrEqualTo(ProductSeoMetaService.MAX_DESCRIPTION_LENGTH);
    }

    @Test
    void descriptionNeverInventsAPriceOrAScore() {
        Product product = product("HISENSE", "WD3I8043BBF2");
        index(product, "WASHING_CAPACITY", "8");

        String description = service.buildMetaDescription(product, null, washingMachineConfig(), DomainLanguage.fr);

        assertThat(description).isEqualTo("Hisense WD3I8043BBF2, lave-linge : 8 kg. Comparez les offres sur Nudger.");
        assertThat(description).doesNotContain("prix de").doesNotContain("€").doesNotContain("/20");
    }

    @Test
    void descriptionWithSingleOfferAndNoCategory() {
        Product product = product("ELECTROLUX", "E62LB200S");
        offers(product, 700.0);

        String description = service.buildMetaDescription(product, null, null, DomainLanguage.fr);

        assertThat(description).isEqualTo("Electrolux E62LB200S : prix : 700 €. Comparez les offres sur Nudger.");
    }

    @Test
    void descriptionInEnglish() {
        Product product = product("ELECTROLUX", "E62LB200S");
        product.setScores(Map.of("ECOSCORE", new Score("ECOSCORE", 13.04)));
        offers(product, 454.99, 699.99);

        String description = service.buildMetaDescription(product, null, dishwasherConfig(), DomainLanguage.en);

        assertThat(description).isEqualTo("Electrolux E62LB200S, dishwasher: impact score 13/20, "
                + "prices from €454.99 to €699.99. Compare offers on Nudger.");
    }

    @Test
    void descriptionStaysWithinLimitForLongNames() {
        Product product = product("ELECTROLUX",
                "E62LB200S TOUT INTEGRABLE 60 CM SILENCE PLUS EDITION LIMITEE AVEC PANIER COUVERTS SUPPLEMENTAIRE");
        index(product, "PLACE_SETTINGS", "14");
        index(product, "NOISE_LEVEL", "44");
        product.setScores(Map.of("ECOSCORE", new Score("ECOSCORE", 13.04)));
        offers(product, 454.99, 699.99);

        String description = service.buildMetaDescription(product, null, dishwasherConfig(), DomainLanguage.fr);

        assertThat(description).hasSizeLessThanOrEqualTo(ProductSeoMetaService.MAX_DESCRIPTION_LENGTH);
        assertThat(description).contains("score impact 13/20");
    }

    @Test
    void stripManufacturerReferenceOnlyRemovesTheModelReference() {
        Product product = product("ELECTROLUX", "E62LB200S 911572124");

        assertThat(service.stripManufacturerReference("ELECTROLUX E62LB200S 911572124", product))
                .isEqualTo("ELECTROLUX E62LB200S");
        assertThat(service.stripManufacturerReference("Lave-vaisselle 911572124 ELECTROLUX E62LB200S", product))
                .isEqualTo("Lave-vaisselle ELECTROLUX E62LB200S");
        assertThat(service.stripManufacturerReference("ELECTROLUX E62LB200S", product(null, "E62LB200S")))
                .isEqualTo("ELECTROLUX E62LB200S");
    }

    @Test
    void cleanModelHandlesUnderscoreAndKeepsShortNumbers() {
        assertThat(ProductSeoMetaService.cleanModel("EFMH65N1_8011281")).isEqualTo("EFMH65N1");
        assertThat(ProductSeoMetaService.cleanModel("QE55Q60D 2024")).isEqualTo("QE55Q60D 2024");
        assertThat(ProductSeoMetaService.cleanModel("911572124")).isEqualTo("911572124");
    }

    @Test
    void displayBrandTitleCasesCapitalsButKeepsAcronyms() {
        assertThat(ProductSeoMetaService.displayBrand("ELECTROLUX")).isEqualTo("Electrolux");
        assertThat(ProductSeoMetaService.displayBrand("DE DIETRICH")).isEqualTo("De Dietrich");
        assertThat(ProductSeoMetaService.displayBrand("ESSENTIEL B")).isEqualTo("Essentiel B");
        assertThat(ProductSeoMetaService.displayBrand("LG")).isEqualTo("LG");
        assertThat(ProductSeoMetaService.displayBrand("AEG")).isEqualTo("AEG");
        assertThat(ProductSeoMetaService.displayBrand("Fairphone")).isEqualTo("Fairphone");
    }

    private static Product product(String brand, String model) {
        Product product = new Product(7333394145372L);
        if (brand != null) {
            product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, brand);
        }
        if (model != null) {
            product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, model);
        }
        return product;
    }

    private static void index(Product product, String key, String value) {
        product.getAttributes().getIndexed().put(key, new IndexedAttribute(key, value));
    }

    private static void offers(Product product, double... prices) {
        Set<AggregatedPrice> offers = new LinkedHashSet<>();
        for (double price : prices) {
            AggregatedPrice offer = new AggregatedPrice();
            offer.setPrice(price);
            offer.setCurrency(Currency.EUR);
            offer.setDatasourceName("shop-" + price);
            offers.add(offer);
        }
        AggregatedPrices aggregatedPrices = new AggregatedPrices();
        aggregatedPrices.setOffers(offers);
        product.setPrice(aggregatedPrices);
    }

    private static VerticalConfig dishwasherConfig() {
        return verticalConfig("|| lave-vaisselle | lave-vaisselles ||", "|| dishwasher | dishwashers ||",
                List.of("PLACE_SETTINGS", "NOISE_LEVEL", "INSTALLATION_TYPE"),
                List.of(attribute("PLACE_SETTINGS", "couverts", "place settings"), attribute("NOISE_LEVEL", "dB", "dB"),
                        attribute("INSTALLATION_TYPE", null, null)));
    }

    private static VerticalConfig washingMachineConfig() {
        return verticalConfig("|| lave-linge | machine-a-laver ||", "|| washing machine | washer ||",
                List.of("WASHING_CAPACITY", "LOADING_TYPE", "SPIN_SPEED"),
                List.of(attribute("WASHING_CAPACITY", "kg", "kg"), attribute("LOADING_TYPE", null, null),
                        attribute("SPIN_SPEED", "tr/min", "rpm")));
    }

    private static VerticalConfig verticalConfig(String frPrefix, String enPrefix, List<String> popular,
            List<AttributeConfig> attributes) {
        VerticalConfig config = new VerticalConfig();
        ProductI18nElements fr = new ProductI18nElements();
        fr.getUrl().setPrefix(frPrefix);
        ProductI18nElements en = new ProductI18nElements();
        en.getUrl().setPrefix(enPrefix);
        config.getI18n().put("fr", fr);
        config.getI18n().put("en", en);
        config.setPopularAttributes(popular);
        config.setAttributesConfig(new AttributesConfig(attributes));
        return config;
    }

    private static AttributeConfig attribute(String key, String frSuffix, String enSuffix) {
        AttributeConfig config = new AttributeConfig();
        config.setKey(key);
        if (frSuffix != null) {
            Localisable<String, String> suffix = new Localisable<>();
            suffix.put("fr", frSuffix);
            suffix.put("en", enSuffix);
            config.setSuffix(suffix);
        }
        return config;
    }
}
