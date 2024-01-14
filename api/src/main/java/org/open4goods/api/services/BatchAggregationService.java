
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.AbstractBatchAggregationService;
import org.open4goods.api.services.aggregation.aggregator.RealTimeAggregator;
import org.open4goods.api.services.aggregation.aggregator.SanitisationBatchedAggregator;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.api.services.aggregation.services.batch.UnmappedAttributeCleaningBatchAggregationService;
import org.open4goods.api.services.aggregation.services.batch.VerticalBatchedAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.Attribute2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.Brand2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.CleanScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.DataCompletion2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.EcoScoreAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.NamesAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.BrandService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.Gs1PrefixService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.textgen.BlablaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import jakarta.annotation.PreDestroy;

/**
 * This service is in charge of building Product in realtime mode
 *
 * @author goulven
 *
 */
public class BatchAggregationService  {

	protected static final Logger logger = LoggerFactory.getLogger(BatchAggregationService.class);


	private EvaluationService evaluationService;

	private ReferentielService referentielService;

	private StandardiserService standardiserService;

	private AutowireCapableBeanFactory autowireBeanFactory;

	private ProductRepository aggregatedDataRepository;

	private ApiProperties apiProperties;

	private Gs1PrefixService gs1prefixService;

	private DataSourceConfigService dataSourceConfigService;

	private VerticalsConfigService verticalConfigService;

	private ScoringBatchedAggregator aggregator;

	private BarcodeValidationService barcodeValidationService;


	private BrandService brandService;

	private BlablaService blablaService;


	public BatchAggregationService(EvaluationService evaluationService,
			ReferentielService referentielService, StandardiserService standardiserService,
			AutowireCapableBeanFactory autowireBeanFactory, ProductRepository aggregatedDataRepository,
			ApiProperties apiProperties, Gs1PrefixService gs1prefixService,
			DataSourceConfigService dataSourceConfigService, VerticalsConfigService configService,
			BarcodeValidationService barcodeValidationService,
			BrandService brandService,
			BlablaService blablaService
			) {
		super();
		this.evaluationService = evaluationService;
		this.referentielService = referentielService;
		this.standardiserService = standardiserService;
		this.autowireBeanFactory = autowireBeanFactory;
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.apiProperties = apiProperties;
		this.gs1prefixService = gs1prefixService;
		this.dataSourceConfigService = dataSourceConfigService;
		verticalConfigService = configService;
		
		this.blablaService = blablaService;
		this.barcodeValidationService = barcodeValidationService;
		this.brandService = brandService;

		aggregator = getScoringAggregator(configService.getConfigById(VerticalsConfigService.MAIN_VERTICAL_NAME));
	}





	@PreDestroy
	public void shutdown() {
		aggregator.close();


	}

	/**
	 * The aggregator used to batch score verticals
	 *
	 * @param config
	 * @return
	 */
	public ScoringBatchedAggregator getScoringAggregator(VerticalConfig config) {

		//		final CapsuleGenerationConfig config = generationConfig;

		if (null == config) {
			logger.error("No capsule generation config");
			return null;
		}

		final List<AbstractAggregationService> services = new ArrayList<>();

		services.add(new VerticalBatchedAggregationService( apiProperties.logsFolder(), verticalConfigService, apiProperties.isDedicatedLoggerToConsole() ));
		
		
		services.add(new CleanScoreAggregationService(config.getAttributesConfig(), apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));

		services.add(new Attribute2ScoreAggregationService(config.getAttributesConfig(), apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));
		services.add(new Brand2ScoreAggregationService(config.getAttributesConfig(), apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));
		services.add(new DataCompletion2ScoreAggregationService(config.getAttributesConfig(), apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));
		services.add(new EcoScoreAggregationService(config.getEcoscoreConfig(), apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));


		final ScoringBatchedAggregator ret = new ScoringBatchedAggregator(services);

		autowireBeanFactory.autowireBean(ret);

		return ret;
	}



	/**
	 * The aggregator used for sanitisation 
	 * @param config
	 * @return
	 */
	public SanitisationBatchedAggregator getFullSanitisationAggregator() {

		final List<AbstractAggregationService> services = new ArrayList<>();

		services.add(new VerticalBatchedAggregationService( apiProperties.logsFolder(), verticalConfigService, apiProperties.isDedicatedLoggerToConsole() ));
		
		services.add(new UnmappedAttributeCleaningBatchAggregationService(apiProperties.logsFolder(), verticalConfigService, apiProperties.isDedicatedLoggerToConsole()));

		services.add(new AbstractBatchAggregationService(apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()) {

			private NamesAggregationService nameAggregationService = new NamesAggregationService(apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole(), verticalConfigService, evaluationService, blablaService);
			@Override
			public void onProduct(Product data) {
				try {
					nameAggregationService.handle(data);
				} catch (AggregationSkipException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		final SanitisationBatchedAggregator ret = new SanitisationBatchedAggregator(services);

		autowireBeanFactory.autowireBean(ret);

		return ret;
	}


	/**
	 * The aggregator used for sanitisation of all items (sanitisation do not need the all products buffering, so here is a trick
	 * to avoid memory load, by using a customized realtimeaggregator
	 * @param configalT
	 * @return
	 */
	public RealTimeAggregator getVerticalisedSanitisationAggregator() {

		final List<AbstractAggregationService> services = new ArrayList<>();

		services.add(new VerticalBatchedAggregationService( apiProperties.logsFolder(), verticalConfigService, apiProperties.isDedicatedLoggerToConsole() ));
		
		services.add(new UnmappedAttributeCleaningBatchAggregationService(apiProperties.logsFolder(), verticalConfigService, apiProperties.isDedicatedLoggerToConsole()));


		final RealTimeAggregator ret = new RealTimeAggregator(services);

		autowireBeanFactory.autowireBean(ret);

		return ret;
	}




}
