package org.open4goods.nudgerfrontapi.config;

import org.open4goods.nudgerfrontapi.interceptor.XLocaleHeaderInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

import org.open4goods.nudgerfrontapi.config.UserPreferenceLocaleResolver;

/**
 * Beans related to locale resolution.
 */
@Configuration
public class LocaleBeansConfig {

    @Bean
    public LocaleResolver localeResolver() {
        return new UserPreferenceLocaleResolver();
    }

    @Bean
    public XLocaleHeaderInterceptor xLocaleHeaderInterceptor(LocaleResolver localeResolver) {
        return new XLocaleHeaderInterceptor(localeResolver);
    }
}
