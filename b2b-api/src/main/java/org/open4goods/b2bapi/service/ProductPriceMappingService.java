package org.open4goods.b2bapi.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.open4goods.b2bapi.dto.product.B2bOfferDto;
import org.open4goods.b2bapi.dto.product.B2bPriceDto;
import org.open4goods.b2bapi.dto.product.B2bPriceHistorySummaryDto;
import org.open4goods.b2bapi.dto.product.B2bPriceTrendDto;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.PriceHistory;
import org.open4goods.model.price.PriceTrend;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.springframework.stereotype.Service;

/**
 * Maps product price aggregates to the sanitized B2B price facet DTO.
 */
@Service
public class ProductPriceMappingService {

    private static final String FAVICON_ENDPOINT = "/api/favicon?url=";
    private final Clock clock;

    /**
     * Creates a mapper using the system UTC clock for freshness calculations.
     */
    public ProductPriceMappingService() {
        this(Clock.systemUTC());
    }

    ProductPriceMappingService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Maps a product to the public price facet payload.
     *
     * @param product product aggregate from Elasticsearch
     * @param gtin normalized request GTIN to echo in the response
     * @param freshnessDays maximum offer age counted as fresh
     * @return sanitized price facet DTO
     */
    public B2bPriceDto map(Product product, String gtin, int freshnessDays) {
        AggregatedPrices prices = product == null ? null : product.getPrice();
        List<AggregatedPrice> offers = offers(prices);
        List<AggregatedPrice> freshOffers = offers.stream()
                .filter(offer -> isFresh(offer, freshnessDays))
                .toList();
        Map<ProductCondition, List<B2bOfferDto>> offersByCondition = mapOffersByCondition(freshOffers, freshnessDays);

        return new B2bPriceDto(
                firstNonBlank(gtin, product == null ? null : product.gtin()),
                safeProductCall(product, Product::bestName),
                safeProductCall(product, Product::brand),
                safeProductCall(product, Product::model),
                offers.size(),
                freshOffers.size(),
                mapOffer(bestOffer(freshOffers), freshnessDays),
                mapOffer(bestOffer(freshOffers, ProductCondition.NEW), freshnessDays),
                mapOffer(bestOffer(freshOffers, ProductCondition.OCCASION), freshnessDays),
                offersByCondition,
                mapTrend(prices, ProductCondition.NEW),
                mapTrend(prices, ProductCondition.OCCASION),
                mapHistorySummary(prices, ProductCondition.NEW, responseCurrency(freshOffers, prices)),
                mapHistorySummary(prices, ProductCondition.OCCASION, responseCurrency(freshOffers, prices)));
    }

    private List<AggregatedPrice> offers(AggregatedPrices prices) {
        if (prices == null || prices.getOffers() == null || prices.getOffers().isEmpty()) {
            return Collections.emptyList();
        }
        return prices.getOffers().stream()
                .filter(Objects::nonNull)
                .filter(offer -> offer.getPrice() != null)
                .sorted(Comparator.comparing(AggregatedPrice::getPrice))
                .toList();
    }

    private Map<ProductCondition, List<B2bOfferDto>> mapOffersByCondition(List<AggregatedPrice> offers, int freshnessDays) {
        if (offers.isEmpty()) {
            return Collections.emptyMap();
        }
        return offers.stream()
                .filter(offer -> offer.getProductState() != null)
                .collect(Collectors.groupingBy(
                        AggregatedPrice::getProductState,
                        () -> new EnumMap<>(ProductCondition.class),
                        Collectors.mapping(offer -> mapOffer(offer, freshnessDays), Collectors.toList())));
    }

    private B2bOfferDto mapOffer(AggregatedPrice price, int freshnessDays) {
        if (price == null) {
            return null;
        }
        return new B2bOfferDto(
                price.shortDataSourceName(),
                price.getOfferName(),
                price.getUrl(),
                price.getProductState(),
                price.getPrice(),
                toJavaCurrency(price.getCurrency()),
                toInstant(price.getTimeStamp()),
                freshnessAgeDays(price),
                buildFaviconUrl(price.shortDataSourceName()));
    }

    private AggregatedPrice bestOffer(List<AggregatedPrice> offers) {
        return offers.stream()
                .min(Comparator.comparing(AggregatedPrice::getPrice))
                .orElse(null);
    }

