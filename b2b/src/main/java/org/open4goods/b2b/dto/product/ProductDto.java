package org.open4goods.b2b.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import model.facets.PriceFacet;

/**
 * DTO representing a product view returned by the API.
 */
public class ProductDto {

    @Schema(description = "Product GTIN, it is the unique identifier", example = "7612345678901")
    private long gtin;

    @Schema(implementation = ProductDto.class)
    private PriceFacet priceFacet;

    @Schema(implementation = RequestMetadata.class)
    private RequestMetadata requestMetadatas = new RequestMetadata();

    // --- Constructors ---
    public ProductDto() {
    }

    public ProductDto(long gtin) {
        this.gtin = gtin;
    }

    // --- Getters & Setters ---
    public long getGtin() {
        return gtin;
    }

    public void setGtin(long gtin) {
        this.gtin = gtin;
    }

    public PriceFacet getPriceFacet() {
        return priceFacet;
    }

    public void setPriceFacet(PriceFacet priceFacet) {
        this.priceFacet = priceFacet;
    }

    public RequestMetadata getRequestMetadatas() {
		return requestMetadatas;
	}

	public void setRequestMetadatas(RequestMetadata requestMetadata) {
		this.requestMetadatas = requestMetadata;
	}

	// --- toString ---
    @Override
    public String toString() {
        return "ProductDto{" +
                "gtin=" + gtin +
                '}';
    }
}
