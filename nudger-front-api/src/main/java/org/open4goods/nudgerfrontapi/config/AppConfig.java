package org.open4goods.nudgerfrontapi.config;

import java.util.Collections;
import java.util.Set;

import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;
import org.open4goods.nudgerfrontapi.config.IncludeArgumentResolver;

import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class AppConfig {

    @Bean
    RemoteFileCachingService remoteFileCachingService(RemoteFileCachingProperties props) {
        return new RemoteFileCachingService("./cache", props);
    }
//
//    @Bean
//    GoogleTaxonomyService googleTaxonomyService(RemoteFileCachingService cachingService) {
//        return new GoogleTaxonomyService(cachingService);
//    }

    @Bean
    ProductRepository productRepository() {
        return new ProductRepository();
    }

    @Bean
    @RequestScope
    FilterProvider filterProvider(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Set<String> includes = (Set<String>) request.getAttribute(IncludeArgumentResolver.ATTRIBUTE_NAME);
        if (includes == null) {
            includes = Collections.emptySet();
        }
        SimpleBeanPropertyFilter filter = includes.isEmpty()
                ? SimpleBeanPropertyFilter.serializeAll()
                : SimpleBeanPropertyFilter.filterOutAllExcept(includes);
        return new SimpleFilterProvider()
                .setFailOnUnknownId(false)
                .addFilter("inc", filter);
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer(FilterProvider filterProvider) {
        return builder -> builder.filters(filterProvider);
    }
//
//    @Bean
//    SearchService searchService(ProductRepository repository) {
//        return new SearchService(repository, "./logs");
//    }
}
