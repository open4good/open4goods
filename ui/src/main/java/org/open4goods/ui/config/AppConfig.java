
package org.open4goods.ui.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.open4goods.dao.ProductRepository;
import org.open4goods.helper.DevModeService;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.constants.Currency;
import org.open4goods.model.data.Price;
import org.open4goods.model.product.Product;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.BrandService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.FeedbackService;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.IcecatService;
import org.open4goods.services.ImageGenerationService;
import org.open4goods.services.ImageMagickService;
import org.open4goods.services.MailService;
import org.open4goods.services.RecaptchaService;
import org.open4goods.services.RemoteFileCachingService;
import org.open4goods.services.ResourceService;
import org.open4goods.services.SearchService;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.ai.AiService;
import org.open4goods.store.repository.elastic.BrandScoresRepository;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.services.BlogService;
import org.open4goods.ui.services.GtinService;
import org.open4goods.ui.services.ImageService;
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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

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
	BarcodeValidationService barcodeValidationService () {
		return new BarcodeValidationService();
	}
	
	@Bean UiService uiService () {
		return new UiService();
	}
	
	@Bean
	@Autowired
	public DevModeService devModeService (ProductRepository repository, SerialisationService serialisationService, VerticalsConfigService verticalsConfigService) {
		return new DevModeService(config.getDevModeConfig(),repository, serialisationService, verticalsConfigService);
	}
	
	
	  @Bean
	  public RedisTemplate<String, Product> redisTemplate(RedisConnectionFactory connectionFactory) {
		  RedisTemplate<String, Product> template = new RedisTemplate<>();
		    template.setConnectionFactory(connectionFactory);
		    
		    // Configure serialization
		    template.setKeySerializer(new StringRedisSerializer());
		    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

		    
		    // Add some specific configuration here. Key serializers, etc.
		    return template;
	  }
	  
	  
	  
    @Bean
	public BlogService blogService(@Autowired XwikiFacadeService xwikiReadService, @Autowired UiConfig config) {
		return new BlogService(xwikiReadService, config.getBlogConfig(), config.getNamings().getBaseUrls());
	}

	@Bean
	public ImageGenerationService imageGenerationService(@Autowired OpenAiImageModel imageModel, @Autowired UiConfig uiConfig) {
		return new ImageGenerationService(imageModel, uiConfig.getImageGenerationConfig(), uiConfig.getGeneratedImagesFolder());
	}
	  
	@Bean
	FeedbackService feedbackService(@Autowired UiConfig config) {
		return new FeedbackService(config.getFeedbackConfig());
	}

	  
	
	@Bean
	ImageService imageService(@Autowired ImageMagickService imageMagickService, @Autowired ResourceService resourceService) {
		return new ImageService(imageMagickService, resourceService);
	}

	@Bean
	@Autowired
	AiService aiService (OpenAiChatModel chatModel, VerticalsConfigService verticalService, EvaluationService spelEvaluationService) {
		return new AiService(chatModel, verticalService, spelEvaluationService);
	}



	/** Override the default RestTemplate with a custom one that has a longer timeout (For ImageGenerationService) **/
	@Bean
	public RestClientCustomizer restClientCustomizer() {
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
	
    @Bean
	// TODO : should not be required at ui side
    public GoogleTaxonomyService googleTaxonomyService(@Autowired RemoteFileCachingService remoteFileCachingService) {
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
		return new VerticalsConfigService( serialisationService,config.getVerticalsFolder(), googleTaxonomyService, productRepository, resourceResolver, imageGenerationService);
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




	//////////////////////////////////////////////
	// The uidMap managers
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
				Caffeine.newBuilder().expireAfterWrite(minutesToExpire, TimeUnit.MINUTES).ticker(ticker).build());
	}

	@Bean
	Ticker ticker() {
		return Ticker.systemTicker();
	}

}
