package org.open4goods.nudgerfrontapi.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.PageMetaDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.Filter;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterOperator;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.open4goods.nudgerfrontapi.service.SearchService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link ProductController}.
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductMappingService productMappingService;

    @Mock
    private VerticalsConfigService verticalsConfigService;

    @Mock
    private SearchService searchService;

    private ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(productMappingService, verticalsConfigService, searchService);
    }

    @Test
    void aggregatedScoreFiltersArePermitted() {
        VerticalConfig config = new VerticalConfig();
        config.setId("electronics");

        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey("POWER_USAGE");
        attributeConfig.setAsScore(true);
        attributeConfig.setParticipateInScores(Set.of("ENERGY_CONSUMPTION"));
        config.setAttributesConfig(new AttributesConfig(List.of(attributeConfig)));

        when(verticalsConfigService.getConfigById("electronics")).thenReturn(config);

        Filter filter = new Filter("scores.ENERGY_CONSUMPTION.value", FilterOperator.range, null, 0.0, 100.0);
        ProductSearchRequestDto searchRequest = new ProductSearchRequestDto(null, null,
                new FilterRequestDto(List.of(filter), List.of()), null);

        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of());
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        when(productMappingService.searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(), any(), any(), any(), any(),
                anyBoolean()))
                .thenReturn(responseDto);

        ResponseEntity<ProductSearchResponseDto> response = controller.products(PageRequest.of(0, 20), Set.of(),
                "electronics", null, DomainLanguage.fr, Locale.FRANCE, searchRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<FilterRequestDto> filterCaptor = ArgumentCaptor.forClass(FilterRequestDto.class);
        verify(productMappingService).searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(), any(), any(), any(),
                filterCaptor.capture(), anyBoolean());

        assertThat(filterCaptor.getValue().filters()).extracting(Filter::field)
                .contains("scores.ENERGY_CONSUMPTION.value");
    }

    @Test
    void attributeFiltersNotListedExplicitlyArePermitted() {
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");

        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey("BRAND_SUSTAINABILITY");
        config.setAttributesConfig(new AttributesConfig(List.of(attributeConfig)));

        when(verticalsConfigService.getConfigById("tv")).thenReturn(config);

        Filter filter = new Filter("attributes.indexed.BRAND_SUSTAINABILITY.value", FilterOperator.term,
                List.of("AA"), null, null);
        ProductSearchRequestDto searchRequest = new ProductSearchRequestDto(null, null,
                new FilterRequestDto(List.of(filter), List.of()), null);

        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of());
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        when(productMappingService.searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(), any(), any(), any(),
                any(), anyBoolean()))
                .thenReturn(responseDto);

        ResponseEntity<ProductSearchResponseDto> response = controller.products(PageRequest.of(0, 20), Set.of(),
                "tv", null, DomainLanguage.fr, Locale.FRANCE, searchRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<FilterRequestDto> filterCaptor = ArgumentCaptor.forClass(FilterRequestDto.class);
        verify(productMappingService).searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(), any(), any(), any(),
                filterCaptor.capture(), anyBoolean());

        assertThat(filterCaptor.getValue().filters()).extracting(Filter::field)
                .contains("attributes.indexed.BRAND_SUSTAINABILITY.value");
    }
}
