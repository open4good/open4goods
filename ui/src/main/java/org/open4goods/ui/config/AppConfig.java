
package org.open4goods.ui.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.api.services.feed.AbstractFeedService;
import org.open4goods.api.services.feed.AwinFeedService;
import org.open4goods.api.services.feed.EffiliationFeedService;
import org.open4goods.api.services.feed.FeedConfiguration;
import org.open4goods.api.services.feed.FeedService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.GoogleTaxonomyService;
import org.open4goods.commons.services.IcecatService;
import org.open4goods.commons.services.MailService;
import org.open4goods.commons.services.ResourceBundle;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.commons.store.repository.elastic.BrandScoresRepository;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.price.Currency;
import org.open4goods.model.price.Price;
import org.open4goods.services.evaluation.config.EvaluationConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.imageprocessing.service.ImageMagickService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.interceptors.GenericTemplateInterceptor;
import org.open4goods.ui.interceptors.ImageResizeInterceptor;
import org.open4goods.ui.repository.ContributionVoteRepository;
import org.open4goods.ui.services.BlogService;
import org.open4goods.ui.services.ContributionService;
import org.open4goods.ui.services.GoogleIndexationService;
import org.open4goods.ui.services.GtinService;
import org.open4goods.ui.services.OpenDataService;
import org.open4goods.ui.services.SitemapGenerationService;
import org.open4goods.ui.services.todo.TodoService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.util.UrlPathHelper;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
	@Qualifier("perplexityChatModel")
	OpenAiApi perplexityApi(@Autowired PromptServiceConfig genAiConfig) {
		return new OpenAiApi(genAiConfig.getPerplexityBaseUrl(),
							 genAiConfig.getPerplexityApiKey(),
							 genAiConfig.getPerplexityCompletionsPath(),
							 "/v1/embeddings", RestClient.builder(), WebClient.builder(),
							 RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	@Bean
	@Qualifier("openAiCustomApi")
	OpenAiApi openAiCustomApi(@Autowired PromptServiceConfig genAiConfig) {
		return new OpenAiApi(genAiConfig.getOpenaiApiKey());
	}


	@Bean
	PromptService genAiService (@Autowired @Qualifier("perplexityChatModel") OpenAiApi perplexityApi,
								@Autowired @Qualifier("openAiCustomApi") OpenAiApi openAiCustomApi,
								@Autowired  EvaluationService spelEvaluationService,
								@Autowired  SerialisationService serialisationService,
								@Autowired PromptServiceConfig genAiConfig,
								@Autowired MeterRegistry meterRegistry) {
		return new PromptService(genAiConfig, perplexityApi, openAiCustomApi, serialisationService, spelEvaluationService, meterRegistry);
	}



	@Bean
	TodoService todoService() {
		return new TodoService(config.getTagListUrl());
	}

	@Bean
	SitemapGenerationService sitemapGenerationService(ProductRepository repository, VerticalsConfigService verticalConfigService, BlogService blogService, XwikiFacadeService wikiService, ApplicationContext context ) {
		return new SitemapGenerationService(repository, config, verticalConfigService, blogService, wikiService, context);
	}

	@Bean
	GoogleIndexationService googleIndexationService(ProductRepository repository, VerticalsConfigService verticalConfigService) {
		return new GoogleIndexationService(config.getGoogleApiJson(), config.getGoogleIndexationMarkerFile(), repository, verticalConfigService);
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
    public ResourceBundle messageSource() {
        ResourceBundle messageSource = new ResourceBundle();

        // Set multiple base names for properties files
        messageSource.setBasenames(
            "classpath:i18n/messages",  // default.properties
            "classpath:i18n/metas",      // metas.properties
            "classpath:i18n/product"      // metas.properties
        );

        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600);  // Refresh every hour
        return messageSource;
    }

    @Bean
    ContributionService contributionService (CacheManager cacheManager, SerialisationService serialisationService, ContributionVoteRepository repository, UiConfig uiConfig, ElasticsearchOperations esOps) {
    	return new ContributionService(cacheManager, serialisationService, repository, uiConfig.getReversementConfig(), esOps);
    }



    /** Override the default RestTemplate with a custom one that has a longer timeout (For ImageGenerationService) **/
    @Bean
    RestClientCustomizer restClientCustomizer() {
		return restClientBuilder -> restClientBuilder
				.requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
						.withConnectTimeout(Duration.ofSeconds(60))
						.withReadTimeout(Duration.ofSeconds(60))));
	}

	@Bean
	BrandService brandService(@Autowired RemoteFileCachingService rfc, @Autowired  UiConfig properties, @Autowired  BrandScoresRepository brandRepository, SerialisationService serialisationService) throws Exception {
		return new BrandService( rfc,properties.logsFolder(), serialisationService);
	}

	@Bean
	IcecatService icecatFeatureService(UiConfig properties, RemoteFileCachingService fileCachingService, BrandService brandService, VerticalsConfigService verticalConfigService) throws SAXException {
		// TODO : xmlMapper not injected because corruct the springdoc used one. Should use a @Primary derivation
		return new IcecatService(new XmlMapper(), properties.getIcecatFeatureConfig(), fileCachingService, properties.getRemoteCachingFolder(), brandService, verticalConfigService);
	}


	@Bean
	OpenDataService openDataService(@Autowired ProductRepository aggregatedDataRepository, @Autowired UiConfig props, @Autowired OpenDataConfig openDataConfig) {
		return new OpenDataService(aggregatedDataRepository, props, openDataConfig);
	}


	// TODO(note) : DISABLING SITE MAP GENERATION
	//	@Bean
	//	public SitemapGenerationService sitemapGenerationService (@Autowired ProductRepository aggregatedDataRepository, @Autowired UiConfig props ) {
	//		return new SitemapGenerationService(aggregatedDataRepository, props);
	//	}
	//


