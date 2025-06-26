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


    @Bean
    ProductRepository productRepository() {
        return new ProductRepository();
    }

}
