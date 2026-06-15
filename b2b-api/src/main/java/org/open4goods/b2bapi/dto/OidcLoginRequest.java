package org.open4goods.b2bapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.open4goods.b2bapi.model.OidcProvider;

/**
 * Dashboard login request carrying a provider token.
 *
 * @param provider external identity provider
 * @param idToken OIDC ID token, or GitHub OAuth access token for GitHub
 */
public record OidcLoginRequest(
        @NotNull OidcProvider provider,
        @NotBlank String idToken) {
}
