package org.open4goods.api.services.store;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.model.datafragment.DataFragment;
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

		while (!service.getServiceShutdown().get()) {
			try {
				List<DataFragment> buffer = new ArrayList<>(dequeuePageSize);
				DataFragment first = service.getQueue().poll(pauseDuration, TimeUnit.MILLISECONDS);
				if (first == null) {
					continue;
				}

				buffer.add(first);
				service.getQueue().drainTo(buffer, dequeuePageSize - buffer.size());

				service.aggregateAndstore(buffer);
				logger.info("{} has handled {} DataFragments. {} Remaining in queue", workerName, buffer.size(),
						service.getQueue().size());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} catch (final Exception e) {
				logger.error("Error while dequeing DataFragments", e);
			}
		}
		logger.info("{} thread terminated", workerName);
	}
}
