package org.open4goods.nudgerfrontapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/** Provider token received from the server-side Nuxt OAuth callback. */
public record GoogleLoginRequest(
        @Schema(description = "Google OpenID Connect ID token", requiredMode = Schema.RequiredMode.REQUIRED)
        String idToken) { }
