package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;
import java.util.Map;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.model.product.Product;
import org.slf4j.Logger;

public class CleanScoreAggregationService extends AbstractScoreAggregationService {

	public CleanScoreAggregationService(final Logger logger) {
		super(logger);
	}



	@Override
	public void init(Collection<Product> datas) {
		super.init(datas);
		
		for (Product d : datas) {
			d.getScores().clear();
		}
	}



	@Override
	public Map<String, Object> onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {
		return null;
		
	}
	
}