//	@Bean AuthenticationProvider xwikiAuthenticationProvider(@Autowired XWikiAuthenticationService xwikiAuthenticationService) {
//		return new XwikiAuthenticationProvider(xwikiAuthenticationService);
//	}

//	@Bean
//	XWikiReadService readService(@Autowired UiConfig props, @Autowired XwikiMappingService mappingService) {
//		return new XWikiReadService(mappingService, props.getWikiConfig());
//	}




	/** The bean providing datasource configurations **/
	@Bean DataSourceConfigService datasourceConfigService(@Autowired final UiConfig config) {
		return new DataSourceConfigService(config.getDatasourcesfolder());
	}



	@Bean MailService mailService(@Autowired final JavaMailSender sender) {
		return new MailService(sender);
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
	GtinService gtinService(@Autowired ResourceService resourceService) {
		return new GtinService(resourceService);
	}


	@Bean
	ProductRepository aggregatedDataRepo() {
		return new ProductRepository();
	}


	@Bean
	SearchService searchService(@Autowired ProductRepository aggregatedDataRepository, @Autowired final UiConfig uiconfig) {
		return new SearchService(aggregatedDataRepository, uiconfig.logsFolder());
	}


	@Bean
	StandardiserService standardiserService() {
		return new StandardiserService() {
			@Override
			public void standarise(final Price price, final Currency currency) {
			}
		};
	}

	/**
	 * The service that hot evaluates thymeleaf / spel expressions
	 *
	 * @return
	 */
	@Bean
	EvaluationService evaluationService(@Autowired EvaluationConfig evalConfig) {
		return new EvaluationService(evalConfig);
	}



	// TODO : should not be required at ui side
	@Bean RemoteFileCachingService remoteFileCachingService() {
		return new RemoteFileCachingService(config.getRemoteCachingFolder());
	}

    // TODO : should not be required at ui side
    @Bean
    GoogleTaxonomyService googleTaxonomyService(@Autowired RemoteFileCachingService remoteFileCachingService) {
		GoogleTaxonomyService gts = new GoogleTaxonomyService(remoteFileCachingService);

		// TODO : From conf
		// TODO : Add others
        try {
			gts.loadGoogleTaxonUrl("https://www.google.com/basepages/producttype/taxonomy-with-ids.fr-FR.txt", "fr");
			gts.loadGoogleTaxonUrl("https://www.google.com/basepages/producttype/taxonomy-with-ids.en-US.txt", "en");
//			gts.loadGoogleTaxonUrl("https://www.google.com/basepages/producttype/taxonomy-with-ids.de-DE.txt", "de");
//			gts.loadGoogleTaxonUrl("https://www.google.com/basepages/producttype/taxonomy-with-ids.es-ES.txt", "es");
//			gts.loadGoogleTaxonUrl("https://www.google.com/basepages/producttype/taxonomy-with-ids.nl-NL.txt", "nl");




		} catch (Exception e) {
			// TODO
			e.printStackTrace();
		}


        return gts;
	}


	@Bean
	VerticalsConfigService verticalConfigsService(ResourcePatternResolver resourceResolver, SerialisationService serialisationService,  GoogleTaxonomyService googleTaxonomyService, ProductRepository productRepository) throws IOException {
		return new VerticalsConfigService( serialisationService, googleTaxonomyService, productRepository, resourceResolver);
	}

	////////////////////////////////////
	// Locale resolution
	////////////////////////////////////

	@Bean
	LocaleResolver localeResolver() {
		return new AcceptHeaderLocaleResolver();
	}

	static LocaleChangeInterceptor localeChangeInterceptor() {
		final LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
		lci.setParamName("lang");
		return lci;
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

	@Bean
	TimedAspect timedAspect(MeterRegistry registry) {
		return new TimedAspect(registry);
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
				registry.addInterceptor(new GenericTemplateInterceptor());
				registry.addInterceptor(new ImageResizeInterceptor(resourceService(),config.getAllowedImagesSizeSuffixes()));

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
