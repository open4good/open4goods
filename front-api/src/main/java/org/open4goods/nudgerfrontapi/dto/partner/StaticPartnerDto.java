package org.open4goods.nudgerfrontapi.dto.partner;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing a static partner configured from application properties.
 */
public record StaticPartnerDto(
        @Schema(description = "Display name of the partner.", example = "French Tech Ouest")
        String name,

        @Schema(description = "XWiki bloc identifier containing the partner content.",
                example = "pages:ecosystem:french-tech")
        String blocId,

        @Schema(description = "Public URL pointing to the partner website.",
                example = "https://www.ft-brestbretagneouest.bzh/")
        String url,

        @Schema(description = "Relative path to the partner image served by the frontend.",
                example = "/images/ecosystem/frenchTech.jpeg")
        String imageUrl) {
}
