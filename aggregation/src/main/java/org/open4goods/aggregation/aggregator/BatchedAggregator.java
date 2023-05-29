package org.open4goods.aggregation.aggregator;

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
	private Set<Product> datas;

	

	public BatchedAggregator(final List<AbstractAggregationService> services) {
		super(services);
	}


	public void close(Set<Product> datas) {
		super.close();
		this.datas = null;
		
	}


	public void beforeStart(Set<Product> datas) {
		super.beforeStart();
		this.datas =datas;
		
		
	}

	
	
	/**
	 * Build the Product using the services registered on this aggregator
	 * @param datas
	 * @return
	 * @throws AggregationSkipException 
	 */
	public Product update(final Product data, Set<Product> datas) throws AggregationSkipException {

		logger.info("Updating Product with AggragatedData {} and using {} services",data,services.size());

		Product ret = null;
		// Call transformation building registered service
		for (final AbstractAggregationService service : services) {

			try {
				ret = service.onAggregatedData(data,datas);

			}
			catch (final Exception e) {
				logger.warn("AggregationService {} throw an exception while processing data {}",service.getClass().getName(), data,e);

			}
		}

		return ret;
	}
	
}
