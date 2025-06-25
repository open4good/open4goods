package org.open4goods.nudgerfrontapi.config;

import org.open4goods.nudgerfrontapi.interceptor.XLocaleHeaderInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for locale resolution and interceptor registration.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    private final XLocaleHeaderInterceptor xLocaleHeaderInterceptor;

    public LocaleConfig(XLocaleHeaderInterceptor xLocaleHeaderInterceptor) {
        this.xLocaleHeaderInterceptor = xLocaleHeaderInterceptor;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new UserPreferenceLocaleResolver();
    }

    @Bean
    public XLocaleHeaderInterceptor xLocaleHeaderInterceptor(LocaleResolver localeResolver) {
        return new XLocaleHeaderInterceptor(localeResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(xLocaleHeaderInterceptor);
    }
}
