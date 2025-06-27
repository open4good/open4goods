package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.open4goods.nudgerfrontapi.dto.AbstractDTO;

import io.swagger.v3.oas.annotations.media.Schema;

public class ProductDto extends AbstractDTO {

	/**
	 * Available facets on ProductsDto
	 */
        public enum ProductDtoComponent {
                base,
                names,
                resources,
                aiTexts,
                aiReview,
                offers
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
			return Optional.ofNullable(LOOKUP.get(text));
		}

	}

	@Schema(description = "Product GTIN, it is the unique identifier", example = "7612345678901")
	long gtin;

        private ProductBaseDto base;

        private ProductNamesDto names;

        private ProductResourcesDto resources;

        private ProductAiTextsDto aiTexts;

        private ProductAiReviewDto aiReview;

        private ProductOffersDto offers;

	public long getGtin() {
		return gtin;
	}

	public void setGtin(long gtin) {
		this.gtin = gtin;
	}

        public ProductBaseDto getBase() {
                return base;
        }

        public void setBase(ProductBaseDto base) {
                this.base = base;
        }

        public ProductNamesDto getNames() {
                return names;
        }

        public void setNames(ProductNamesDto names) {
                this.names = names;
        }

        public ProductResourcesDto getResources() {
                return resources;
        }

        public void setResources(ProductResourcesDto resources) {
                this.resources = resources;
        }

        public ProductAiTextsDto getAiTexts() {
                return aiTexts;
        }

        public void setAiTexts(ProductAiTextsDto aiTexts) {
                this.aiTexts = aiTexts;
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

}
