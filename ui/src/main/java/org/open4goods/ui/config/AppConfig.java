
package org.open4goods.ui.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.interceptors.ImageResizeInterceptor;
import org.open4goods.ui.services.SitemapGenerationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import io.micrometer.core.instrument.MeterRegistry;



@Configuration
public class AppConfig {



	// TODO : Cache period pageNumber conf
	public static final int CACHE_PERIOD_SECONDS = 3600*24*7;
	private final UiConfig config;



	public AppConfig(UiConfig config) {
		this.config = config;
	}

	@Bean
	SitemapGenerationService sitemapGenerationService(ProductRepository repository,
			VerticalsConfigService verticalConfigService, MeterRegistry meterRegistry) {
		return new SitemapGenerationService(repository, config, verticalConfigService, meterRegistry);
	}

	@Bean
        BrandService brandService(@Autowired RemoteFileCachingService rfc, SerialisationService serialisationService) throws Exception {
                return new BrandService(rfc, serialisationService);
        }




	/** The bean providing datasource configurations **/
	@Bean DataSourceConfigService datasourceConfigService(@Autowired final UiConfig config) {
		return new DataSourceConfigService(config.getDatasourcesfolder());
	}

	@Bean
	ResourceService resourceService() {
		return new ResourceService(config.getRemoteCachingFolder());
	}




	/**
	 * Product repository backed by the Spring Data Elasticsearch operations.
	 * The no-arg constructor leaves the operations {@code null}, which broke
	 * every sitemap generation from 2026-06 (GOU-1365).
	 */
	@Bean
	ProductRepository aggregatedDataRepo(ElasticsearchOperations elasticsearchOperations) {
		return new ProductRepository(elasticsearchOperations);
	}

	// TODO : should not be required at ui side
        @Bean RemoteFileCachingService remoteFileCachingService(RemoteFileCachingProperties remoteFileCachingProperties) {
                return new RemoteFileCachingService(config.getRemoteCachingFolder(), remoteFileCachingProperties);
        }

    // TODO : should not be required at ui side
        @Bean
    GoogleTaxonomyService googleTaxonomyService(@Autowired RemoteFileCachingService remoteFileCachingService) {
                GoogleTaxonomyService gts = new GoogleTaxonomyService(remoteFileCachingService);

        try {
                        for (var entry : config.getGoogleTaxonomy().entrySet()) {
                                gts.loadGoogleTaxonUrl(entry.getValue(), entry.getKey());
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }

        return gts;
        }


	@Bean
	VerticalsConfigService verticalConfigsService(ResourcePatternResolver resourceResolver, SerialisationService serialisationService,  GoogleTaxonomyService googleTaxonomyService,
			@Value("${" + VerticalsConfigService.METHODOLOGY_VERSION_OVERRIDES_PROPERTY + ":}") String methodologyVersionOverrides) throws IOException {
		return new VerticalsConfigService( serialisationService, googleTaxonomyService, resourceResolver, methodologyVersionOverrides);
	}


	@Bean
	HttpFirewall allowUrlEncodedSlashHttpFirewall() {
		StrictHttpFirewall firewall = new StrictHttpFirewall();
		firewall.setAllowUrlEncodedSlash(true);
		firewall.setAllowBackSlash(true);
		firewall.setAllowNull(true);
		firewall.setAllowSemicolon(true);
		firewall.setUnsafeAllowAnyHttpMethod(true);
		firewall.setUnsafeAllowAnyHttpMethod(true);
		firewall.setAllowUrlEncodedPercent(true);



		return firewall;
	}




	//////////////////////////////////////////////
	// The cache managers
	//////////////////////////////////////////////

	@Bean
	CacheManager cacheManager(@Autowired final Ticker ticker) {
		final CaffeineCache fCache = buildCache(CacheConstants.FOREVER_LOCAL_CACHE_NAME, ticker, 30000000);
		final CaffeineCache hCache = buildCache(CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, ticker, 60);
		final CaffeineCache mCache = buildCache(CacheConstants.ONE_MINUTE_LOCAL_CACHE_NAME, ticker, 1);
		final CaffeineCache dCache = buildCache(CacheConstants.ONE_DAY_LOCAL_CACHE_NAME, ticker, 60 * 24);
		final SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(fCache, dCache, hCache,mCache));
		return manager;
	}

	private CaffeineCache buildCache(final String name, final Ticker ticker, final int minutesToExpire) {
		return new CaffeineCache(name,
				Caffeine.newBuilder()
				.recordStats()
				.expireAfterWrite(minutesToExpire, TimeUnit.MINUTES)
				.ticker(ticker).build());
	}

	@Bean
	Ticker ticker() {
		return Ticker.systemTicker();
	}


	///////////////////////////////////
	// Web MVC Config
	///////////////////////////////////


	@Bean
	WebMvcConfigurer configurer() {

		return new WebMvcConfigurer() {

			@Override
			public void addInterceptors(final InterceptorRegistry registry) {
                registry.addInterceptor(new ImageResizeInterceptor(resourceService(), config.getAllowedImagesSizeSuffixes(), config.getImageBaseUrl()));

			}

			@Override
			public void configurePathMatch(PathMatchConfigurer configurer) {
				UrlPathHelper urlPathHelper = new UrlPathHelper();
				urlPathHelper.setUrlDecode(false);
				configurer.setUrlPathHelper(urlPathHelper);
			}
		};
	}






}
