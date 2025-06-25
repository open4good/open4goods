package org.open4goods.nudgerfrontapi.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration registering custom argument resolvers.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final IncludeArgumentResolver includeArgumentResolver;

    public WebConfig(IncludeArgumentResolver includeArgumentResolver) {
        this.includeArgumentResolver = includeArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(includeArgumentResolver);
    }
}
