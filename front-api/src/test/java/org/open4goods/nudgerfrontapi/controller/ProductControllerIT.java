package org.open4goods.nudgerfrontapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.nullable;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.ArgumentCaptor;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.ai.AiReview;
import org.open4goods.nudgerfrontapi.controller.api.ProductController;
import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.PageMetaDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoAggregatableFields;
import org.open4goods.nudgerfrontapi.dto.product.ProductReviewDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationBucketDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationResponseDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.Filter;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterField;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterOperator;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;
import org.open4goods.nudgerfrontapi.dto.RequestMetadata;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

@SpringBootTest(properties = {"front.cache.path=${java.io.tmpdir}",
        "front.security.enabled=true",
        "front.security.shared-token=test-token"})
@AutoConfigureMockMvc
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductController controller;

    @MockBean
    private ProductMappingService service;

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
                .param("domainLanguage", "FR")
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
                .willReturn(new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null));

        mockMvc.perform(get("/products/{gtin}", gtin)
                        .param("include", "gtin")
                        .param("domainLanguage", "FR")
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
                        .param("domainLanguage", "FR")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void productsEndpointReturnsPage() throws Exception {
        var product = new ProductDto(0L, null, null, null, null, null, null, null, null, null, null, null);
        PageDto<ProductDto> page = new PageDto<>(new PageMetaDto(0, 20, 1, 1), List.of(product));
        ProductSearchResponseDto responseDto = new ProductSearchResponseDto(page, List.of());
        given(service.searchProducts(any(Pageable.class), any(Locale.class), anySet(), any(), any(DomainLanguage.class), nullable(String.class), nullable(String.class), nullable(FilterRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(get("/products")
                        .param("domainLanguage", "FR")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=3600"))
                .andExpect(jsonPath("$.products.page.number").value(0));

    }


    @Test
    void productsEndpointReturns400OnInvalidFilters() throws Exception {
        mockMvc.perform(get("/products")
                        .param("filters", "{invalid")
                        .param("domainLanguage", "FR")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productsEndpointReturns400OnInvalidSort() throws Exception {
        mockMvc.perform(get("/products")
                        .param("sort", "invalid,asc")
                        .param("domainLanguage", "FR")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productsEndpointReturns400OnInvalidInclude() throws Exception {
        mockMvc.perform(get("/products")
                        .param("include", "wrong")
                        .param("domainLanguage", "FR")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sortableFieldsEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/products/fields/sortable")
                        .param("domainLanguage", "FR")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void componentsEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/products/fields/components")
                        .param("domainLanguage", "FR")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void aggregatableFieldsEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/products/fields/aggregatable")
                        .param("domainLanguage", "FR")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }




    @Test
    void postReviewUsesCaptcha() throws Exception {
        long gtin = 123L;

        mockMvc.perform(post("/products/{gtin}/reviews", gtin)
                .param("hcaptchaResponse", "resp")
                .param("domainLanguage", "FR")
                .header("X-Shared-Token", SHARED_TOKEN)
                .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
            .andExpect(status().isAccepted());
    }
}
