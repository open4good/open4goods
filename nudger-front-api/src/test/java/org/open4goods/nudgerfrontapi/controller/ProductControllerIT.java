package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.ai.AiReview;
import org.open4goods.nudgerfrontapi.controller.api.ProductController;
import org.open4goods.nudgerfrontapi.dto.product.ProductReviewDto;
import org.open4goods.nudgerfrontapi.service.ProductService;
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
    private ProductService service;

    @Autowired
    private HealthEndpoint healthEndpoint;




    @Test
    void reviewsEndpointReturnsList() throws Exception {
        long gtin = 123L;
        given(service.getReviews(any())).willReturn(List.of(new ProductReviewDto("fr", new AiReview(), 1L)));

        mockMvc.perform(get("/product/{gtin}/reviews", gtin).with(jwt()))
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
