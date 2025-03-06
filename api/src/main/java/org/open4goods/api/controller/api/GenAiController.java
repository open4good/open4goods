

package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.Map;

import org.open4goods.api.services.ProductsReviewGenerationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.model.AiSourcedPage;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * This controller allows informations and communications about DatasourceConfigurations
 * TODO : Should split, into scoringController / resourceController, ....
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
public class GenAiController {


	private PromptService aiService;

	private ProductsReviewGenerationService pageGenService;
	
	private VerticalsConfigService verticalsConfigService;


	public GenAiController(PromptService aiService, ProductsReviewGenerationService pageGenService,  VerticalsConfigService verticalsConfigService) {
		this.aiService = aiService;
		this.pageGenService = pageGenService;
		this.verticalsConfigService = verticalsConfigService;
	}
	
	
	
	@GetMapping("/prompt/json")
	@Operation(summary="Launch prompt")
	public Map<String, Object> promptJson(@RequestParam(defaultValue = "test") String key, 
			@RequestParam Map<String,Object> context) throws ResourceNotFoundException, SerialisationException {
		
		return aiService.jsonPrompt(key, context).getBody();
	}
	
	@GetMapping("/prompt/text")
	@Operation(summary="Launch prompt")
	public String prompt(@RequestParam(defaultValue = "test") String key, 
			@RequestParam Map<String,Object> context) throws ResourceNotFoundException, SerialisationException {
		
		return aiService.prompt(key, context).getRaw();
	}
	
	
	@GetMapping("/page/generate")
	@Operation(summary="Generate a page")
	public AiSourcedPage prompt(@RequestParam(defaultValue = "test") String key, 
			String question,
			String vertical,
			String id,
			String title) throws ResourceNotFoundException, SerialisationException{
		
		
		VerticalConfig vc = verticalsConfigService.getConfigById(vertical);
		
		AiSourcedPage ret = pageGenService.perplexityCompletion(vc, question, id, question, title);
		
		return ret;
	}
}
