

package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.BatchService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.VerticalsConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
public class BatchController {

	private final VerticalsConfigService service;
	
	private final SerialisationService serialisationService;
	
	private final BatchService batchService;
	
	public BatchController(BatchService batchService, SerialisationService serialisationService, VerticalsConfigService verticalsConfigService) {
		this.serialisationService = serialisationService;
		this.service = verticalsConfigService;
		this.batchService = batchService;
	}

	@PutMapping(path="/batch/verticals/")
	@Operation(summary="Create or update a vertical with a full yaml config. Can be long time processing")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public String fullUpdateFromConf( @RequestBody @NotBlank final String verticalConfig ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException{
		
		// Adding the verticam to the vertical service
		VerticalConfig v = serialisationService.fromYaml(verticalConfig, VerticalConfig.class);
		service.addTmpConfig(v);
		
		// This is initial submission, batching the products to update catégories				
		batchService.batchScore(v);
		
		
		return "done";
	}


	@PostMapping(path="/batch/verticals/")
	@Operation(summary="Create or update a vertical with a given config name. Can be long time processing")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public String fullUpdateFromName( @RequestParam @NotBlank final String verticalConfig ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException{
		
		// This is initial submission, batching the products to update catégories				
		batchService.batchScore(service.getConfigById(verticalConfig).orElse(null));		
		
		return "done";
	}

	
	@GetMapping("/batch/verticals")
	@Operation(summary="Update all verticlas (launch the scheduled batch that score all verticals)")
	public void scoreVerticals() throws InvalidParameterException, IOException {
		batchService.scoreAll();
	}

	@GetMapping("/sanitisation")
	@Operation(summary="Launch sanitisation of all products")
	public void sanitize() throws InvalidParameterException, IOException {
		batchService.sanitize();
	}
	
}
