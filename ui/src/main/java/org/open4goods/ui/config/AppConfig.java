
package org.open4goods.ui.config;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.constants.Currency;
import org.open4goods.model.data.Price;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.ImageMagickService;
import org.open4goods.services.MailService;
import org.open4goods.services.RecaptchaService;
import org.open4goods.services.RemoteFileCachingService;
import org.open4goods.services.ResourceService;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.TagCloudService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.XwikiService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.interceptors.GenericTemplateInterceptor;
import org.open4goods.ui.services.GtinService;
import org.open4goods.ui.services.ImageService;
import org.open4goods.ui.services.OpenDataService;
import org.open4goods.ui.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.util.UrlPathHelper;
import org.yaml.snakeyaml.Yaml;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

@Configuration
public class AppConfig {

	// TODO : Cache period pageNumber conf
	public static final int CACHE_PERIOD_SECONDS = 3600*24*7;
	private @Autowired UiConfig config;

	@Bean
	public ImageService imageService (@Autowired ImageMagickService imageMagickService, @Autowired ResourceService resourceService ) {
		return new ImageService(imageMagickService, resourceService);
	}
	
	@Bean
	public OpenDataService openDataService (@Autowired AggregatedDataRepository aggregatedDataRepository, @Autowired UiConfig props ) {
		return new OpenDataService(aggregatedDataRepository, props);
	}

	
	// TODO(note) : DISABLING SITE MAP GENERATION
//	@Bean
//	public SitemapGenerationService sitemapGenerationService (@Autowired AggregatedDataRepository aggregatedDataRepository, @Autowired UiConfig props ) {
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
	public @Bean DataSourceConfigService datasourceConfigService(@Autowired final UiConfig config) {
		return new DataSourceConfigService(config.getDatasourcesfolder());
	}

	
	@Bean
	public  RecaptchaService recaptchaService() {
		return new RecaptchaService();
	}
	
	
	@Bean MailService mailService(@Autowired final JavaMailSender sender) {
		return new MailService(sender);
	}
	
	@Bean
	public  ImageMagickService imageMagickService() {
		return new ImageMagickService();
	}
	
	@Bean
	public  ResourceService resourceService() {
		return new ResourceService(config.getRemoteCachingFolder());
	}
	
	
	@Bean
	public  GtinService gtinService(@Autowired ResourceService resourceService ) {
		return new GtinService(resourceService);
	}
	
	
	@Bean
	public AggregatedDataRepository aggregatedDataRepo() {
		return new AggregatedDataRepository();
	}

	public @Bean RemoteFileCachingService remoteFileCachingService() {
		return new RemoteFileCachingService(config.getRemoteCachingFolder());
	}

	
	@Bean	
	public SearchService searchService(@Autowired AggregatedDataRepository aggregatedDataRepository, @Autowired final UiConfig uiconfig) {
		return new SearchService(aggregatedDataRepository, uiconfig.logsFolder());
	}

	@Bean
	public TagCloudService tagCloudService(@Autowired final ResourceService resourceService, @Autowired final UiConfig uiconfig) {
		return new TagCloudService(resourceService, uiconfig.getTagCloudConfig());
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
	public VerticalsConfigService verticalConfigsService( SerialisationService serialisationService) throws IOException {
		return new VerticalsConfigService( serialisationService,config.getVerticalsFolder());
	}
	
	////////////////////////////////////
	// Locale resolution
	////////////////////////////////////
	
	@Bean
	public LocaleResolver localeResolver() {
		return new AcceptHeaderLocaleResolver();
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		final LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
		lci.setParamName("lang");
		return lci;
	}

	@Bean
	public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
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
	

	///////////////////////////////////
	// Resources
	///////////////////////////////////
	@Bean
	WebMvcConfigurer configurer() {
		return new WebMvcConfigurer() {

			@Override
			public void extendMessageConverters(final List<HttpMessageConverter<?>> converters) {
				converters.add(new YamlHttpMessageConverter<>());
			}

			@Override
			public void addInterceptors(final InterceptorRegistry registry) {
				registry.addInterceptor(localeChangeInterceptor());
				registry.addInterceptor(new GenericTemplateInterceptor());
			}
			
		    @Override
		    public void configurePathMatch(PathMatchConfigurer configurer) {
		        UrlPathHelper urlPathHelper = new UrlPathHelper();
		        urlPathHelper.setUrlDecode(false);
		        configurer.setUrlPathHelper(urlPathHelper);
		    }


			/**
			 * Define explicitly each static resources (because of the controller /* in the
			 * CommonPageCOntroller) Also overrides classpath values with the ones pageNumber
			 * filesystem
			 */
		    @Override
			public void addResourceHandlers(final ResourceHandlerRegistry registry) {

				registry.setOrder(Ordered.LOWEST_PRECEDENCE);
				registry.addResourceHandler("/sitemap/**").addResourceLocations("file:" + config.siteMapFolder().getAbsolutePath() + "/");
				
				registry
	             .addResourceHandler("/assets/**")
	             .addResourceLocations("classpath:/static/assets/")
	             .setCachePeriod(CACHE_PERIOD_SECONDS);
	        
		        registry
	            .addResourceHandler("/css/**")
	            .addResourceLocations("classpath:/static/css/")
	            .setCachePeriod(CACHE_PERIOD_SECONDS);
		        
		        
			}
		    

		};
	}

	///////////////////////////////
	// The converter for yaml messages
	///////////////////////////////

	class YamlHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

		public YamlHttpMessageConverter() {
			super(new MediaType("text", "yaml"));
		}

		@Override
		protected boolean supports(final Class<?> clazz) {
			return true;
		}

		@Override
		protected T readInternal(final Class<? extends T> clazz, final HttpInputMessage inputMessage)
				throws IOException, HttpMessageNotReadableException {
			final Yaml yaml = new Yaml();
			final T t = yaml.loadAs(inputMessage.getBody(), clazz);
			return t;
		}

		@Override
		protected void writeInternal(final T t, final HttpOutputMessage outputMessage)
				throws IOException, HttpMessageNotWritableException {
			final Yaml yaml = new Yaml();

			final OutputStreamWriter writer = new OutputStreamWriter(outputMessage.getBody());
			yaml.dump(t, writer);
			writer.close();
		}
	}
	

	//////////////////////////////////////////////
	// The uidMap managers
	//////////////////////////////////////////////

	@Bean
	public CacheManager cacheManager(@Autowired final Ticker ticker) {
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
	public Ticker ticker() {
		return Ticker.systemTicker();
	}

}