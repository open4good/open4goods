package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.price.PriceHistory;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.nudgerfrontapi.config.properties.OfferFreshnessProperties;
import org.open4goods.services.productrepository.services.ProductRepository;

/**
 * Unit tests for {@link OfferFreshnessService}: an offer older than {@code front.offers.max-age} is never
 * served, and nothing precomputed from a stale offer survives.
 */
class OfferFreshnessServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");
    private static final long ONE_HOUR = Duration.ofHours(1).toMillis();
    private static final long THREE_DAYS = Duration.ofDays(3).toMillis();

    private final OfferFreshnessService service = new OfferFreshnessService(new OfferFreshnessProperties(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void defaultMaxAgeIsTheApiRule() {
        assertThat(new OfferFreshnessProperties().getMaxAge())
                .isEqualTo(Duration.ofMillis(ProductRepository.VALID_UNTIL_DURATION))
                .isEqualTo(Duration.ofDays(2));
    }

    @Test
    void freshOfferIsServedAndStaleOfferIsNot() {
        long now = NOW.toEpochMilli();
        long maxAge = ProductRepository.VALID_UNTIL_DURATION;

        assertThat(service.isFresh(offer("shop", 10.0, ProductCondition.NEW, now - ONE_HOUR))).isTrue();
        assertThat(service.isFresh(offer("shop", 10.0, ProductCondition.NEW, now - maxAge))).isTrue();
        assertThat(service.isFresh(offer("shop", 10.0, ProductCondition.NEW, now - maxAge - 1))).isFalse();
        assertThat(service.isFresh(offer("shop", 10.0, ProductCondition.NEW, null))).isFalse();
        assertThat(service.isFresh(null)).isFalse();
    }

    @Test
    void maxAgeIsConfigurable() {
        OfferFreshnessProperties properties = new OfferFreshnessProperties();
        properties.setMaxAge(Duration.ofDays(30));
        OfferFreshnessService lenient = new OfferFreshnessService(properties, Clock.fixed(NOW, ZoneOffset.UTC));

        AggregatedPrice threeDaysOld = offer("shop", 10.0, ProductCondition.NEW, NOW.toEpochMilli() - THREE_DAYS);

        assertThat(service.isFresh(threeDaysOld)).isFalse();
        assertThat(lenient.isFresh(threeDaysOld)).isTrue();
    }

    @Test
    void freshPricesDropStaleOffersAndRecomputeMinPriceAndConditions() {
        long now = NOW.toEpochMilli();
        AggregatedPrice freshNew = offer("fnac", 120.0, ProductCondition.NEW, now - ONE_HOUR);
        AggregatedPrice staleNew = offer("rakuten", 80.0, ProductCondition.NEW, now - THREE_DAYS);
        AggregatedPrice staleOccasion = offer("rakuten", 50.0, ProductCondition.OCCASION, now - THREE_DAYS);
        Product product = product(staleNew, freshNew, staleOccasion);
        // Precomputed at aggregation time from a stale offer.
        product.getPrice().setMinPrice(staleOccasion);
        List<PriceHistory> history = product.getPrice().getNewPricehistory();

        AggregatedPrices fresh = service.freshPrices(product);

        assertThat(fresh.getOffers()).containsExactly(freshNew);
        assertThat(fresh.getMinPrice()).isSameAs(freshNew);
        assertThat(fresh.getConditions()).containsExactly(ProductCondition.NEW);
        assertThat(fresh.getNewPricehistory()).isSameAs(history);
        assertThat(service.bestPrice(product)).isSameAs(freshNew);
        assertThat(service.offersCount(product)).isEqualTo(1);
        // The indexed product is left untouched.
        assertThat(product.getPrice().getOffers()).hasSize(3);
        assertThat(product.bestPrice()).isSameAs(staleOccasion);
    }

    @Test
    void productWithOnlyStaleOffersHasNoPrice() {
        long now = NOW.toEpochMilli();
        AggregatedPrice stale = offer("rakuten", 9.9, ProductCondition.NEW, now - Duration.ofDays(28).toMillis());
        Product product = product(stale);
        product.getPrice().setMinPrice(stale);

        AggregatedPrices fresh = service.freshPrices(product);

        assertThat(fresh.getOffers()).isEmpty();
        assertThat(fresh.getMinPrice()).isNull();
        assertThat(fresh.getConditions()).isEmpty();
        assertThat(service.bestPrice(product)).isNull();
        assertThat(service.offersCount(product)).isZero();
    }

    @Test
    void productWithoutPricesStaysWithoutPrices() {
        Product product = new Product(1L);
        product.setPrice(null);

        assertThat(service.freshPrices(product)).isNull();
        assertThat(service.freshPrices((Product) null)).isNull();
        assertThat(service.bestPrice(product)).isNull();
        assertThat(service.offersCount(product)).isZero();
    }

    static AggregatedPrice offer(String datasource, double price, ProductCondition condition, Long timeStamp) {
        AggregatedPrice offer = new AggregatedPrice();
        offer.setDatasourceName(datasource);
        offer.setUrl("https://" + datasource + ".example/offer/" + price);
        offer.setOfferName(datasource + " offer");
        offer.setPrice(price);
        offer.setCurrency(Currency.EUR);
        offer.setProductState(condition);
        offer.setTimeStamp(timeStamp);
        return offer;
    }

    private static Product product(AggregatedPrice... offers) {
        Product product = new Product(9782070360024L);
        AggregatedPrices prices = new AggregatedPrices();
        prices.setOffers(Set.of(offers));
        EnumSet<ProductCondition> conditions = EnumSet.noneOf(ProductCondition.class);
        for (AggregatedPrice offer : offers) {
            conditions.add(offer.getProductState());
        }
        prices.setConditions(conditions);
        prices.setNewPricehistory(new ArrayList<>());
        prices.setTrends(Map.of());
        product.setPrice(prices);
        product.setOffersCount(offers.length);
        return product;
    }
}
