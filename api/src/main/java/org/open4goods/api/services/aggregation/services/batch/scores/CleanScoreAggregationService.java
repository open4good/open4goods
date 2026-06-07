package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;

import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * First step of the scoring pipeline: clears all existing scores on every
 * product before the other scoring services populate them.
 *
 * <p>Running this as the first service guarantees a clean slate and prevents
 * stale scores from previous batch runs from accumulating in the product.
 */
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
