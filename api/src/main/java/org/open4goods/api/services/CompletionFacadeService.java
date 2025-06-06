
package org.open4goods.api.services;

import java.io.IOException;
import java.util.Set;

import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
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

public class CompletionFacadeService {

	protected static final Logger logger = LoggerFactory.getLogger(CompletionFacadeService.class);

	private ResourceCompletionService resourceCompletionService;
//	private AmazonCompletionService amazonCompletionService;
	private IcecatCompletionService icecatCompletionService;

	public CompletionFacadeService(
			ResourceCompletionService resourceCompletionService,  IcecatCompletionService icecatCompletionService) {
		this.resourceCompletionService = resourceCompletionService;
		this.icecatCompletionService = icecatCompletionService;
	}


	/**
	 * Complete the provided products with all completors
	 * @param products
	 * @param vertical
	 */
	public void processAll(Set<Product> products, VerticalConfig vertical) {
		logger.info("Completing {] products",products.size());
		products.forEach(product -> {
			// TODO(p2, perf) : should paralellize (on verticals, at upper level)
			resourceCompletionService.process(vertical, product);
			icecatCompletionService.process(vertical, product);

//			amazonCompletionService.process(vertical, product);
		});

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



//	///////////////////////////////////
//	// Amazon completion
//	///////////////////////////////////
//	public void amazonCompletionAll() throws InvalidParameterException, IOException {
//		logger.warn("Completing verticals with amazon");
//		amazonCompletionService.completeAll(false);
//	}

	///////////////////////////////////
	// Icecat completion
	///////////////////////////////////
	public void icecatCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with icecat");
		icecatCompletionService.completeAll(true);
	}



}
