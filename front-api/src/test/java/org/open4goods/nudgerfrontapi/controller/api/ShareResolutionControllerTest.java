package org.open4goods.nudgerfrontapi.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.share.ShareCandidateDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionRequestDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionResponseDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionStatus;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.share.ShareResolutionService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link ShareResolutionController}.
 */
class ShareResolutionControllerTest {

    private ShareResolutionService shareResolutionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        shareResolutionService = mock(ShareResolutionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ShareResolutionController(shareResolutionService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void createResolutionReturnsAcceptedSnapshot() throws Exception {
        ShareResolutionResponseDto pending = new ShareResolutionResponseDto("token", ShareResolutionStatus.PENDING,
                "https://example.org", Instant.parse("2024-06-10T10:00:00Z"), null, null, List.of(), null);
        when(shareResolutionService.createResolution(any(ShareResolutionRequestDto.class), eq(DomainLanguage.fr)))
                .thenReturn(pending);

        mockMvc.perform(post("/share/resolutions")
                        .param("domainLanguage", "fr")
                        .contentType("application/json")
                        .content("{\"url\":\"https://example.org\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Locale", "fr"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    void getResolutionReturnsNotFoundWhenMissing() throws Exception {
        when(shareResolutionService.getResolution("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/share/resolutions/missing").param("domainLanguage", "en"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getResolutionReturnsSnapshot() throws Exception {
        ShareCandidateDto candidate = new ShareCandidateDto("1", "name", null, null, null, null, 0.5d);
        ShareResolutionResponseDto response = new ShareResolutionResponseDto("token", ShareResolutionStatus.RESOLVED,
                "https://example.org", Instant.parse("2024-06-10T10:00:00Z"), Instant.parse("2024-06-10T10:00:01Z"), null,
                List.of(candidate), null);
        when(shareResolutionService.getResolution("token")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/share/resolutions/token").param("domainLanguage", "en"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "en"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.candidates[0].productId").value("1"));
    }
}
