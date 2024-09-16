

package org.open4goods.api.controller.api;

import java.io.IOException;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
	
	private final AggregationFacadeService aggregationFacadeService;

	private final GenAiCompletionService aiCompletionService;

	@Autowired
	private  ProductRepository repository;

	private ResourceCompletionService resourceCompletionService;

	private BatchService batchService;
	
	
	public BatchController(BatchService batchService, AggregationFacadeService aggregationFacadeService, SerialisationService serialisationService, VerticalsConfigService verticalsConfigService, GenAiCompletionService aiCompletionService, ResourceCompletionService resourceCompletionService) {
		this.serialisationService = serialisationService;
		this.verticalConfigService = verticalsConfigService;
		this.aggregationFacadeService = aggregationFacadeService;
		this.aiCompletionService =  aiCompletionService;
		this.resourceCompletionService = resourceCompletionService;
		this.batchService = batchService;
		
	}
	@PutMapping(path="/batch/")
	@Operation(summary="Launch the scoring and completion batch, iso has @Scheduled")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public String batch( ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, InterruptedException{
		
		batchService.batch();
		
		return "done";
	}
	
//	@PutMapping(path="/batch/verticals/")
//	@Operation(summary="Create or update a vertical with a full yaml config. Can be long time processing")
//	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
//	public String fullUpdateFromConf( @RequestBody @NotBlank final String verticalConfig ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, InterruptedException{
//		
//		// Adding the verticam to the vertical service
//		VerticalConfig v = serialisationService.fromYaml(verticalConfig, VerticalConfig.class);
//		verticalConfigService.addTmpConfig(v);
//		
//		// This is initial submission, batching the products to update catégories				
//		aggregationFacadeService. sanitizeVertical(v);
//		Thread.sleep(5000);
//		aggregationFacadeService. score(v);
//		
//		return "done";
//	}


	@PostMapping(path="/full/verticals/")
	@Operation(summary="Create or update a vertical with a given config name. Can be long time processing")
	public String fullUpdateFromName( @RequestParam @NotBlank final String verticalConfig ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, InterruptedException{
		
		// This is initial submission, batching the products to update catégories				
		aggregationFacadeService.sanitizeVertical(verticalConfigService.getConfigById(verticalConfig));		
		Thread.sleep(5000);
		aggregationFacadeService. score(verticalConfigService.getConfigById(verticalConfig));
		return "done";
	}

	
	@PostMapping(path="/score/verticals/")
	@Operation(summary="Score the vertical")
	public String scoreFromName( @RequestParam @NotBlank final String verticalConfig ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, InterruptedException{
		
		aggregationFacadeService. score(verticalConfigService.getConfigById(verticalConfig));
		return "done";
	}
	
	@GetMapping("/full/verticals")
	@Operation(summary="Update all verticals (sanitisation + launch the scheduled batch that score all verticals)")
	public void fullVerticals() throws InvalidParameterException, IOException, InterruptedException {
		
		aggregationFacadeService.sanitizeAllVerticals();	
		Thread.sleep(5000);
		aggregationFacadeService.scoreAll();
	}

	
	@GetMapping("/score/verticals")
	@Operation(summary="Score all verticals (sanitisation + launch the scheduled batch that score all verticals)")
	public void scoreVerticals() throws InvalidParameterException, IOException, InterruptedException {
		aggregationFacadeService.scoreAll();
	}
	
	
	
	
	@GetMapping("/sanitisation")
	@Operation(summary="Launch sanitisation of all products")
	public void sanitize() throws InvalidParameterException, IOException {
		aggregationFacadeService.sanitizeAll();
	}

	@GetMapping("/sanitisation/{gtin}")
	@Operation(summary="Launch sanitisation of a given products")
	public void sanitizeOne(@PathVariable Long gtin ) throws InvalidParameterException, IOException, ResourceNotFoundException, AggregationSkipException {
		aggregationFacadeService.sanitizeOne(repository.getById(gtin));
	}
	
}
