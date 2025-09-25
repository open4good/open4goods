package org.open4goods.nudgerfrontapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after a successful logout request.
 */
public record LogoutResponse(
        @Schema(description = "Indicates whether the logout request succeeded.")
        boolean success
) {
}
