package org.open4goods.b2b.config;

import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * Beans providing core infrastructure services for the frontend API.
 */
public class AppConfig {



    @Bean
    ProductRepository productRepository() {
        return new ProductRepository();
    }

}
