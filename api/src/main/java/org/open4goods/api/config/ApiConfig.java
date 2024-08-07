package org.open4goods.api.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.BatchService;
import org.open4goods.api.services.CompletionFacadeService;
import org.open4goods.api.services.FetcherOrchestrationService;
import org.open4goods.api.services.completion.AmazonCompletionService;
import org.open4goods.api.services.completion.GenAiCompletionService;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.config.yml.attributes.PromptConfig;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.repository.CsvIndexationRepository;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.services.ApiSynchService;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.FeedService;
import org.open4goods.crawler.services.FetchersService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.crawler.services.fetching.WebDatasourceFetchingService;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.DevModeService;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.constants.Currency;
import org.open4goods.model.constants.TimeConstants;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Price;
import org.open4goods.model.product.Product;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.BrandService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.Gs1PrefixService;
import org.open4goods.services.IcecatService;
import org.open4goods.services.ImageGenerationService;
import org.open4goods.services.ImageMagickService;
import org.open4goods.services.RemoteFileCachingService;
import org.open4goods.services.ResourceService;
import org.open4goods.services.SearchService;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.ai.AiService;
import org.open4goods.services.textgen.BlablaService;
import org.open4goods.store.repository.elastic.BrandScoresRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.aop.TimedAspect;
//import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class ApiConfig {

	private final ApiProperties apiProperties;
	private static final Logger logger = LoggerFactory.getLogger(ApiConfig.class);


	protected final Environment env;

	public ApiConfig(ApiProperties apiProperties, Environment env) {
		this.env =	env;
		this.apiProperties = apiProperties;
	}

    
	  @Bean
	  public RedisTemplate<String, Product> redisTemplate(RedisConnectionFactory connectionFactory) {
		  RedisTemplate<String, Product> template = new RedisTemplate<>();
		    template.setConnectionFactory(connectionFactory);
		    
		    // Configure serialization
		    template.setKeySerializer(new StringRedisSerializer());
		    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

		    return template;
	  }
	 
	@Bean
	SerialisationService serialisationService() {
		return new SerialisationService();
	}

	
	@Bean
	@Autowired
	IcecatService icecatFeatureService( RemoteFileCachingService fileCachingService, BrandService brandService,  VerticalsConfigService verticalConfigService) throws SAXException {
		// TODO : xmlMapper not injected because corruct the springdoc used one. Should use a @Primary derivation
		return new IcecatService(new XmlMapper(), apiProperties.getIcecatFeatureConfig(), fileCachingService, apiProperties.remoteCachingFolder(), brandService, verticalConfigService);
	}
	
	@Bean
	@Autowired
	GenAiCompletionService aiCompletionService(AiService aiService, ProductRepository productRepository, VerticalsConfigService verticalConfigService, PromptConfig aiConfig) {
		return new GenAiCompletionService(aiService, productRepository, verticalConfigService, apiProperties, aiConfig);
	}

	@Bean
	@Autowired
	AiService aiService(OpenAiChatModel chatModel, final VerticalsConfigService verticalService, EvaluationService spelEvaluationService) {
		return new AiService(chatModel, verticalService, spelEvaluationService);
	}
	
	@Bean
	@Autowired
	IcecatCompletionService icecatCompletionService( ProductRepository productRepository, VerticalsConfigService verticalConfigService, DataSourceConfigService dataSourceConfigService, AggregationFacadeService aggregationFacade) throws TechnicalException {
		return new IcecatCompletionService(productRepository, verticalConfigService, apiProperties, dataSourceConfigService, aggregationFacade);
	}

	@Bean	
	@Autowired
	AmazonCompletionService amazonCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService,
			ApiProperties apiProperties, DataSourceConfigService dataSourceConfigService, AggregationFacadeService aggregationFacade) throws TechnicalException {
		return new AmazonCompletionService(dataRepository, verticalConfigService, apiProperties, dataSourceConfigService, aggregationFacade);
	}

	
	@Bean
	@Autowired
	BatchService batchService(AggregationFacadeService aggregationFacadeService,
			CompletionFacadeService completionFacadeService, VerticalsConfigService verticalsConfigService) {
		return new BatchService(aggregationFacadeService, completionFacadeService, verticalsConfigService);
	}
	
	
	@Bean
	@Autowired
	 CompletionFacadeService completionFacadeService(GenAiCompletionService aiCompletionService,
			ResourceCompletionService resourceCompletionService, AmazonCompletionService amazonCompletionService, IcecatCompletionService icecatCompletionService) {
		return new CompletionFacadeService(aiCompletionService, resourceCompletionService, amazonCompletionService, icecatCompletionService);
	}
	
	@Bean	
	BlablaService blablaService(@Autowired EvaluationService evaluationService) {
		return new BlablaService(evaluationService);
	}

	@Bean
	@Autowired
	public DevModeService devModeService (ProductRepository repository, SerialisationService serialisationService, VerticalsConfigService verticalsConfigService) {
		return new DevModeService(apiProperties.getDevModeConfig(),repository, serialisationService, verticalsConfigService);
	}
	
	
    @Bean
    @Autowired
    FeedService feedService(SerialisationService serialisationService, DataSourceConfigService datasourceConfigService, CsvDatasourceFetchingService fetchingService,FetcherProperties  fetcherProperties) {
		return new FeedService(serialisationService, datasourceConfigService, fetchingService, fetcherProperties.getFeedConfigs());
    }


	@Bean
	@Autowired
	VerticalsConfigService verticalConfigsService(ResourcePatternResolver resolver, SerialisationService serialisationService, GoogleTaxonomyService googleTaxonomyService, ProductRepository productRepository, ImageGenerationService imageGenService) throws IOException {
		return new VerticalsConfigService(serialisationService,apiProperties.getVerticalsFolder(), googleTaxonomyService, productRepository, resolver,imageGenService);
	}
	
	@Bean
	public ImageGenerationService imageGenerationService(@Autowired OpenAiImageModel imageModel) {
		return new ImageGenerationService(imageModel, apiProperties.getImageGenerationConfig(), apiProperties.getGeneratedImagesFolder());
	}
	  
	
	@Bean
	SearchService searchService(@Autowired ProductRepository aggregatedDataRepository, @Autowired  String logsFolder) {
		return new SearchService(aggregatedDataRepository, logsFolder);
	}

	@Bean
	BrandService brandService(@Autowired RemoteFileCachingService rfc, @Autowired  ApiProperties properties, @Autowired BrandScoresRepository brandRepository) {
		return new BrandService(properties.getBrandConfig(),  rfc, brandRepository, properties.logsFolder());
	}

	@Bean
	public PromptConfig aiConfig() { return new PromptConfig(); }

	@Bean
	@Autowired  
	ResourceCompletionService resourceCompletionService (ImageMagickService imageService, VerticalsConfigService verticalConfigService, ResourceService resourceService, ProductRepository dataRepository, ApiProperties apiProperties) {
		return new ResourceCompletionService(imageService, verticalConfigService, resourceService, dataRepository, apiProperties);
		
		
	}
    @Bean
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
			logger.error("Error loading google taxonomy", e);
		}        
        
        
        return gts;
	}

	 
	@Bean
	AggregationFacadeService realtimeAggregationService( @Autowired EvaluationService evaluationService,
			@Autowired StandardiserService standardiserService,
			@Autowired AutowireCapableBeanFactory autowireBeanFactory, @Autowired ProductRepository aggregatedDataRepository,
			@Autowired ApiProperties apiProperties, @Autowired Gs1PrefixService gs1prefixService,
			@Autowired DataSourceConfigService dataSourceConfigService, 
			@Autowired VerticalsConfigService configService, 
			@Autowired BarcodeValidationService barcodeValidationService,
			@Autowired BrandService brandservice,
			@Autowired GoogleTaxonomyService gts,
			@Autowired BlablaService blablaService,
			@Autowired IcecatService icecatFeatureService) {
		return new AggregationFacadeService(evaluationService, standardiserService, autowireBeanFactory, aggregatedDataRepository, apiProperties, gs1prefixService, dataSourceConfigService, configService,  barcodeValidationService,brandservice, gts, blablaService, icecatFeatureService);
	}



	//////////////////////////////////////////////////////////
	// SwaggerConfig
	//////////////////////////////////////////////////////////

	@Bean
	GroupedOpenApi adminApi() {
		return GroupedOpenApi.builder()
				
				.group("api")
				//	              .pathsToMatch("/admin/**")
				.packagesToScan("org.open4goods.api")
				.addOpenApiCustomizer(apiSecurizer())

				.build();
	}

	@Bean
	OpenApiCustomizer apiSecurizer() {
		return openApi -> openApi.addSecurityItem(new SecurityRequirement().addList("Authorization"))
				.components(new Components()
						.addSecuritySchemes(UrlConstants.APIKEY_PARAMETER, new SecurityScheme()
								.in(SecurityScheme.In.HEADER)
								.type(SecurityScheme.Type.APIKEY)
								.name(UrlConstants.APIKEY_PARAMETER)
								)

						);

	}


	//////////////////////////////////////////////////////////
	// The scheduling thread pool executor
	//////////////////////////////////////////////////////////

	

	
