package org.open4goods.nudgerfrontapi.config;

import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

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
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    SimpleFilterProvider simpleFilterProvider() {
        return new SimpleFilterProvider()
                .setDefaultFilter(SimpleBeanPropertyFilter.serializeAll())
                .setFailOnUnknownId(false);
    }

    @Bean
    BeanPostProcessor filterProviderCustomizer(SimpleFilterProvider filters) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String name) {
                if (bean instanceof Jackson2ObjectMapperBuilder builder) {
                    builder.filters(filters);
                }
                return bean;
            }
        };
    }
//
//    @Bean
//    SearchService searchService(ProductRepository repository) {
//        return new SearchService(repository, "./logs");
//    }
}
