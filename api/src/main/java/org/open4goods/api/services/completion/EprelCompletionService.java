package org.open4goods.api.services.completion;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.controller.api.CsvEnrichmentController;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.eprelservice.model.EprelProduct;
import org.open4goods.services.eprelservice.service.EprelSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Complete products with Eprel Datas
 */
public class EprelCompletionService  extends AbstractCompletionService{


	private static final int REFRESH_IN_DAYS = 40;
	private ApiProperties apiProperties;
	private EprelSearchService eprelSearchService;

	Logger logger = LoggerFactory.getLogger(CsvEnrichmentController.class);


	public EprelCompletionService(VerticalsConfigService verticalConfigService, ProductRepository dataRepository, ApiProperties apiProperties, EprelSearchService eprelSearchService ) {

		// TODO(p3,conf) : Should set a specific log level here (not "agg(regation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());

		this.apiProperties = apiProperties;

		this.eprelSearchService = eprelSearchService;
	}


	@Override
	public boolean shouldProcess(VerticalConfig vertical, Product data) {
		Long lastProcessed = data.getDatasourceCodes().get(getDatasourceName());
		if (null != lastProcessed &&  REFRESH_IN_DAYS * 1000 * 3600 * 24 < System.currentTimeMillis() - lastProcessed ) {
			// TODO : return false
			return true;
		} else {
			return true;
		}

	}

	@Override
	public String getDatasourceName() {
		// No datasource name for resource completion
		return "eprel";
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
			logger.warn("No EPREL results when completing {}", data);
		} else if (results.size() > 1) {
			logger.warn("Too many EPREL results ({}) when completing {}", results.size(), data);
		} else {
			logger.info("Completing product {} with EPREL datas", data);

			EprelProduct eprelData = results.get(0);



			// TODO : Implement

			// Excluded : false
			// Set model name
			// Set attributes

			// Setting the computed flag
			data.getDatasourceCodes().put(getDatasourceName(), System.currentTimeMillis());

			logger.info("product {} completed with EPREL datas ", data);

		}



	}


}