//	@Bean
//	public TaskScheduler heartBeatScheduler() {
//	    return new ThreadPoolTaskScheduler();
//	}
//	
	@Bean TaskScheduler threadPoolTaskScheduler() {
		final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(2);
		threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
		return threadPoolTaskScheduler;
	}

	//////////////////////////////////////////////
	// The uidMap managers
	//////////////////////////////////////////////

	@Bean
	CacheManager cacheManager(@Autowired final Ticker ticker) {
		final CaffeineCache fCache = buildExpiryCache(CacheConstants.FOREVER_LOCAL_CACHE_NAME, ticker, 30000000);
		final CaffeineCache hCache = buildExpiryCache(CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, ticker, 60);
		final CaffeineCache mCache = buildExpiryCache(CacheConstants.ONE_MINUTE_LOCAL_CACHE_NAME, ticker, 1);
		final CaffeineCache dCache = buildExpiryCache(CacheConstants.ONE_DAY_LOCAL_CACHE_NAME, ticker, 60 * 24);
		final SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(fCache, dCache, hCache,mCache));
		return manager;
	}

	
	
	private CaffeineCache buildExpiryCache(final String name, final Ticker ticker, final int minutesToExpire) {
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

	///////////////////////////
	// Metrics
	//////////////////////////

//	/**
//	 * To enable metrics on all methods / services
//	 *
//	 * @param registry
//	 * @return
//	 */
//	@Bean TimedAspect timedAspect(final MeterRegistry registry) {
//		return new TimedAspect(registry);
//	}

	//////////////////////////////////////////////////////////
	// API services
	//////////////////////////////////////////////////////////

	/** The bean providing datasource configurations **/
	@Bean DataSourceConfigService datasourceConfigService(@Autowired final ApiProperties config) {
		return new DataSourceConfigService(config.getDatasourcesfolder());
	}


	@Bean ResourceService resourceService() {
		return new ResourceService(apiProperties.remoteCachingFolder());
	}


	@Bean
	String logsFolder(@Autowired final ApiProperties config) {
		return config.logsFolder();
	}


	@Bean ImageMagickService imageService() {
		return new ImageMagickService();
	}

	@Bean RemoteFileCachingService remoteFileCachingService(@Autowired final ApiProperties config) {
		return new RemoteFileCachingService(config.remoteCachingFolder());
	}


	@Bean DataFragmentStoreService dataFragmentStoreService(
			@Autowired final ApiProperties config, @Autowired final SerialisationService serialisationService, @Autowired StandardiserService standardiserService, @Autowired AggregationFacadeService generationService, @Autowired ProductRepository aggregatedDataRepository) {
		return new DataFragmentStoreService(standardiserService, generationService, aggregatedDataRepository);
	}

	/**
	 * The service that hot evaluates thymeleaf / spel expressions
	 *
	 * @return
	 */
	@Bean EvaluationService evaluationService() {
		return new EvaluationService();
	}

	@Bean
	ProductRepository aggregatedDatasRepository(@Autowired final ApiProperties config) {
		return new ProductRepository();
	}


	@Bean
	StandardiserService standardiserService() {
		return new StandardiserService() {

			@Override
			public void standarise(final Price price, final Currency currency) {
				// TODO(feature, 0.5, P3) : implements standardisation
			}
		};
	}


	@Bean
	BarcodeValidationService barcodeValidationService () {
		return new BarcodeValidationService();
	}



	//	@Bean
	//	AggregationFacadeService fullGenerationService( @Autowired EvaluationService evaluationService,
	//			@Autowired ReferentielService referentielService, @Autowired StandardiserService standardiserService,
	//			@Autowired AutowireCapableBeanFactory autowireBeanFactory, @Autowired ProductRepository aggregatedDataRepository,
	//			@Autowired ApiProperties apiProperties, @Autowired Gs1PrefixService gs1prefixService,
	//			@Autowired DataSourceConfigService dataSourceConfigService, @Autowired VerticalsConfigService configService, @Autowired BarcodeValidationService barcodeValidationService, @Autowired GoogleTaxonomyService taxonomyService) {
	//		return new AggregationFacadeService(repository, evaluationService, referentielService, standardiserService, autowireBeanFactory, aggregatedDataRepository, apiProperties, gs1prefixService, dataSourceConfigService, configService,  barcodeValidationService, taxonomyService);
	//	}
	//


	@Bean Gs1PrefixService gs1prefixService (@Autowired ResourcePatternResolver resourceResolver) throws IOException{
		return new Gs1PrefixService("classpath:/gs1-prefix.csv", resourceResolver);

	}

	//////////////////////////////////////////////
	// Embeded crawler configuration
	//////////////////////////////////////////////


	// For the crawlController, inported from crawler
	@Bean FetcherProperties fetcherProperties(@Autowired final ApiProperties apiProperties) {
		return apiProperties.getFetcherProperties();
	}

 
    
	@Bean
	CsvDatasourceFetchingService csvDatasourceFetchingService(
			@Autowired final DataFragmentCompletionService completionService,
			@Autowired final IndexationService indexationService, @Autowired final ApiProperties apiProperties,
			@Autowired final WebDatasourceFetchingService webDatasourceFetchingService,
			@Autowired final IndexationRepository indexationRepository,
			  @Autowired CsvIndexationRepository csvIndexationRepo
			
			
			) {
		
		boolean toConsole = ArrayUtils.contains(env.getActiveProfiles(), "dev") || ArrayUtils.contains(env.getActiveProfiles(), "devsec");
		// TODO : Not nice, mutualize


        return new CsvDatasourceFetchingService(csvIndexationRepo, completionService, indexationService,
				apiProperties.getFetcherProperties(), webDatasourceFetchingService,indexationRepository, webDatasourceFetchingService, apiProperties.logsFolder(), toConsole);
	}

	@Bean
	WebDatasourceFetchingService webDatasourceFetchingService(@Autowired final IndexationRepository indexationRepository,
			@Autowired final IndexationService indexationService, @Autowired final ApiProperties apiProperties) {

		// Logging to console according to dev profile and conf
		boolean toConsole = ArrayUtils.contains(env.getActiveProfiles(), "dev") || ArrayUtils.contains(env.getActiveProfiles(), "devsec");
		// TODO : Not nice, mutualize


        return new WebDatasourceFetchingService(indexationService, apiProperties.getFetcherProperties(),indexationRepository,
				apiProperties.logsFolder(), toConsole);
	}


	/**
	 * A custom "direct" implementation to update directly the local crawler status,
	 * bypassing HTTP transport and serialisation
	 *
	 * @return
	 */
	@Bean
	IndexationService indexationService(@Autowired final DataFragmentStoreService dataFragmentStoreService) {
		return new IndexationService() {

			@Override
			protected void indexInternal(final DataFragment data) throws ValidationException {
				// Direct indexation on the store service
				dataFragmentStoreService.queueDataFragment(data);
			}
		};
	}

	@Bean
	FetchersService crawlersInterface(@Autowired final ApiProperties apiProperties,
			@Autowired final CsvDatasourceFetchingService csvDatasourceFetchingService,
			@Autowired final WebDatasourceFetchingService webDatasourceFetchingService
			) {
		return new FetchersService(apiProperties.getFetcherProperties(), webDatasourceFetchingService,
				csvDatasourceFetchingService);
	}

	@Bean
	@Autowired
	FetcherOrchestrationService fetcherOrchestrationService(TaskScheduler taskScheduler, DataSourceConfigService dataSourceConfigService) {
		return new FetcherOrchestrationService(taskScheduler, dataSourceConfigService);
	}

	@Bean
	DataFragmentCompletionService offerCompletionService(@Autowired BrandService brandService) {
		return new DataFragmentCompletionService(brandService);
	}


	@Bean
	/**
	 * A custom "direct" implementation to update directly the local crawler status,
	 * bypassing HTTP transport and serialisation
	 *
	 * @return
	 */
	@Autowired
	ApiSynchService apiSynchService(final ApiProperties apiProperties, FetchersService crawlersInterface, FetcherOrchestrationService fetcherOrchestrationService) {
		return new ApiSynchService(apiProperties.getFetcherProperties().getApiSynchConfig(), crawlersInterface, null,
				null) {
			@Override
			@Scheduled(initialDelay = 0L, fixedDelay = TimeConstants.CRAWLER_UPDATE_STATUS_TO_API_MS)
			public void updateStatus() {
				fetcherOrchestrationService.updateClientStatus(crawlersInterface.stats());
			}
		};
	}

	
	@Bean
	public TimedAspect timedAspect(MeterRegistry registry) {
	  return new TimedAspect(registry);
	}
	


}
