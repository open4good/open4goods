package org.open4goods.nudgerfrontapi.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
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
import org.open4goods.nudgerfrontapi.service.GoogleIndexationDispatchService;
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

    @Mock
    private GoogleIndexationDispatchService googleIndexationDispatchService;

    private ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(productMappingService, verticalsConfigService, searchService, googleIndexationDispatchService);
    }

    @Test
    void aggregatedScoreFiltersArePermitted() {
        // Setup SearchCapabilities mock
        Set<String> allowedFilters = new HashSet<>();
        allowedFilters.add("scores.ENERGY_CONSUMPTION.value");
        SearchService.SearchCapabilities capabilities = new SearchService.SearchCapabilities(allowedFilters, Set.of(), Set.of());
        when(searchService.buildSearchCapabilities(any(), any(), any())).thenReturn(capabilities);

        Filter filter = new Filter("scores.ENERGY_CONSUMPTION.value", FilterOperator.range, null, 0.0, 100.0);
        ProductSearchRequestDto searchRequest = new ProductSearchRequestDto(null, null,
                new FilterRequestDto(List.of(filter), List.of()), null, null);

        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of());
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        when(searchService.searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(), any(), any(), any(), any(),
                anyBoolean(), any()))
                .thenReturn(responseDto);

        ResponseEntity<ProductSearchResponseDto> response = controller.products(PageRequest.of(0, 20), Set.of(),
                "electronics", null, DomainLanguage.fr, Locale.FRANCE, searchRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<FilterRequestDto> filterCaptor = ArgumentCaptor.forClass(FilterRequestDto.class);
        verify(searchService).searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(), any(), any(), any(),
                filterCaptor.capture(), anyBoolean(), any());

        assertThat(filterCaptor.getValue().filters()).extracting(Filter::field)
                .contains("scores.ENERGY_CONSUMPTION.value");
    }

    @Test
    void attributeFiltersNotListedExplicitlyArePermitted() {
        // Setup SearchCapabilities mock
        Set<String> allowedFilters = new HashSet<>();
        allowedFilters.add("attributes.indexed.BRAND_SUSTAINALYTICS_SCORING.value");
        SearchService.SearchCapabilities capabilities = new SearchService.SearchCapabilities(allowedFilters, Set.of(), Set.of());
        when(searchService.buildSearchCapabilities(any(), any(), any())).thenReturn(capabilities);

        Filter filter = new Filter("attributes.indexed.BRAND_SUSTAINALYTICS_SCORING.value", FilterOperator.term,
                List.of("AA"), null, null);
        ProductSearchRequestDto searchRequest = new ProductSearchRequestDto(null, null,
                new FilterRequestDto(List.of(filter), List.of()), null, null);

        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of());
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        when(searchService.searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(), any(), any(), any(),
                any(), anyBoolean(), any()))
                .thenReturn(responseDto);

        ResponseEntity<ProductSearchResponseDto> response = controller.products(PageRequest.of(0, 20), Set.of(),
                "tv", null, DomainLanguage.fr, Locale.FRANCE, searchRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<FilterRequestDto> filterCaptor = ArgumentCaptor.forClass(FilterRequestDto.class);
        verify(searchService).searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(), any(), any(), any(),
                filterCaptor.capture(), anyBoolean(), any());

        assertThat(filterCaptor.getValue().filters()).extracting(Filter::field)
                .contains("attributes.indexed.BRAND_SUSTAINALYTICS_SCORING.value");
    }


    @Test
    void compositeScoreFiltersArePermitted() {
        // Setup SearchCapabilities mock
        Set<String> allowedFilters = new HashSet<>();
        allowedFilters.add("scores.ECOSCORE.value");
        SearchService.SearchCapabilities capabilities = new SearchService.SearchCapabilities(allowedFilters, Set.of(), Set.of());
        when(searchService.buildSearchCapabilities(any(), any(), any())).thenReturn(capabilities);

        Filter filter = new Filter("scores.ECOSCORE.value", FilterOperator.range, null, 0.0, 100.0);
        ProductSearchRequestDto searchRequest = new ProductSearchRequestDto(null, null,
                new FilterRequestDto(List.of(filter), List.of()), null, null);

        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of());
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        when(searchService.searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(), any(), any(), any(), any(),
                anyBoolean(), any()))
                .thenReturn(responseDto);

        ResponseEntity<ProductSearchResponseDto> response = controller.products(PageRequest.of(0, 20), Set.of(),
                "tv", null, DomainLanguage.fr, Locale.FRANCE, searchRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void globalSearchWithEmptyAggsShouldBeAllowed() {
        // Setup SearchCapabilities mock for Global Search
        SearchService.SearchCapabilities capabilities = new SearchService.SearchCapabilities(Collections.emptySet(), Set.of(), Set.of());
        // For global search, buildSearchCapabilities might be called with verticalId=null.
        // We can match any args.
        when(searchService.buildSearchCapabilities(any(), any(), any())).thenReturn(capabilities);

        ProductSearchRequestDto searchRequest = new ProductSearchRequestDto(null, null,
                new FilterRequestDto(List.of(), List.of()), null, null);

        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of());
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        
        // Mock service call to succeed
        when(searchService.searchProducts(
                any(Pageable.class), 
                any(Locale.class), 
                anySet(), 
                any(),    // aggs
                any(),    // domainLanguage
                any(),    // verticalId
                any(),    // query
                any(),    // filter
                anyBoolean(), 
                any()))
                .thenReturn(responseDto);

        ResponseEntity<ProductSearchResponseDto> response = controller.products(
                PageRequest.of(0, 20), 
                Set.of(),
                null, // verticalId = null for global
                null, 
                DomainLanguage.fr, 
                Locale.FRANCE, 
                searchRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
