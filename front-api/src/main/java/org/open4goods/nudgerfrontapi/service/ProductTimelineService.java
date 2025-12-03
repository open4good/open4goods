package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.PriceHistory;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.nudgerfrontapi.dto.product.ProductTimelineDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductTimelineEventDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductTimelineEventSource;
import org.open4goods.nudgerfrontapi.dto.product.ProductTimelineEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds lifecycle timelines for {@link Product} instances by combining price history and
 * EPREL metadata.
 */
@Service
public class ProductTimelineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductTimelineService.class);
    private static final long EPOCH_SECOND_THRESHOLD = 10_000_000_000L;

    /**
     * Map a domain product to its timeline representation.
     *
     * @param product source product
     * @return populated timeline or {@code null} when no events are available
     */
    public ProductTimelineDto mapTimeline(Product product) {
        if (product == null) {
            return null;
        }
        List<ProductTimelineEventDto> events = new ArrayList<>();
        appendProductImportEvent(product, events);
        appendPriceHistoryEvents(product, events);
        appendEprelEvents(product, events);
        if (events.isEmpty()) {
            return null;
        }
        events.sort(Comparator.comparing(ProductTimelineEventDto::timestamp));
        return new ProductTimelineDto(List.copyOf(events));
    }

    private void appendPriceHistoryEvents(Product product, List<ProductTimelineEventDto> events) {
        AggregatedPrices prices = product.getPrice();
        if (prices == null) {
            return;
        }
        addPriceEvent(prices, ProductTimelineEventType.PRICE_FIRST_SEEN_NEW, ProductCondition.NEW, true, events);
        addPriceEvent(prices, ProductTimelineEventType.PRICE_LAST_SEEN_NEW, ProductCondition.NEW, false, events);
        addPriceEvent(prices, ProductTimelineEventType.PRICE_FIRST_SEEN_OCCASION, ProductCondition.OCCASION, true, events);
        addPriceEvent(prices, ProductTimelineEventType.PRICE_LAST_SEEN_OCCASION, ProductCondition.OCCASION, false, events);
    }

    private void addPriceEvent(AggregatedPrices prices, ProductTimelineEventType type, ProductCondition condition,
            boolean earliest, List<ProductTimelineEventDto> events) {
        TimelinePriceCandidate candidate = earliest
                ? resolveEarliestCandidate(prices, condition)
                : resolveLatestCandidate(prices, condition);
        if (candidate == null || candidate.timestamp() == null) {
            return;
        }
        events.add(new ProductTimelineEventDto(candidate.timestamp(), type, ProductTimelineEventSource.PRICE_HISTORY, condition,
                candidate.price()));
    }

    private TimelinePriceCandidate resolveEarliestCandidate(AggregatedPrices prices, ProductCondition condition) {
        TimelinePriceCandidate fromHistory = earliestFromHistory(prices.getHistory(condition));
        if (fromHistory != null) {
            return fromHistory;
        }
        return earliestFromOffers(prices.sortedOffers(condition));
    }

    private TimelinePriceCandidate resolveLatestCandidate(AggregatedPrices prices, ProductCondition condition) {
        TimelinePriceCandidate fromOffers = latestFromOffers(prices.sortedOffers(condition));
        if (fromOffers != null) {
            return fromOffers;
        }
        return latestFromHistory(prices.getHistory(condition));
    }

    private TimelinePriceCandidate earliestFromHistory(List<PriceHistory> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        Optional<TimelinePriceCandidate> candidate = history.stream()
                .filter(Objects::nonNull)
                .map(entry -> buildCandidate(entry.getTimestamp(), entry.getPrice()))
                .filter(Objects::nonNull)
                .min(Comparator.comparing(TimelinePriceCandidate::timestamp));
        return candidate.orElse(null);
    }

    private TimelinePriceCandidate latestFromHistory(List<PriceHistory> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        Optional<TimelinePriceCandidate> candidate = history.stream()
                .filter(Objects::nonNull)
                .map(entry -> buildCandidate(entry.getTimestamp(), entry.getPrice()))
                .filter(Objects::nonNull)
                .max(Comparator.comparing(TimelinePriceCandidate::timestamp));
        return candidate.orElse(null);
    }

    private TimelinePriceCandidate earliestFromOffers(List<AggregatedPrice> offers) {
        if (offers == null || offers.isEmpty()) {
            return null;
        }
        Optional<TimelinePriceCandidate> candidate = offers.stream()
                .filter(Objects::nonNull)
                .map(offer -> buildCandidate(offer.getTimeStamp(), offer.getPrice()))
                .filter(Objects::nonNull)
                .min(Comparator.comparing(TimelinePriceCandidate::timestamp));
        return candidate.orElse(null);
    }

    private TimelinePriceCandidate latestFromOffers(List<AggregatedPrice> offers) {
        if (offers == null || offers.isEmpty()) {
            return null;
        }
        Optional<TimelinePriceCandidate> candidate = offers.stream()
                .filter(Objects::nonNull)
                .map(offer -> buildCandidate(offer.getTimeStamp(), offer.getPrice()))
                .filter(Objects::nonNull)
                .max(Comparator.comparing(TimelinePriceCandidate::timestamp));
        return candidate.orElse(null);
    }

    private void appendEprelEvents(Product product, List<ProductTimelineEventDto> events) {
        EprelProduct eprel = product.getEprelDatas();
        if (eprel == null) {
            return;
        }
        addEprelEvent(events, ProductTimelineEventType.EPREL_ON_MARKET_START, eprel.getOnMarketStartDateTs(),
                eprel.getOnMarketStartDate());
        addEprelEvent(events, ProductTimelineEventType.EPREL_ON_MARKET_END, eprel.getOnMarketEndDateTs(),
                eprel.getOnMarketEndDate());
        addEprelEvent(events, ProductTimelineEventType.EPREL_ON_MARKET_FIRST_START, eprel.getOnMarketFirstStartDateTs(),
                eprel.getOnMarketFirstStartDate());
        addEprelEvent(events, ProductTimelineEventType.EPREL_EXPORT, eprel.getExportDateTs(), null);
        if (eprel.getOrganisation() != null) {
            addEprelEvent(events, ProductTimelineEventType.EPREL_ORGANISATION_CLOSED,
                    eprel.getOrganisation().getCloseDate(), null);
        }
    }

    private void addEprelEvent(List<ProductTimelineEventDto> events, ProductTimelineEventType type, Long preferred,
            Long fallback) {
        Long timestamp = normaliseTimestamp(preferred != null ? preferred : fallback);
        if (timestamp == null) {
            return;
        }
        events.add(new ProductTimelineEventDto(timestamp, type, ProductTimelineEventSource.EPREL, null, null));
    }

    private void appendProductImportEvent(Product product, List<ProductTimelineEventDto> events) {
        Long creationDate = normaliseTimestamp(product.getCreationDate());
        if (creationDate == null) {
            return;
        }
        events.add(new ProductTimelineEventDto(creationDate, ProductTimelineEventType.EPREL_IMPORTED,
                ProductTimelineEventSource.PRICE_HISTORY, null, null));
    }

    private TimelinePriceCandidate buildCandidate(Long rawTimestamp, Double price) {
        Long timestamp = normaliseTimestamp(rawTimestamp);
        if (timestamp == null) {
            return null;
        }
        return new TimelinePriceCandidate(timestamp, price);
    }

    private Long normaliseTimestamp(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return null;
        }
        if (timestamp < 0) {
            LOGGER.debug("Ignoring negative timestamp {}", timestamp);
            return null;
        }
        if (timestamp < EPOCH_SECOND_THRESHOLD) {
            return timestamp * 1000;
        }
        return timestamp;
    }

    private record TimelinePriceCandidate(Long timestamp, Double price) {
    }
}
