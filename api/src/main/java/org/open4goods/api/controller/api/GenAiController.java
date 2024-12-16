

package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.Map;

import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.EcoscoreResponse;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.ai.GenAiService;
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


	private GenAiService aiService;



	public GenAiController(GenAiService aiService) {
		this.aiService = aiService;
	}
	
	
	
	@GetMapping("/prompt/json")
	@Operation(summary="Launch prompt")
	public Map<String, Object> promptJson(@RequestParam(defaultValue = "test") String key, 
			@RequestParam Map<String,Object> context) throws InvalidParameterException, IOException, ResourceNotFoundException, AggregationSkipException {
		
		return aiService.jsonPrompt(key, context).getBody();
	}
	
	@GetMapping("/prompt/text")
	@Operation(summary="Launch prompt")
	public String prompt(@RequestParam(defaultValue = "test") String key, 
			@RequestParam Map<String,Object> context) throws InvalidParameterException, IOException, ResourceNotFoundException, AggregationSkipException {
		
		return aiService.prompt(key, context).getRaw();
	}
}
