
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.config.yml.AggregationPipelineProperties;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.api.services.aggregation.services.batch.scores.Attribute2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.CleanScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.DataCompletion2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.EcoScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.ParticipatingScoresAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.SustainalyticsAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.AttributeRealtimeAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.IdentityAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.MediaAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.NamesAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.PriceAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.TaxonomyRealTimeAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.brand.service.BrandScoreService;
import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import jakarta.annotation.PreDestroy;

/**
 * This service is in charge of building Product in realtime mode
 * @author goulven
 *
 */
public class AggregationFacadeService {

	protected static final Logger logger = LoggerFactory.getLogger(AggregationFacadeService.class);

	private static final Set<String> SUPPORTED_SERVICE_IDS = Set.of(
			"identity",
			"taxonomy",
			"attributes",
			"names",
			"price",
			"media",
			"clean-score",
			"attribute-score",
			"sustainalytics",
			"data-quality",
			"eco-score",
			"participating");

	private EvaluationService evaluationService;

	private StandardiserService standardiserService;

	private AutowireCapableBeanFactory autowireBeanFactory;

	private ProductRepository dataRepository;

	private ApiProperties apiProperties;

	private Gs1PrefixService gs1prefixService;

	private DataSourceConfigService dataSourceConfigService;

	private VerticalsConfigService verticalConfigService;


	private BarcodeValidationService barcodeValidationService;

	private BrandService brandService;
	private BrandScoreService brandScoreService;

	private GoogleTaxonomyService taxonomyService;

	private BlablaService blablaService;

	private StandardAggregator realtimeAggregator;

	private IcecatService icecatFeatureService;
	
	private DjlTextEmbeddingService embeddingService;
	private DjlEmbeddingProperties embeddingProperties;

	private SerialisationService serialisationService;

	private AggregationPipelineProperties aggregationPipelineProperties;

	public AggregationFacadeService(EvaluationService evaluationService,
			StandardiserService standardiserService,
			AutowireCapableBeanFactory autowireBeanFactory, ProductRepository aggregatedDataRepository,
			ApiProperties apiProperties, Gs1PrefixService gs1prefixService,
			DataSourceConfigService dataSourceConfigService, VerticalsConfigService configService,
			BarcodeValidationService barcodeValidationService,
			BrandService brandService,
			GoogleTaxonomyService taxonomyService,
			BlablaService blablaService,
			IcecatService icecatFeatureService,
			SerialisationService serialisationService,
			BrandScoreService brandScoreService,
			DjlTextEmbeddingService embeddingService,
			DjlEmbeddingProperties embeddingProperties,
			AggregationPipelineProperties aggregationPipelineProperties
			) {
		super();
		this.evaluationService = evaluationService;
		this.standardiserService = standardiserService;
		this.autowireBeanFactory = autowireBeanFactory;
		this.dataRepository = aggregatedDataRepository;
		this.apiProperties = apiProperties;
		this.gs1prefixService = gs1prefixService;
		this.dataSourceConfigService = dataSourceConfigService;
		verticalConfigService = configService;
		this.brandService=brandService;
		this.barcodeValidationService = barcodeValidationService;
		this.taxonomyService = taxonomyService;
		this.blablaService = blablaService;
		this.icecatFeatureService = icecatFeatureService;
		this.embeddingService = embeddingService;
		this.embeddingProperties = embeddingProperties;
		this.aggregationPipelineProperties = aggregationPipelineProperties;
		validatePipelineConfiguration();
		this.realtimeAggregator = getStandardAggregator("realtime");
		this.serialisationService = serialisationService;
		this.brandScoreService = brandScoreService;

	}

	/**
	 * update all verticals. Scheduled
     *
	 */
	public void scoreAll()  {
		for (VerticalConfig vertical : verticalConfigService.getConfigsWithoutDefault()) {
			score(vertical);
		}
	}


	/**
	 * Score verticals with the batch Aggragator
	 * @throws AggregationSkipException
	 */
	public void score(VerticalConfig vertical)  {

		logger.info("Score batching for {}", vertical.getId());

		ScoringBatchedAggregator batchAgg = getScoringAggregator();

		List<Product> productBag = dataRepository.exportVerticalWithValidDate (vertical, false).toList();
		// Batched (scoring) aggregation
		try {
			batchAgg.score(productBag, verticalConfigService.getConfigByIdOrDefault(vertical.getId()));
		} catch (AggregationSkipException e) {
			logger.error("Skipping product during batched scoring : ",e);
		}
		logger.info("Score batching : indexing {} products", productBag.size());
		dataRepository.addToFullindexationQueue(productBag);

	}

	/**
	 * Score verticals with the batch Aggragator
	 * @throws AggregationSkipException
	 */
	public void score(VerticalConfig vertical, Set<Product> products)  {

		logger.info("Score batching for {} products in {}", products.size(), vertical.getId());

		ScoringBatchedAggregator batchAgg = getScoringAggregator();

		// Batched (scoring) aggregation
		try {
			batchAgg.score(products, verticalConfigService.getConfigByIdOrDefault(vertical.getId()));
		} catch (AggregationSkipException e) {
			logger.error("Skipping product during batched scoring : ",e);
		}
	}

