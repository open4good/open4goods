package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.PriceHistory;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.nudgerfrontapi.dto.product.ProductTimelineDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductTimelineEventDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductTimelineEventType;

class ProductTimelineServiceTest {

    private final ProductTimelineService service = new ProductTimelineService();

    @Test
    void mapTimelineReturnsNullWhenNoEvents() {
        Product product = new Product(1L);

        assertThat(service.mapTimeline(product)).isNull();
    }

    @Test
    void mapTimelineReturnsNullWhenProductIsNull() {
        assertThat(service.mapTimeline(null)).isNull();
    }

    @Test
    void mapTimelineIncludesPriceHistoryEvents() {
        Product product = new Product(2L);
        AggregatedPrices prices = new AggregatedPrices();
        prices.setNewPricehistory(List.of(
                new PriceHistory(1_700_000_000_000L, 499.0),
                new PriceHistory(1_701_000_000_000L, 450.0)));
        prices.setOccasionPricehistory(List.of(new PriceHistory(1_702_000_000_000L, 320.0)));
        product.setPrice(prices);

        ProductTimelineDto dto = service.mapTimeline(product);

        assertThat(dto).isNotNull();
        assertThat(dto.events())
                .extracting(ProductTimelineEventDto::type)
                .containsExactlyInAnyOrder(
                        ProductTimelineEventType.PRICE_FIRST_SEEN_NEW,
                        ProductTimelineEventType.PRICE_LAST_SEEN_NEW,
                        ProductTimelineEventType.PRICE_FIRST_SEEN_OCCASION,
                        ProductTimelineEventType.PRICE_LAST_SEEN_OCCASION);
        assertThat(dto.events())
                .isSortedAccordingTo(Comparator.comparing(ProductTimelineEventDto::timestamp));
        assertThat(dto.events())
                .filteredOn(event -> ProductTimelineEventType.PRICE_LAST_SEEN_NEW.equals(event.type()))
                .allSatisfy(event -> {
                    assertThat(event.condition()).isEqualTo(ProductCondition.NEW);
                    assertThat(event.price()).isEqualTo(450.0);
                });
    }

    @Test
    void mapTimelineIncludesEprelEvents() {
        Product product = new Product(3L);
        EprelProduct eprel = new EprelProduct();
        eprel.setOnMarketStartDateTs(1_600_000_000_000L);
        eprel.setOnMarketEndDateTs(1_700_000_000_000L);
        eprel.setFirstPublicationDateTs(1_650_000_000_000L);
        eprel.setPublishedOnDateTs(1_750_000_000_000L);
        eprel.setExportDateTs(1_760_000_000_000L);
        eprel.setImportedOn(1_770_000_000_000L);
        EprelProduct.Organisation organisation = new EprelProduct.Organisation();
        organisation.setCloseDate(1_780_000_000_000L);
        eprel.setOrganisation(organisation);
        product.setEprelDatas(eprel);

        ProductTimelineDto dto = service.mapTimeline(product);

        assertThat(dto).isNotNull();
        assertThat(dto.events())
                .extracting(ProductTimelineEventDto::type)
                .contains(
                        ProductTimelineEventType.EPREL_ON_MARKET_START,
                        ProductTimelineEventType.EPREL_ON_MARKET_END,
                        ProductTimelineEventType.EPREL_EXPORT,
                        ProductTimelineEventType.EPREL_ORGANISATION_CLOSED);
        assertThat(dto.events())
                .isSortedAccordingTo(Comparator.comparing(ProductTimelineEventDto::timestamp));
    }

    @Test
    void mapTimelineFallsBackToEpochSecondDatesWhenTimestampsMissing() {
        Product product = new Product(4L);
        EprelProduct eprel = new EprelProduct();
        eprel.setOnMarketStartDate(1_600_000_000L);
        product.setEprelDatas(eprel);

        ProductTimelineDto dto = service.mapTimeline(product);

        assertThat(dto).isNotNull();
        assertThat(dto.events())
                .hasSize(1)
                .first()
                .satisfies(event -> {
                    assertThat(event.type()).isEqualTo(ProductTimelineEventType.EPREL_ON_MARKET_START);
                    assertThat(event.timestamp()).isEqualTo(1_600_000_000_000L);
                });
    }
}
