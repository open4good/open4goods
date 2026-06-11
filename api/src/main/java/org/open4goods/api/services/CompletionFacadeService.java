
package org.open4goods.api.services;

import java.io.IOException;
import java.util.Set;

import org.open4goods.api.services.completion.AmazonCompletionService;
import org.open4goods.api.services.completion.EprelCompletionService;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.api.services.completion.WikidataCompletionService;
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
	 * @param products the products to enrich
	 * @param vertical the vertical context
	 */
	public void processAll(Set<Product> products, VerticalConfig vertical) {
		logger.info("Completing {} products", products.size());
		int concurrency = apiProperties.getCompletionConcurrency();
		if (concurrency <= 1) {
			products.forEach(product -> {
				try {
					resourceCompletionService.process(vertical, product);
					icecatCompletionService.process(vertical, product);
					eprelCompletionService.process(vertical, product);
					wikidataCompletionService.process(vertical, product);
					amazonCompletionService.process(vertical, product);
				} catch (Exception e) {
					logger.error("Error completing product {}", product.getId(), e);
				}
			});
			return;
		}

		java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(concurrency);
		try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
			for (Product product : products) {
				executor.submit(() -> {
					try {
						semaphore.acquire();
						try {
							resourceCompletionService.process(vertical, product);
							icecatCompletionService.process(vertical, product);
							eprelCompletionService.process(vertical, product);
							wikidataCompletionService.process(vertical, product);
							amazonCompletionService.process(vertical, product);
						} finally {
							semaphore.release();
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						logger.error("Product completion interrupted for product {}", product.getId(), e);
					} catch (Exception e) {
						logger.error("Error completing product {}", product.getId(), e);
					}
				});
			}
		}
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
