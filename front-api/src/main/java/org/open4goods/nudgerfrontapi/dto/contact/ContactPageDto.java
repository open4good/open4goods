package org.open4goods.nudgerfrontapi.dto.contact;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Static metadata returned for the contact page.
 */
public record ContactPageDto(
        @Schema(description = "Identifier of the contact page used by the legacy UI.", example = "nous contacter")
        String page) {
}
