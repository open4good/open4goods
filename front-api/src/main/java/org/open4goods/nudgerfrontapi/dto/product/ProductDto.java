package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterField;

/**
 * DTO representing a comprehensive product view returned by the API.
 */
public record ProductDto(
        @Schema(description = "Product GTIN, it is the unique identifier", example = "7612345678901")
        long gtin,
        @Schema(description = "Canonical product URL previously exposed as names.url", example = "https://example.org/products/123")
        String slug,
        @Schema(description = "Fully qualified slug composed of the vertical home URL and the product slug", example = "/telephones-reconditionnes/fairphone-4")
        String fullSlug,
        @Schema(description = "Basic product metadata")
        ProductBaseDto base,
        @Schema(description = "Identity facet exposing brand, model and alternate identifiers")
        ProductIdentityDto identity,
        @Schema(description = "Localised textual information resolved using the domainLanguage query parameter when available.")
        ProductNamesDto names,
        @Schema(description = "Structured attributes aggregated from all datasources")
        ProductAttributesDto attributes,
        @Schema(description = "Associated media resources")
        ProductResourcesDto resources,
        @Schema(description = "Datasource related information")
        ProductDatasourcesDto datasources,
        @Schema(description = "Score and ranking related information")
        ProductScoresDto scores,
        @Schema(description = "AI generated review resolved for the requested domain language")
        ProductAiReviewDto aiReview,
        @Schema(description = "EPREL product information when available", implementation = ProductEprelDto.class)
        ProductEprelDto eprel,
        @Schema(description = "Product offers and pricing information")
        ProductOffersDto offers,
        @Schema(description = "Lifecycle timeline combining price history and EPREL milestones")
        ProductTimelineDto timeline
) {

        /**
         * Available facets on ProductsDto.
         */
        public enum ProductDtoComponent {
                base,
                identity,
                names,
                attributes,
                resources,
                datasources,
                scores,
                aiReview,
                aiTexts,
                eprel,
                offers,
                timeline
        }

        /**
         * Allowed sort values on Products. (must exactly match elastic mapping)
         */
        public enum ProductDtoSortableFields {
                price("price.minPrice.price"),
                offersCount("offersCount"),
                brand("attributes.referentielAttributes.BRAND"),
                model("attributes.referentielAttributes.MODEL"),
                ecoscore("scores.ECOSCORE.value");


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

        /**
         * Allowed filter fields on Products. (must match elastic mapping)
         */
        public enum ProductDtoFilterFields {
                price(FilterField.price),
                offersCount(FilterField.offersCount),
                condition(FilterField.condition),
                brand(FilterField.brand),
                country(FilterField.country),
                ecoscore(FilterField.ecoscore);

                private final FilterField delegate;

                ProductDtoFilterFields(FilterField delegate) {
                        this.delegate = delegate;
                }

                public String getText() {
                        return delegate.fieldPath();
                }

                public FilterField getDelegate() {
                        return delegate;
                }

                @Override
                public String toString() {
                        return delegate.fieldPath();
                }

                private static final Map<String, ProductDtoFilterFields> LOOKUP = Arrays.stream(values())
                                .collect(Collectors.toMap(ProductDtoFilterFields::getText, e -> e));

                public static Optional<ProductDtoFilterFields> fromText(String text) {
                        return Optional.ofNullable(LOOKUP.get(text));
                }
        }
}
