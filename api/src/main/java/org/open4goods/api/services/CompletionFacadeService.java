
package org.open4goods.api.services;

import java.io.IOException;
import java.util.Set;

import org.open4goods.api.services.completion.AmazonCompletionService;
import org.open4goods.api.services.completion.GenAiCompletionService;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.open4goods.commons.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is in charge of building Product in realtime mode TODO :
 * Maintain a state machine to disable multiple launching
 * 
 * @author goulven
 *
 */

// TODO : Scheduling from conf
public class CompletionFacadeService {

	protected static final Logger logger = LoggerFactory.getLogger(CompletionFacadeService.class);

	private final GenAiCompletionService aiCompletionService;
	private ResourceCompletionService resourceCompletionService;
	private AmazonCompletionService amazonCompletionService;
	private IcecatCompletionService icecatCompletionService;

	public CompletionFacadeService(GenAiCompletionService aiCompletionService,
			ResourceCompletionService resourceCompletionService, AmazonCompletionService amazonCompletionService, IcecatCompletionService icecatCompletionService) {
		this.aiCompletionService = aiCompletionService;
		this.resourceCompletionService = resourceCompletionService;
		this.amazonCompletionService = amazonCompletionService;
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
			resourceCompletionService.process(vertical, product);
			icecatCompletionService.process(vertical, product);
			aiCompletionService.process(vertical, product);
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
		aiCompletionService.completeAll(false);
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
