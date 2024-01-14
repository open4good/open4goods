package org.open4goods.api.services.aggregation;

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
public abstract class AbstractRealTimeAggregationService  extends AbstractAggregationService{

	protected Logger dedicatedLogger;

	public AbstractRealTimeAggregationService (final String logsFolder, final boolean toConsole) {
		super(logsFolder, toConsole);
		dedicatedLogger = GenericFileLogger.initLogger("aggregation-realtime-"+getClass().getSimpleName().toLowerCase(), Level.WARN, logsFolder, toConsole);

	}

	/**
	 * Called on each participant DataFragment, in realtime mode
	 * @param data
	 */
	public abstract void onDataFragment (final DataFragment input, final Product output) throws AggregationSkipException;


	/**
	 * Delegation of handlings than can operate at the product level, call when availlable from onDataFragment.
	 * This allow the re-use of realTime aggregation for safety / cleaning / update scenaris
	 * @param output
	 * @throws AggregationSkipException
	 */
	public abstract void handle(Product output) throws AggregationSkipException;








}