    private AggregatedPrice bestOffer(List<AggregatedPrice> offers, ProductCondition condition) {
        return offers.stream()
                .filter(offer -> condition == offer.getProductState())
                .min(Comparator.comparing(AggregatedPrice::getPrice))
                .orElse(null);
    }

    private B2bPriceTrendDto mapTrend(AggregatedPrices prices, ProductCondition condition) {
        AggregatedPrice actual = prices == null ? null : prices.bestOffer(condition);
        List<PriceHistory> history = prices == null ? Collections.emptyList() : safeHistory(prices, condition);
        PriceTrend trend = null;
        if (!history.isEmpty()) {
            trend = PriceTrend.of(history, actual);
        }
        Integer direction = trend != null ? trend.trend() : null;
        if ((direction == null || direction == 0) && history.size() < 2 && prices != null && prices.getTrends() != null) {
            direction = prices.getTrends().get(condition);
        }
        if (trend == null && direction == null) {
            return null;
        }
        Double variationPercent = trend == null ? null : trend.percentVariation();
        return new B2bPriceTrendDto(
                condition,
                direction,
                trend == null ? amount(actual) : trend.actualPrice(),
                trend == null ? null : trend.lastPrice(),
                trend == null ? null : trend.variation(),
                variationPercent,
                trend == null ? null : trend.historicalLowestPrice(),
                trend == null ? null : trend.historicalVariation(),
                trend == null ? null : trend.period());
    }

    private B2bPriceHistorySummaryDto mapHistorySummary(
            AggregatedPrices prices,
            ProductCondition condition,
            java.util.Currency currency) {
        List<PriceHistory> history = prices == null ? Collections.emptyList() : safeHistory(prices, condition);
        if (history.isEmpty()) {
            return null;
        }
        PriceHistory lowest = history.stream()
                .filter(entry -> entry.price() != null)
                .min(Comparator.comparing(PriceHistory::price))
                .orElse(null);
        PriceHistory highest = history.stream()
                .filter(entry -> entry.price() != null)
                .max(Comparator.comparing(PriceHistory::price))
                .orElse(null);
        Double average = history.stream()
                .filter(entry -> entry.price() != null)
                .mapToDouble(PriceHistory::price)
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);
        return new B2bPriceHistorySummaryDto(
                condition,
                lowest == null ? null : lowest.price(),
                lowest == null ? null : toInstant(lowest.timestamp()),
                highest == null ? null : highest.price(),
                highest == null ? null : toInstant(highest.timestamp()),
                average,
                currency);
    }

    private List<PriceHistory> safeHistory(AggregatedPrices prices, ProductCondition condition) {
        try {
            List<PriceHistory> history = prices.getHistory(condition);
            return history == null ? Collections.emptyList() : history;
        } catch (IllegalArgumentException ex) {
            return Collections.emptyList();
        }
    }

    private java.util.Currency responseCurrency(List<AggregatedPrice> offers, AggregatedPrices prices) {
        Optional<java.util.Currency> offerCurrency = offers.stream()
                .map(AggregatedPrice::getCurrency)
                .map(this::toJavaCurrency)
                .filter(Objects::nonNull)
                .findFirst();
        if (offerCurrency.isPresent()) {
            return offerCurrency.get();
        }
        AggregatedPrice best = prices == null ? null : prices.getMinPrice();
        return best == null ? null : toJavaCurrency(best.getCurrency());
    }

    private boolean isFresh(AggregatedPrice price, int freshnessDays) {
        Integer ageDays = freshnessAgeDays(price);
        return ageDays == null || ageDays <= freshnessDays;
    }

    private Integer freshnessAgeDays(AggregatedPrice price) {
        if (price == null || price.getTimeStamp() == null) {
            return null;
        }
        long ageMillis = Math.max(0L, clock.millis() - price.getTimeStamp());
        return Math.toIntExact(ageMillis / 86_400_000L);
    }

    private Instant toInstant(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private java.util.Currency toJavaCurrency(org.open4goods.model.price.Currency currency) {
        return currency == null ? null : java.util.Currency.getInstance(currency.name());
    }

    private String buildFaviconUrl(String merchant) {
        return isBlank(merchant) ? null : FAVICON_ENDPOINT + merchant;
    }

    private Double amount(AggregatedPrice price) {
        return price == null ? null : price.getPrice();
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private String safeProductCall(Product product, ProductStringGetter getter) {
        if (product == null) {
            return null;
        }
        try {
            return getter.get(product);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface ProductStringGetter {
        String get(Product product);
    }
}
