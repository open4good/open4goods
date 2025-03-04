

package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.BatchService;
import org.open4goods.api.services.completion.PerplexityAttributesCompletionService;
import org.open4goods.api.services.completion.PerplexityReviewCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.exceptions.ResourceNotFoundException;
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
import jakarta.websocket.server.PathParam;

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

	private final PerplexityReviewCompletionService aiCompletionService;

	private PerplexityAttributesCompletionService perplexityAttributesCompletionService;
	@Autowired
	private  ProductRepository repository;

	private ResourceCompletionService resourceCompletionService;

	private BatchService batchService;
	
	
	public BatchController(BatchService batchService, AggregationFacadeService aggregationFacadeService, SerialisationService serialisationService, VerticalsConfigService verticalsConfigService, PerplexityReviewCompletionService aiCompletionService, ResourceCompletionService resourceCompletionService, PerplexityAttributesCompletionService perplexityAttributesCompletionService) {
		this.serialisationService = serialisationService;
		this.verticalConfigService = verticalsConfigService;
		this.aggregationFacadeService = aggregationFacadeService;
		this.aiCompletionService =  aiCompletionService;
		this.resourceCompletionService = resourceCompletionService;
		this.perplexityAttributesCompletionService = perplexityAttributesCompletionService;
		this.batchService = batchService;
		
	}
	@PostMapping(path="/batch/")
	@Operation(summary="Launch the full batch (scoring, aggregation, completion batch), iso has @Scheduled")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void batch( ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, InterruptedException{
		batchService.batch();
	}
	
	@PostMapping(path="/batch/{vertical}")
	@Operation(summary="Batch a specific vertical")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void batchVertical( @PathVariable @NotBlank final String vertical ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, InterruptedException{
		batchService.batch(verticalConfigService.getConfigById(vertical));
	}
	

	
	@PostMapping(path="/score/{vertical}")
	@Operation(summary="Score a specific vertical")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void scoreFromName(  @PathVariable @NotBlank final String vertical ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, InterruptedException{
		
		aggregationFacadeService. score(verticalConfigService.getConfigById(vertical));
	}
	
	
	@GetMapping("/score/")
	@Operation(summary="Score all verticals (sanitisation + launch the scheduled batch that score all verticals)")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void scoreVerticals() throws InvalidParameterException, IOException, InterruptedException {
		aggregationFacadeService.scoreAll();
	}
	
	
	@GetMapping("/aggregate/verticals")
	@Operation(summary="Launch aggregation of all verticals")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")	
	public void sanitizeAllVertical() throws InvalidParameterException, IOException {
		aggregationFacadeService.sanitizeAllVerticals();
	}

	@GetMapping("/aggregate/verticals/{vertical}")
	@Operation(summary="Launch aggregation of a specific vertical")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")	
	public void sanitizeSpecificVertical(@PathVariable String vertical) throws InvalidParameterException, IOException {
		aggregationFacadeService.sanitizeVertical(verticalConfigService.getConfigById(vertical));
	}
	
	
	@GetMapping("/aggregate/products")
	@Operation(summary="Launch aggregation of all products")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")	
	public void sanitize() throws InvalidParameterException, IOException {
		aggregationFacadeService.sanitizeAll();
	}

	@GetMapping("/aggregate/gtin/{gtin}")
	@Operation(summary="Launch sanitisation of a given products")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")	
	public void sanitizeOne(@PathVariable Long gtin ) throws InvalidParameterException, IOException, ResourceNotFoundException, AggregationSkipException {
		aggregationFacadeService.sanitizeAndSave(repository.getById(gtin));
	}
	
}
