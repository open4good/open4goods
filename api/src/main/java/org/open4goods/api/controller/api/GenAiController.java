

package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.Map;

import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.BatchService;
import org.open4goods.api.services.completion.GenAiCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.commons.services.ai.AiService;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;

/**
 * This controller allows informations and communications about DatasourceConfigurations
 * TODO : Should split, into scoringController / resourceController, ....
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
public class GenAiController {


	private AiService aiService;



	public GenAiController(AiService aiService) {
		this.aiService = aiService;
	}
	
	
	@GetMapping("/prompt/")
	@Operation(summary="Launch prompt")
	public String sanitizeOne(@RequestParam(defaultValue = "Agis en tant qu'agent IA offrant de l'information factuelle, précise. Réponds au format JSON") String systemMessage, 
			@RequestParam String userMessage) throws InvalidParameterException, IOException, ResourceNotFoundException, AggregationSkipException {
		String response = aiService.prompt(systemMessage, userMessage);
		return response;
		
	}
	
}
