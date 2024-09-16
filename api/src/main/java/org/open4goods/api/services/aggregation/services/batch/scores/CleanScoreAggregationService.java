package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.model.product.LegacyProduct;
import org.slf4j.Logger;

public class CleanScoreAggregationService extends AbstractScoreAggregationService {

	public CleanScoreAggregationService(final Logger logger) {
		super(logger);
	}



	@Override
	public void init(Collection<LegacyProduct> datas) {
		super.init(datas);
		
		for (LegacyProduct d : datas) {
			d.getScores().clear();
		}
	}



	@Override
	public void onProduct(LegacyProduct data, VerticalConfig vConf) throws AggregationSkipException {
		
	}
	
}
