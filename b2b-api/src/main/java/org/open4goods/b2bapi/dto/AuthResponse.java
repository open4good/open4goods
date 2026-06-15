package org.open4goods.b2bapi.dto;

import java.time.Instant;
import java.util.UUID;
import org.open4goods.b2bapi.model.OrganizationRole;

/**
 * Dashboard session response returned after login or refresh.
 *
 * @param accessToken bearer-compatible access JWT
 * @param accessExpiresAt access token expiration instant
 * @param refreshToken bearer-compatible refresh JWT
 * @param refreshExpiresAt refresh token expiration instant
 * @param user current user
 * @param organization active organization
 * @param role current user's role in the active organization
 */
public record AuthResponse(
        String accessToken,
        Instant accessExpiresAt,
        String refreshToken,
        Instant refreshExpiresAt,
        AuthUserDto user,
        AuthOrganizationDto organization,
        OrganizationRole role) {

    /**
     * Sanitized dashboard user payload.
     *
     * @param id user id
     * @param email email address
     * @param displayName display name
     * @param avatarUrl avatar URL
     * @param platformAdmin true when the user is in the platform admin allowlist
     */
    public record AuthUserDto(
            UUID id,
            String email,
            String displayName,
            String avatarUrl,
            boolean platformAdmin) {
    }

    /**
     * Active organization payload.
     *
     * @param id organization id
     * @param name organization display name
     * @param slug organization slug
     * @param balanceCredits authoritative live credit balance
     */
    public record AuthOrganizationDto(
            UUID id,
            String name,
            String slug,
            long balanceCredits) {
    }
}
