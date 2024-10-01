
package org.open4goods.ui.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.helper.DevModeService;
import org.open4goods.commons.interceptors.BanCheckerInterceptor;
import org.open4goods.commons.model.constants.CacheConstants;
import org.open4goods.commons.model.constants.Currency;
import org.open4goods.commons.model.data.Price;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.EvaluationService;
import org.open4goods.commons.services.FeedbackService;
import org.open4goods.commons.services.GoogleTaxonomyService;
import org.open4goods.commons.services.IcecatService;
import org.open4goods.commons.services.ImageGenerationService;
import org.open4goods.commons.services.ImageMagickService;
import org.open4goods.commons.services.MailService;
import org.open4goods.commons.services.RecaptchaService;
import org.open4goods.commons.services.RemoteFileCachingService;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.commons.services.StandardiserService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.commons.services.ai.AiService;
import org.open4goods.commons.store.repository.elastic.BrandScoresRepository;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.interceptors.GenericTemplateInterceptor;
import org.open4goods.ui.repository.ContributionVoteRepository;
import org.open4goods.ui.services.BlogService;
import org.open4goods.ui.services.ContributionService;
import org.open4goods.ui.services.GoogleIndexationService;
import org.open4goods.ui.services.GtinService;
import org.open4goods.ui.services.OpenDataService;
import org.open4goods.ui.services.SitemapGenerationService;
import org.open4goods.ui.services.todo.TodoService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.beans.factory.annotation.Autowired;
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
	TodoService todoService() {
		return new TodoService(config.getTagListUrl());
	}
	
	@Bean 
	@Autowired
	SitemapGenerationService sitemapGenerationService(ProductRepository repository, VerticalsConfigService verticalConfigService, BlogService blogService, XwikiFacadeService wikiService, ApplicationContext context ) {
		return new SitemapGenerationService(repository, config, verticalConfigService, blogService, wikiService, context);
	}
	
	@Bean
	@Autowired
	GoogleIndexationService googleIndexationService(ProductRepository repository, VerticalsConfigService verticalConfigService) {
		return new GoogleIndexationService(config.getGoogleApiJson(), config.getGoogleIndexationMarkerFile(), repository, verticalConfigService);
	}

	@Bean
	BarcodeValidationService barcodeValidationService () {
		return new BarcodeValidationService();
	}
	
	@Bean UiService uiService () {
		return new UiService();
	}


    @Bean
    @Autowired
    DevModeService devModeService(ProductRepository repository, SerialisationService serialisationService, VerticalsConfigService verticalsConfigService) {
		return new DevModeService(config.getDevModeConfig(),repository, serialisationService, verticalsConfigService);
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
    BlogService blogService(@Autowired XwikiFacadeService xwikiReadService, @Autowired UiConfig config) {
		return new BlogService(xwikiReadService, config.getBlogConfig(), config.getNamings().getBaseUrls());
	}

    
    @Bean
    @Autowired
    ContributionService contributionService (CacheManager cacheManager, SerialisationService serialisationService, ContributionVoteRepository repository, UiConfig uiConfig, ElasticsearchOperations esOps) {
    	return new ContributionService(cacheManager, serialisationService, repository, uiConfig.getReversementConfig(), esOps);
    }
    
    
    @Bean
    ImageGenerationService imageGenerationService(@Autowired OpenAiImageModel imageModel, @Autowired UiConfig uiConfig) {
		return new ImageGenerationService(imageModel, uiConfig.getImageGenerationConfig(), uiConfig.getGeneratedImagesFolder());
	}
	  
	@Bean
	FeedbackService feedbackService(@Autowired UiConfig config) {
		return new FeedbackService(config.getFeedbackConfig());
	}


	@Bean
	@Autowired
	AiService aiService (OpenAiChatModel chatModel,  EvaluationService spelEvaluationService, SerialisationService serialisationService) {
		return new AiService(chatModel, spelEvaluationService, serialisationService);
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
	BrandService brandService(@Autowired RemoteFileCachingService rfc, @Autowired  UiConfig properties, @Autowired  BrandScoresRepository brandRepository) {
		return new BrandService(properties.getBrandConfig(),  rfc,brandRepository,properties.logsFolder());
	}

	@Bean
	@Autowired
	IcecatService icecatFeatureService(UiConfig properties, RemoteFileCachingService fileCachingService, BrandService brandService, VerticalsConfigService verticalConfigService) throws SAXException {
		// TODO : xmlMapper not injected because corruct the springdoc used one. Should use a @Primary derivation
		return new IcecatService(new XmlMapper(), properties.getIcecatFeatureConfig(), fileCachingService, properties.getRemoteCachingFolder(), brandService, verticalConfigService);
	}
	
	
	@Bean
	OpenDataService openDataService(@Autowired ProductRepository aggregatedDataRepository, @Autowired UiConfig props) {
		return new OpenDataService(aggregatedDataRepository, props);
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


	@Bean
	RecaptchaService recaptchaService() {
		return new RecaptchaService();
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

	@Bean
	EvaluationService evaluationService() {
		return new EvaluationService();
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
			gts.loadGoogleTaxonUrl("https://www.google.com/basepages/producttype/taxonomy-with-ids.de-DE.txt", "de");
			gts.loadGoogleTaxonUrl("https://www.google.com/basepages/producttype/taxonomy-with-ids.es-ES.txt", "es");
			gts.loadGoogleTaxonUrl("https://www.google.com/basepages/producttype/taxonomy-with-ids.nl-NL.txt", "nl");
			
			
			
			
		} catch (Exception e) {
			// TODO
			e.printStackTrace();
		}        
        
        
        return gts;
	}
    
    
	@Bean
	@Autowired
	VerticalsConfigService verticalConfigsService(ResourcePatternResolver resourceResolver, SerialisationService serialisationService,  GoogleTaxonomyService googleTaxonomyService, ProductRepository productRepository, ImageGenerationService imageGenerationService) throws IOException {
		return new VerticalsConfigService( serialisationService, googleTaxonomyService, productRepository, resourceResolver, imageGenerationService);
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
				registry.addInterceptor(new BanCheckerInterceptor(config.getBancheckerConfig()));
				registry.addInterceptor(AppConfig.localeChangeInterceptor());
				registry.addInterceptor(new GenericTemplateInterceptor());
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
