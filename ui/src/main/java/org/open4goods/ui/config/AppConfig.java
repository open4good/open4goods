
package org.open4goods.ui.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.services.blog.service.BlogService;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.feedservice.service.AbstractFeedService;
import org.open4goods.services.feedservice.service.AwinFeedService;
import org.open4goods.services.feedservice.service.EffiliationFeedService;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.imageprocessing.service.ImageMagickService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.interceptors.ImageResizeInterceptor;
import org.open4goods.ui.services.SitemapGenerationService;
import org.open4goods.ui.services.todo.TodoService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import io.micrometer.core.aop.TimedAspect;
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
    public AwinFeedService awinFeedService(
                                           RemoteFileCachingService remoteFileCachingService,
                                           DataSourceConfigService dataSourceConfigService,
                                           SerialisationService serialisationService,
                                           UiConfig uiConfig

    		) {
        // Retrieve Awin-specific feed configuration from the fetcher properties
        FeedConfiguration awinConfig = uiConfig.getFeedConfigs().get("awin");
        return new AwinFeedService(awinConfig, remoteFileCachingService, dataSourceConfigService, serialisationService, uiConfig.getAffiliationConfig().getAwinAdvertiserId(), uiConfig.getAffiliationConfig().getAwinAccessToken());
    }

    @Bean
    public EffiliationFeedService effiliationFeedService(
                                                         RemoteFileCachingService remoteFileCachingService,
                                                         DataSourceConfigService dataSourceConfigService,
                                                         SerialisationService serialisationService,
                                                         UiConfig uiConfig) {
        // Retrieve Effiliation-specific feed configuration from the fetcher properties
        FeedConfiguration effiliationConfig = uiConfig.getFeedConfigs().get("effiliation");
        return new EffiliationFeedService(effiliationConfig, remoteFileCachingService, dataSourceConfigService, serialisationService, uiConfig.getAffiliationConfig().getEffiliationApiKey());
    }

    @Bean
    public FeedService feedService(List<AbstractFeedService> feedServices,
                                   DataSourceConfigService dataSourceConfigService) {
        // The FeedService aggregates all concrete feed implementations.
        return new FeedService(feedServices, dataSourceConfigService);
    }



	@Bean
	TodoService todoService() {
		return new TodoService(config.getTagListUrl());
	}

	@Bean
	SitemapGenerationService sitemapGenerationService(ProductRepository repository, VerticalsConfigService verticalConfigService, BlogService blogService, XwikiFacadeService wikiService) {
		return new SitemapGenerationService(repository, config, verticalConfigService, blogService, wikiService);
	}

	@Bean
	BarcodeValidationService barcodeValidationService () {
		return new BarcodeValidationService();
	}




//    @Bean
//    RedisTemplate<String, Product> redisTemplate(RedisConnectionFactory connectionFactory) {
//		  RedisTemplate<String, Product> template = new RedisTemplate<>();
//		    template.setConnectionFactory(connectionFactory);
//
//		    // Configure serialization
//		    template.setKeySerializer(new StringRedisSerializer());
//		    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//
//
//		    // Add some specific configuration here. Key serializers, etc.
//		    return template;
//	  }




//    @Bean
//    public ResourceBundle messageSource() {
//        ResourceBundle messageSource = new ResourceBundle();
//
//        // Set multiple base names for properties files
//        messageSource.setBasenames(
//            "classpath:i18n/messages",  // default.properties
//            "classpath:i18n/metas",      // metas.properties
//            "classpath:i18n/product"      // metas.properties
//        );
//
//        messageSource.setDefaultEncoding("UTF-8");
//        messageSource.setCacheSeconds(3600);  // Refresh every hour
//        return messageSource;
//    }



//    /** Override the default RestTemplate with a custom one that has a longer timeout (For ImageGenerationService) **/
//    @Bean
//    RestClientCustomizer restClientCustomizer() {
//		return restClientBuilder -> restClientBuilder
//				.requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
//						.withConnectTimeout(Duration.ofSeconds(60))
//						.withReadTimeout(Duration.ofSeconds(60))));
//	}

	@Bean
        BrandService brandService(@Autowired RemoteFileCachingService rfc, SerialisationService serialisationService) throws Exception {
                return new BrandService(rfc, serialisationService);
        }




	/** The bean providing datasource configurations **/
	@Bean DataSourceConfigService datasourceConfigService(@Autowired final UiConfig config) {
		return new DataSourceConfigService(config.getDatasourcesfolder());
	}



	@Bean
	ImageMagickService imageMagickService() {
		return new ImageMagickService();
	}

	@Bean
	ResourceService resourceService() {
		return new ResourceService(config.getRemoteCachingFolder());
	}




	@Bean
	ProductRepository aggregatedDataRepo() {
		return new ProductRepository();
	}


//	@Bean
//	StandardiserService standardiserService() {
//		return new StandardiserService() {
//			@Override
//			public void standarise(final Price price, final Currency currency) {
//			}
//		};
//	}

	/**
	 * The service that hot evaluates thymeleaf / spel expressions
	 *
	 * @return
	 */
//	@Bean
//	EvaluationService evaluationService(@Autowired EvaluationConfig evalConfig) {
//		return new EvaluationService(evalConfig);
//	}



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
	VerticalsConfigService verticalConfigsService(ResourcePatternResolver resourceResolver, SerialisationService serialisationService,  GoogleTaxonomyService googleTaxonomyService) throws IOException {
		return new VerticalsConfigService( serialisationService, googleTaxonomyService, resourceResolver);
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
//				registry.addInterceptor(new BanCheckerInterceptor(config.getBancheckerConfig()));
//				registry.addInterceptor(AppConfig.localeChangeInterceptor());
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



	// TODO : Should be removed


	@Bean
	@Qualifier("openAiCustomApi")
	OpenAiApi openAiCustomApi(@Autowired PromptServiceConfig genAiConfig) {
		return new OpenAiApi(genAiConfig.getOpenaiApiKey());
	}


}
