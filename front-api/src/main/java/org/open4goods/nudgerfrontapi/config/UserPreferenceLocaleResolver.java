package org.open4goods.nudgerfrontapi.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the locale based on a user preference stored in a cookie or JWT
 * claim, falling back to the request {@code Accept-Language} header.
 */
public class UserPreferenceLocaleResolver extends AcceptHeaderLocaleResolver {

    private static final String COOKIE_NAME = "locale";
    private static final String CLAIM_NAME = "locale";

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        if (request == null) {
            return Locale.getDefault();
        }

        // Check cookie preference
        if (request.getCookies() != null) {
            Optional<String> cookie = Arrays.stream(request.getCookies())
                    .filter(c -> COOKIE_NAME.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
            if (cookie.isPresent()) {
                return Locale.forLanguageTag(cookie.get());
            }
        }

        // Check JWT claim
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            Object claim = token.getToken().getClaim(CLAIM_NAME);
            if (claim != null) {
                return Locale.forLanguageTag(claim.toString());
            }
        }

        // Fallback to Accept-Language
        return request.getLocale();
    }
}
