package org.open4goods.aggregation;

import java.io.Closeable;
import java.io.IOException;

import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
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
	 * Called on each participant DataFragment
	 * @param data
	 */
	public void onDataFragment (final DataFragment input, final AggregatedData output) {}

    /**
	 * Called after aggregation process, used to ending buffers / components (flush datas, close buffers, so on...)
	 */
	public void close() throws IOException {

	}

	/**
	 * Called before data aggregation
	 */
	public void init() {
		
	}



}
