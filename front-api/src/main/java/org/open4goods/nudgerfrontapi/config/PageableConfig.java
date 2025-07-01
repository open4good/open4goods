package org.open4goods.nudgerfrontapi.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

@Configuration
/**
 * Customises Spring MVC pageable parameter names for the API.
 */
public class PageableConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {

        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
        pageableResolver.setPageParameterName("pageNumber");
        pageableResolver.setSizeParameterName("pageSize");

        // TODO : Document, from conf
        pageableResolver.setMaxPageSize(50);

        resolvers.add(pageableResolver);

    }
}
