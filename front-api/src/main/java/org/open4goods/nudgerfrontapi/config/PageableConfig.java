package org.open4goods.nudgerfrontapi.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

/**
 * Web MVC configuration for pageable parameters.
 */

@Configuration
public class PageableConfig implements WebMvcConfigurer {

    private final PaginationProperties paginationProperties;

    public PageableConfig(PaginationProperties paginationProperties) {
        this.paginationProperties = paginationProperties;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {

        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
        pageableResolver.setPageParameterName("page[number]");
        pageableResolver.setSizeParameterName("page[size]");

        pageableResolver.setMaxPageSize(paginationProperties.getMaxPageSize());

        resolvers.add(pageableResolver);

    }
}