	/**
	 * Launch batch sanitization : on a vertical if specified, on all items if not
	 * Sanitizer aggregator
	 * @throws AggregationSkipException
	 */
	public void sanitizeAll()  {

		logger.info("started : Sanitisation batching for all items");
		StandardAggregator batchAgg = getStandardAggregator();

		dataRepository.exportAll().forEach(p -> {
			if (toBeDeleted(p)) {
				logger.warn("Deleting item : {}",p);
					dataRepository.delete(p);
				} else {
					try {
						batchAgg.onProduct(p);
						dataRepository.index(p);
					} catch (AggregationSkipException e) {
						logger.error("Skipping product during batched sanitisation : ",e);
					}
				}
            });
			logger.info("done: Sanitisation batching for all items");
	}


	/**
	 * Hacky method
	 * Test if an item must be deleted
	 * @param p
	 * @return
	 */
	private boolean toBeDeleted(Product p) {

		return false;
	}

	/**
	 * Launch batch sanitization on all verticals
	 */
	public void sanitizeAllVerticals()  {
		for (VerticalConfig vertical : verticalConfigService.getConfigsWithoutDefault()) {
			sanitizeVertical(vertical);
		}
	}
	/**
	 * Launch batch sanitization : with direct persistence
	 * Sanitizer aggregator
	 * @throws AggregationSkipException
	 */
	public void sanitizeVertical(VerticalConfig vertical)  {

		logger.info("started : Sanitisation batching for vertical : {}",vertical);
		StandardAggregator batchAgg = getStandardAggregator();

		// Note : a strong choice to rely on categories, to replay aggregation process from unmapped / unverticalized items
		Set<Product> products = dataRepository.getProductsMatchingCategoriesOrVerticalId(vertical).collect(Collectors.toSet());
		try {
			batchAgg.onProducts(products, vertical);
			products.forEach(dataRepository::index);
		} catch (AggregationSkipException e) {
			logger.error("Skipping product during batched sanitisation : ",e);
		}
		logger.info("done: Sanitisation batching for all items");
	}


	/**
	 * Operates sanitisation on a collection of provided products, no persistence
	 * @param vertical
	 * @param products
	 */
	public void aggregateProducts(VerticalConfig vertical, Set<Product> products)  {

		logger.info("started : Sanitisation batching for {} products in vertical : {}",products.size(),  vertical);
		StandardAggregator batchAgg = getStandardAggregator();

		// Note : a strong choice to rely on categories, to replay aggregation process from unmapped / unverticalized items
		try {
			batchAgg.onProducts(products, vertical);
		} catch (AggregationSkipException e) {
			logger.error("Skipping product during batched sanitisation : ",e);
		}
		logger.info("done: Sanitisation batching for all items");
}

	/**
	 * Operates simple vertical (de) classification
	 * @param vertical
	 * @param products
	 */
	public void classificationAggregator(VerticalConfig vertical, Set<Product> products)  {

		logger.info("started : Sanitisation batching for {} products in vertical : {}",products.size(),  vertical);
		StandardAggregator batchAgg = getCategorieClassificationAggregator();

		// Note : a strong choice to rely on categories, to replay aggregation process from unmapped / unverticalized items
		try {
			batchAgg.onProducts(products, vertical);
		} catch (AggregationSkipException e) {
			logger.error("Skipping product during batched sanitisation : ",e);
		}
		logger.info("done: classification");
}



	/**
	 * Launch the sanitisation of one product and save
	 * @param product
	 * @throws AggregationSkipException
	 */
	public void sanitizeAndSave(Product product) throws AggregationSkipException {
		aggregate(product);
	    dataRepository.forceIndex(product);
		logger.info("done : Sanitisation batching for {}", product);
	}

	/**
	 * Launch the sanitisation of one product
	 * @param product
	 * @throws AggregationSkipException
	 */
	public void aggregate(Product product) throws AggregationSkipException {
		logger.info("started : Sanitisation batching for {}",product);
		StandardAggregator batchAgg = getStandardAggregator();

	    batchAgg.onProduct(product);
		logger.info("done : Sanitisation batching for {}", product);
	}


	/**
	 * Process a DataFragment against a Product
	 * @param df
	 * @param data
	 * @return
	 * @throws AggregationSkipException
	 */
	public Product updateOne(DataFragment df, Product data) throws AggregationSkipException {
		return realtimeAggregator.onDatafragment(df, data);
	}



	@PreDestroy
	public void shutdown() {
		realtimeAggregator.close();


	}

