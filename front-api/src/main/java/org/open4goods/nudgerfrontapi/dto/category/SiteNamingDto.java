package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing the localised site naming configuration.
 */
public record SiteNamingDto(
        @Schema(description = "Localised server name used for the site.")
        String serverName,
        @Schema(description = "Localised base URL associated with the server.")
        String baseUrl
) {
}
