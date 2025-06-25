package org.open4goods.nudgerfrontapi.dto;

/**
 * Request object used when retrieving a product view.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductViewRequest(
        @Schema(description = "Global Trade Item Number", example = "7612345678901")
        Long gtin) {
}
