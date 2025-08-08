package org.open4goods.b2bapi.config;

import org.open4goods.b2bapi.interceptor.ApiCallCountingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiCallCountingInterceptor countingInterceptor;

    public WebConfig(ApiCallCountingInterceptor countingInterceptor) {
        this.countingInterceptor = countingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(countingInterceptor)
                .addPathPatterns("/api/**");
    }
}
