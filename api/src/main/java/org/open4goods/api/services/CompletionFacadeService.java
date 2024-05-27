
package org.open4goods.api.services;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.open4goods.api.services.completion.AmazonCompletionService;
import org.open4goods.api.services.completion.GenAiCompletionService;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.exceptions.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

	///////////////////////////////////
	// Resource completion
	///////////////////////////////////
	@Scheduled(timeUnit = TimeUnit.HOURS, fixedDelay = 12, initialDelay = 1)
	public void resourceCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with resources");
		resourceCompletionService.completeAll();
	}

	///////////////////////////////////
	// Genai completion
	///////////////////////////////////

	@Scheduled(timeUnit = TimeUnit.HOURS, fixedDelay = 24, initialDelay = 2)
	public void genaiCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with genAI content");
		aiCompletionService.completeAll();
	}

	///////////////////////////////////
	// Amazon completion
	///////////////////////////////////
	@Scheduled(timeUnit = TimeUnit.HOURS, fixedDelay = 24, initialDelay = 3)
	public void amazonCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with amazon");
		amazonCompletionService.completeAll();
	}

	///////////////////////////////////
	// Amazon completion
	///////////////////////////////////
	@Scheduled(timeUnit = TimeUnit.HOURS, fixedDelay = 24, initialDelay = 4)
	public void icecatCompletionAll() throws InvalidParameterException, IOException {
		logger.warn("Completing verticals with amazon");
		icecatCompletionService.completeAll();
	}

}
