package org.open4goods.b2bapi.service;

import jakarta.servlet.http.HttpServletResponse;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Writes and clears HttpOnly dashboard session cookies.
 */
@Service
public class JwtCookieService {

    public static final String ACCESS_COOKIE = "pdapi_access";
    public static final String REFRESH_COOKIE = "pdapi_refresh";

    private final B2bApiProperties properties;

    public JwtCookieService(final B2bApiProperties properties) {
        this.properties = properties;
    }

    /**
     * Adds access and refresh JWT cookies to the response.
     *
     * @param response servlet response
     * @param tokens session token pair
     */
    public void writeSessionCookies(final HttpServletResponse response, final JwtTokenPair tokens) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, tokens.accessToken())
                .maxAge(properties.getSecurity().getAccessTokenTtl())
                .build()
                .toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, tokens.refreshToken())
                .maxAge(properties.getSecurity().getRefreshTokenTtl())
                .build()
                .toString());
    }

    /**
     * Adds expired access and refresh cookies to the response.
     *
     * @param response servlet response
     */
    public void clearSessionCookies(final HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "")
                .maxAge(0)
                .build()
                .toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "")
                .maxAge(0)
                .build()
                .toString());
    }

    private ResponseCookie.ResponseCookieBuilder cookie(final String name, final String value) {
        return ResponseCookie.from(name, value)
                .domain(properties.getSecurity().getCookieDomain())
                .path("/")
                .httpOnly(true)
                .secure(properties.getSecurity().isCookieSecure())
                .sameSite(properties.getSecurity().getCookieSameSite());
    }
}
