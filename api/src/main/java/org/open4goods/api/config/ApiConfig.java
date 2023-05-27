package org.open4goods.api.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.BatchAggregationService;
import org.open4goods.api.services.BatchService;
import org.open4goods.api.services.FetcherOrchestrationService;
import org.open4goods.api.services.RealtimeAggregationService;
import org.open4goods.api.services.ReferentielService;
import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.services.ApiSynchService;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.FetchersService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.crawler.services.fetching.WebDatasourceFetchingService;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.constants.Currency;
import org.open4goods.model.constants.TimeConstants;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Price;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.Gs1PrefixService;
import org.open4goods.services.ImageMagickService;
import org.open4goods.services.RemoteFileCachingService;
import org.open4goods.services.ResourceService;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class ApiConfig {

	@Autowired ApiProperties apiProperties;


	private static final Logger logger = LoggerFactory.getLogger(ApiConfig.class);

	
	@Bean
	@Autowired
	public VerticalsConfigService verticalConfigsService(SerialisationService serialisationService) throws IOException {
		return new VerticalsConfigService(serialisationService,apiProperties.getVerticalsFolder());
	}
	
	@Bean
	@Autowired
	public BatchService batchService(AggregatedDataRepository dataRepository, VerticalsConfigService verticalsConfigService, BatchAggregationService batchAggregationService) throws IOException {
		return new BatchService(dataRepository, apiProperties, verticalsConfigService,batchAggregationService);
	}


	/**
	 * Various generation (json, yaml, binary)
	 *
	 * @return
	 */
	@Bean
	SerialisationService serialisationService() {
		return new SerialisationService();
	}

	
	@Bean
	RealtimeAggregationService realtimeAggregationService( @Autowired EvaluationService evaluationService,
			@Autowired ReferentielService referentielService, @Autowired StandardiserService standardiserService,
			@Autowired AutowireCapableBeanFactory autowireBeanFactory, @Autowired AggregatedDataRepository aggregatedDataRepository,
			@Autowired ApiProperties apiProperties, @Autowired Gs1PrefixService gs1prefixService,
			@Autowired DataSourceConfigService dataSourceConfigService, @Autowired VerticalsConfigService configService, @Autowired BarcodeValidationService barcodeValidationService) {
		return new RealtimeAggregationService(evaluationService, referentielService, standardiserService, autowireBeanFactory, aggregatedDataRepository, apiProperties, gs1prefixService, dataSourceConfigService, configService,  barcodeValidationService);
	}
	
	@Bean
	BatchAggregationService batchAggregationService( @Autowired EvaluationService evaluationService,
			@Autowired ReferentielService referentielService, @Autowired StandardiserService standardiserService,
			@Autowired AutowireCapableBeanFactory autowireBeanFactory, @Autowired AggregatedDataRepository aggregatedDataRepository,
			@Autowired ApiProperties apiProperties, @Autowired Gs1PrefixService gs1prefixService,
			@Autowired DataSourceConfigService dataSourceConfigService, @Autowired VerticalsConfigService configService, @Autowired BarcodeValidationService barcodeValidationService) {
		return new BatchAggregationService(evaluationService, referentielService, standardiserService, autowireBeanFactory, aggregatedDataRepository, apiProperties, gs1prefixService, dataSourceConfigService, configService,  barcodeValidationService);
	}
	
	
	//////////////////////////////////////////////////////////
	// SwaggerConfig
	//////////////////////////////////////////////////////////

	  @Bean
	  public GroupedOpenApi adminApi() {
	      return GroupedOpenApi.builder()
	              .group("api")
//	              .pathsToMatch("/admin/**")
	              .packagesToScan("org.open4goods.api")
	              .addOpenApiCustomizer(apiSecurizer())
	              
	              .build();
	  }
	  
	    @Bean
	    public OpenApiCustomizer apiSecurizer() {
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

	public @Bean ThreadPoolTaskScheduler threadPoolTaskScheduler() {
		final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(2);
		threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
		return threadPoolTaskScheduler;
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

	///////////////////////////
	// Metrics
	//////////////////////////

	/**
	 * To enable metrics on all methods / services
	 *
	 * @param registry
	 * @return
	 */
	public @Bean TimedAspect timedAspect(final MeterRegistry registry) {
		return new TimedAspect(registry);
	}

	//////////////////////////////////////////////////////////
	// API services
	//////////////////////////////////////////////////////////

	/** The bean providing datasource configurations **/
	public @Bean DataSourceConfigService datasourceConfigService(@Autowired final ApiProperties config) {
		return new DataSourceConfigService(config.getDatasourcesfolder());
	}

	
	@Bean ResourceService resourceService() {
		return new ResourceService(apiProperties.remoteCachingFolder());
	}
	
	
	@Bean
	public String logsFolder(@Autowired final ApiProperties config) {
		return config.logsFolder();
	}


	public @Bean ImageMagickService imageService() {
		return new ImageMagickService();
	}

	public @Bean ReferentielService referentielService(@Autowired final ApiProperties config) {
		return new ReferentielService(config.logsFolder());
	}

	public @Bean RemoteFileCachingService remoteFileCachingService(@Autowired final ApiProperties config) {
		return new RemoteFileCachingService(config.remoteCachingFolder());
	}


	public @Bean DataFragmentStoreService dataFragmentStoreService(
			 @Autowired final ApiProperties config, @Autowired final SerialisationService serialisationService, @Autowired StandardiserService standardiserService, @Autowired RealtimeAggregationService generationService, @Autowired AggregatedDataRepository aggregatedDataRepository) {
		return new DataFragmentStoreService(standardiserService, generationService, aggregatedDataRepository);
	}

	/**
	 * The service that hot evaluates thymeleaf / spel expressions
	 * 
	 * @return
	 */
	public @Bean EvaluationService evaluationService() {
		return new EvaluationService();
	}

	@Bean
	AggregatedDataRepository aggregatedDatasRepository(@Autowired final ApiProperties config) {
		return new AggregatedDataRepository();
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
//	RealtimeAggregationService fullGenerationService( @Autowired EvaluationService evaluationService,
//			@Autowired ReferentielService referentielService, @Autowired StandardiserService standardiserService,
//			@Autowired AutowireCapableBeanFactory autowireBeanFactory, @Autowired AggregatedDataRepository aggregatedDataRepository,
//			@Autowired ApiProperties apiProperties, @Autowired Gs1PrefixService gs1prefixService,
//			@Autowired DataSourceConfigService dataSourceConfigService, @Autowired VerticalsConfigService configService, @Autowired BarcodeValidationService barcodeValidationService, @Autowired GoogleTaxonomyService taxonomyService) {
//		return new RealtimeAggregationService(repository, evaluationService, referentielService, standardiserService, autowireBeanFactory, aggregatedDataRepository, apiProperties, gs1prefixService, dataSourceConfigService, configService,  barcodeValidationService, taxonomyService);
//	}
//	


	@Bean Gs1PrefixService gs1prefixService (@Autowired ResourcePatternResolver resourceResolver) throws IOException{
		return new Gs1PrefixService("classpath:/gs1-prefix.csv", resourceResolver);
		
	}

	//////////////////////////////////////////////
	// Embeded crawler configuration
	//////////////////////////////////////////////


	// For the crawlController, inported from crawler
	public @Bean FetcherProperties fetcherProperties(@Autowired final ApiProperties apiProperties) {
		return apiProperties.getFetcherProperties();
	}

	@Bean
	public CsvDatasourceFetchingService csvDatasourceFetchingService(
			@Autowired final DataFragmentCompletionService completionService,
			@Autowired final IndexationService indexationService, @Autowired final ApiProperties apiProperties,
			@Autowired final WebDatasourceFetchingService webDatasourceFetchingService
			) {
		return new CsvDatasourceFetchingService(completionService, indexationService,
				apiProperties.getFetcherProperties(), webDatasourceFetchingService, apiProperties.logsFolder());
	}

	@Bean
	public WebDatasourceFetchingService webDatasourceFetchingService(
			@Autowired final IndexationService indexationService, @Autowired final ApiProperties apiProperties) {
		return new WebDatasourceFetchingService(indexationService, apiProperties.getFetcherProperties(),
				apiProperties.logsFolder());
	}


	/**
	 * A custom "direct" implementation to update directly the local crawler status,
	 * bypassing HTTP transport and serialisation
	 *
	 * @return
	 */
	@Bean
	public IndexationService indexationService(@Autowired final DataFragmentStoreService dataFragmentStoreService) {
		return new IndexationService() {

			@Override
			protected void indexInternal(final DataFragment data) {
				// Direct indexation on the store service
				dataFragmentStoreService.queueDataFragment(data);
			}
		};
	}

	@Bean
	public FetchersService crawlersInterface(@Autowired final ApiProperties apiProperties,
			@Autowired final CsvDatasourceFetchingService csvDatasourceFetchingService,
			@Autowired final WebDatasourceFetchingService webDatasourceFetchingService
		) {
		return new FetchersService(apiProperties.getFetcherProperties(), webDatasourceFetchingService,
				csvDatasourceFetchingService);
	}

	@Bean
	@Autowired
	public FetcherOrchestrationService fetcherOrchestrationService(ThreadPoolTaskScheduler taskScheduler, DataSourceConfigService dataSourceConfigService) {
		return new FetcherOrchestrationService(taskScheduler, dataSourceConfigService);
	}

	@Bean
	public DataFragmentCompletionService offerCompletionService() {
		return new DataFragmentCompletionService();
	}
	

	

	@Bean
	/**
	 * A custom "direct" implementation to update directly the local crawler status,
	 * bypassing HTTP transport and serialisation
	 *
	 * @return
	 */
	@Autowired
	public ApiSynchService apiSynchService( final ApiProperties apiProperties, FetchersService crawlersInterface,FetcherOrchestrationService fetcherOrchestrationService) {
		return new ApiSynchService(apiProperties.getFetcherProperties().getApiSynchConfig(), crawlersInterface, null,
				null) {
			@Override
			@Scheduled(initialDelay = 0L, fixedDelay = TimeConstants.CRAWLER_UPDATE_STATUS_TO_API_MS)
			public void updateStatus() {
				fetcherOrchestrationService.updateClientStatus(crawlersInterface.stats());
			}
		};
	}

	

}
