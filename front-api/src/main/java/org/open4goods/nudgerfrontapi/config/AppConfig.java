package org.open4goods.nudgerfrontapi.config;

import org.open4goods.commons.services.GoogleTaxonomyService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.services.blog.config.BlogConfiguration;
import org.open4goods.services.blog.service.BlogService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.xwiki.services.XwikiFacadeService;

import org.open4goods.nudgerfrontapi.config.CacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.support.ResourcePatternResolver;

import org.open4goods.model.vertical.VerticalConfig;

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
                                                  GoogleTaxonomyService googleTaxonomyService,
                                                  ProductRepository productRepository) {
        return new VerticalsConfigService(serialisationService, googleTaxonomyService, productRepository, resourceResolver);
    }

}
