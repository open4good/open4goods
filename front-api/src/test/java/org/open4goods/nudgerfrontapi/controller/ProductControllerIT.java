package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
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
import org.open4goods.model.ai.AiReview;
import org.open4goods.nudgerfrontapi.controller.api.ProductController;
import org.open4goods.nudgerfrontapi.dto.product.ProductReviewDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.RequestMetadata;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
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




    @Test
    void reviewsEndpointReturnsList() throws Exception {
        long gtin = 123L;
        var page = new PageImpl<>(List.of(new ProductReviewDto("fr", new AiReview(), 1L)), PageRequest.of(0, 20), 1);
        given(service.getReviews(anyLong(), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/products/{gtin}/reviews", gtin)
                .header("Accept-Language", "de")
                .with(jwt()))
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
        given(service.getProduct(anyLong(), Locale.ENGLISH, anySet())).willReturn(new ProductDto());

        mockMvc.perform(get("/products/{gtin}", gtin)
                        .param("include", "gtin")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gtin").value(gtin))
                .andExpect(jsonPath("$.metadatas").doesNotExist());
    }

    @Test
    void productEndpointReturns404WhenServiceThrows() throws Exception {
        long gtin = 999L;
        given(service.getProduct(anyLong(), any(Locale.class), anySet()))
                .willThrow(new ResourceNotFoundException());

        mockMvc.perform(get("/products/{gtin}", gtin).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void productsEndpointReturnsPage() throws Exception {
        var page = new PageImpl<>(List.of(new ProductDto()), PageRequest.of(0, 20), 1);
        given(service.getProducts(any(Pageable.class), any(Locale.class), anySet())).willReturn(page);

        mockMvc.perform(get("/products")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=3600"))
                .andExpect(jsonPath("$.page.number").value(0));
    }




    @Test
    void postReviewUsesCaptcha() throws Exception {
        long gtin = 123L;

        mockMvc.perform(post("/products/{gtin}/reviews", gtin)
                .param("hcaptchaResponse", "resp")
                .with(jwt()))
            .andExpect(status().isAccepted());
    }
}
