package org.open4goods.nudgerfrontapi.config;

import java.io.IOException;

import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.commons.services.GoogleTaxonomyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

@Configuration
public class AppConfig {

    @Bean
    RemoteFileCachingService remoteFileCachingService(RemoteFileCachingProperties props) {
        return new RemoteFileCachingService("./cache", props);
    }

    @Bean
    GoogleTaxonomyService googleTaxonomyService(RemoteFileCachingService cachingService) {
        return new GoogleTaxonomyService(cachingService);
    }

    @Bean
    ProductRepository productRepository() {
        return new ProductRepository();
    }

    @Bean
    VerticalsConfigService verticalsConfigService(ResourcePatternResolver resolver,
                                                  SerialisationService serialisationService,
                                                  GoogleTaxonomyService gts,
                                                  ProductRepository productRepository) throws IOException {
        return new VerticalsConfigService(serialisationService, gts, productRepository, resolver);
    }

    @Bean
    SearchService searchService(ProductRepository repository) {
        return new SearchService(repository, "./logs");
    }
}
