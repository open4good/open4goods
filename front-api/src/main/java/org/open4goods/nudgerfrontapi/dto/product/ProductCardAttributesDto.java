package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Attributes facet of a {@link ProductCardDto}.
 *
 * <p>Indexed attributes are limited to the keys a listing displays: the vertical popular attributes, its
 * technical and eco filters (table columns) and the attribute of the active sort.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardAttributesDto(
        @Schema(description = "Referential attributes (brand, model, GTIN) keyed by attribute name")
        Map<String, String> referentialAttributes,
        @Schema(description = "Indexed attributes displayed by listings, keyed by attribute key")
        Map<String, ProductCardAttributeDto> indexedAttributes
) {
}