	/**
	 * Return an instance of realtime aggregator
	 * @param config
	 * @return
	 */
	public StandardAggregator getStandardAggregator(String name) {

		Logger logger = GenericFileLogger.initLogger(name, apiProperties.aggLogLevel(), apiProperties.logsFolder()+"/aggregation");

		List<String> pipeline = resolveStandardPipeline(name);
		final List<AbstractAggregationService> services = buildPipeline(pipeline, logger);

		final StandardAggregator ret = new StandardAggregator(services, verticalConfigService);
		autowireBeanFactory.autowireBean(ret);

		return ret;
	}

	/**
	 * Return an instance of realtime aggregator
	 * @param config
	 * @return
	 */
	public StandardAggregator getCategorieClassificationAggregator() {

		Logger logger = GenericFileLogger.initLogger("categores_classif", apiProperties.aggLogLevel(), apiProperties.logsFolder()+"/aggregation");

		final List<AbstractAggregationService> services = buildPipeline(
				aggregationPipelineProperties.getPipelines().getClassification(),
				logger);

		final StandardAggregator ret = new StandardAggregator(services, verticalConfigService);
		autowireBeanFactory.autowireBean(ret);

		return ret;
	}



	/**
	 * The aggregator used to batch score verticals
	 *
	 * @param config
	 * @return
	 */
	public ScoringBatchedAggregator getScoringAggregator() {
		Logger logger = GenericFileLogger.initLogger("score", apiProperties.aggLogLevel(), apiProperties.logsFolder()+"/aggregation");

		final List<AbstractAggregationService> services = buildPipeline(
				aggregationPipelineProperties.getPipelines().getScoring(),
				logger);

		final ScoringBatchedAggregator ret = new ScoringBatchedAggregator(services);
		autowireBeanFactory.autowireBean(ret);

		return ret;
	}

	/**
	 * The aggregator used for sanitisation. The same than in realtime, only the productMEthod,
	 * allowing a nice mutualisation between rt and batch handlings if aggregationservice are well designed
	 * @param config
	 * @return
	 */
	public StandardAggregator getStandardAggregator() {
		return getStandardAggregator("sanitisation");
	}

	private void validatePipelineConfiguration()
	{
		AggregationPipelineProperties.Pipelines pipelines = aggregationPipelineProperties.getPipelines();
		validatePipeline("realtime", pipelines.getRealtime());
		validatePipeline("sanitisation", pipelines.getSanitisation());
		validatePipeline("classification", pipelines.getClassification());
		validatePipeline("scoring", pipelines.getScoring());
	}

	private void validatePipeline(String pipelineName, List<String> serviceIds)
	{
		if (serviceIds == null || serviceIds.isEmpty()) {
			throw new IllegalStateException("Aggregation pipeline '" + pipelineName + "' must not be empty.");
		}
		List<String> invalid = serviceIds.stream()
				.filter(id -> id == null || id.isBlank() || !SUPPORTED_SERVICE_IDS.contains(id))
				.toList();
		if (!invalid.isEmpty()) {
			throw new IllegalStateException("Aggregation pipeline '" + pipelineName + "' contains unsupported services: "
					+ invalid);
		}
	}

	private List<String> resolveStandardPipeline(String name)
	{
		if ("sanitisation".equalsIgnoreCase(name)) {
			return aggregationPipelineProperties.getPipelines().getSanitisation();
		}
		return aggregationPipelineProperties.getPipelines().getRealtime();
	}

	private List<AbstractAggregationService> buildPipeline(List<String> serviceIds, Logger logger)
	{
		if (serviceIds == null) {
			throw new IllegalArgumentException("Aggregation pipeline configuration is missing.");
		}
		List<AbstractAggregationService> services = new ArrayList<>();
		for (String serviceId : serviceIds) {
			services.add(buildService(serviceId, logger));
		}
		return services;
	}

	private AbstractAggregationService buildService(String serviceId, Logger logger)
	{
		return switch (serviceId) {
			case "identity" -> new IdentityAggregationService(logger, gs1prefixService, barcodeValidationService);
			case "taxonomy" -> new TaxonomyRealTimeAggregationService(logger, verticalConfigService, taxonomyService);
			case "attributes" -> new AttributeRealtimeAggregationService(verticalConfigService, brandService, logger,
					icecatFeatureService);
			case "names" -> new NamesAggregationService(logger, verticalConfigService, evaluationService, blablaService,
					embeddingService, embeddingProperties);
			case "price" -> new PriceAggregationService(logger);
			case "media" -> new MediaAggregationService(logger);
			case "clean-score" -> new CleanScoreAggregationService(logger);
			case "attribute-score" -> new Attribute2ScoreAggregationService(logger);
			case "sustainalytics" -> new SustainalyticsAggregationService(logger, brandService, verticalConfigService,
					brandScoreService);
			case "data-quality" -> new DataCompletion2ScoreAggregationService(logger);
			case "eco-score" -> new EcoScoreAggregationService(logger);
			case "participating" -> new ParticipatingScoresAggregationService(logger);
			default -> throw new IllegalArgumentException("Unsupported aggregation service id: " + serviceId);
		};
	}

}
