package org.open4goods.nudgerfrontapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Login request payload.
 */
public record LoginRequest(
        @Schema(description = "XWiki username", example = "john")
        String username,
        @Schema(description = "XWiki password", example = "secret")
        String password
) {
}
