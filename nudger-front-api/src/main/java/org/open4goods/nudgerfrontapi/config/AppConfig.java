package org.open4goods.nudgerfrontapi.config;

import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
//
//    @Bean
//    SearchService searchService(ProductRepository repository) {
//        return new SearchService(repository, "./logs");
//    }
}
