package org.open4goods.nudgerfrontapi.dto;

/**
 * Authentication request containing credentials.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthRequest(
        @Schema(description = "User login name", example = "user@example.com")
        String username,

        @Schema(description = "Plain-text password", example = "secret")
        String password) {
}
