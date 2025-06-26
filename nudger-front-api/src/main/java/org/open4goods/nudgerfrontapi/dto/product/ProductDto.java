package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.open4goods.nudgerfrontapi.dto.AbstractDTO;

import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

public class ProductDto extends AbstractDTO {

	/**
	 * Available facets on ProductsDto
	 */
	public enum ProductDtoComponent {
		aiReview, offers, images
	}

	/**
	 * Allowed sort values on Products. (must exactly match elastic mapping)
	 */
	public enum ProductDtoSortableFields {
		price("price.minPrice.price"),
		offersCount("offersCount");

		private final String text;

		ProductDtoSortableFields(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		@Override
		public String toString() {
			return text;
		}

		/**
		 * Optional: parse from the text value.
		 */
		private static final Map<String, ProductDtoSortableFields> LOOKUP = Arrays.stream(values()).collect(Collectors.toMap(ProductDtoSortableFields::getText, e -> e));

		public static Optional<ProductDtoSortableFields> fromText(String text) {
			return Optional.fromNullable(LOOKUP.get(text));
		}

	}

	@Schema(description = "Product GTIN, it is the unique identifier", example = "7612345678901")
	long gtin;

	private ProductAiReviewDto aiReview;

	private ProductOffersDto offers;

	private ProductImagesDto images;

	public long getGtin() {
		return gtin;
	}

	public void setGtin(long gtin) {
		this.gtin = gtin;
	}

	public ProductAiReviewDto getAiReview() {
		return aiReview;
	}

	public void setAiReview(ProductAiReviewDto aiReview) {
		this.aiReview = aiReview;
	}

	public ProductOffersDto getOffers() {
		return offers;
	}

	public void setOffers(ProductOffersDto offers) {
		this.offers = offers;
	}

	public ProductImagesDto getImages() {
		return images;
	}

	public void setImages(ProductImagesDto images) {
		this.images = images;
	}

}
