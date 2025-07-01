package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing a product view returned by the API.
 */
public record ProductDto(
        @Schema(description = "Product GTIN, it is the unique identifier", example = "7612345678901")
        long gtin,
        @Schema(description = "Basic product metadata")
        ProductBaseDto base,
        @Schema(description = "Localised textual information")
        ProductNamesDto names,
        @Schema(description = "Associated media resources")
        ProductResourcesDto resources,
        @Schema(description = "AI generated texts")
        ProductAiTextsDto aiTexts,
        @Schema(description = "AI-generated review")
        ProductAiReviewDto aiReview,
        @Schema(description = "Product offers")
        ProductOffersDto offers
) {

	/**
	 * Available facets on ProductsDto
	 */
        public enum ProductDtoComponent {
                base,
                names,
                resources,
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
}

        /**
         * Allowed aggregation fields.
         */
        public enum ProductDtoAggregatableFields {
                vertical("vertical"),
                taxonomy("googleTaxonomyId"),
                country("gtinInfos.country");

                private final String text;

                ProductDtoAggregatableFields(String text) {
                        this.text = text;
                }

                public String getText() {
                        return text;
                }

                @Override
                public String toString() {
                        return text;
                }

                private static final Map<String, ProductDtoAggregatableFields> LOOKUP =
                        Arrays.stream(values()).collect(Collectors.toMap(ProductDtoAggregatableFields::getText, e -> e));

                public static Optional<ProductDtoAggregatableFields> fromText(String text) {
                        return Optional.ofNullable(LOOKUP.get(text));
                }
        }
}
