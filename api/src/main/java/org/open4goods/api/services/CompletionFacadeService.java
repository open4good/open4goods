
package org.open4goods.api.services;

import java.io.IOException;
import java.util.Set;

import org.open4goods.api.services.completion.AmazonCompletionService;
import org.open4goods.api.services.completion.PerplexityReviewCompletionService;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.PerplexityAttributesCompletionService;
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

	private final PerplexityReviewCompletionService perplexityReviewCompletionService;
	private PerplexityAttributesCompletionService perplexityAttributesCompletionService;
	private ResourceCompletionService resourceCompletionService;
	private AmazonCompletionService amazonCompletionService;
	private IcecatCompletionService icecatCompletionService;

	public CompletionFacadeService(PerplexityReviewCompletionService aiCompletionService,
			ResourceCompletionService resourceCompletionService, AmazonCompletionService amazonCompletionService, IcecatCompletionService icecatCompletionService, PerplexityAttributesCompletionService perplexityAttributesCompletionService) {
		this.perplexityReviewCompletionService = aiCompletionService;
		this.resourceCompletionService = resourceCompletionService;
		this.amazonCompletionService = amazonCompletionService;
		this.icecatCompletionService = icecatCompletionService;
		this.perplexityAttributesCompletionService = perplexityAttributesCompletionService;
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
			perplexityAttributesCompletionService.process(vertical, product);
			perplexityReviewCompletionService.process(vertical, product);
			
			amazonCompletionService.process(vertical, product);
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

	public void genaiCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with genAI content");
		perplexityReviewCompletionService.completeAll(false);
	}

	///////////////////////////////////
	// Amazon completion
	///////////////////////////////////
	public void amazonCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with amazon");
		amazonCompletionService.completeAll(false);
	}

	///////////////////////////////////
	// Icecat completion
	///////////////////////////////////
	public void icecatCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with icecat");
		icecatCompletionService.completeAll(true);
	}

	

}
