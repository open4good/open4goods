package org.open4goods.b2b.config;

import org.open4goods.b2b.interceptor.LoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers application-wide HTTP logging interceptor.
 */
@Configuration
public class LoggingConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    public LoggingConfig(LoggingInterceptor loggingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor);
    }
}
