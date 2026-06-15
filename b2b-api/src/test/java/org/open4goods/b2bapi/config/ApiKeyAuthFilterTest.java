package org.open4goods.b2bapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.b2bapi.exception.InvalidApiKeyException;
import org.open4goods.b2bapi.service.ApiKeyAuthenticationService;
import org.open4goods.b2bapi.service.ApiKeyPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for the Product API bearer key servlet filter.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onlyFiltersProductApiPaths() {
        final ApiKeyAuthFilter filter = new ApiKeyAuthFilter(apiKeyAuthenticationService);

        assertThat(filter.shouldNotFilter(request("/api/v1/products/abc"))).isFalse();
        assertThat(filter.shouldNotFilter(request("/api/v1/auth/me"))).isTrue();
        assertThat(filter.shouldNotFilter(request("/actuator/health"))).isTrue();
    }

    @Test
    void rejectsMissingBearerHeader() throws Exception {
        final ApiKeyAuthFilter filter = new ApiKeyAuthFilter(apiKeyAuthenticationService);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicBoolean continued = new AtomicBoolean(false);

        filter.doFilterInternal(request("/api/v1/products/abc"), response, markContinued(continued));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(continued).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsInvalidBearerKey() throws Exception {
        when(apiKeyAuthenticationService.authenticate("pdapi_bad"))
                .thenThrow(new InvalidApiKeyException("Unknown API key."));
        final ApiKeyAuthFilter filter = new ApiKeyAuthFilter(apiKeyAuthenticationService);
        final MockHttpServletRequest request = request("/api/v1/products/abc");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer pdapi_bad");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicBoolean continued = new AtomicBoolean(false);

        filter.doFilterInternal(request, response, markContinued(continued));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(continued).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authenticatesBearerKeyAndContinuesChain() throws Exception {
        final var authentication = new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(UUID.randomUUID(), UUID.randomUUID()),
                "pdapi_clear",
                List.of(new SimpleGrantedAuthority(ApiKeyAuthenticationService.API_KEY_AUTHORITY)));
        when(apiKeyAuthenticationService.authenticate("pdapi_clear")).thenReturn(authentication);
        final ApiKeyAuthFilter filter = new ApiKeyAuthFilter(apiKeyAuthenticationService);
        final MockHttpServletRequest request = request("/api/v1/products/abc");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer pdapi_clear");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicBoolean continued = new AtomicBoolean(false);

        filter.doFilterInternal(request, response, markContinued(continued));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(continued).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        verify(apiKeyAuthenticationService).authenticate("pdapi_clear");
    }

    private MockHttpServletRequest request(final String path) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }

    private FilterChain markContinued(final AtomicBoolean continued) {
        return (request, response) -> continued.set(true);
    }
}
