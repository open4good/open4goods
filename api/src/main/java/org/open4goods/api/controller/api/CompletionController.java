
package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

	private ResourceCompletionService resourceCompletionService;
//	private AmazonCompletionService amazonCompletionService;

	@Autowired
	private ProductRepository repository;

	private IcecatCompletionService iceCatService;

	public CompletionController(VerticalsConfigService verticalsConfigService,
			ResourceCompletionService resourceCompletionService,
//			AmazonCompletionService amazonCompletionService,
			IcecatCompletionService iceCatService
			) {
		this.verticalConfigService = verticalsConfigService;
		this.resourceCompletionService = resourceCompletionService;
//		this.amazonCompletionService = amazonCompletionService;
		this.iceCatService = iceCatService;
	}

	///////////////////////////////////
	// Resource completion
	///////////////////////////////////

	@GetMapping("/completion/resources")
	@Operation(summary = "Launch resource completion on all verticals")
	public void resourceCompletionAll() throws InvalidParameterException, IOException {
		resourceCompletionService.completeAll(false);
	}

	@GetMapping("/completion/resources/")
	@Operation(summary = "Launch resource completion on the specified vertical")
	public void resourceCompletionVertical(@RequestParam @NotBlank final String verticalConfig)
			throws InvalidParameterException, IOException {
		resourceCompletionService.complete(verticalConfigService.getConfigById(verticalConfig),false);
	}

	@GetMapping("/completion/resources/gtin/")
	@Operation(summary = "Launch resource completion on the specified vertical")
	public void resourceCompletionProduct(@RequestParam final Long gtin) {
		Product data;
		try {
			data = repository.getById(gtin);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		resourceCompletionService.completeAndIndexProduct(verticalConfigService.getConfigByIdOrDefault(data.getVertical()),
				data);
	}



//	///////////////////////////////////
//	// Amazon completion
//	///////////////////////////////////
//
//	@GetMapping("/completion/amazon")
//	@Operation(summary = "Launch amazon completion on all verticals")
//	public void amazonCompletionAll() throws InvalidParameterException, IOException {
//// TODO : From conf
//		amazonCompletionService.completeAll(false);
//	}
//
//	@GetMapping("/completion/amazon/")
//	@Operation(summary = "Launch amazon completion on the specified vertical")
//	public void amazonCompletionVertical(@RequestParam @NotBlank final String verticalConfig, @RequestParam Integer max)
//			throws InvalidParameterException, IOException {
//		amazonCompletionService.complete(verticalConfigService.getConfigById(verticalConfig), max, false);
//	}
//
//	@GetMapping("/completion/amazon/gtin/")
//	@Operation(summary = "Launch amazon completion on the specified vertical")
//	public void amazonCompletionProduct(@RequestParam final Long gtin) {
//		Product data;
//		try {
//			data = repository.getById(gtin);
//		} catch (ResourceNotFoundException e) {
//			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//		}
//		amazonCompletionService.completeAndIndexProduct(verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
//	}
//
//



	///////////////////////////////////
	// Icecat completion
	///////////////////////////////////

	@GetMapping("/completion/icecat")
	@Operation(summary = "Launch icecat completion on all verticals")
	public void icecatCompletionAll() throws InvalidParameterException, IOException {
// TODO : From conf
		iceCatService.completeAll(true);
	}

	@GetMapping("/completion/icecat/")
	@Operation(summary = "Launch icecat completion on the specified vertical")
	public void icecatCompletionVertical(@RequestParam @NotBlank final String verticalConfig, @RequestParam Integer max)
			throws InvalidParameterException, IOException {
		iceCatService.complete(verticalConfigService.getConfigById(verticalConfig), max,true);
	}

	@GetMapping("/completion/icecat/gtin/")
	@Operation(summary = "Launch icecat completion on the specified vertical")
	public void icecatCompletionProduct(@RequestParam final Long gtin) {
		Product data;
		try {
			data = repository.getById(gtin);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		iceCatService.completeAndIndexProduct(verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
	}
}
