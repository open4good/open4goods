package org.open4goods.b2bapi.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.open4goods.b2bapi.exception.InvalidCredentialsException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Extracts dashboard session JWTs from bearer headers or HttpOnly cookies.
 */
@Service
public class AuthTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Resolves an access token for authenticated dashboard requests.
     *
     * @param request servlet request
     * @return signed access JWT
     */
    public String resolveAccessToken(final HttpServletRequest request) {
        return resolveBearer(request).or(() -> resolveCookie(request, JwtCookieService.ACCESS_COOKIE))
                .orElseThrow(() -> new InvalidCredentialsException("Missing access token."));
    }

    /**
     * Resolves a refresh token for refresh requests.
     *
     * @param request servlet request
     * @return signed refresh JWT
     */
    public String resolveRefreshToken(final HttpServletRequest request) {
        return resolveBearer(request).or(() -> resolveCookie(request, JwtCookieService.REFRESH_COOKIE))
                .orElseThrow(() -> new InvalidCredentialsException("Missing refresh token."));
    }

    private Optional<String> resolveBearer(final HttpServletRequest request) {
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        final String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
    }

    private Optional<String> resolveCookie(final HttpServletRequest request, final String cookieName) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }
}
