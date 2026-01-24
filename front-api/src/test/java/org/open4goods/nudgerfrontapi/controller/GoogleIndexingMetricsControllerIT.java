package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.config.TestTextEmbeddingConfig;
import org.open4goods.nudgerfrontapi.controller.api.GoogleIndexingMetricsController;
import org.open4goods.nudgerfrontapi.service.GoogleIndexingService;
import org.open4goods.nudgerfrontapi.service.dto.GoogleIndexingMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"front.cache.path=${java.io.tmpdir}",
        "front.security.enabled=true",
        "front.security.shared-token=test-token",
        "front.google-indexing.site-base-url=https://example.org"})
@AutoConfigureMockMvc
@Import(TestTextEmbeddingConfig.class)
class GoogleIndexingMetricsControllerIT {

    private static final String SHARED_TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GoogleIndexingMetricsController controller;

    @MockBean
    private GoogleIndexingService googleIndexingService;

    @Test
    void metricsEndpointReturnsSnapshot() throws Exception {
        GoogleIndexingMetrics metrics = new GoogleIndexingMetrics(
                true,
                2,
                4,
                1,
                Instant.parse("2024-05-01T10:15:30Z"),
                Instant.parse("2024-05-02T10:15:30Z"),
                50,
                Duration.ofMinutes(30),
                5,
                true);
        given(googleIndexingService.metrics()).willReturn(metrics);

        mockMvc.perform(get("/metrics/google-indexing")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.pendingCount").value(2))
                .andExpect(jsonPath("$.indexedCount").value(4))
                .andExpect(jsonPath("$.deadLetterCount").value(1))
                .andExpect(jsonPath("$.batchSize").value(50))
                .andExpect(jsonPath("$.maxAttempts").value(5));
    }
}
