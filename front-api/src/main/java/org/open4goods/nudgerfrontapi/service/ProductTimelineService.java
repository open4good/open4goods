package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.model.eprel.EprelProduct;
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
        addPriceEvent(prices.getHistory(ProductCondition.NEW), ProductTimelineEventType.PRICE_FIRST_SEEN_NEW,
                ProductTimelineEventSource.PRICE_HISTORY, ProductCondition.NEW, Comparator.comparing(PriceHistory::getTimestamp),
                events);
        addPriceEvent(prices.getHistory(ProductCondition.NEW), ProductTimelineEventType.PRICE_LAST_SEEN_NEW,
                ProductTimelineEventSource.PRICE_HISTORY, ProductCondition.NEW,
                Comparator.comparing(PriceHistory::getTimestamp).reversed(), events);
        addPriceEvent(prices.getHistory(ProductCondition.OCCASION), ProductTimelineEventType.PRICE_FIRST_SEEN_OCCASION,
                ProductTimelineEventSource.PRICE_HISTORY, ProductCondition.OCCASION,
                Comparator.comparing(PriceHistory::getTimestamp), events);
        addPriceEvent(prices.getHistory(ProductCondition.OCCASION), ProductTimelineEventType.PRICE_LAST_SEEN_OCCASION,
                ProductTimelineEventSource.PRICE_HISTORY, ProductCondition.OCCASION,
                Comparator.comparing(PriceHistory::getTimestamp).reversed(), events);
    }

    private void addPriceEvent(List<PriceHistory> history, ProductTimelineEventType type,
            ProductTimelineEventSource source, ProductCondition condition, Comparator<PriceHistory> comparator,
            List<ProductTimelineEventDto> events) {
        if (history == null || history.isEmpty()) {
            return;
        }
        Optional<PriceHistory> candidate = history.stream()
                .filter(Objects::nonNull)
                .filter(entry -> entry.getTimestamp() != null)
                .min(comparator);
        if (candidate.isEmpty()) {
            return;
        }
        PriceHistory selected = candidate.get();
        Long timestamp = normaliseTimestamp(selected.getTimestamp());
        if (timestamp == null) {
            return;
        }
        events.add(new ProductTimelineEventDto(timestamp, type, source, condition, selected.getPrice()));
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
        addEprelEvent(events, ProductTimelineEventType.EPREL_FIRST_PUBLICATION, eprel.getFirstPublicationDateTs(),
                eprel.getFirstPublicationDate());
        addEprelEvent(events, ProductTimelineEventType.EPREL_LAST_PUBLICATION, eprel.getPublishedOnDateTs(),
                eprel.getPublishedOnDate());
        addEprelEvent(events, ProductTimelineEventType.EPREL_EXPORT, eprel.getExportDateTs(), null);
        addEprelEvent(events, ProductTimelineEventType.EPREL_IMPORTED, eprel.getImportedOn(), null);
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

    private Long normaliseTimestamp(Long timestamp) {
        if (timestamp == null) {
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
}
