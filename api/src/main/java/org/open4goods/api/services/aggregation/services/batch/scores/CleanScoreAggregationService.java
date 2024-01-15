package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;

import org.open4goods.config.yml.ui.AttributesConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;
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
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {
		
	}
	
}
