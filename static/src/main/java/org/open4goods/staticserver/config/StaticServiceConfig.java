package org.open4goods.staticserver.config;

import java.util.Collections;
import java.util.List;

import org.open4goods.services.feedservice.service.AbstractFeedService;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.GoogleTaxonomyService;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class StaticServiceConfig {

    @Bean
    RemoteFileCachingService remoteFileCachingService(StaticUiConfig config,
                                                      RemoteFileCachingProperties properties) {
        return new RemoteFileCachingService(config.getRemoteCachingFolder(), properties);
    }

    @Bean
    DataSourceConfigService dataSourceConfigService(StaticUiConfig config) {
        return new DataSourceConfigService(config.getDatasourcesfolder());
    }

    @Bean
    ResourceService resourceService(StaticUiConfig config) {
        return new ResourceService(config.getRemoteCachingFolder());
    }

    @Bean
    BrandService brandService(RemoteFileCachingService rfc,
                              StaticUiConfig config,
                              SerialisationService serialisationService) throws Exception {
        return new BrandService(rfc, config.logsFolder(), serialisationService);
    }

    @Bean
    ProductRepository productRepository() {
        return new ProductRepository();
    }

    @Bean
    GoogleTaxonomyService googleTaxonomyService(RemoteFileCachingService rfc) {
        return new GoogleTaxonomyService(rfc);
    }

    @Bean
    VerticalsConfigService verticalsConfigService(SerialisationService serialisationService,
                                                  GoogleTaxonomyService googleTaxonomyService,
                                                  ProductRepository productRepository) {
        return new VerticalsConfigService(serialisationService, googleTaxonomyService,
                productRepository, new PathMatchingResourcePatternResolver());
    }

    @Bean
    FeedService feedService(DataSourceConfigService dataSourceConfigService) {
        List<AbstractFeedService> services = Collections.emptyList();
        return new FeedService(services, dataSourceConfigService);
    }
}
