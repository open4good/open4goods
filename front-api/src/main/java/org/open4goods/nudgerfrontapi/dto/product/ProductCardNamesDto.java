package org.open4goods.nudgerfrontapi.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Names facet of a {@link ProductCardDto}: the localised names rendered by cards and list rows.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardNamesDto(
        @Schema(description = "Localised display name (short title)", nullable = true)
        String displayName,
        @Schema(description = "Localised card name", nullable = true)
        String cardName
) {
}
