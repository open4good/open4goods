package org.open4goods.api.services.aggregation.services.batch;

import java.util.List;
import java.util.Map.Entry;

import org.open4goods.api.services.aggregation.AbstractBatchAggregationService;
import org.open4goods.api.services.ai.AiService;
import org.open4goods.config.yml.attributes.AiConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.data.AiDescription;
import org.open4goods.model.product.Product;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.VerticalsConfigService;

/**
 * Service in charge of mapping product categories to verticals
 * @author goulven
 *
 */
public class AiCompletionAggregationService extends AbstractBatchAggregationService {

	private AiService aiService;


	public AiCompletionAggregationService( final String logsFolder, AiService aiService, boolean toConsole) {
		super(logsFolder, toConsole);
		this.aiService = aiService;
		
	}

	@Override
	public void onProduct(Product data) {

		aiService.complete(data);

	}



}
