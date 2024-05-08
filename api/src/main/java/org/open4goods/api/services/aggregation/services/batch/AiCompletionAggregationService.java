package org.open4goods.api.services.aggregation.services.batch;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.product.Product;
import org.open4goods.services.ai.AiService;
import org.slf4j.Logger;

/**
 * Service in charge of mapping product categories to verticals
 * @author goulven
 *
 */
public class AiCompletionAggregationService extends AbstractAggregationService {

	private AiService aiService;


	public AiCompletionAggregationService( final Logger logger, AiService aiService) {
		super(logger);
		this.aiService = aiService;
		
	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) {
		aiService.complete(data, vConf);

	}



}
