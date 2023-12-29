package org.open4goods.api.services.aggregation.aggregator;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.AbstractBatchAggregationService;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A batched sanitisation aggregator that do not load all elements in memory (used to process all items)
 * @author goulven
 *
 */
public class SanitisationBatchedAggregator extends AbstractAggregator {

	protected static final Logger logger = LoggerFactory.getLogger(SanitisationBatchedAggregator.class);



	public SanitisationBatchedAggregator(final List<AbstractAggregationService> services) {
		super(services);
	}


	public void close(Set<Product> datas) {
		super.close();


	}



	/**
	 * Build the Product using the services registered on this aggregator. 
	 * Will run in batched mode : for each service, do the before start, process datas, then do the done()
	 * @param datas
	 * @return
	 * @throws AggregationSkipException
	 */
	public Product update(final Product data){


		Product ret = null;
		// Call transformation building registered service
		for (final AbstractAggregationService s: services) {
			
			AbstractBatchAggregationService service = null;
			if (s instanceof AbstractBatchAggregationService) {
				service = (AbstractBatchAggregationService)s;
			} else {
				logger.warn("Aggrgegator {} is used in batch mode, but is a AbstractBatchAggregationService", s.getClass().getCanonicalName());
				continue;
			}
			
			service.onProduct(data);				

		}

		return ret;
	}

}
