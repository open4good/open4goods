package org.open4goods.services.reviewgeneration.dto;

import java.util.Objects;

import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * DTO representing a review generation request.
 */
public class ReviewRequest {

    private Product product;
    
    /**
     * Vertical configuration (provided as an object; replace with a concrete type if available).
     */
    private VerticalConfig verticalConfig;

    // Getters and setters

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }


    public VerticalConfig getVerticalConfig() {
		return verticalConfig;
	}

	public void setVerticalConfig(VerticalConfig verticalConfig) {
		this.verticalConfig = verticalConfig;
	}

	@Override
    public String toString() {
        return "ReviewRequest{" +
                "product=" + product +
                ", verticalConfig=" + verticalConfig +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, verticalConfig);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReviewRequest)) return false;
        ReviewRequest that = (ReviewRequest) o;
        return Objects.equals(product, that.product) &&
               Objects.equals(verticalConfig, that.verticalConfig);
    }
}
