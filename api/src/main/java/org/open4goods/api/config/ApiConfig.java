package org.open4goods.api.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.BatchService;
import org.open4goods.api.services.CompletionFacadeService;
import org.open4goods.api.services.ScrapperOrchestrationService;
import org.open4goods.api.services.VerticalsGenerationService;
import org.open4goods.api.services.completion.EprelCompletionService;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.api.services.completion.image.DjlImageEmbeddingService;
import org.open4goods.api.services.completion.image.ImageEmbeddingService;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.brand.repository.BrandScoresRepository;
import org.open4goods.brand.service.BrandScoreService;
import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.helper.DevModeService;
import org.open4goods.commons.model.constants.TimeConstants;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.commons.services.ProductNameSelectionService;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.services.ApiSynchService;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.FetchersService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.crawler.services.fetching.WebDatasourceFetchingService;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.price.Currency;
import org.open4goods.model.price.Price;
import org.open4goods.model.vertical.LegacyPromptConfig;
import org.open4goods.services.eprelservice.client.EprelApiClient;
import org.open4goods.services.eprelservice.client.RestEprelApiClient;
import org.open4goods.services.eprelservice.config.EprelServiceProperties;
import org.open4goods.services.eprelservice.service.EprelCatalogueService;
import org.open4goods.services.eprelservice.service.EprelSearchService;
import org.open4goods.services.eprelservice.service.JsonZipEprelCatalogueParser;
import org.open4goods.services.evaluation.config.EvaluationConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.feedservice.service.AbstractFeedService;
import org.open4goods.services.feedservice.service.AwinFeedService;
import org.open4goods.services.feedservice.service.EffiliationFeedService;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.imageprocessing.service.ImageMagickService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
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
		this.env = env;
		this.apiProperties = apiProperties;
	}




	@Bean EprelApiClient apiClient(@Autowired  RestTemplate restTemplate, @Autowired  EprelServiceProperties properties) {
		return new RestEprelApiClient(restTemplate, properties);
	}
	@Bean
	EprelSearchService eprelSearchService(@Autowired ElasticsearchOperations elasticsearchOperations, @Autowired  EprelServiceProperties properties) {
		return new EprelSearchService(elasticsearchOperations, properties);
	}
	@Bean
	EprelCatalogueService eprelCatalogueService(@Autowired EprelApiClient apiClient,
			@Autowired ElasticsearchOperations elasticsearchOperations, @Autowired EprelServiceProperties properties) {
		return new EprelCatalogueService(apiClient, new JsonZipEprelCatalogueParser(), elasticsearchOperations, properties);
	}

	@Bean
	SerialisationService serialisationService() {
		return new SerialisationService();
	}

	@Bean
	VerticalsGenerationService verticalsGenerationService(ProductRepository pRepo, SerialisationService serialisationService, GoogleTaxonomyService gTaxoService, VerticalsConfigService verticalsConfigService, ResourcePatternResolver resourceResolver,
			EvaluationService evaluationService, IcecatService icecatService, PromptService genAiService) throws SAXException {
		return new VerticalsGenerationService(apiProperties.getVerticalsGenerationConfig(), pRepo, serialisationService, gTaxoService, verticalsConfigService, resourceResolver, evaluationService, icecatService, genAiService);
	}

	@Bean
	ProductNameSelectionService productNameSelectionService() {
		return new ProductNameSelectionService();
	}


	@Bean
	FeatureLoader featureLoader(RemoteFileCachingService fileCachingService, BrandService brandService) {
                return new FeatureLoader(new XmlMapper(), apiProperties.getIcecatFeatureConfig(), fileCachingService, apiProperties.remoteCachingFolder(), brandService);
        }

	@Bean
	CategoryLoader categoryLoader(RemoteFileCachingService fileCachingService, VerticalsConfigService verticalConfigService, FeatureLoader featureLoader) {
                return new CategoryLoader(new XmlMapper(), apiProperties.getIcecatFeatureConfig(), fileCachingService, apiProperties.remoteCachingFolder(), verticalConfigService, featureLoader);
        }

    @Bean
    IcecatService icecatFeatureService(RemoteFileCachingService fileCachingService, FeatureLoader featureLoader, CategoryLoader categoryLoader) {
                // NOTE : xmlMapper not injected because corruct the springdoc used one. Could
                // use a @Primary derivation
                return new IcecatService(new XmlMapper(), apiProperties.getIcecatFeatureConfig(), fileCachingService, apiProperties.remoteCachingFolder(), featureLoader, categoryLoader);
        }


	@Bean
	@Qualifier("perplexityChatModel")
	OpenAiApi perplexityApi(ApiProperties apiConfig) {
		return new OpenAiApi(apiConfig.getGenAiConfig().getPerplexityBaseUrl(),
							 apiConfig.getGenAiConfig().getPerplexityApiKey(),
							 apiConfig.getGenAiConfig().getPerplexityCompletionsPath(),
							 "/v1/embeddings", RestClient.builder(), WebClient.builder(),
							 RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	@Bean
	@Qualifier("openAiCustomApi")
	OpenAiApi openAiCustomApi(ApiProperties apiConfig) {
		return new OpenAiApi(apiConfig.getGenAiConfig().getOpenaiApiKey());
	}

//	@Bean
//	PromptService genAiService (@Autowired @Qualifier("perplexityChatModel") OpenAiApi perplexityApi,
//								@Autowired @Qualifier("openAiCustomApi") OpenAiApi openAiCustomApi,
//								ApiProperties apiConfig, EvaluationService spelEvaluationService, SerialisationService serialisationService,
//								@Autowired MeterRegistry meterRegistry) {
//		return new PromptService(apiProperties.getGenAiConfig(), perplexityApi, openAiCustomApi, serialisationService, spelEvaluationService,  meterRegistry);
//	}



	@Bean
	IcecatCompletionService icecatCompletionService(ProductRepository productRepository, VerticalsConfigService verticalConfigService, DataSourceConfigService dataSourceConfigService, AggregationFacadeService aggregationFacade) throws TechnicalException {
		return new IcecatCompletionService(productRepository, verticalConfigService, apiProperties, dataSourceConfigService, aggregationFacade);
	}

	@Bean
	EprelCompletionService eprelCompletionService(ProductRepository productRepository, VerticalsConfigService verticalConfigService, DataSourceConfigService dataSourceConfigService, AggregationFacadeService aggregationFacade, EprelSearchService eprelSearchService) throws TechnicalException {
		return new EprelCompletionService(verticalConfigService, productRepository, apiProperties, eprelSearchService,aggregationFacade );
	}





//	@Bean
//
//	AmazonCompletionService amazonCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService, ApiProperties apiProperties, DataSourceConfigService dataSourceConfigService, AggregationFacadeService aggregationFacade) throws TechnicalException {
//		return new AmazonCompletionService(dataRepository, verticalConfigService, apiProperties, dataSourceConfigService, aggregationFacade);
//	}

	@Bean
	BatchService batchService(AggregationFacadeService aggregationFacadeService, CompletionFacadeService completionFacadeService, VerticalsConfigService verticalsConfigService, ProductRepository productRepository, CsvDatasourceFetchingService csvDatasourceFetchingService, FeedService feedService, SerialisationService serialisationService) {
		return new BatchService(aggregationFacadeService, completionFacadeService, verticalsConfigService, productRepository,  csvDatasourceFetchingService, feedService, serialisationService);
	}

	@Bean
	CompletionFacadeService completionFacadeService(ResourceCompletionService resourceCompletionService,  IcecatCompletionService icecatCompletionService, EprelCompletionService eprelCompletionService) {
		return new CompletionFacadeService(resourceCompletionService, icecatCompletionService, eprelCompletionService);
	}

	@Bean
	BlablaService blablaService(@Autowired EvaluationService evaluationService) {
		return new BlablaService(evaluationService);
	}

	@Bean
	DevModeService devModeService(ProductRepository repository, SerialisationService serialisationService, VerticalsConfigService verticalsConfigService) {
		return new DevModeService(apiProperties.getDevModeConfig(), repository, serialisationService, verticalsConfigService);
	}


    @Bean
    public AwinFeedService awinFeedService(FetcherProperties fetcherProperties,
                                           RemoteFileCachingService remoteFileCachingService,
                                           DataSourceConfigService dataSourceConfigService,
                                           SerialisationService serialisationService,
                                           ApiProperties props

    		) {
        // Retrieve Awin-specific feed configuration from the fetcher properties
        FeedConfiguration awinConfig = fetcherProperties.getFeedConfigs().get("awin");
        return new AwinFeedService(awinConfig, remoteFileCachingService, dataSourceConfigService, serialisationService, props.getAffiliationConfig().getAwinAdvertiserId(), props.getAffiliationConfig().getAwinAccessToken());
    }

    @Bean
    public EffiliationFeedService effiliationFeedService(FetcherProperties fetcherProperties,
                                                         RemoteFileCachingService remoteFileCachingService,
                                                         DataSourceConfigService dataSourceConfigService,
                                                         SerialisationService serialisationService,
                                                         ApiProperties props) {
        // Retrieve Effiliation-specific feed configuration from the fetcher properties
        FeedConfiguration effiliationConfig = fetcherProperties.getFeedConfigs().get("effiliation");
        return new EffiliationFeedService(effiliationConfig, remoteFileCachingService, dataSourceConfigService, serialisationService, props.getAffiliationConfig().getEffiliationApiKey());
    }

    @Bean
    public FeedService feedService(List<AbstractFeedService> feedServices,
                                   DataSourceConfigService dataSourceConfigService) {
        // The FeedService aggregates all concrete feed implementations.
        return new FeedService(feedServices, dataSourceConfigService);
    }

	@Bean
	VerticalsConfigService verticalConfigsService(ResourcePatternResolver resolver, SerialisationService serialisationService, GoogleTaxonomyService googleTaxonomyService) throws IOException {
		return new VerticalsConfigService(serialisationService, googleTaxonomyService, resolver);
	}


	@Bean
    SearchService searchService(@Autowired ProductRepository aggregatedDataRepository, @Autowired String logsFolder,
                    @Autowired DjlTextEmbeddingService textEmbeddingService) {
            return new SearchService(aggregatedDataRepository, logsFolder, textEmbeddingService);
    }

	@Bean
        BrandService brandService(@Autowired RemoteFileCachingService rfc, @Autowired SerialisationService serialisationService) throws Exception {
                return new BrandService(rfc, serialisationService);
        }



        @Bean
        BrandScoreService brandScoreService(@Autowired BrandScoresRepository brandScoreRepository) {
                return new BrandScoreService(brandScoreRepository);
        }



	@Bean
	LegacyPromptConfig aiConfig() {
		return new LegacyPromptConfig();
	}


	@Bean
	ImageEmbeddingService imageEmbeddingService(ApiProperties apiProperties) {
		return new DjlImageEmbeddingService(apiProperties);
	}
	@Bean
	ResourceCompletionService resourceCompletionService(ImageMagickService imageService, VerticalsConfigService verticalConfigService, ResourceService resourceService, ProductRepository dataRepository, ApiProperties apiProperties, ImageEmbeddingService imageEmbeddingService) {
		return new ResourceCompletionService(imageService, verticalConfigService, resourceService, dataRepository, apiProperties,  imageEmbeddingService);

	}

	@Bean
        GoogleTaxonomyService googleTaxonomyService( RemoteFileCachingService remoteFileCachingService,
                        ApiProperties apiProperties) {
                GoogleTaxonomyService gts = new GoogleTaxonomyService(remoteFileCachingService);

                try {
                        for (var entry : apiProperties.getGoogleTaxonomy().entrySet()) {
                                gts.loadGoogleTaxonUrl(entry.getValue(), entry.getKey());
                        }
                } catch (Exception e) {
                        logger.error("Error loading google taxonomy", e);
                }

                return gts;
	}

	@Bean
	AggregationFacadeService realtimeAggregationService(@Autowired EvaluationService evaluationService, StandardiserService standardiserService, AutowireCapableBeanFactory autowireBeanFactory, @Autowired ProductRepository aggregatedDataRepository, ApiProperties apiProperties,
			@Autowired Gs1PrefixService gs1prefixService, DataSourceConfigService dataSourceConfigService, VerticalsConfigService configService, BarcodeValidationService barcodeValidationService, BrandService brandservice, GoogleTaxonomyService gts, BlablaService blablaService,
			IcecatService icecatFeatureService, SerialisationService serialisationService, BrandScoreService brandScoreService, DjlTextEmbeddingService embeddingService) {
		return new AggregationFacadeService(evaluationService, standardiserService, autowireBeanFactory, aggregatedDataRepository, apiProperties, gs1prefixService, dataSourceConfigService, configService, barcodeValidationService, brandservice, gts, blablaService, icecatFeatureService,
				serialisationService, brandScoreService, embeddingService);
	}

	//////////////////////////////////////////////////////////
	// SwaggerConfig
	//////////////////////////////////////////////////////////

	@Bean
	GroupedOpenApi adminApi() {
		return GroupedOpenApi.builder()

				.group("api")
				// .pathsToMatch("/admin/**")
				.packagesToScan("org.open4goods.api").addOpenApiCustomizer(apiSecurizer())

				.build();
	}

	@Bean
	OpenApiCustomizer apiSecurizer() {
		return openApi -> openApi.addSecurityItem(new SecurityRequirement().addList("Authorization")).components(new Components().addSecuritySchemes(UrlConstants.APIKEY_PARAMETER, new SecurityScheme().in(SecurityScheme.In.HEADER).type(SecurityScheme.Type.APIKEY).name(UrlConstants.APIKEY_PARAMETER))

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
	@Bean
	TaskScheduler threadPoolTaskScheduler() {
		final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(5);
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
		manager.setCaches(Arrays.asList(fCache, dCache, hCache, mCache));
		return manager;
	}

	private CaffeineCache buildExpiryCache(final String name, final Ticker ticker, final int minutesToExpire) {
		return new CaffeineCache(name, Caffeine.newBuilder().recordStats().expireAfterWrite(minutesToExpire, TimeUnit.MINUTES).ticker(ticker).build());
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
	@Bean
	DataSourceConfigService datasourceConfigService(@Autowired final ApiProperties config) {
		return new DataSourceConfigService(config.getDatasourcesfolder());
	}

	@Bean
	ResourceService resourceService() {
		return new ResourceService(apiProperties.remoteCachingFolder());
	}

	@Bean
	String logsFolder(@Autowired final ApiProperties config) {
		return config.logsFolder();
	}

	@Bean
	ImageMagickService imageService() {
		return new ImageMagickService();
	}

        @Bean
        RemoteFileCachingService remoteFileCachingService(@Autowired final ApiProperties config,
                        RemoteFileCachingProperties remoteFileCachingProperties) {
                return new RemoteFileCachingService(config.remoteCachingFolder(), remoteFileCachingProperties);
        }

	@Bean
	DataFragmentStoreService dataFragmentStoreService(final ApiProperties config, @Autowired StandardiserService standardiserService, @Autowired AggregationFacadeService generationService, @Autowired ProductRepository aggregatedDataRepository) {
		return new DataFragmentStoreService(standardiserService, generationService, aggregatedDataRepository, config.getIndexationConfig());
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

	@Bean
	@org.springframework.context.annotation.Profile("!local")
	ProductRepository aggregatedDatasRepository(@Autowired final ApiProperties config) {
		return new ProductRepository(config.getIndexationConfig());
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
	BarcodeValidationService barcodeValidationService() {
		return new BarcodeValidationService();
	}


	@Bean
	Gs1PrefixService gs1prefixService(@Autowired ResourcePatternResolver resourceResolver) throws IOException {
		return new Gs1PrefixService("classpath:/gs1-prefix.csv", resourceResolver);

	}

	//////////////////////////////////////////////
	// Embeded crawler configuration
	//////////////////////////////////////////////

	// For the crawlController, inported from crawler
	@Bean
	FetcherProperties fetcherProperties(@Autowired final ApiProperties apiProperties) {
		return apiProperties.getFetcherProperties();
	}

	@Bean
	CsvDatasourceFetchingService csvDatasourceFetchingService(final DataFragmentCompletionService completionService, final IndexationService indexationService, @Autowired final ApiProperties apiProperties, final WebDatasourceFetchingService webDatasourceFetchingService,
			final IndexationRepository indexationRepository, IndexationRepository csvIndexationRepo, RemoteFileCachingService remoteFileCachingService

	) {

		return new CsvDatasourceFetchingService(csvIndexationRepo, completionService, indexationService, apiProperties.getFetcherProperties(), webDatasourceFetchingService, indexationRepository, webDatasourceFetchingService, remoteFileCachingService, apiProperties.logsFolder());
	}

	@Bean
	WebDatasourceFetchingService webDatasourceFetchingService(@Autowired final IndexationRepository indexationRepository, final IndexationService indexationService, @Autowired final ApiProperties apiProperties) {

		return new WebDatasourceFetchingService(indexationService, apiProperties.getFetcherProperties(), indexationRepository, apiProperties.logsFolder());
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
	FetchersService crawlersInterface(@Autowired final ApiProperties apiProperties, final CsvDatasourceFetchingService csvDatasourceFetchingService, final WebDatasourceFetchingService webDatasourceFetchingService) {
		return new FetchersService(apiProperties.getFetcherProperties(), webDatasourceFetchingService, csvDatasourceFetchingService);
	}

	@Bean

	ScrapperOrchestrationService fetcherOrchestrationService(TaskScheduler taskScheduler, DataSourceConfigService dataSourceConfigService, ApiProperties apiProperties) {
		return new ScrapperOrchestrationService(taskScheduler, dataSourceConfigService, apiProperties);
	}

	@Bean
	DataFragmentCompletionService offerCompletionService(@Autowired BrandScoreService brandScoreService) {
		return new DataFragmentCompletionService(brandScoreService);
	}

	@Bean
	/**
	 * A custom "direct" implementation to update directly the local crawler status,
	 * bypassing HTTP transport and serialisation
	 *
	 * @return
	 */

	ApiSynchService apiSynchService(final ApiProperties apiProperties, FetchersService crawlersInterface, ScrapperOrchestrationService fetcherOrchestrationService) {
		return new ApiSynchService(apiProperties.getFetcherProperties().getApiSynchConfig(), crawlersInterface, null, null) {
			@Override
			@Scheduled(initialDelay = 0L, fixedDelay = TimeConstants.CRAWLER_UPDATE_STATUS_TO_API_MS)
			public void updateStatus() {
				fetcherOrchestrationService.updateClientStatus(crawlersInterface.stats());
			}
		};
	}

	@Bean
	TimedAspect timedAspect(MeterRegistry registry) {
		return new TimedAspect(registry);
	}

}
