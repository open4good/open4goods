package org.open4goods.nudgerfrontapi.dto;

/**
 * Refresh token request.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshRequest(
        @Schema(description = "Previous refresh token", example = "abcd1234")
        String refreshToken) {
}
