package org.open4goods.b2bapi.config;

import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public ProductRepository productRepository() {
        return new ProductRepository();
    }

}
