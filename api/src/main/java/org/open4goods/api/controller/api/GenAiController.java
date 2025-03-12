

package org.open4goods.api.controller.api;

import java.util.Map;

import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
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

	
	private VerticalsConfigService verticalsConfigService;


	public GenAiController(PromptService aiService,  VerticalsConfigService verticalsConfigService) {
		this.aiService = aiService;
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
	
	
	
}
