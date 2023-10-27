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

	public AbstractAggregationService (final String logsFolder, final boolean toConsole) {
	}




	/**
	 * Called after aggregation process, used to ending buffers / components (flush datas, close buffers, so on...)
	 */
	@Override
	public void close() throws IOException {

	}

	/**
	 * Called before data aggregation
	 * @param datas 
	 */
	public void init() {

	}









}
