package org.open4goods.api.services.store;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Worker thread that asynchronously dequeue the DataFragments from the file queue. It
 * @author goulven
 *
 */
public class DataFragmentAggregationWorker implements Runnable {


	private static final Logger logger = LoggerFactory.getLogger(DataFragmentAggregationWorker.class);

	/** The service used to "atomically" fetch and store / update DataFragments **/
	private final DataFragmentStoreService  service;

	/** Size of pages that will be bulked to the DataFragmentStore**/
	private final int dequeuePageSize;

	/** The duration of the worker thread pause when nothing to get from the queue **/
	private final int pauseDuration;

	/**
	 * The worker name
	 */
	private final String workerName;

	/**
	 * Constructor
	 * @param dataFragmentStoreService
	 * @param dequeuePageSize
	 */
	public DataFragmentAggregationWorker(final DataFragmentStoreService dataFragmentStoreService, final int dequeuePageSize, final int pauseDuration, String workerName) {
		service = dataFragmentStoreService;
		this.dequeuePageSize = dequeuePageSize;
		this.pauseDuration = pauseDuration;
		this.workerName = workerName;
	}

	@Override
	public void run() {

		// TODO : exit thread condition
		while (true) {
			try {
				if (!service.getQueue().isEmpty()) {
					// There is data to consume and queue consummation is enabled
					final Set<DataFragment> buffer = new HashSet<>();	
					
					for (int i = 0; i < dequeuePageSize; i++) {
							buffer.add(service.getQueue().take());
					}
					
					// Aggregating
					service.aggregateAndstore(buffer);
					
					logger.warn("{} has indexed {} DataFragments. {} Remaining in queue",workerName,  buffer.size(), service.getQueue().size());

				} else {
					try {
						logger.debug("No DataFragments to dequeue. Will sleep {}ms",pauseDuration);
						Thread.sleep(pauseDuration + (int) (Math.random() * 1000));
					} catch (final InterruptedException e) {
					}
				}
			} catch (final Exception e) {
				logger.error("Error while dequeing DataFragments",e);
			}
		}
	}
}