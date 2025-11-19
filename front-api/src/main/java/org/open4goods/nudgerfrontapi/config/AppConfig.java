package org.open4goods.nudgerfrontapi.config;

import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.nudgerfrontapi.config.properties.BlogProperties;
import org.open4goods.nudgerfrontapi.config.properties.CacheProperties;
import org.open4goods.nudgerfrontapi.config.properties.GoogleTaxonomyProperties;
import org.open4goods.services.blog.config.BlogConfiguration;
import org.open4goods.services.blog.service.BlogService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@Configuration
/**
 * Beans providing core infrastructure services for the frontend API.
 */
public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

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
    BrandService brandService(RemoteFileCachingService remoteFileCachingService,
            SerialisationService serialisationService) throws Exception {
        return new BrandService(remoteFileCachingService, serialisationService);
    }

    @Bean
    FeatureLoader featureLoader(RemoteFileCachingService fileCachingService, BrandService brandService,
            @Autowired IcecatConfiguration icecatFeatureConfig, @Autowired CacheProperties cacheProperties) {
        return new FeatureLoader(new XmlMapper(), icecatFeatureConfig, fileCachingService, cacheProperties.getPath(),
                brandService);
    }

    @Bean
    CategoryLoader categoryLoader(RemoteFileCachingService fileCachingService,
            VerticalsConfigService verticalConfigService, FeatureLoader featureLoader,
            @Autowired IcecatConfiguration icecatFeatureConfig, @Autowired CacheProperties cacheProperties) {
        return new CategoryLoader(new XmlMapper(), icecatFeatureConfig, fileCachingService, cacheProperties.getPath(),
                verticalConfigService, featureLoader);
    }

    @Bean
    IcecatService icecatFeatureService(@Autowired RemoteFileCachingService fileCachingService,
            @Autowired IcecatConfiguration icecatFeatureConfig, @Autowired FeatureLoader featureLoader,
            @Autowired CategoryLoader categoryLoader, @Autowired CacheProperties cacheProperties) {
        // NOTE : xmlMapper not injected because corruct the springdoc used one. Could
        // use a @Primary derivation
        return new IcecatService(new XmlMapper(), icecatFeatureConfig, fileCachingService, cacheProperties.getPath(),
                featureLoader, categoryLoader);
    }

    @Bean
    BlogService blogService(@Autowired XwikiFacadeService xwikiFacadeService, @Autowired BlogConfiguration blogConfig,
            @Autowired BlogProperties blogProperties) {
        return new BlogService(xwikiFacadeService, blogConfig, blogProperties.getBaseUrls());
    }

    @Bean
    GoogleTaxonomyService googleTaxonomyService(@Autowired RemoteFileCachingService remoteFileCachingService,
            @Autowired GoogleTaxonomyProperties taxonomyProperties) {
        GoogleTaxonomyService gts = new GoogleTaxonomyService(remoteFileCachingService);

        try {
            gts.loadGoogleTaxonUrl(taxonomyProperties.getFrenchTaxonomyUrl(), "fr");
            gts.loadGoogleTaxonUrl(taxonomyProperties.getEnglishTaxonomyUrl(), "en");

            // Load additional taxonomy URLs if configured
            taxonomyProperties.getTaxonomyUrls().forEach((lang, url) -> {
                try {
                    gts.loadGoogleTaxonUrl(url, lang);
                } catch (Exception ex) {
                    logger.error("Failed to load Google taxonomy for language '{}' from URL: {}", lang, url, ex);
                }
            });

        } catch (Exception e) {
            logger.error("Failed to load Google taxonomy data", e);
        }

        return gts;
    }

    @Bean
    VerticalsConfigService verticalsConfigService(ResourcePatternResolver resourceResolver,
            SerialisationService serialisationService,
            GoogleTaxonomyService googleTaxonomyService) {
        return new VerticalsConfigService(serialisationService, googleTaxonomyService, resourceResolver);
    }

}
