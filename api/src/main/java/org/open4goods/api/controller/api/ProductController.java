

package org.open4goods.api.controller.api;

import java.util.Map;

import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.data.AiDescription;
import org.open4goods.model.product.Product;
import org.open4goods.services.ai.AiCompletionAggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;

/**
 * This controller allows informations and communications about DatasourceConfigurations
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
public class ProductController {

	@Autowired
	private  ProductRepository repository;
	
	@Autowired
	private   AiCompletionAggregationService aiCompletionAggregationService;

	@GetMapping(path="/product/")
	@Operation(summary="Get a product from it's GTIN")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Product get( @RequestParam @NotBlank final String gtin) throws ResourceNotFoundException {
		return repository.getById(gtin);
		
	}

	@PostMapping(path="/product/ai")
	@Operation(summary="Generate the Ai assisted content")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Map<String, AiDescription> genAi( @RequestParam @NotBlank final String gtin) throws ResourceNotFoundException {
		
		Product data = repository.getById(gtin);
		
		aiCompletionAggregationService.onProduct(data);
		
		repository.index(data);

		return data.getAiDescriptions();
	}
	
	
	
	
	
	
}
