
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.aggregation.aggregator.RealTimeAggregator;
import org.open4goods.aggregation.services.aggregation.AttributeRealtimeAggregationService;
import org.open4goods.aggregation.services.aggregation.realtime.BarCodeAggregationService;
import org.open4goods.aggregation.services.aggregation.realtime.DescriptionsAggregationService;
import org.open4goods.aggregation.services.aggregation.realtime.IdAggregationService;
import org.open4goods.aggregation.services.aggregation.realtime.MediaAggregationService;
import org.open4goods.aggregation.services.aggregation.realtime.NamesAggregationService;
import org.open4goods.aggregation.services.aggregation.realtime.PriceAggregationService;
import org.open4goods.aggregation.services.aggregation.realtime.VerticalRealTimeAggregationService;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.config.yml.ui.VerticalProperties;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
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
public class RealtimeAggregationService {

	protected static final Logger logger = LoggerFactory.getLogger(RealtimeAggregationService.class);

	private EvaluationService evaluationService;

	private ReferentielService referentielService;

	private StandardiserService standardiserService;

	private AutowireCapableBeanFactory autowireBeanFactory;

	private ProductRepository aggregatedDataRepository;

	private ApiProperties apiProperties;

	private Gs1PrefixService gs1prefixService;

	private DataSourceConfigService dataSourceConfigService;

	private VerticalsConfigService verticalConfigService;

	private RealTimeAggregator aggregator;

	private BarcodeValidationService barcodeValidationService;

	private BrandService brandService;

	public RealtimeAggregationService(EvaluationService evaluationService,
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
		this.brandService=brandService;
		this.barcodeValidationService = barcodeValidationService;


		aggregator = getAggregator(configService.getConfigById(VerticalsConfigService.MAIN_VERTICAL_NAME).get());


		// Calling aggregator.BEFORE
		aggregator.beforeStart();

	}



	public Product process(DataFragment df, Product data) throws AggregationSkipException {
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

		services.add(new BarCodeAggregationService(apiProperties.logsFolder(), gs1prefixService,barcodeValidationService, apiProperties.isDedicatedLoggerToConsole()));

		services.add(new AttributeRealtimeAggregationService(config.getAttributesConfig(), brandService, apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));


		services.add(new NamesAggregationService(apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));

		//		services.add(new CategoryService(apiProperties.logsFolder(), taxonomyService));


		services.add(new VerticalRealTimeAggregationService( apiProperties.logsFolder(), verticalConfigService, apiProperties.isDedicatedLoggerToConsole()));

		services.add(new IdAggregationService( apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));

		//		services.add(new UrlsAggregationService(evaluationService, apiProperties.logsFolder(),
		//				config.getNamings().getProductUrlTemplates()));

		services.add(new PriceAggregationService(apiProperties.logsFolder(), dataSourceConfigService,config.getSegment(), apiProperties.isDedicatedLoggerToConsole()));

		//		services.add(new CommentsAggregationService(apiProperties.logsFolder(), config.getCommentsConfig()));
		//		services.add(new ProsAndConsAggregationService(apiProperties.logsFolder()));
		//		services.add(new QuestionsAggregationService(apiProperties.logsFolder()));

		services.add(new DescriptionsAggregationService(config.getDescriptionsAggregationConfig(),
				apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));


		services.add(new MediaAggregationService(config, apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));

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
