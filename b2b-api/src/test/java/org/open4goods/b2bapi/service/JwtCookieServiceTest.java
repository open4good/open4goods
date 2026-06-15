package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for dashboard JWT cookie writing.
 */
class JwtCookieServiceTest {

    @Test
    void writesHttpOnlySecureSameSiteCookiesForConfiguredDomain() {
        final B2bApiProperties properties = new B2bApiProperties();
        final JwtCookieService service = new JwtCookieService(properties);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JwtTokenPair tokens = new JwtTokenPair(
                "access.jwt",
                Instant.parse("2026-06-15T12:15:00Z"),
                "refresh.jwt",
                Instant.parse("2026-07-15T12:00:00Z"));

        service.writeSessionCookies(response, tokens);

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .hasSize(2)
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("pdapi_access=access.jwt")
                        .contains("Domain=.product-data-api.com")
                        .contains("Path=/")
                        .contains("Max-Age=900")
                        .contains("Secure")
                        .contains("HttpOnly")
                        .contains("SameSite=Lax"))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("pdapi_refresh=refresh.jwt")
                        .contains("Max-Age=2592000"));
    }

    @Test
    void clearsSessionCookies() {
        final JwtCookieService service = new JwtCookieService(new B2bApiProperties());
        final MockHttpServletResponse response = new MockHttpServletResponse();

        service.clearSessionCookies(response);

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .hasSize(2)
                .allSatisfy(cookie -> assertThat(cookie).contains("Max-Age=0"));
    }
}
