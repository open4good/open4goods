package org.open4goods.nudgerfrontapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.junit.jupiter.api.Test;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.ai.AiReview;
import org.open4goods.nudgerfrontapi.controller.api.ProductController;
import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.PageMetaDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductReviewDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.Filter;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterField;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterOperator;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;
import org.open4goods.nudgerfrontapi.dto.RequestMetadata;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import org.open4goods.nudgerfrontapi.config.TestTextEmbeddingConfig;

@SpringBootTest(properties = {"front.cache.path=${java.io.tmpdir}",
        "front.security.enabled=true",
        "front.security.shared-token=test-token"})
@AutoConfigureMockMvc
@Import(TestTextEmbeddingConfig.class)
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductController controller;

    @MockBean
    private ProductMappingService service;

    @MockBean
    private VerticalsConfigService verticalsConfigService;

    @Autowired
    private HealthEndpoint healthEndpoint;
    private static final String SHARED_TOKEN = "test-token";
    @Test
    void reviewsEndpointReturnsList() throws Exception {
        long gtin = 123L;
        var page = new PageImpl<>(List.of(new ProductReviewDto("fr", new AiReview(), 1L)), PageRequest.of(0, 20), 1);
        given(service.getReviews(anyLong(), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/products/{gtin}/reviews", gtin)
                .header("Accept-Language", "de")
                .header("X-Shared-Token", SHARED_TOKEN)
                .param("domainLanguage", "fr")
                .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "public, max-age=3600"))
            .andExpect(header().string("X-Locale", "de"))
            .andExpect(header().exists("Link"))
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void includeParameterFiltersFields() throws Exception {
        long gtin = 321L;
        given(service.getProduct(anyLong(), any(Locale.class), anySet(), any(DomainLanguage.class)))
                .willReturn(new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null));

        mockMvc.perform(get("/products/{gtin}", gtin)
                        .param("include", "gtin")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gtin").value(gtin))
                .andExpect(jsonPath("$.metadatas").doesNotExist());
    }

    @Test
    void productEndpointReturns404WhenServiceThrows() throws Exception {
        long gtin = 999L;
        given(service.getProduct(anyLong(), any(Locale.class), anySet(), any(DomainLanguage.class)))
                .willThrow(new ResourceNotFoundException());

        mockMvc.perform(get("/products/{gtin}", gtin)
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void productsEndpointReturnsPage() throws Exception {
        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), nullable(FilterRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/products")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=3600"))
                .andExpect(jsonPath("$.products.page.number").value(0));

    }

    @Test
    void productsEndpointParsesAggregationArraySyntax() throws Exception {
        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), nullable(FilterRequestDto.class)))
                .willReturn(responseDto);

        VerticalConfig config = new VerticalConfig();
        config.setId("electronics");
        given(verticalsConfigService.getConfigById("electronics")).willReturn(config);

        mockMvc.perform(post("/products")
                        .param("aggs", "[{\"name\":\"per_price\",\"field\":\"price.minPrice.price\",\"type\":\"terms\"}]")
                        .param("verticalId", "electronics")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk());

        ArgumentCaptor<AggregationRequestDto> captor = ArgumentCaptor.forClass(AggregationRequestDto.class);
        then(service).should().searchProducts(any(Pageable.class), any(Locale.class), anySet(), captor.capture(), any(DomainLanguage.class), nullable(String.class), nullable(String.class), nullable(FilterRequestDto.class));

        AggregationRequestDto aggregationRequestDto = captor.getValue();
        assertThat(aggregationRequestDto.aggs()).hasSize(1);
        AggregationRequestDto.Agg agg = aggregationRequestDto.aggs().get(0);
        assertThat(agg.name()).isEqualTo("per_price");
        assertThat(agg.field()).isEqualTo("price.minPrice.price");
    }

    @Test
    void productsEndpointParsesAggregationObjectSyntax() throws Exception {
        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), nullable(FilterRequestDto.class)))
                .willReturn(responseDto);

        VerticalConfig config = new VerticalConfig();
        config.setId("electronics");
        given(verticalsConfigService.getConfigById("electronics")).willReturn(config);

        mockMvc.perform(post("/products")
                        .param("aggs", "{\"aggs\":[{\"name\":\"per_price\",\"field\":\"price.minPrice.price\",\"type\":\"terms\"}]}")
                        .param("verticalId", "electronics")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk());

        ArgumentCaptor<AggregationRequestDto> captor = ArgumentCaptor.forClass(AggregationRequestDto.class);
        then(service).should().searchProducts(any(Pageable.class), any(Locale.class), anySet(), captor.capture(), any(DomainLanguage.class), nullable(String.class), nullable(String.class), nullable(FilterRequestDto.class));

        AggregationRequestDto aggregationRequestDto = captor.getValue();
        assertThat(aggregationRequestDto.aggs()).hasSize(1);
        AggregationRequestDto.Agg agg = aggregationRequestDto.aggs().get(0);
        assertThat(agg.name()).isEqualTo("per_price");
        assertThat(agg.field()).isEqualTo("price.minPrice.price");
    }


    @Test
    void productsEndpointReturns400OnInvalidFilters() throws Exception {
        mockMvc.perform(post("/products")
                        .param("filters", "{invalid")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productsEndpointReturns400OnInvalidSort() throws Exception {
        mockMvc.perform(post("/products")
                        .param("sort", "invalid,asc")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productsEndpointRejectsAggregationWithoutVertical() throws Exception {
        mockMvc.perform(post("/products")
                        .param("aggs", "[{\"name\":\"by_price\",\"field\":\"price.minPrice.price\",\"type\":\"range\"}]")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productsEndpointReturns400OnInvalidInclude() throws Exception {
        mockMvc.perform(post("/products")
                        .param("include", "wrong")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productsEndpointAllowsVerticalAttributeSort() throws Exception {
        VerticalConfig config = verticalConfigWithNumericAttribute("battery_life");
        given(verticalsConfigService.getConfigById("electronics")).willReturn(config);

        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), nullable(FilterRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/products")
                        .param("sort", "attributes.indexed.battery_life.numericValue,asc")
                        .param("verticalId", "electronics")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk());
    }

    @Test
    void productsEndpointAllowsGlobalSortWhenVerticalSpecified() throws Exception {
        VerticalConfig config = new VerticalConfig();
        config.setId("electronics");
        given(verticalsConfigService.getConfigById("electronics")).willReturn(config);

        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), nullable(FilterRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/products")
                        .param("sort", "attributes.referentielAttributes.BRAND,asc")
                        .param("verticalId", "electronics")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk());
    }

    @Test
    void productsEndpointAcceptsJsonSortSyntax() throws Exception {
        VerticalConfig config = verticalConfigWithNumericAttribute("battery_life");
        given(verticalsConfigService.getConfigById("electronics")).willReturn(config);

        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), nullable(FilterRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/products")
                        .param("sort", "[{\"field\":\"attributes.indexed.battery_life.numericValue\",\"order\":\"desc\"}]")
                        .param("verticalId", "electronics")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk());
    }

    @Test
    void productsEndpointRejectsSortOutsideVerticalMetadata() throws Exception {
        VerticalConfig config = verticalConfigWithNumericAttribute("battery_life");
        given(verticalsConfigService.getConfigById("electronics")).willReturn(config);

        mockMvc.perform(post("/products")
                        .param("sort", "attributes.indexed.weight.numericValue,desc")
                        .param("verticalId", "electronics")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productsEndpointAllowsVerticalAggregation() throws Exception {
        VerticalConfig config = verticalConfigWithNumericAttribute("battery_life");
        given(verticalsConfigService.getConfigById("electronics")).willReturn(config);

        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), nullable(FilterRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/products")
                        .param("aggs", "{\"aggs\":[{\"name\":\"by_battery\",\"field\":\"attributes.indexed.battery_life.numericValue\",\"type\":\"range\"}]}")
                        .param("verticalId", "electronics")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk());
    }

    @Test
    void productsEndpointRejectsAggregationOutsideVerticalMetadata() throws Exception {
        VerticalConfig config = verticalConfigWithNumericAttribute("battery_life");
        given(verticalsConfigService.getConfigById("electronics")).willReturn(config);

        mockMvc.perform(post("/products")
                        .param("aggs", "{\"aggs\":[{\"name\":\"by_weight\",\"field\":\"attributes.indexed.weight.numericValue\",\"type\":\"range\"}]}")
                        .param("verticalId", "electronics")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productsEndpointAllowsVerticalAttributeFilter() throws Exception {
        VerticalConfig config = verticalConfigWithNumericAttribute("battery_life");
        given(verticalsConfigService.getConfigById("electronics")).willReturn(config);

        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), any(FilterRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/products")
                        .param("filters", "{\"filters\":[{\"field\":\"attributes.indexed.battery_life.numericValue\",\"operator\":\"range\",\"min\":10,\"max\":50}]}")
                        .param("verticalId", "electronics")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk());
    }

    @Test
    void productsEndpointRejectsFilterOutsideVerticalMetadata() throws Exception {
        VerticalConfig config = verticalConfigWithNumericAttribute("battery_life");
        given(verticalsConfigService.getConfigById("electronics")).willReturn(config);

        mockMvc.perform(post("/products")
                        .param("filters", "{\"filters\":[{\"field\":\"attributes.indexed.weight.numericValue\",\"operator\":\"range\",\"min\":10,\"max\":50}]}")
                        .param("verticalId", "electronics")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sortableFieldsEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/products/fields/sortable")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void sortableFieldsForVerticalReturnsList() throws Exception {
        VerticalConfig config = new VerticalConfig();
        config.setId("oven");
        given(verticalsConfigService.getConfigById("oven")).willReturn(config);

        mockMvc.perform(get("/products/fields/sortable/oven")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_FRONTEND)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.global").isArray())
                .andExpect(jsonPath("$.global[?(@.mapping=='scores.ECOSCORE.value')]").exists());
    }

    @Test
    void productsEndpointParsesFilterArraySyntax() throws Exception {
        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), any(FilterRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/products")
                        .param("filters", "[{\"field\":\"price\",\"operator\":\"range\",\"min\":10,\"max\":50}]")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk());

        ArgumentCaptor<FilterRequestDto> captor = ArgumentCaptor.forClass(FilterRequestDto.class);
        then(service).should().searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), captor.capture());

        FilterRequestDto filterRequestDto = captor.getValue();
        assertThat(filterRequestDto.filters()).hasSize(1);
        Filter filter = filterRequestDto.filters().get(0);
        assertThat(filter.field()).isEqualTo(FilterField.price);
        assertThat(filter.operator()).isEqualTo(FilterOperator.range);
    }

    @Test
    void productsEndpointParsesFilterObjectSyntax() throws Exception {
        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), any(FilterRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/products")
                        .param("filters", "{\"filters\":[{\"field\":\"price\",\"operator\":\"range\",\"min\":10,\"max\":50}]}")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk());

        ArgumentCaptor<FilterRequestDto> captor = ArgumentCaptor.forClass(FilterRequestDto.class);
        then(service).should().searchProducts(any(Pageable.class), any(Locale.class), anySet(), nullable(AggregationRequestDto.class), any(DomainLanguage.class), nullable(String.class), nullable(String.class), captor.capture());

        FilterRequestDto filterRequestDto = captor.getValue();
        assertThat(filterRequestDto.filters()).hasSize(1);
        Filter filter = filterRequestDto.filters().get(0);
        assertThat(filter.field()).isEqualTo(FilterField.price);
        assertThat(filter.operator()).isEqualTo(FilterOperator.range);
    }

    @Test
    void componentsEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/products/fields/components")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void aggregatableFieldsEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/products/fields/aggregatable")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }




    @Test
    void postReviewUsesCaptcha() throws Exception {
        long gtin = 123L;
        given(service.createReview(anyLong(), anyString(), any(HttpServletRequest.class))).willReturn(gtin);

        mockMvc.perform(post("/products/{gtin}/review", gtin)
                .param("hcaptchaResponse", "resp")
                .param("domainLanguage", "fr")
                .header("X-Shared-Token", SHARED_TOKEN)
                .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
            .andExpect(status().isOk());
    }

    @Test
    void postReviewRejectsWithoutAuthentication() throws Exception {
        long gtin = 456L;

        mockMvc.perform(post("/products/{gtin}/review", gtin)
                .param("hcaptchaResponse", "resp")
                .param("domainLanguage", "fr"))
            .andExpect(status().isUnauthorized());
    }

    private VerticalConfig verticalConfigWithNumericAttribute(String attributeKey) {
        VerticalConfig config = new VerticalConfig();
        config.setId("electronics");
        config.setTechnicalFilters(List.of(attributeKey));
        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey(attributeKey);
        attributeConfig.setFilteringType(AttributeType.NUMERIC);
        AttributesConfig attributesConfig = new AttributesConfig();
        attributesConfig.setConfigs(List.of(attributeConfig));
        config.setAttributesConfig(attributesConfig);
        return config;
    }
}
