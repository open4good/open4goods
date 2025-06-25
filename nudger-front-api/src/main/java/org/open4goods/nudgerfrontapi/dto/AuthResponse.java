package org.open4goods.nudgerfrontapi.dto;

/**
 * Authentication response containing a bearer token.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "JWT bearer token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token) {
}
