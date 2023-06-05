package org.open4goods.aggregation;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;


/**
 * Defines the contract for an AggregationService, that participates in building AggregatedDatas from DataFragments
 * @author Goulven.Furet
 *
 */
public abstract class AbstractAggregationService  implements Closeable {

	protected Logger dedicatedLogger;

	public AbstractAggregationService (final String logsFolder) {
		dedicatedLogger = GenericFileLogger.initLogger("aggregation-"+getClass().getSimpleName().toLowerCase(), Level.WARN, logsFolder, false);
	}



	/**
	 * Called on each participant DataFragment, in realtime mode
	 * @param data
	 */
	public void onDataFragment (final DataFragment input, final Product output) throws AggregationSkipException {}


	/**
	 * Call in verticals batch update
	 * @param data
	 * @param datas
	 * @return
	 */
	public  void onProduct(Product data) {}



	/**
	 * Called after aggregation process, used to ending buffers / components (flush datas, close buffers, so on...)
	 */
	@Override
	public void close() throws IOException {

	}

	/**
	 * Called before data aggregation in realtime mode
	 * @param datas 
	 */
	public void init() {

	}


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








}
