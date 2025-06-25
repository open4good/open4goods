package org.open4goods.nudgerfrontapi.interceptor;

import java.util.Locale;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor adding the resolved locale to the {@code X-Locale} response
 * header so the Nuxt frontend can pick it up.
 */
public class XLocaleHeaderInterceptor implements HandlerInterceptor {

    private final LocaleResolver localeResolver;

    public XLocaleHeaderInterceptor(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Locale locale = localeResolver.resolveLocale(request);
        response.setHeader("X-Locale", locale.toLanguageTag());
        return true;
    }
}
