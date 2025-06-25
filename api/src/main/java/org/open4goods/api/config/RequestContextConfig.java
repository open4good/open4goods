package org.open4goods.api.config;

import org.open4goods.commons.interceptors.RequestContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers web interceptors for the API module.
 */
@Configuration
public class RequestContextConfig implements WebMvcConfigurer {

    private final RequestContextInterceptor requestContextInterceptor;

    public RequestContextConfig(RequestContextInterceptor requestContextInterceptor) {
        this.requestContextInterceptor = requestContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestContextInterceptor);
    }
}
