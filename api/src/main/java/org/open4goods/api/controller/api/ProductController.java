

package org.open4goods.api.controller.api;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.ProductNameSelectionService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

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
	private  VerticalsConfigService configService;


	@Autowired
	private  ProductNameSelectionService productNameSelectionService;


	@GetMapping(path="/product/", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary="Get a product from it's GTIN")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Product get( @RequestParam final Long gtin) throws ResourceNotFoundException {
		return repository.getById(gtin);
	}


	@GetMapping(path="/product/bestname", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary="Get a product best name")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public String getBestName( @RequestParam final Long gtin) throws ResourceNotFoundException {

		List<String> names = new ArrayList<>();
		names.addAll(repository.getById(gtin).getOfferNames());

		return  productNameSelectionService.selectBestNameIndustrial(names).orElse(null);
	}
}
