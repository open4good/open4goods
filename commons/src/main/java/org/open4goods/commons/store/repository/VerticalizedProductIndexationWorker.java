package org.open4goods.commons.store.repository;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.model.product.VerticalizedProduct;
import org.open4goods.commons.services.VerticalsRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Worker thread that asynchronously dequeue the DataFragments from the file queue. It
 * @author goulven
 *
 */
public class VerticalizedProductIndexationWorker implements Runnable {


	private static final Logger logger = LoggerFactory.getLogger(VerticalizedProductIndexationWorker.class);


	private VerticalsRepositoryService verticalRepoService;

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
	public VerticalizedProductIndexationWorker(final VerticalsRepositoryService verticalRepoService, final int dequeuePageSize, final int pauseDuration, String workerName) {
		this.dequeuePageSize = dequeuePageSize;
		this.pauseDuration = pauseDuration;
		this.workerName = workerName;
		this.verticalRepoService = verticalRepoService;
	}

	@Override
	public void run() {

		// TODO : exit thread condition
		while (true) {
			try {
				
				// Computing if items presents, and how many to take
				int itemsToTake = verticalRepoService.getVerticalizedProductQueue().size();
				if (itemsToTake > dequeuePageSize) {
					itemsToTake = dequeuePageSize;
				}
				
				if (itemsToTake > 0) {
					// There is data to consume and queue consummation is enabled
					// A map to deduplicate --> MEANS WE CAN SOMETIMES LOOSE DATAFRAMENTS IF 2 ENTRIES ARE IN THE SAME BAG (no because we put back in queue)
					final Set<VerticalizedProduct> buffer = new HashSet<>();	
										
					// Dequeuing
					for (int i = 0; i < itemsToTake; i++) {
						VerticalizedProduct item = verticalRepoService.getVerticalizedProductQueue().take();
						
						buffer.add(item);
						
					}
					
					verticalRepoService.index(buffer);
					
					logger.info("{} has put in queue {} verticalized products. {} Remaining in queue",workerName,  buffer.size(), verticalRepoService.getVerticalizedProductQueue().size());

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