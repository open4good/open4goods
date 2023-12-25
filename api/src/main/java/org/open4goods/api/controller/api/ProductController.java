

package org.open4goods.api.controller.api;

import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.product.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
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

	@PutMapping(path="/product/")
	@Operation(summary="Get a product from it's GTIN")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Product fullUpdateFromConf( @RequestParam @NotBlank final String gtin) throws ResourceNotFoundException {
		return repository.getById(gtin);
		
	}

	
	
}
