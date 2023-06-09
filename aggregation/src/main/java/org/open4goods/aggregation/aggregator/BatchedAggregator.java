package org.open4goods.aggregation.aggregator;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The real time AggregationService is stateless and thread safe.
 * @author goulven
 *
 */
public class BatchedAggregator extends AbstractAggregator {

	protected static final Logger logger = LoggerFactory.getLogger(BatchedAggregator.class);



	public BatchedAggregator(final List<AbstractAggregationService> services) {
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
	public Product update(final Collection<Product> datas){


		Product ret = null;
		// Call transformation building registered service
		for (final AbstractAggregationService service : services) {
			logger.warn("Initializing {} products using {} service",datas.size() ,service.getClass().getSimpleName());

			
			// Init
			service.init(datas);
			
			logger.warn("Processing {} products using {} service",datas.size() ,service.getClass().getSimpleName());

			// Processing Products
			for (Product p : datas) {
				service.onProduct(p);				
			}

			logger.warn("Post computing {} products using {} service",datas.size() ,service.getClass().getSimpleName());

			// Done 
			service.done(datas);
		}

		return ret;
	}

}
