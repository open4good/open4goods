package org.open4goods.nudgerfrontapi.dto;

/**
 * This is a dedicated view of a @see Product, dedicated to frontend rendering.
 * The object is cleaned from useless fields, is localised
 */
// TODO : I don'tl ike this name... But Product is my main persisted object model. Any correct naming conventions
// TODO : Convert as record
public class ProductViewResponse {

	// The initial resquet
	ProductViewRequest request;
	
	RequestMetadata metadatas;
	
	
	public ProductViewResponse(ProductViewRequest productViewRequest) {
		this.request = productViewRequest;
	}


	private long gtin;

	// TODO : add other fields

}
