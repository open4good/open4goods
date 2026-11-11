
package org.open4goods.api.services;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.open4goods.api.services.completion.AmazonCompletionService;
import org.open4goods.api.services.completion.EprelCompletionService;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.api.services.completion.WikidataCompletionService;
import org.open4goods.commons.services.AbstractCompletionService;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is in charge of building Product in realtime mode TODO :
 * Maintain a state machine to disable multiple launching
 *
 * @author goulven
 *
 */

import org.open4goods.api.config.yml.ApiProperties;

public class CompletionFacadeService {

	protected static final Logger logger = LoggerFactory.getLogger(CompletionFacadeService.class);

	/** Batch timings, logged at INFO in prod (GOU-2099). */
	private static final Logger TIMINGS = LoggerFactory.getLogger(BatchService.TIMINGS_LOGGER);

	/** Number of completed products between two progress lines. */
	static final int PROGRESS_INTERVAL = 1000;

	private final ResourceCompletionService resourceCompletionService;
	private final IcecatCompletionService icecatCompletionService;
	private final EprelCompletionService eprelCompletionService;
	private final WikidataCompletionService wikidataCompletionService;
	private final AmazonCompletionService amazonCompletionService;
	private final ApiProperties apiProperties;

	public CompletionFacadeService(
			ResourceCompletionService resourceCompletionService,
			IcecatCompletionService icecatCompletionService,
			EprelCompletionService eprelCompletionService,
			WikidataCompletionService wikidataCompletionService,
			AmazonCompletionService amazonCompletionService,
			ApiProperties apiProperties) {
		this.resourceCompletionService = resourceCompletionService;
		this.icecatCompletionService = icecatCompletionService;
		this.eprelCompletionService = eprelCompletionService;
		this.wikidataCompletionService = wikidataCompletionService;
		this.amazonCompletionService = amazonCompletionService;
		this.apiProperties = apiProperties;
	}

	/**
	 * Complete the provided products with all completors.
	 *
	 * <p>Logs on the batch timings logger ({@link BatchService#TIMINGS_LOGGER}, INFO in prod) the progress every
	 * {@link #PROGRESS_INTERVAL} products and, at the end, the time spent and the products processed and skipped
	 * by each completion service (GOU-2099).
	 *
	 * @param products the products to enrich
	 * @param vertical the vertical context
	 * @return the counters of the pass
	 */
	public CompletionTimings processAll(Set<Product> products, VerticalConfig vertical) {
		logger.info("Completing {} products", products.size());
		String verticalId = vertical == null ? null : vertical.getId();
		CompletionTimings timings = new CompletionTimings();
		long start = System.nanoTime();
		int concurrency = apiProperties.getCompletionConcurrency();
		if (concurrency <= 1) {
			products.forEach(product -> complete(vertical, product, timings, products.size(), start));
		} else {
			java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(concurrency);
			try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
				for (Product product : products) {
					executor.submit(() -> {
						try {
							semaphore.acquire();
							try {
								complete(vertical, product, timings, products.size(), start);
							} finally {
								semaphore.release();
							}
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							logger.error("Product completion interrupted for product {}", product.getId(), e);
						}
					});
				}
			}
		}
		TIMINGS.info("Completion of vertical {} : {} products in {} s, concurrency {} ; {}", verticalId,
				products.size(), (System.nanoTime() - start) / 1_000_000_000L, concurrency, timings.summary());
		return timings;
	}

	/**
	 * Runs every completion service on a product, in order. A service that throws stops the following ones for
	 * this product, as before the timings were recorded.
	 */
	private void complete(VerticalConfig vertical, Product product, CompletionTimings timings, int total, long start) {
		String current = null;
		long callStart = 0;
		try {
			for (AbstractCompletionService service : completionServices()) {
				current = service.getClass().getSimpleName();
				callStart = System.nanoTime();
				boolean processed = service.process(vertical, product);
				timings.record(current, processed, System.nanoTime() - callStart);
			}
		} catch (Exception e) {
			timings.recordFailure(current, System.nanoTime() - callStart);
			logger.error("Error completing product {}", product.getId(), e);
		}
		long done = timings.productDone();
		if (done % PROGRESS_INTERVAL == 0) {
			TIMINGS.info("Completion of vertical {} : {}/{} products in {} s ; {}",
					vertical == null ? null : vertical.getId(), done, total,
					(System.nanoTime() - start) / 1_000_000_000L, timings.summary());
		}
	}

	/**
	 * @return the completion services, in the order they run on each product
	 */
	private List<AbstractCompletionService> completionServices() {
		return List.of(resourceCompletionService, icecatCompletionService, eprelCompletionService,
				wikidataCompletionService, amazonCompletionService);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// TODO(P3,design) : Should be legacy, through a standard spring bean registration mechanism
	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////
	// Resource completion
	///////////////////////////////////
	public void resourceCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with resources");
		resourceCompletionService.completeAll(false);
	}

	///////////////////////////////////
	// Genai completion
	///////////////////////////////////



	///////////////////////////////////
	// Amazon completion
	///////////////////////////////////
	public void amazonCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with amazon");
		amazonCompletionService.completeAll(amazonCompletionService.getAmazonConfig().getMaxCallsPerBatch(), false);
	}

	///////////////////////////////////
	// Eprel completion
	///////////////////////////////////
	public void eprelCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with eprel");
		eprelCompletionService.completeAll(true);
	}

	///////////////////////////////////
	// Icecat completion
	///////////////////////////////////
	public void icecatCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with icecat");
		icecatCompletionService.completeAll(true);
	}


}
