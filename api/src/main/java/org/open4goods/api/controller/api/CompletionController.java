
package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.completion.AmazonCompletionService;
import org.open4goods.api.services.completion.GenAiCompletionService;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.product.Product;
import org.open4goods.services.VerticalsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;

/**
 * This controller allows informations and communications about
 * DatasourceConfigurations TODO : Scheduling done here, not good
 * 
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class CompletionController {

	private final VerticalsConfigService verticalConfigService;

	private final GenAiCompletionService aiCompletionService;
	private ResourceCompletionService resourceCompletionService;
	private AmazonCompletionService amazonCompletionService;

	@Autowired
	private ProductRepository repository;

	private IcecatCompletionService iceCatService;

	public CompletionController(VerticalsConfigService verticalsConfigService,
			GenAiCompletionService aiCompletionService,
			ResourceCompletionService resourceCompletionService,
			AmazonCompletionService amazonCompletionService,
			IcecatCompletionService iceCatService) {
		this.verticalConfigService = verticalsConfigService;
		this.aiCompletionService = aiCompletionService;
		this.resourceCompletionService = resourceCompletionService;
		this.amazonCompletionService = amazonCompletionService;
		this.iceCatService = iceCatService;
	}

	///////////////////////////////////
	// Resource completion
	///////////////////////////////////

	@GetMapping("/completion/resources")
	@Operation(summary = "Launch resource completion on all verticals")
	public void resourceCompletionAll() throws InvalidParameterException, IOException {
		resourceCompletionService.completeAll();
	}

	@GetMapping("/completion/resources/")
	@Operation(summary = "Launch resource completion on the specified vertical")
	public void resourceCompletionVertical(@RequestParam @NotBlank final String verticalConfig)
			throws InvalidParameterException, IOException {
		resourceCompletionService.complete(verticalConfigService.getConfigById(verticalConfig));
	}

	@GetMapping("/completion/resources/gtin/")
	@Operation(summary = "Launch resource completion on the specified vertical")
	public void resourceCompletionProduct(@RequestParam @NotBlank final String gtin) {
		Product data;
		try {
			data = repository.getById(gtin);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		resourceCompletionService.completeProduct(verticalConfigService.getConfigByIdOrDefault(data.getVertical()),
				data);
	}

	///////////////////////////////////
	// Genai completion
	///////////////////////////////////

	@GetMapping("/completion/genai")
	@Operation(summary = "Launch genai completion on all verticals")
	public void genaiCompletionAll() throws InvalidParameterException, IOException {
		// TODO : From conf
		aiCompletionService.completeAll(10);
	}

	@GetMapping("/completion/genai/")
	@Operation(summary = "Launch genai completion on the specified vertical")
	public void genaiCompletionVertical(@RequestParam @NotBlank final String verticalConfig, @RequestParam Integer max)
			throws InvalidParameterException, IOException {
		aiCompletionService.complete(verticalConfigService.getConfigById(verticalConfig), max);
	}

	@GetMapping("/completion/genai/gtin/")
	@Operation(summary = "Launch genai completion on the specified vertical")
	public void genaiCompletionProduct(@RequestParam @NotBlank final String gtin) {
		Product data;
		try {
			data = repository.getById(gtin);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		aiCompletionService.completeProduct(verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
	}

	///////////////////////////////////
	// Amazon completion
	///////////////////////////////////

	@GetMapping("/completion/amazon")
	@Operation(summary = "Launch amazon completion on all verticals")
	public void amazonCompletionAll() throws InvalidParameterException, IOException {
// TODO : From conf
		amazonCompletionService.completeAll();
	}

	@GetMapping("/completion/amazon/")
	@Operation(summary = "Launch amazon completion on the specified vertical")
	public void amazonCompletionVertical(@RequestParam @NotBlank final String verticalConfig, @RequestParam Integer max)
			throws InvalidParameterException, IOException {
		amazonCompletionService.complete(verticalConfigService.getConfigById(verticalConfig), max);
	}

	@GetMapping("/completion/amazon/gtin/")
	@Operation(summary = "Launch amazon completion on the specified vertical")
	public void amazonCompletionProduct(@RequestParam @NotBlank final String gtin) {
		Product data;
		try {
			data = repository.getById(gtin);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		amazonCompletionService.completeProduct(verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
	}

	
	
	
	
	///////////////////////////////////
	// Icecat completion
	///////////////////////////////////

	@GetMapping("/completion/icecat")
	@Operation(summary = "Launch icecat completion on all verticals")
	public void icecatCompletionAll() throws InvalidParameterException, IOException {
// TODO : From conf
		iceCatService.completeAll();
	}

	@GetMapping("/completion/icecat/")
	@Operation(summary = "Launch icecat completion on the specified vertical")
	public void icecatCompletionVertical(@RequestParam @NotBlank final String verticalConfig, @RequestParam Integer max)
			throws InvalidParameterException, IOException {
		iceCatService.complete(verticalConfigService.getConfigById(verticalConfig), max);
	}

	@GetMapping("/completion/icecat/gtin/")
	@Operation(summary = "Launch icecat completion on the specified vertical")
	public void icecatCompletionProduct(@RequestParam @NotBlank final String gtin) {
		Product data;
		try {
			data = repository.getById(gtin);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		iceCatService.completeProduct(verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
	}
}
