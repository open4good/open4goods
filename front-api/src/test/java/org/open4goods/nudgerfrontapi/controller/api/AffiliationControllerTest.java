package org.open4goods.nudgerfrontapi.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.nudgerfrontapi.controller.advice.GlobalExceptionHandler;
import org.open4goods.nudgerfrontapi.service.AffiliationService;
import org.open4goods.nudgerfrontapi.service.exception.InvalidAffiliationTokenException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link AffiliationController}.
 */
@ExtendWith(MockitoExtension.class)
class AffiliationControllerTest {

    @Mock
    private AffiliationService affiliationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AffiliationController controller = new AffiliationController(affiliationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void getRedirectShouldReturnMovedPermanently() throws Exception {
        when(affiliationService.trackRedirect(anyString(), anyString(), any()))
                .thenReturn("https://example.com");

        mockMvc.perform(get("/affiliations/redirect")
                .param("token", "token-value")
                .param("domainLanguage", "fr")
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .with(remoteAddress("203.0.113.10")))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com"))
                .andExpect(header().string("X-Locale", "fr"));

        verify(affiliationService).trackRedirect("token-value", "203.0.113.10", "Mozilla/5.0");
    }

    @Test
    void postRedirectShouldReturnMovedPermanently() throws Exception {
        when(affiliationService.trackRedirect(anyString(), anyString(), any()))
                .thenReturn("https://example.com/post");

        mockMvc.perform(post("/affiliations/redirect")
                .param("token", "token-value")
                .param("domainLanguage", "en")
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11)")
                .with(remoteAddress("198.51.100.7"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/post"))
                .andExpect(header().string("X-Locale", "en"));

        verify(affiliationService).trackRedirect("token-value", "198.51.100.7", "Mozilla/5.0 (X11)");
    }

    @Test
    void shouldReturnBadRequestWhenTokenIsInvalid() throws Exception {
        when(affiliationService.trackRedirect(anyString(), anyString(), any()))
                .thenThrow(new InvalidAffiliationTokenException("Invalid token"));

        mockMvc.perform(get("/affiliations/redirect")
                .param("token", "broken")
                .param("domainLanguage", "fr")
                .with(remoteAddress("192.0.2.1")))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    private RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
