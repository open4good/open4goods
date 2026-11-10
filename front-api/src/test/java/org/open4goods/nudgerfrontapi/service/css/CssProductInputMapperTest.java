package org.open4goods.nudgerfrontapi.service.css;

import static org.assertj.core.api.Assertions.assertThat;
import static org.open4goods.nudgerfrontapi.service.css.CssTestFixtures.offer;
import static org.open4goods.nudgerfrontapi.service.css.CssTestFixtures.product;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCanonicalUrl;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.config.properties.CssProperties;
import org.open4goods.nudgerfrontapi.config.properties.OfferFreshnessProperties;
import org.open4goods.nudgerfrontapi.service.OfferFreshnessService;

import com.google.protobuf.util.JsonFormat;
import com.google.shopping.css.v1.CssProductInput;

class CssProductInputMapperTest {

    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");

    private final CssProductInputMapper mapper = new CssProductInputMapper(CssTestFixtures.eligibility(),
            new CssProperties(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void mapsRequiredFields() {
        AggregatedPrice cheap = offer("darty.com", 99.99);
        cheap.setShippingCost(4.9);
        Product p = product(cheap, offer("boulanger.com", 120));
        CssProductInput input = mapper.map(p, CssTestFixtures.vertical());

        assertThat(input.getRawProvidedId()).isEqualTo(CssTestFixtures.GTIN);
        assertThat(input.getContentLanguage()).isEqualTo("fr");
        assertThat(input.getFeedLabel()).isEqualTo("FR");
        var a = input.getAttributes();
        assertThat(a.getGtin()).isEqualTo(CssTestFixtures.GTIN);
        assertThat(a.getTitle()).isEqualTo("Acme X-100 55 pouces");
        assertThat(a.getBrand()).isEqualTo("Acme");
        assertThat(a.getMpn()).isEqualTo("X-100");
        assertThat(a.getImageLink()).isEqualTo("https://static.nudger.fr/images/acme_abc.webp");
        assertThat(a.getProductTypesList()).containsExactly("Téléviseurs");
        assertThat(a.getIncludedDestinationsList()).containsExactly("css_organic_experiences");
        assertThat(a.getExpirationDate().getSeconds()).isEqualTo(NOW.plusSeconds(3 * 86400).getEpochSecond());
        assertThat(a.getHeadlineOfferCondition()).isEqualTo("new");
        assertThat(a.getHeadlineOfferShippingPrice().getAmountMicros()).isEqualTo(4_900_000L);
    }

    @Test
    void cppLinkIsTheCanonicalUrlAndHeadlineLinkTheProductPage() {
        Product p = product(offer("darty.com", 100), offer("boulanger.com", 120));
        CssProductInput input = mapper.map(p, CssTestFixtures.vertical());
        String path = ProductCanonicalUrl.fullSlug(p, CssTestFixtures.vertical(), DomainLanguage.fr);
        assertThat(path).isEqualTo("/televisions/acme-x-100-4006381333931");
        assertThat(input.getAttributes().getCppLink()).isEqualTo("https://nudger.fr" + path);
        assertThat(input.getAttributes().getHeadlineOfferLink()).isEqualTo("https://nudger.fr" + path);
    }

    @Test
    void pricesAreTheFreshPricesOfTheDisplayedProduct() {
        var stale = offer("cheap-but-stale.com", 1);
        stale.setTimeStamp(1L);
        Product p = product(offer("darty.com", 100.1), offer("boulanger.com", 120), stale);
        var displayed = new OfferFreshnessService(new OfferFreshnessProperties()).freshPrices(p);

        var a = mapper.map(p, CssTestFixtures.vertical()).getAttributes();
        assertThat(a.getLowPrice().getAmountMicros())
                .isEqualTo(Math.round(displayed.getMinPrice().getPrice() * 1_000_000));
        assertThat(a.getLowPrice().getAmountMicros()).isEqualTo(100_100_000L);
        assertThat(a.getHighPrice().getAmountMicros()).isEqualTo(120_000_000L);
        assertThat(a.getLowPrice().getCurrencyCode()).isEqualTo("EUR");
        assertThat(a.getNumberOfOffers()).isEqualTo(displayed.getOffers().size()).isEqualTo(2);
        assertThat(a.getHeadlineOfferPrice()).isEqualTo(a.getLowPrice());
    }

    @Test
    void neverSetsRatings() throws Exception {
        Product p = product(offer("darty.com", 100), offer("boulanger.com", 120));
        CssProductInput input = mapper.map(p, CssTestFixtures.vertical());
        var a = input.getAttributes();
        assertThat(a.getRating()).isZero();
        assertThat(a.getReviewCount()).isZero();
        assertThat(a.getMinRating()).isZero();
        assertThat(a.getMaxRating()).isZero();
        String json = JsonFormat.printer().omittingInsignificantWhitespace().print(input);
        assertThat(json).doesNotContain("rating", "Rating", "reviewCount").doesNotContain("\n");
    }

    @Test
    void certificationFromEprelEnergyClass() {
        Product p = product(offer("darty.com", 100), offer("boulanger.com", 120));
        var eprel = new org.open4goods.model.eprel.EprelProduct();
        eprel.setEnergyClass("E");
        p.setEprelDatas(eprel);
        var a = mapper.map(p, CssTestFixtures.vertical()).getAttributes();
        assertThat(a.getCertificationsList()).singleElement().satisfies(c -> {
            assertThat(c.getName()).isEqualTo("EPREL");
            assertThat(c.getCode()).isEqualTo("E");
        });
    }

    @Test
    void priceUsesDecimalMicros() {
        assertThat(CssProductInputMapper.price(0.1 + 0.2, "EUR").getAmountMicros()).isEqualTo(300_000L);
        assertThat(CssProductInputMapper.price(19.99, "EUR").getAmountMicros()).isEqualTo(19_990_000L);
    }
}
