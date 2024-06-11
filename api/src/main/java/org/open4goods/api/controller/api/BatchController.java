

package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.completion.GenAiCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.VerticalsConfigService;
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
public class BatchController {

	private final VerticalsConfigService verticalConfigService;
	
	private final SerialisationService serialisationService;
	
	private final AggregationFacadeService batchService;

	private final GenAiCompletionService aiCompletionService;

	@Autowired
	private  ProductRepository repository;

	private ResourceCompletionService resourceCompletionService;
	
	
	public BatchController(AggregationFacadeService batchService, SerialisationService serialisationService, VerticalsConfigService verticalsConfigService, GenAiCompletionService aiCompletionService, ResourceCompletionService resourceCompletionService) {
		this.serialisationService = serialisationService;
		this.verticalConfigService = verticalsConfigService;
		this.batchService = batchService;
		this.aiCompletionService =  aiCompletionService;
		this.resourceCompletionService = resourceCompletionService;
		
	}

	@PutMapping(path="/batch/verticals/")
	@Operation(summary="Create or update a vertical with a full yaml config. Can be long time processing")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public String fullUpdateFromConf( @RequestBody @NotBlank final String verticalConfig ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, InterruptedException{
		
		// Adding the verticam to the vertical service
		VerticalConfig v = serialisationService.fromYaml(verticalConfig, VerticalConfig.class);
		verticalConfigService.addTmpConfig(v);
		
		// This is initial submission, batching the products to update catégories				
		batchService. sanitizeVertical(v);
		Thread.sleep(5000);
		batchService. score(v);
		
		return "done";
	}


	@PostMapping(path="/full/verticals/")
	@Operation(summary="Create or update a vertical with a given config name. Can be long time processing")
	public String fullUpdateFromName( @RequestParam @NotBlank final String verticalConfig ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, InterruptedException{
		
		// This is initial submission, batching the products to update catégories				
		batchService.sanitizeVertical(verticalConfigService.getConfigById(verticalConfig));		
		Thread.sleep(5000);
		batchService. score(verticalConfigService.getConfigById(verticalConfig));
		return "done";
	}

	
	@PostMapping(path="/score/verticals/")
	@Operation(summary="Create or update a vertical with a given config name. Can be long time processing")
	public String scoreFromName( @RequestParam @NotBlank final String verticalConfig ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, InterruptedException{
		
		batchService. score(verticalConfigService.getConfigById(verticalConfig));
		return "done";
	}
	
	@GetMapping("/full/verticals")
	@Operation(summary="Update all verticals (sanitisation + launch the scheduled batch that score all verticals)")
	public void fullVerticals() throws InvalidParameterException, IOException, InterruptedException {
		
		batchService.sanitizeAllVerticals();	
		Thread.sleep(5000);
		batchService.scoreAll();
	}

	
	@GetMapping("/score/verticals")
	@Operation(summary="Score all verticals (sanitisation + launch the scheduled batch that score all verticals)")
	public void scoreVerticals() throws InvalidParameterException, IOException, InterruptedException {
		batchService.scoreAll();
	}
	
	
	
	
	@GetMapping("/sanitisation")
	@Operation(summary="Launch sanitisation of all products")
	public void sanitize() throws InvalidParameterException, IOException {
		batchService.sanitizeAll();
	}

	@GetMapping("/sanitisation/{gtin}")
	@Operation(summary="Launch sanitisation of all products")
	public void sanitizeOne(@PathVariable String gtin ) throws InvalidParameterException, IOException, ResourceNotFoundException, AggregationSkipException {
		batchService.sanitizeOne(repository.getById(gtin));
	}
	
}
