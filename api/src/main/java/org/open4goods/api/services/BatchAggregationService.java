
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.aggregation.aggregator.BatchedAggregator;
import org.open4goods.aggregation.services.aggregation.AttributeAggregationService;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.Gs1PrefixService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.store.repository.DataFragmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * This service is in charge of building AggregatedData in realtime mode
 * 
 * @author goulven
 *
 */
public class BatchAggregationService  {

	protected static final Logger logger = LoggerFactory.getLogger(BatchAggregationService.class);

	private  DataFragmentRepository repository;

	private EvaluationService evaluationService;

	private ReferentielService referentielService;

	private StandardiserService standardiserService;

	private AutowireCapableBeanFactory autowireBeanFactory;

	private AggregatedDataRepository aggregatedDataRepository;

	private ApiProperties apiProperties;

	private Gs1PrefixService gs1prefixService;

	private DataSourceConfigService dataSourceConfigService;

	private VerticalsConfigService verticalConfigService;

	private BatchedAggregator aggregator;
	
	private BarcodeValidationService barcodeValidationService;
	

	
	
	public BatchAggregationService(DataFragmentRepository repository, EvaluationService evaluationService,
			ReferentielService referentielService, StandardiserService standardiserService,
			AutowireCapableBeanFactory autowireBeanFactory, AggregatedDataRepository aggregatedDataRepository,
			ApiProperties apiProperties, Gs1PrefixService gs1prefixService,
			DataSourceConfigService dataSourceConfigService, VerticalsConfigService configService, 
			BarcodeValidationService barcodeValidationService) {
		super();
		this.repository = repository;
		this.evaluationService = evaluationService;
		this.referentielService = referentielService;
		this.standardiserService = standardiserService;
		this.autowireBeanFactory = autowireBeanFactory;
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.apiProperties = apiProperties;
		this.gs1prefixService = gs1prefixService;
		this.dataSourceConfigService = dataSourceConfigService;
		this.verticalConfigService = configService;
		
		this.barcodeValidationService = barcodeValidationService;
		
		
		this.aggregator = getAggregator(configService.getConfigById(VerticalsConfigService.MAIN_VERTICAL_NAME).get());
		
		// Initializing index
		aggregatedDataRepository.initIndex(VerticalsConfigService.MAIN_VERTICAL_NAME);
		

		
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


		services.add(new AttributeAggregationService(config.getAttributesConfig(), apiProperties.logsFolder()));

		

		
		final BatchedAggregator ret = new BatchedAggregator(services);

		autowireBeanFactory.autowireBean(ret);

		return ret;
	}



	






}
