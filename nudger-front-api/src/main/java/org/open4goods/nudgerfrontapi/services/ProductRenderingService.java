package org.open4goods.nudgerfrontapi.services;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;

/**
 * This service takes in input repository objects (through the DAO), then apply frontend transformation logic (internationalisation, enrichment)
 */
public class ProductRenderingService {


	//  TODO : Inject productRepository OR a easy mock repository if in dev mode

	public ProductViewResponse render(ProductViewRequest productViewRequest) {
    	ProductViewResponse response = new ProductViewResponse(productViewRequest);
    	// TODO : fetch in repository


    	// TODO : Transform
		return response;

	}
}
