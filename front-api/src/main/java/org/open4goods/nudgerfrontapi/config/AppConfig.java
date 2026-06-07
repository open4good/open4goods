package org.open4goods.nudgerfrontapi.config;

import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.services.IcecatFileDownloadService;
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
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import tools.jackson.dataformat.xml.XmlMapper;

@Configuration
/**
 * Beans providing core infrastructure services for the frontend API.
 */
public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    @Bean
    @org.springframework.context.annotation.Primary
    RemoteFileCachingService remoteFileCachingService(CacheProperties cacheProperties,
            RemoteFileCachingProperties props) {
        return new RemoteFileCachingService(cacheProperties.getPath(), props);
    }

    @Bean
    @org.springframework.context.annotation.Profile("!local")
    ProductRepository productRepository(ElasticsearchOperations elasticsearchOperations) {
        return new ProductRepository(elasticsearchOperations);
    }

    @Bean
    BrandService brandService(RemoteFileCachingService remoteFileCachingService,
            SerialisationService serialisationService) throws Exception {
        return new BrandService(remoteFileCachingService, serialisationService);
    }

    @Bean
    IcecatFileDownloadService icecatFileDownloadService(RemoteFileCachingService fileCachingService,
            @Autowired IcecatConfiguration icecatFeatureConfig, @Autowired CacheProperties cacheProperties) {
        return new IcecatFileDownloadService(fileCachingService, cacheProperties.getPath(), icecatFeatureConfig);
    }

    @Bean
    FeatureLoader featureLoader(IcecatFileDownloadService icecatFileDownloadService, BrandService brandService,
            @Autowired IcecatConfiguration icecatFeatureConfig) {
        return new FeatureLoader(new XmlMapper(), icecatFeatureConfig, icecatFileDownloadService, brandService);
    }

    @Bean
    CategoryLoader categoryLoader(IcecatFileDownloadService icecatFileDownloadService,
            VerticalsConfigService verticalConfigService, FeatureLoader featureLoader,
            @Autowired IcecatConfiguration icecatFeatureConfig) {
        return new CategoryLoader(new XmlMapper(), icecatFeatureConfig, icecatFileDownloadService,
                verticalConfigService, featureLoader);
    }

    @Bean
    IcecatService icecatFeatureService(IcecatFileDownloadService icecatFileDownloadService,
            @Autowired IcecatConfiguration icecatFeatureConfig, @Autowired FeatureLoader featureLoader,
            @Autowired CategoryLoader categoryLoader) {
        // NOTE: xmlMapper not injected because sharing the Spring-managed one corrupts springdoc.
        return new IcecatService(new XmlMapper(), icecatFeatureConfig, icecatFileDownloadService,
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
