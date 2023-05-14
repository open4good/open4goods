
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.aggregation.aggregator.RealTimeAggregator;
import org.open4goods.aggregation.services.aggregation.AttributeAggregationService;
import org.open4goods.aggregation.services.aggregation.BarCodeAggregationService;
import org.open4goods.aggregation.services.aggregation.CommentsAggregationService;
import org.open4goods.aggregation.services.aggregation.DescriptionsAggregationService;
import org.open4goods.aggregation.services.aggregation.IdAggregationService;
import org.open4goods.aggregation.services.aggregation.MediaAggregationService;
import org.open4goods.aggregation.services.aggregation.NamesAggregationService;
import org.open4goods.aggregation.services.aggregation.PriceAggregationService;
import org.open4goods.aggregation.services.aggregation.ProsAndConsAggregationService;
import org.open4goods.aggregation.services.aggregation.QuestionsAggregationService;
import org.open4goods.aggregation.services.aggregation.VerticalAggregationService;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.config.yml.ui.VerticalProperties;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.Gs1PrefixService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.store.repository.DataFragmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * This service is in charge of building AggregatedData in realtime mode
 * 
 * @author goulven
 *
 */
public class RealtimeAggregationService {

	protected static final Logger logger = LoggerFactory.getLogger(RealtimeAggregationService.class);

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

	private RealTimeAggregator aggregator;
	
	private BarcodeValidationService barcodeValidationService;



	
	
	@Autowired
	public RealtimeAggregationService(DataFragmentRepository repository, EvaluationService evaluationService,
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
		
		// Calling aggregator.BEFORE
		aggregator.beforeStart();
		
	}



	public AggregatedData process(DataFragment df, AggregatedData data) throws AggregationSkipException {
		return aggregator.build(df, data);
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
	RealTimeAggregator getAggregator(VerticalConfig config) {

//		final CapsuleGenerationConfig config = generationConfig;

		if (null == config) {
			logger.error("No capsule generation config");
			return null;
		}

		final List<AbstractAggregationService> services = new ArrayList<>();

		services.add(new BarCodeAggregationService(apiProperties.logsFolder(), gs1prefixService,barcodeValidationService));

		services.add(new AttributeAggregationService(config.getAttributesConfig(), apiProperties.logsFolder()));

		
		services.add(new NamesAggregationService(config.getNamings(), evaluationService, apiProperties.logsFolder()));

//		services.add(new CategoryService(apiProperties.logsFolder(), taxonomyService));

		
		services.add(new VerticalAggregationService( apiProperties.logsFolder(), verticalConfigService));
		
		services.add(new IdAggregationService( apiProperties.logsFolder()));

//		services.add(new UrlsAggregationService(evaluationService, apiProperties.logsFolder(),
//				config.getNamings().getProductUrlTemplates()));

		services.add(new PriceAggregationService(apiProperties.logsFolder(), dataSourceConfigService,config.getSegment()));

		services.add(new CommentsAggregationService(apiProperties.logsFolder(), config.getCommentsConfig()));
		services.add(new ProsAndConsAggregationService(apiProperties.logsFolder()));
		services.add(new QuestionsAggregationService(apiProperties.logsFolder()));

		services.add(new DescriptionsAggregationService(config.getDescriptionsAggregationConfig(),
				apiProperties.logsFolder()));


		services.add(new MediaAggregationService(config, apiProperties.logsFolder()));

		final RealTimeAggregator ret = new RealTimeAggregator(services);

		autowireBeanFactory.autowireBean(ret);

		return ret;
	}



	/**
	 * Add a Capsule Generation job to the working queue
	 * 
	 * @param capsuleProperties
	 */

	DataFragment cleanDataFragment(final DataFragment data, final VerticalProperties segmentProperties) {

//		if (null == data.gtin()) {
//			return null;
//		}
//
//		// Evicting items that do not meet the minimum price
//		if (data.hasPrice() && null != segmentProperties.getMinimumEvictionPrice()
//				&& data.getPrice().getPrice() < segmentProperties.getMinimumEvictionPrice()) {
//			return null;
//		}

//		// Updating the  compensation
//		if ( data.hasPrice() && data.affiliated()) {
//			try {
//				final Double reversment = segmentProperties.getDatasources().get(data.getDatasourceName()).getPercentCompensation();
//				if (null == reversment ) {
//					logger.warn("No compensation defined for {}",data.getDatasourceName());
//				} else {
//					data.setEcologicalCompensationAmount(reversment*data.getPrice().getPrice());
//				}
//			} catch (final Exception e) {
//				logger.error("Cannot compute compensation for {} : {}",data,e);
//			}
//		}

		// Sanitizing the branduid
		referentielService.sanitizeBrandUid(data, segmentProperties);

		return data;

	}







}
