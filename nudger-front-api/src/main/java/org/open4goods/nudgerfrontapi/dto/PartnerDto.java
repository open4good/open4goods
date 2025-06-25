package org.open4goods.nudgerfrontapi.dto;

/**
 * Partner information shown in partners list.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record PartnerDto(
        @Schema(description = "Partner display name", example = "WWF")
        String name,

        @Schema(description = "Partner website", format = "uri", example = "https://wwf.fr")
        String url) {
}
