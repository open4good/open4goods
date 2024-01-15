package org.open4goods.api.services.aggregation;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;


/**
 * Defines the contract for an AggregationFacadeService, that participates in building AggregatedDatas from DataFragments
 * @author Goulven.Furet
 *
 */
public abstract class AbstractAggregationService  implements Closeable {


	protected Logger dedicatedLogger;

	public AbstractAggregationService(Logger logger) {
		this.dedicatedLogger = logger;
	}


	/**
	 * Called on each participant DataFragment, in realtime mode
	 * @param data
	 */
	public void onDataFragment (final DataFragment input, final Product output, VerticalConfig vConf) throws AggregationSkipException {
		
	}

	
	/**
	 * Delegation of handlings than can operate at the product level, call when availlable from onDataFragment.
	 * This allow the re-use of realTime aggregation for safety / cleaning / update scenaris
	 * @param output
	 * @throws AggregationSkipException
	 */
	public abstract void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException;
	


	/**
	 * Called before data aggregation in batch mode
	 * @param datas 
	 */
	public void init(Collection<Product> datas) {

	}

	/**
	 * Called after data aggregation in batchmode
	 * @param datas 
	 */
	public void done(Collection<Product> datas) {
		
	}
	
	/**
	 * Called after aggregation process, used to ending buffers / components (flush datas, close buffers, so on...)
	 */
	@Override
	public void close() throws IOException {

	}
	



}
