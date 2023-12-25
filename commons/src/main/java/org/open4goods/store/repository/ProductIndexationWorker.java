package org.open4goods.store.repository;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.dao.ProductRepository;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Worker thread that asynchronously dequeue the DataFragments from the file queue. It
 * @author goulven
 *
 */
public class ProductIndexationWorker implements Runnable {


	private static final Logger logger = LoggerFactory.getLogger(ProductIndexationWorker.class);

	/** The service used to "atomically" fetch and store / update DataFragments **/
	private final ProductRepository service;

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
	 * @param owningService
	 * @param dequeuePageSize
	 */
	public ProductIndexationWorker(final ProductRepository owningService, final int dequeuePageSize, final int pauseDuration, String workerName) {
		service = owningService;
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
					final Set<Product> buffer = new HashSet<>();	
										
					for (int i = 0; i < dequeuePageSize; i++) {
						buffer.add(service.getQueue().take());
					}
					
					// Store operation retrieve fragments, historize and re-index
					long now = System.currentTimeMillis();
					
					service.store(buffer);
					
					logger.info("{} has indexed {} DataFragments in {}ms. {} Remaining in queue",workerName,  buffer.size(),System.currentTimeMillis()-now, service.getQueue().size());

				} else {
					try {
						logger.debug("No DataFragments to dequeue. Will sleep {}ms",pauseDuration);
						Thread.sleep(pauseDuration);
					} catch (final InterruptedException e) {
					}
				}
			} catch (final Exception e) {
				logger.error("Error while dequeing DataFragments",e);
			}
		}
	}
}