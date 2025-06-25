package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import org.open4goods.model.price.AggregatedPrice;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Pricing information for a product.
 */
public record ProductOffersDto(
        @Schema(description = "List of offers sorted by price", example = "[]")
        List<AggregatedPrice> offers) {}
