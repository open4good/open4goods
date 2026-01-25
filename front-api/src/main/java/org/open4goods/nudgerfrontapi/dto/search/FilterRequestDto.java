package org.open4goods.nudgerfrontapi.dto.search;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Describes the filters that can be applied when searching for products.
 *
 * <p>The structure mirrors the JSON payload accepted by the {@code filters}
 * property of the {@code POST /products} request body. Clients can combine
 * multiple clauses. Legacy {@link #filters()} are evaluated with AND semantics
 * on the Elasticsearch query. {@link #filterGroups()} enables composing
 * conjunctions of grouped filters where each group can express its own AND and
 * OR clauses.</p>
 */
public record FilterRequestDto(
        @Schema(description = "Collection of filter clauses. When omitted no additional filtering is applied.")
        List<Filter> filters,
        @Schema(description = "Collection of filter groups combined with AND. Each group can mix mandatory (must) and optional (should) clauses.")
        List<FilterGroup> filterGroups) {

    /**
     * Groups filter clauses to preserve AND combinations before joining groups
     * with AND semantics.
     */
    public record FilterGroup(
            @Schema(description = "Filters that must all match within the group to be eligible for selection.")
            List<Filter> must,
            @Schema(description = "Filters evaluated with OR semantics within the group. This field also accepts the legacy 'filters' key for backward compatibility.")
            @JsonAlias("filters")
            List<Filter> should) {
    }

    /**
     * Describes a single filter clause sent by the client.
     */
    public record Filter(
            @Schema(description = "Field mapping targeted by the filter.", example = "price.minPrice.price")
            String field,
            @Schema(description = "Filtering strategy to apply.", implementation = FilterOperator.class)
            FilterOperator operator,
            @Schema(description = "Exact values accepted when the operator is <code>term</code>. Multiple values are combined with OR semantics.",
                    example = "[\"Fairphone\",\"Samsung\"]")
            List<String> terms,
            @Schema(description = "Inclusive lower bound used when the operator is <code>range</code>.", example = "100.0")
            Double min,
            @Schema(description = "Inclusive upper bound used when the operator is <code>range</code>.", example = "500.0")
            Double max) {
    }

    /**
     * Supported fields for product filtering along with the mapping to the Elasticsearch index.
     */
    public enum FilterField {
        @Schema(description = "Filter on the minimum price of the product", example = "price")
        price("price.minPrice.price", FilterValueType.numeric),
        @Schema(description = "Filter on the number of offers available", example = "offersCount")
        offersCount("offersCount", FilterValueType.numeric),
        @Schema(description = "Filter on the condition of the offers (e.g. NEW, USED)", example = "condition")
        condition("price.conditions", FilterValueType.keyword),
        @Schema(description = "Filter on the Google taxonomy identifier associated with the product", example = "1234")
        googleTaxonomyId("googleTaxonomyId", FilterValueType.numeric),
        @Schema(description = "Filter on the product brand reported by datasources", example = "brand")
        brand("attributes.referentielAttributes.BRAND", FilterValueType.keyword),
        @Schema(description = "Filter on the country resolved from the GTIN prefix", example = "country")
        country("gtinInfos.country", FilterValueType.keyword),
        @Schema(description = "Filter on the datasources contributing to the aggregation", example = "datasource")
        datasource("datasourceCodes", FilterValueType.keyword),
        @Schema(description = "Filter on the moderation causes applied to the product", example = "excludedCauses")
        excludedCauses("excludedCauses", FilterValueType.keyword),
        @Schema(description = "Filter on the product ecoscore", example = "ecoscore")
        ecoscore("scores.ECOSCORE.value", FilterValueType.numeric);

        private final String fieldPath;
        private final FilterValueType valueType;

        FilterField(String fieldPath, FilterValueType valueType) {
            this.fieldPath = fieldPath;
            this.valueType = valueType;
        }

        public String fieldPath() {
            return fieldPath;
        }

        /**
         * @return type of values supported by the field.
         */
        public FilterValueType valueType() {
            return valueType;
        }

        /**
         * Determine whether the field supports the requested operator.
         *
         * @param operator operator requested by the client
         * @return {@code true} when the operator is compatible with the field type
         */
        public boolean supports(FilterOperator operator) {
            return switch (valueType) {
            case keyword -> operator == FilterOperator.term;
            case numeric -> operator == FilterOperator.range || operator == FilterOperator.term;
            };
        }
        private static final Map<String, FilterField> LOOKUP = Arrays.stream(values())
                .collect(Collectors.toMap(FilterField::fieldPath, f -> f));

        public static Optional<FilterField> fromFieldPath(String fieldPath) {
            return Optional.ofNullable(LOOKUP.get(fieldPath));
        }
    }

    /**
     * Supported operator types for the filters.
     */
    public enum FilterOperator {
        @Schema(description = "Matches documents where the field equals one of the provided terms.")
        term,
        @Schema(description = "Matches documents where the field is within the inclusive range defined by min and/or max.")
        range
    }

    /**
     * Describes the nature of values supported by a filter field.
     */
    public enum FilterValueType {
        @Schema(description = "The field expects exact string values.")
        keyword,
        @Schema(description = "The field expects numeric values and supports range queries.")
        numeric
    }
}
