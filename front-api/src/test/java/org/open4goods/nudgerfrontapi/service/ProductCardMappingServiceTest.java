package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.PageMetaDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductOffersDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductScoresDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationResponseDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductCardSearchResponseDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.domain.Sort;

/**
 * Contract of the listing card projection (GOU-2710).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductCardMappingServiceTest {

    @Mock
    private VerticalsConfigService verticalsConfigService;

    private ProductCardMappingService service;
    private ProductDto full;

    @BeforeEach
    void setUp() {
        when(verticalsConfigService.getConfigById("tv")).thenReturn(ProductDtoFixtures.tvVerticalConfig());
        service = new ProductCardMappingService(verticalsConfigService);
        full = ProductDtoFixtures.tvListingProduct();
    }

    @Test
    void cardKeepsTheValuesACardReads() {
        ProductCardDto card = service.toCard(full, service.cardAttributeKeys("tv", Set.of()));

        assertThat(card.gtin()).isEqualTo(full.gtin());
        assertThat(card.slug()).isEqualTo(full.slug());
        assertThat(card.fullSlug()).isEqualTo(full.fullSlug());
        assertThat(card.base().vertical()).isEqualTo("tv");
        assertThat(card.base().bestName()).isEqualTo(full.base().bestName());
        assertThat(card.base().coverImagePath()).isEqualTo(full.base().coverImagePath());
        assertThat(card.identity().brand()).isEqualTo(full.identity().brand());
        assertThat(card.identity().model()).isEqualTo(full.identity().model());
        assertThat(card.names().cardName()).isEqualTo(full.names().cardName());
        assertThat(card.names().displayName()).isEqualTo(full.names().displayName());
        assertThat(card.attributes().referentialAttributes()).isEqualTo(full.attributes().referentialAttributes());
        assertThat(card.resources().coverImagePath()).isEqualTo(full.resources().coverImagePath());
        assertThat(card.resources().externalCover()).isEqualTo(full.resources().externalCover());
        assertThat(card.resources().images()).singleElement()
                .satisfies(image -> {
                    assertThat(image.url()).isEqualTo(full.resources().images().get(0).url());
                    assertThat(image.originalUrl()).isEqualTo(full.resources().images().get(0).originalUrl());
                });

        assertThat(card.scores().impactScore()).isEqualTo(full.scores().impactScore());
        assertThat(card.scores().scores()).containsOnlyKeys("ECOSCORE");
        assertThat(card.scores().scores().get("ECOSCORE").value())
                .isEqualTo(full.scores().scores().get("ECOSCORE").value());

        ProductOffersDto offers = full.offers();
        assertThat(card.offers().offersCount()).isEqualTo(offers.offersCount());
        assertThat(card.offers().bestPrice().price()).isEqualTo(offers.bestPrice().price());
        assertThat(card.offers().bestPrice().url()).isEqualTo(offers.bestPrice().url());
        assertThat(card.offers().bestNewOffer().datasourceName()).isEqualTo(offers.bestNewOffer().datasourceName());
        assertThat(card.offers().bestNewOffer().shortPrice()).isEqualTo(offers.bestNewOffer().shortPrice());
        assertThat(card.offers().bestNewOffer().currency()).isEqualTo(offers.bestNewOffer().currency());
        assertThat(card.offers().bestOccasionOffer()).isNull();
        assertThat(card.offers().offersByCondition().get(ProductCondition.NEW))
                .hasSameSizeAs(offers.offersByCondition().get(ProductCondition.NEW));
        assertThat(card.offers().newTrend()).isEqualTo(offers.newTrend());
    }

    @Test
    void indexedAttributesAreTheListingKeysOfTheVerticalAndTheSort() {
        Set<String> sortKeys = ProductCardMappingService.attributeKeysOf(
                Sort.by(Sort.Order.desc("attributes.indexed.WEIGHT.numericValue")));
        ProductCardDto card = service.toCard(full, service.cardAttributeKeys("tv", sortKeys));

        assertThat(card.attributes().indexedAttributes()).containsOnlyKeys("WEIGHT", "DIAGONALE_POUCES",
                "DISPLAY_TECHNOLOGY", "HDMI_PORTS_QUANTITY", "REPAIRABILITY_INDEX", "CLASSE_ENERGY");
        assertThat(card.attributes().indexedAttributes().get("DIAGONALE_POUCES"))
                .satisfies(attribute -> {
                    assertThat(attribute.value()).isEqualTo("40");
                    assertThat(attribute.numericValue()).isEqualTo(40d);
                    assertThat(attribute.name()).isEqualTo(
                            full.attributes().indexedAttributes().get("DIAGONALE_POUCES").name());
                });
    }

    @Test
    void attributeKeysOfReadsIndexedAndReferentialSortMappings() {
        Sort sort = Sort.by(
                Sort.Order.asc("attributes.indexed.WEIGHT.numericValue"),
                Sort.Order.asc("attributes.referentielAttributes.BRAND"),
                Sort.Order.desc("price.minPrice.price"),
                Sort.Order.desc("scores.ECOSCORE.value"));

        assertThat(ProductCardMappingService.attributeKeysOf(sort)).containsExactly("WEIGHT", "BRAND");
        assertThat(ProductCardMappingService.attributeKeysOf(Sort.unsorted())).isEmpty();
        assertThat(ProductCardMappingService.attributeKeysOf(null)).isEmpty();
    }

    @Test
    void unknownVerticalKeepsOnlyTheSortAttributes() {
        assertThat(service.cardAttributeKeys("unknown", Set.of("WEIGHT"))).containsExactly("WEIGHT");
        assertThat(service.cardAttributeKeys(null, Set.of())).isEmpty();
    }

    @Test
    void cardWeighsLessThanFiveKilobytesAndDropsWhatCardsNeverRead() {
        String fullJson = ProductDtoFixtures.MAPPER.writeValueAsString(full);
        String cardJson = ProductDtoFixtures.MAPPER.writeValueAsString(
                service.toCard(full, service.cardAttributeKeys("tv", Set.of())));

        assertThat(fullJson.getBytes(StandardCharsets.UTF_8).length).isGreaterThan(40_000);
        assertThat(cardJson.getBytes(StandardCharsets.UTF_8).length).isLessThan(5_000);
        assertThat(cardJson).doesNotContain("\"sourcing\"", "\"allAttributes\"", "\"classifiedAttributes\"",
                "\"newHistory\"", "\"occasionHistory\"", "\"videos\"", "\"pdfs\"", "\"compensation\"",
                "\"offerName\"", "\"ranking\"", "\"metaDescription\"", "DATA_QUALITY", "\"affiliationToken\":null");
    }

    @Test
    void absentFacetsAndValuesStayAbsent() {
        ProductDto bare = new ProductDto(42L, null, null, null, null, null, null, null, null,
                new ProductScoresDto(null, null, null, null), null,
                new ProductOffersDto(0, false, null, null, null, null, null, null, null, null, null, null, null),
                null);

        ProductCardDto card = service.toCard(bare, Set.of("WEIGHT"));
        String json = ProductDtoFixtures.MAPPER.writeValueAsString(card);

        assertThat(card.base()).isNull();
        assertThat(card.attributes()).isNull();
        assertThat(card.resources()).isNull();
        assertThat(card.scores().scores()).isNull();
        assertThat(card.scores().impactScore()).isNull();
        assertThat(card.offers().offersCount()).isZero();
        assertThat(card.offers().bestPrice()).isNull();
        assertThat(card.offers().offersByCondition()).isNull();
        assertThat(json).isEqualTo("{\"gtin\":42,\"scores\":{},\"offers\":{\"offersCount\":0}}");
        assertThat(service.toCard(null, Set.of())).isNull();
    }

    @Test
    void cardResponseKeepsPaginationAndAggregations() {
        PageMetaDto meta = new PageMetaDto(1, 15, 2124, 142);
        List<AggregationResponseDto> aggregations = List.of();
        ProductSearchResponseDto response = new ProductSearchResponseDto(new PageDto<>(meta, List.of(full)),
                aggregations);

        ProductCardSearchResponseDto cards = service.toCardResponse(response, Sort.unsorted());

        assertThat(cards.products().page()).isEqualTo(meta);
        assertThat(cards.products().data()).singleElement()
                .satisfies(card -> assertThat(card.gtin()).isEqualTo(full.gtin()));
        assertThat(cards.aggregations()).isSameAs(aggregations);
        assertThat(service.toCardResponse(null, null).products()).isNull();
    }

    /**
     * The card of the television fixture is kept as a golden file: the frontend renders it next to the full DTO
     * and checks that cards, list rows and table rows are identical ({@code ProductCardDto.spec.ts}). Regenerate it
     * with {@code -Dgolden.update=true} after a deliberate change of the card contract.
     */
    @Test
    void cardMatchesTheGoldenFixtureSharedWithTheFrontend() throws Exception {
        ProductCardDto card = service.toCard(full, service.cardAttributeKeys("tv", Set.of()));
        String json = ProductDtoFixtures.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(card) + "\n";
        java.nio.file.Path golden = java.nio.file.Path.of("src/test/resources/fixtures/product-card-tv-listing.json");

        if (Boolean.getBoolean("golden.update")) {
            java.nio.file.Files.writeString(golden, json);
        }

        assertThat(ProductDtoFixtures.MAPPER.readTree(java.nio.file.Files.readString(golden)))
                .isEqualTo(ProductDtoFixtures.MAPPER.readTree(json));
    }
}
