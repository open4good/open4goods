package org.open4goods.api.services.completion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.controller.api.CsvEnrichmentController;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.eprelservice.service.EprelSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Complete products with Eprel Datas
 */
public class EprelCompletionService  extends AbstractCompletionService{


	public static final String EPREL_DS_NAME = "eprel";
	// TODO : From conf, not every one days.
	private static final int REFRESH_IN_DAYS = 1;
	private EprelSearchService eprelSearchService;

	Logger logger = LoggerFactory.getLogger(CsvEnrichmentController.class);
	private StandardAggregator aggregator;


	public EprelCompletionService(VerticalsConfigService verticalConfigService, ProductRepository dataRepository, ApiProperties apiProperties, EprelSearchService eprelSearchService, AggregationFacadeService aggregationFacade ) {

		// TODO(p3,conf) : Should set a specific log level here (not "agg(regation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());


		this.eprelSearchService = eprelSearchService;

		this.aggregator = aggregationFacade.getStandardAggregator("eprel-aggregation");;
		this.aggregator.beforeStart();
	}


	@Override
	public boolean shouldProcess(VerticalConfig vertical, Product data) {
		Long lastProcessed = data.getDatasourceCodes().get(getDatasourceName());
		if (null != lastProcessed &&  REFRESH_IN_DAYS * 1000 * 3600 * 24 < System.currentTimeMillis() - lastProcessed ) {
			// TODO : Remove systematic rehandling
			return true;
		} else {
			return true;
		}



	}

	@Override
	public String getDatasourceName() {
		// No datasource name for resource completion
		return EPREL_DS_NAME;
	}



	/**
	 * Process resources for one product
	 *
	 * @param data
	 * @param vertical
	 */

	@Override
	public void processProduct(VerticalConfig vertical, Product data ) {


		List<String> models = new ArrayList<>();
		models.add(data.model());
		models.addAll(data.getAkaModels());

		List<EprelProduct> results = eprelSearchService.search(data.gtin(), models);

		if (null == results || results.size() == 0) {
			logger.warn("No EPREL results when completing {}-{}", data.brand(), data.model());
			return;
		} else if (results.size() > 1) {
			logger.warn("Too many EPREL results ({}) when completing {}", results.size(), data);
			return;
		} else {
			logger.info("Completing product {} with EPREL datas", data);


			EprelProduct eprelData = results.get(0);

			data.setEprelDatas(eprelData);
			data.getExternalIds().setEprel(eprelData.getEprelRegistrationNumber());

			// Set attributes

			Set<DataFragment> fragments = getEprelAttributesFragments(data, vertical);
			// Apply aggregation
			for (DataFragment df : fragments) {
				try {
					aggregator.onDatafragment(df, data);
				} catch (AggregationSkipException e) {
					logger.error("Error occurs during icecat aggregation",e);
				}
			}



			// TODO : Filter per vertical

			//


			// TODO : Implement

			// Excluded : false
			// Set model name
			// Set attributes

			// Setting the computed flag
			data.getDatasourceCodes().put(getDatasourceName(), System.currentTimeMillis());

			logger.info("product {} completed with EPREL datas ", data);

		}



	}


	private Set<DataFragment> getEprelAttributesFragments(Product data, VerticalConfig vertical) {
			Set<DataFragment> fragment = new HashSet<>();

			if (null != data.getEprelDatas()) {

				Map<String, Object> chars = data.getEprelDatas().getCategorySpecificAttributes();

				DataFragment df = initDataFragment(data);
				for (Entry<String, Object> caracteristic : chars.entrySet()) {

					// TODO : Handle the toString on "object" type, we should have nested structures
					// TODO : Correct language injection
					df.addAttribute(caracteristic.getKey(), caracteristic.getValue().toString(), "fr", null);
				}
				fragment.add(df);

			}

		return fragment;
	}

	private DataFragment initDataFragment( Product data) {
		DataFragment df = new DataFragment();
		// TODO(p3,conf) : Constants
		df.setDatasourceName(EPREL_DS_NAME);
		df.setDatasourceConfigName(EPREL_DS_NAME);
		df.setLastIndexationDate(System.currentTimeMillis());
		df.setCreationDate(System.currentTimeMillis());
		df.addReferentielAttribute(ReferentielKey.GTIN, String.valueOf(data.getId()));
		return df;
	}




}
