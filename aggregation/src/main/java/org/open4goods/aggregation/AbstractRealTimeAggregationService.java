package org.open4goods.aggregation;

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
	public void onDataFragment (final DataFragment input, final Product output) throws AggregationSkipException {}









}
