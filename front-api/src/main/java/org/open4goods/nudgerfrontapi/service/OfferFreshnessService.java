package org.open4goods.nudgerfrontapi.service;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.nudgerfrontapi.config.properties.OfferFreshnessProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Single freshness filter for the offers exposed by front-api.
 * <p>
 * An offer is stale when {@code now - timeStamp > maxAge} ({@code front.offers.max-age}, 2 days by default
 * like the api re-aggregation). An offer without timestamp cannot be proven fresh and is treated as stale.
 * Every place that exposes offers (offer list, best prices, offer count, SEO price range, search
 * suggestions and the {@code /contrib/} tokens built from the offer list) reads them through this service,
 * so a product whose offers are all stale is served as a product without offer.
 * <p>
 * The filter never mutates the indexed {@link Product}: it returns filtered copies.
 */
@Service
public class OfferFreshnessService {

    private final Duration maxAge;
    private final Clock clock;

    /**
     * Build the service with the system clock.
     *
     * @param properties freshness configuration
     */
    @Autowired
    public OfferFreshnessService(OfferFreshnessProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /**
     * Build the service with an explicit clock (tests).
     *
     * @param properties freshness configuration
     * @param clock      clock giving the current time
     */
    OfferFreshnessService(OfferFreshnessProperties properties, Clock clock) {
        this.maxAge = Objects.requireNonNull(properties.getMaxAge(), "front.offers.max-age is required");
        this.clock = clock;
    }

    /**
     * Tell whether an offer can be served.
     *
     * @param offer offer to check, may be {@code null}
     * @return {@code true} when the offer has a timestamp no older than {@code maxAge}
     */
    public boolean isFresh(AggregatedPrice offer) {
        if (offer == null || offer.getTimeStamp() == null) {
            return false;
        }
        return clock.millis() - offer.getTimeStamp() <= maxAge.toMillis();
    }

    /**
     * Copy the product prices keeping only the fresh offers.
     * <p>
     * The minimal price and the condition summary are recomputed from the fresh offers, so no precomputed
     * value coming from a stale offer survives. Price histories and trends are history, not offers: they are
     * kept as is.
     *
     * @param prices indexed prices, may be {@code null}
     * @return filtered copy, or {@code null} when {@code prices} is {@code null}
     */
    public AggregatedPrices freshPrices(AggregatedPrices prices) {
        if (prices == null) {
            return null;
        }
        Set<AggregatedPrice> offers = prices.getOffers() == null ? Set.of()
                : prices.getOffers().stream().filter(this::isFresh).collect(Collectors.toSet());

        AggregatedPrices fresh = new AggregatedPrices();
        fresh.setOffers(offers);
        fresh.setMinPrice(offers.stream()
                .filter(offer -> offer.getPrice() != null)
                .min(Comparator.comparingDouble(AggregatedPrice::getPrice))
                .orElse(null));
        fresh.setConditions(offers.stream()
                .map(AggregatedPrice::getProductState)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ProductCondition.class))));
        fresh.setNewPricehistory(prices.getNewPricehistory());
        fresh.setOccasionPricehistory(prices.getOccasionPricehistory());
        fresh.setTrends(prices.getTrends());
        return fresh;
    }

    /**
     * Copy the product prices keeping only the fresh offers.
     *
     * @param product product to read, may be {@code null}
     * @return filtered copy, or {@code null} when the product has no prices
     * @see #freshPrices(AggregatedPrices)
     */
    public AggregatedPrices freshPrices(Product product) {
        return product == null ? null : freshPrices(product.getPrice());
    }

    /**
     * Cheapest fresh offer of a product, replacing {@link Product#bestPrice()} which reads the minimal price
     * precomputed at aggregation time.
     *
     * @param product product to read, may be {@code null}
     * @return cheapest fresh offer, or {@code null} when the product has no fresh offer
     */
    public AggregatedPrice bestPrice(Product product) {
        AggregatedPrices fresh = freshPrices(product);
        return fresh == null ? null : fresh.getMinPrice();
    }

    /**
     * Number of fresh offers of a product.
     *
     * @param product product to read, may be {@code null}
     * @return number of fresh offers, {@code 0} when the product has none
     */
    public int offersCount(Product product) {
        AggregatedPrices fresh = freshPrices(product);
        return fresh == null ? 0 : fresh.getOffers().size();
    }

    /**
     * @return configured maximum offer age
     */
    public Duration getMaxAge() {
        return maxAge;
    }
}
