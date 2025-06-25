package org.open4goods.nudgerfrontapi.dto.product;

/**
 * Frontend view of a product. We make the choixe to wear the mapping logic in the DTO, adapted with our "composition" principe
 */
import io.swagger.v3.oas.annotations.media.Schema;

public class ProductDto extends AbstractDTO {

		public enum ProductDtoComponent {
			aiReview,
			offers,
			images
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
