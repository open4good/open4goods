package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard envelope for successful Product Data API responses.
 *
 * @param data endpoint-specific sanitized payload
 * @param meta response metadata and metering details
 * @param <T> endpoint-specific payload type
 */
@Schema(description = "Standard envelope for successful Product Data API responses.")
public record B2bResponse<T>(
        @Schema(description = "Endpoint-specific sanitized payload.")
        T data,
        @Schema(description = "Response metadata and metering details.")
        B2bMeta meta) {
}
