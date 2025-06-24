package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.service.ProductViewService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductController controller;

    @MockBean
    private ProductViewService renderingService;

    @MockBean
    private ProductRepository repository;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "renderingService", renderingService);
        ReflectionTestUtils.setField(controller, "repository", repository);
    }

    @Test
    void productEndpointReturnsViewAndHealthUp() throws Exception {
        long gtin = 123L;
        ProductViewRequest req = new ProductViewRequest(gtin);
        given(renderingService.render(any())).willReturn(new ProductViewResponse(req));

        mockMvc.perform(get("/product/{gtin}", gtin).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gtin").value(gtin));

        assert healthEndpoint.health().getStatus().equals(Status.UP);
    }
}
