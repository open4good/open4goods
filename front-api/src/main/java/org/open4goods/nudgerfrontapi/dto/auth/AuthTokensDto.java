package org.open4goods.nudgerfrontapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tokens returned after successful authentication.
 */
public record AuthTokensDto(
        @Schema(description = "JWT access token")
        String accessToken,
        @Schema(description = "JWT refresh token")
        String refreshToken
) {
}
