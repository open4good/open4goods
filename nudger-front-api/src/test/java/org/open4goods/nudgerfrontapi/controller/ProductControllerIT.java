package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import java.util.List;

import org.open4goods.model.ai.AiReview;
import org.open4goods.nudgerfrontapi.controller.api.ProductController;
import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductView;
import org.open4goods.nudgerfrontapi.dto.ReviewDto;
import org.open4goods.nudgerfrontapi.dto.product.ImpactScoreDto;
import org.open4goods.nudgerfrontapi.dto.product.OfferDto;
import org.open4goods.nudgerfrontapi.service.ProductService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
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
    private ProductService service;

    @Autowired
    private HealthEndpoint healthEndpoint;


    @Test
    void productEndpointReturnsViewAndHealthUp() throws Exception {
        long gtin = 123L;
        ProductViewRequest req = new ProductViewRequest(gtin);
        given(service.getProduct(any())).willReturn(new ProductView(req));

        mockMvc.perform(get("/product/{gtin}", gtin).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gtin").value(gtin))
            .andExpect(jsonPath("$.metadatas").doesNotExist());

        assert healthEndpoint.health().getStatus().equals(Status.UP);
    }

    @Test
    void reviewsEndpointReturnsList() throws Exception {
        long gtin = 123L;
        given(service.getReviews(any())).willReturn(List.of(new ReviewDto("fr", new AiReview(), 1L)));

        mockMvc.perform(get("/product/{gtin}/reviews", gtin).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "public, max-age=3600"));
    }

    @Test
    void offersEndpointReturnsList() throws Exception {
        long gtin = 123L;
        given(service.getOffers(any())).willReturn(List.of(new OfferDto("ds","offer",1.0,"EUR","url")));

        mockMvc.perform(get("/product/{gtin}/offers", gtin).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "public, max-age=3600"));
    }

    @Test
    void impactEndpointReturnsScore() throws Exception {
        long gtin = 123L;
        given(service.getImpactScore(any())).willReturn(new ImpactScoreDto(Map.of("S",1d),1d));

        mockMvc.perform(get("/product/{gtin}/impact", gtin).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "public, max-age=3600"));
    }

    @Test
    void postReviewUsesCaptcha() throws Exception {
        long gtin = 123L;

        mockMvc.perform(post("/product/{gtin}/reviews", gtin)
                .param("hcaptchaResponse", "resp")
                .with(jwt()))
            .andExpect(status().isAccepted());
    }
}
