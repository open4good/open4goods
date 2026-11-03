package org.open4goods.nudgerfrontapi.controller.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
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
    void getRedirectShouldReturnANonCacheableFound() throws Exception {
        when(affiliationService.trackRedirect(anyString(), anyString()))
                .thenReturn("https://example.com");

        mockMvc.perform(get("/affiliations/redirect")
                .param("token", "token-value")
                .param("domainLanguage", "fr")
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .header(AffiliationController.ORIGIN_HEADER, "/fours/four-encastrable"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("X-Locale", "fr"));

        verify(affiliationService).trackRedirect("token-value", "/fours/four-encastrable");
    }

    @Test
    void getRedirectShouldWorkWithoutOriginHeader() throws Exception {
        when(affiliationService.trackRedirect(anyString(), isNull()))
                .thenReturn("https://example.com");

        mockMvc.perform(get("/affiliations/redirect")
                .param("token", "token-value")
                .param("domainLanguage", "fr"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com"));

        verify(affiliationService).trackRedirect("token-value", null);
    }

    @Test
    void postRedirectShouldReturnANonCacheableFound() throws Exception {
        when(affiliationService.trackRedirect(anyString(), isNull()))
                .thenReturn("https://example.com/post");

        mockMvc.perform(post("/affiliations/redirect")
                .param("token", "token-value")
                .param("domainLanguage", "en")
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11)")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/post"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("X-Locale", "en"));

        verify(affiliationService).trackRedirect("token-value", null);
    }

    @Test
    void shouldReturnBadRequestWhenTokenIsInvalid() throws Exception {
        when(affiliationService.trackRedirect(anyString(), isNull()))
                .thenThrow(new InvalidAffiliationTokenException("Invalid token"));

        mockMvc.perform(get("/affiliations/redirect")
                .param("token", "broken")
                .param("domainLanguage", "fr"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    void redirectShouldEncodeIllegalCharactersInMerchantUrl() throws Exception {
        when(affiliationService.trackRedirect(anyString(), isNull()))
                .thenReturn("https://track.effiliation.com/servlet/effi.redir?id_compteur=1&url=https://www.carrefour.fr/p/jeu de société|v2 {x} é?a=%20b");

        mockMvc.perform(get("/affiliations/redirect")
                .param("token", "t")
                .param("domainLanguage", "fr"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        "https://track.effiliation.com/servlet/effi.redir?id_compteur=1&url=https://www.carrefour.fr/p/jeu%20de%20soci%C3%A9t%C3%A9%7Cv2%20%7Bx%7D%20%C3%A9?a=%20b"));
    }

    @Test
    void redirectShouldEncodeLonePercentBracketsAndExtraFragmentMarker() throws Exception {
        when(affiliationService.trackRedirect(anyString(), isNull()))
                .thenReturn("https://www.carrefour.fr/p/promo-50%-off?f[0]=a%2Fb#top#2");

        mockMvc.perform(get("/affiliations/redirect")
                .param("token", "t")
                .param("domainLanguage", "fr"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        "https://www.carrefour.fr/p/promo-50%25-off?f%5B0%5D=a%2Fb#top%232"));
    }

    @Test
    void redirectShouldRejectNonHttpScheme() throws Exception {
        when(affiliationService.trackRedirect(anyString(), isNull()))
                .thenReturn("javascript:alert(1)");

        mockMvc.perform(get("/affiliations/redirect")
                .param("token", "t")
                .param("domainLanguage", "fr"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }
}
