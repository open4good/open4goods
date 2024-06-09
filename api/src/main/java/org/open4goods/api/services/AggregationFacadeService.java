
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.api.services.aggregation.services.batch.scores.Attribute2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.Brand2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.CleanScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.DataCompletion2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.EcoScoreAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.AttributeRealtimeAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.DescriptionsAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.IdentityAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.MediaAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.NamesAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.PriceAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.TaxonomyRealTimeAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.BrandService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.Gs1PrefixService;
import org.open4goods.services.IcecatService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.textgen.BlablaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.annotation.Scheduled;

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
	
	private GoogleTaxonomyService taxonomyService;

	private BlablaService blablaService;

	private StandardAggregator realtimeAggregator;
	
	private IcecatService icecatFeatureService;
	
	public AggregationFacadeService(EvaluationService evaluationService,
			StandardiserService standardiserService,
			AutowireCapableBeanFactory autowireBeanFactory, ProductRepository aggregatedDataRepository,
			ApiProperties apiProperties, Gs1PrefixService gs1prefixService,
			DataSourceConfigService dataSourceConfigService, VerticalsConfigService configService,
			BarcodeValidationService barcodeValidationService,
			BrandService brandService,
			GoogleTaxonomyService taxonomyService,
			BlablaService blablaService,
			IcecatService icecatFeatureService
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
		this.realtimeAggregator = getStandardAggregator("realtime");
	
	}

	/**
	 * update all verticals. Scheduled
     *
	 */
	@Scheduled( initialDelay = 1000 * 3600*24, fixedDelay = 1000 * 3600*24)
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

		List<Product> productBag = dataRepository.exportVerticalWithValidDate (vertical.getId(), false).toList();
		// Batched (scoring) aggregation
		try {
			batchAgg.score(productBag, verticalConfigService.getConfigByIdOrDefault(vertical.getId()));
		} catch (AggregationSkipException e) {
			logger.error("Skipping product during batched scoring : ",e);
		}
		logger.info("Score batching : indexing {} products", productBag.size());
		dataRepository.index(productBag);

	}
	

	/**
	 * Launch batch sanitization : on a vertical if specified, on all items if not
	 * Sanitizer aggregator
	 * @throws AggregationSkipException 
	 */
	public void sanitizeAll()  {

			logger.info("started : Sanitisation batching for all items");
			StandardAggregator batchAgg = getFullSanitisationAggregator();

			// TODO : Performance, could parallelize
			dataRepository.exportAll().forEach(p -> {
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
	 * Launch batch sanitization on all verticals
	 */
	public void sanitizeAllVerticals()  {
		for (VerticalConfig vertical : verticalConfigService.getConfigsWithoutDefault()) {
			sanitizeVertical(vertical);
		}
	}
	/**
	 * Launch batch sanitization : on a vertical if specified, on all items if not
	 * Sanitizer aggregator
	 * @throws AggregationSkipException 
	 */
	public void sanitizeVertical(VerticalConfig vertical)  {

			logger.info("started : Sanitisation batching for vertical : {}",vertical);
			StandardAggregator batchAgg = getFullSanitisationAggregator();

			// TODO : Performance, could parallelize
			// Note : a strong choice to rely on categories, to replay aggregation process from unmapped / unverticalized items
			dataRepository.getProductsMatchingCategories(vertical) .forEach(p -> {
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
	 * Launch the sanitisation of one product
	 * @param product
	 * @throws AggregationSkipException 
	 */
	public void sanitizeOne(Product product) throws AggregationSkipException {
		logger.info("started : Sanitisation batching for {}",product);
		StandardAggregator batchAgg = getFullSanitisationAggregator();

	    batchAgg.onProduct(product);
	    dataRepository.forceIndex(product);
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
		
		Logger logger = GenericFileLogger.initLogger(name, apiProperties.aggLogLevel(), apiProperties.logsFolder()+"/aggregation", apiProperties.isDevMode());

		final List<AbstractAggregationService> services = new ArrayList<>();
		
		services.add(new IdentityAggregationService( logger, gs1prefixService,barcodeValidationService));
		services.add(new TaxonomyRealTimeAggregationService(  logger, verticalConfigService, taxonomyService));
		services.add(new AttributeRealtimeAggregationService(verticalConfigService, brandService, logger,icecatFeatureService));
		services.add(new NamesAggregationService( logger, verticalConfigService, evaluationService, blablaService));
		//		services.add(new CategoryService( taxonomyService));
		//		services.add(new UrlsAggregationService(evaluationService, 
		//				config.getNamings().getProductUrlTemplates()));
		services.add(new PriceAggregationService( logger, dataSourceConfigService));
		//		services.add(new CommentsAggregationService( config.getCommentsConfig()));
		//		services.add(new ProsAndConsAggregationService(apiProperties.logsFolder()));
		//		services.add(new QuestionsAggregationService(apiProperties.logsFolder()));
		services.add(new DescriptionsAggregationService(logger));
		services.add(new MediaAggregationService(logger));
		
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
	
		Logger logger = GenericFileLogger.initLogger("score", apiProperties.aggLogLevel(), apiProperties.logsFolder()+"/aggregation", apiProperties.isDevMode());
		
		services.add(new CleanScoreAggregationService(logger));
		services.add(new Attribute2ScoreAggregationService(logger));
		services.add(new Brand2ScoreAggregationService( logger, brandService));
		services.add(new DataCompletion2ScoreAggregationService(logger));
		services.add(new EcoScoreAggregationService( logger));

		final ScoringBatchedAggregator ret = new ScoringBatchedAggregator(services);
		autowireBeanFactory.autowireBean(ret);

		return ret;
	}

	/**
	 * The aggregator used for sanitisation. The same than in realtime, only the productMEthod, 
	 * allowing a nice mutualisation netween rt and batch handlings if aggregationservice are well designed
	 * @param config
	 * @return
	 */
	public StandardAggregator getFullSanitisationAggregator() {
		return getStandardAggregator("sanitisation");
	}

}
