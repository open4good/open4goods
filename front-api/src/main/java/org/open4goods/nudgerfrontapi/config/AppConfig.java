package org.open4goods.nudgerfrontapi.config;

import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.CacheProperties;
import org.open4goods.services.blog.config.BlogConfiguration;
import org.open4goods.services.blog.service.BlogService;
import org.open4goods.services.contribution.service.ContributionService;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.service.OpenDataService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
/**
 * Beans providing core infrastructure services for the frontend API.
 */
public class AppConfig {

    @Bean
    RemoteFileCachingService remoteFileCachingService(CacheProperties cacheProperties,
                                                     RemoteFileCachingProperties props) {
        return new RemoteFileCachingService(cacheProperties.getPath(), props);
    }


    @Bean
    ProductRepository productRepository() {
        return new ProductRepository();
    }

    @Bean
    BlogService blogService(@Autowired XwikiFacadeService xwikiFacadeService, @Autowired BlogConfiguration blogConfig) {
        // TODO : the null maps the I18n localisable, used for RSS articles gen
        return new BlogService(xwikiFacadeService, blogConfig, null);
    }

    @Bean
    GoogleTaxonomyService googleTaxonomyService(RemoteFileCachingService remoteFileCachingService) {
        return new GoogleTaxonomyService(remoteFileCachingService) {
            @Override
            public void updateCategoryWithVertical(VerticalConfig verticalConfig) {
                if (verticalConfig == null || verticalConfig.getGoogleTaxonomyId() == null) {
                    return;
                }
                if (byId(verticalConfig.getGoogleTaxonomyId()) != null) {
                    super.updateCategoryWithVertical(verticalConfig);
                }
            }
        };
    }

    @Bean
    VerticalsConfigService verticalsConfigService(ResourcePatternResolver resourceResolver,
                                                  SerialisationService serialisationService,
                                                  GoogleTaxonomyService googleTaxonomyService
                                                  ) {
        return new VerticalsConfigService(serialisationService, googleTaxonomyService, resourceResolver);
    }

}
