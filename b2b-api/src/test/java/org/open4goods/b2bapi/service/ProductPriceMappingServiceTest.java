package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.dto.product.B2bOfferDto;
import org.open4goods.b2bapi.dto.product.B2bPriceDto;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.price.PriceHistory;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;

/**
 * Verifies sanitized B2B price facet mapping from product aggregates.
 */
class ProductPriceMappingServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");
    private final ProductPriceMappingService service = new ProductPriceMappingService(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void mapsOnlyFreshOffersAndGroupsThemByCondition() {
        Product product = productWithPrices(
                offer("amazon.fr", ProductCondition.NEW, 799.99, daysAgo(1)),
                offer("market.example", ProductCondition.OCCASION, 599.99, daysAgo(2)),
                offer("stale.example", ProductCondition.NEW, 699.99, daysAgo(45)));

        B2bPriceDto dto = service.map(product, "00012345678905", 30);

        assertThat(dto.gtin()).isEqualTo("00012345678905");
        assertThat(dto.offersCount()).isEqualTo(3);
        assertThat(dto.freshOffersCount()).isEqualTo(2);
        assertThat(dto.bestPrice().merchant()).isEqualTo("market");
        assertThat(dto.bestNewOffer().amount()).isEqualTo(799.99);
        assertThat(dto.offersByCondition()).containsOnlyKeys(ProductCondition.NEW, ProductCondition.OCCASION);
        assertThat(dto.offersByCondition().get(ProductCondition.NEW)).extracting(B2bOfferDto::merchant)
                .containsExactly("amazon");
        assertThat(dto.offersByCondition().get(ProductCondition.OCCASION)).extracting(B2bOfferDto::freshnessAgeDays)
                .containsExactly(2);
    }

    @Test
    void mapsTrendsHistoryAndCurrenciesFromAggregatedPrices() {
        Product product = productWithPrices(offer("amazon.fr", ProductCondition.NEW, 799.99, daysAgo(1)));
        product.getPrice().setNewPricehistory(List.of(
                new PriceHistory(daysAgo(20), 899.99),
                new PriceHistory(daysAgo(10), 849.99),
                new PriceHistory(daysAgo(1), 799.99)));
        product.getPrice().getTrends().put(ProductCondition.OCCASION, 1);

        B2bPriceDto dto = service.map(product, "0887276812345", 30);

        assertThat(dto.bestPrice().currency().getCurrencyCode()).isEqualTo("EUR");
        assertThat(dto.bestPrice().timestamp()).isEqualTo(Instant.ofEpochMilli(daysAgo(1)));
        assertThat(dto.newTrend().direction()).isEqualTo(-1);
        assertThat(dto.newTrend().actualAmount()).isEqualTo(799.99);
        assertThat(dto.newTrend().previousAmount()).isEqualTo(849.99);
        assertThat(dto.newTrend().variationPercent()).isNegative();
        assertThat(dto.occasionTrend().direction()).isEqualTo(1);
        assertThat(dto.newHistorySummary().lowestAmount()).isEqualTo(799.99);
        assertThat(dto.newHistorySummary().highestAmount()).isEqualTo(899.99);
        assertThat(dto.newHistorySummary().averageAmount()).isCloseTo(849.99, within(0.001));
        assertThat(dto.newHistorySummary().currency().getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void offerDtoDoesNotExposePrivateCommercialFields() {
        Set<String> componentNames = java.util.Arrays.stream(B2bOfferDto.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(componentNames)
                .doesNotContain("datasourceName", "compensation", "affiliationToken",
                        "quantityInStock", "shippingTime", "shippingCost");
    }

    private Product productWithPrices(AggregatedPrice... offers) {
        Product product = new Product();
        product.setId(887276812345L);
        AggregatedPrices prices = new AggregatedPrices();
        prices.setOffers(Set.of(offers));
        prices.setConditions(Set.of(ProductCondition.NEW, ProductCondition.OCCASION));
        prices.setMinPrice(java.util.Arrays.stream(offers)
                .min(java.util.Comparator.comparing(AggregatedPrice::getPrice))
                .orElse(null));
        product.setPrice(prices);
        product.setOffersCount(offers.length);
        return product;
    }

    private AggregatedPrice offer(String datasource, ProductCondition condition, Double amount, long timestamp) {
        AggregatedPrice offer = new AggregatedPrice();
        offer.setDatasourceName(datasource);
        offer.setOfferName("Offer from " + datasource);
        offer.setUrl("https://" + datasource + "/product");
        offer.setProductState(condition);
        offer.setPrice(amount);
        offer.setCurrency(Currency.EUR);
        offer.setTimeStamp(timestamp);
        offer.setCompensation(42.0);
        offer.setAffiliationToken("secret");
        offer.setQuantityInStock(12);
        offer.setShippingCost(4.99);
        offer.setShippingTime(3);
        return offer;
    }

    private long daysAgo(int days) {
        return NOW.minusSeconds(days * 86_400L).toEpochMilli();
    }
}
