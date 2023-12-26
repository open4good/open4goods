package org.open4goods.api.services.aggregation;

import java.util.Collection;

import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;


/**
 * Defines the contract for an AggregationService, that participates in building AggregatedDatas from DataFragments
 * @author Goulven.Furet
 *
 */
public abstract class AbstractBatchAggregationService  extends AbstractAggregationService {

	protected Logger dedicatedLogger;

	public AbstractBatchAggregationService (final String logsFolder, final boolean toConsole) {
		super(logsFolder,toConsole);
		dedicatedLogger = GenericFileLogger.initLogger("aggregation-batched-"+getClass().getSimpleName().toLowerCase(), Level.WARN, logsFolder, toConsole);

	}

	/**
	 * Called before data aggregation in batch mode
	 * @param datas 
	 */
	public void init(Collection<Product> datas) {

	}

	
	/**
	 * Call in verticals batch update
	 * @param data
	 * @param datas
	 * @return
	 */
	public  void onProduct(Product data) {}
	
	/**
	 * Called after data aggregation in batchmode
	 * @param datas 
	 */
	public void done(Collection<Product> datas) {
		
	}

}
