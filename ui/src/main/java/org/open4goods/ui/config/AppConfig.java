
package org.open4goods.ui.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.open4goods.dao.ProductRepository;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.constants.Currency;
import org.open4goods.model.data.Price;
import org.open4goods.model.product.Product;
import org.open4goods.services.BrandService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.ImageMagickService;
import org.open4goods.services.MailService;
import org.open4goods.services.RecaptchaService;
import org.open4goods.services.RemoteFileCachingService;
import org.open4goods.services.ResourceService;
import org.open4goods.services.SearchService;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.XwikiService;
import org.open4goods.services.ai.AiService;
import org.open4goods.services.ai.AiAgent;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.services.GtinService;
import org.open4goods.ui.services.ImageService;
import org.open4goods.ui.services.OpenDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;

@Configuration
public class AppConfig {

	
	
	// TODO : Cache period pageNumber conf
	public static final int CACHE_PERIOD_SECONDS = 3600*24*7;
	private final UiConfig config;

	public AppConfig(UiConfig config) {
		this.config = config;
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
	ImageService imageService(@Autowired ImageMagickService imageMagickService, @Autowired ResourceService resourceService) {
		return new ImageService(imageMagickService, resourceService);
	}

	@Bean
	@Autowired  
	AiService aiService (AiAgent nudgerAgent, VerticalsConfigService verticalService, EvaluationService spelEvaluationService) {
		return new AiService(nudgerAgent, verticalService, spelEvaluationService);
	}
	
	 @Bean
	 AiAgent nudgerAgent(@Autowired ChatLanguageModel chatLanguageModel) {
	        return AiServices.builder(AiAgent.class)
	                .chatLanguageModel(chatLanguageModel)	                
//	                .retriever(retriever)
	                .build();
	    }
	 
	 
	@Bean
	BrandService brandService(@Autowired RemoteFileCachingService rfc, @Autowired  UiConfig properties) {
		return new BrandService(properties.getBrandConfig(),  rfc);
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


	@Bean AuthenticationProvider capsuleAuthenticationProvider() {
		return new XwikiAuthenticationProvider();
	}

	@Bean
	XwikiService xwikiService(@Autowired UiConfig props) {
		return new XwikiService(props.getWikiConfig());
	}


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

	@Bean
	@Autowired
	VerticalsConfigService verticalConfigsService(SerialisationService serialisationService) throws IOException {
		return new VerticalsConfigService( serialisationService,config.getVerticalsFolder());
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