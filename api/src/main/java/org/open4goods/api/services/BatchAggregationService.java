
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.aggregation.aggregator.BatchedAggregator;
import org.open4goods.aggregation.services.aggregation.AttributeAggregationService;
import org.open4goods.aggregation.services.aggregation.batch.VerticalBatchedAggregationService;
import org.open4goods.aggregation.services.aggregation.batch.scores.Attribute2ScoreAggregationService;
import org.open4goods.aggregation.services.aggregation.batch.scores.Brand2ScoreAggregationService;
import org.open4goods.aggregation.services.aggregation.batch.scores.CleanScoreAggregationService;
import org.open4goods.aggregation.services.aggregation.batch.scores.DataCompletion2ScoreAggregationService;
import org.open4goods.aggregation.services.aggregation.batch.scores.EcoScoreAggregationService;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.BrandService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.Gs1PrefixService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.VerticalsConfigService;
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

	private BatchedAggregator aggregator;

	private BarcodeValidationService barcodeValidationService;


	private BrandService brandService;




	public BatchAggregationService(EvaluationService evaluationService,
			ReferentielService referentielService, StandardiserService standardiserService,
			AutowireCapableBeanFactory autowireBeanFactory, ProductRepository aggregatedDataRepository,
			ApiProperties apiProperties, Gs1PrefixService gs1prefixService,
			DataSourceConfigService dataSourceConfigService, VerticalsConfigService configService,
			BarcodeValidationService barcodeValidationService,
			BrandService brandService) {
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

		this.barcodeValidationService = barcodeValidationService;
		this.brandService = brandService;

		aggregator = getAggregator(configService.getConfigById(VerticalsConfigService.MAIN_VERTICAL_NAME).get());
	}





	@PreDestroy
	public void shutdown() {
		aggregator.close();


	}

	/**
	 * List of services in the aggregator
	 *
	 * @param config
	 * @return
	 */
	public BatchedAggregator getAggregator(VerticalConfig config) {

		//		final CapsuleGenerationConfig config = generationConfig;

		if (null == config) {
			logger.error("No capsule generation config");
			return null;
		}

		final List<AbstractAggregationService> services = new ArrayList<>();

		services.add(new VerticalBatchedAggregationService( apiProperties.logsFolder(), verticalConfigService, apiProperties.isDedicatedLoggerToConsole() ));
		
		services.add(new AttributeAggregationService(config.getAttributesConfig(), brandService, apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole() ));
		services.add(new CleanScoreAggregationService(config.getAttributesConfig(), apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));

		services.add(new Attribute2ScoreAggregationService(config.getAttributesConfig(), apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));
		services.add(new Brand2ScoreAggregationService(config.getAttributesConfig(), apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));
		services.add(new DataCompletion2ScoreAggregationService(config.getAttributesConfig(), apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));
		services.add(new EcoScoreAggregationService(config.getEcoscoreConfig(), apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));


		final BatchedAggregator ret = new BatchedAggregator(services);

		autowireBeanFactory.autowireBean(ret);

		return ret;
	}










}
