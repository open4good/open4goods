
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.open4goods.api.config.yml.ApiProperties;
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
 * TODO : Maintain a state machine to disable multiple launching
 * @author goulven
 *
 */
public class AggregationFacadeService {

	protected static final Logger logger = LoggerFactory.getLogger(AggregationFacadeService.class);

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
			DjlEmbeddingProperties embeddingProperties
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

			// TODO : Performance, could parallelize
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

			// TODO : Performance, could parallelize
			// Note : a strong choice to rely on categories, to replay aggregation process from unmapped / unverticalized items
			dataRepository.getProductsMatchingCategoriesOrVerticalId(vertical) .forEach(p -> {
                try {
					batchAgg.onProduct(p);
					dataRepository.index(p);
				} catch (AggregationSkipException e) {
					logger.error("Skipping product during batched sanitisation : ",e);
				}
            });
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

		// TODO : Performance, could parallelize
		// Note : a strong choice to rely on categories, to replay aggregation process from unmapped / unverticalized items
		products.forEach(p -> {
            try {
				batchAgg.onProduct(p);
			} catch (AggregationSkipException e) {
				logger.error("Skipping product during batched sanitisation : ",e);
			}
        });
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

		// TODO : Performance, could parallelize
		// Note : a strong choice to rely on categories, to replay aggregation process from unmapped / unverticalized items
		products.forEach(p -> {
            try {
				batchAgg.onProduct(p);
			} catch (AggregationSkipException e) {
				logger.error("Skipping product during batched sanitisation : ",e);
			}
        });
		logger.info("done: classification");
}



	/**
	 * Launch the sanitisation of one product and save
	 * @param product
	 * @throws AggregationSkipException
	 */
	// TODO(p3,design) : not a good design
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

		final List<AbstractAggregationService> services = new ArrayList<>();

		services.add(new IdentityAggregationService( logger, gs1prefixService,barcodeValidationService));
		services.add(new TaxonomyRealTimeAggregationService(  logger, verticalConfigService, taxonomyService));
		services.add(new AttributeRealtimeAggregationService(verticalConfigService, brandService, logger,icecatFeatureService));
		services.add(new NamesAggregationService( logger, verticalConfigService, evaluationService, blablaService, embeddingService,
				embeddingProperties));
		//		services.add(new CategoryService( taxonomyService));
		//		services.add(new UrlsAggregationService(evaluationService,
		//				config.getNamings().getProductUrlTemplates()));
		services.add(new PriceAggregationService( logger));
		//		services.add(new CommentsAggregationService( config.getCommentsConfig()));
		//		services.add(new ProsAndConsAggregationService(apiProperties.logsFolder()));
		//		services.add(new QuestionsAggregationService(apiProperties.logsFolder()));
		services.add(new MediaAggregationService(logger));

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

		final List<AbstractAggregationService> services = new ArrayList<>();

		services.add(new IdentityAggregationService( logger, gs1prefixService,barcodeValidationService));
		services.add(new TaxonomyRealTimeAggregationService(  logger, verticalConfigService, taxonomyService));

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
		final List<AbstractAggregationService> services = new ArrayList<>();

		Logger logger = GenericFileLogger.initLogger("score", apiProperties.aggLogLevel(), apiProperties.logsFolder()+"/aggregation");

		services.add(new CleanScoreAggregationService(logger));
                services.add(new Attribute2ScoreAggregationService(logger));
                services.add(new SustainalyticsAggregationService( logger, brandService, verticalConfigService,brandScoreService));
                services.add(new DataCompletion2ScoreAggregationService(logger));
                services.add(new EcoScoreAggregationService( logger));
                services.add(new ParticipatingScoresAggregationService(logger));

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

}
