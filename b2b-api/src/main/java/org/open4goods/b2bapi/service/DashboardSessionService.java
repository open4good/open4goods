package org.open4goods.b2bapi.service;

import org.open4goods.b2bapi.dto.AuthResponse;
import org.open4goods.b2bapi.exception.InvalidCredentialsException;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.OrganizationMemberRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads and refreshes dashboard sessions from verified JWT claims.
 */
@Service
public class DashboardSessionService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final CreditBucketRepository creditBucketRepository;
    private final JwtTokenService jwtTokenService;

    public DashboardSessionService(
            final UserRepository userRepository,
            final OrganizationRepository organizationRepository,
            final OrganizationMemberRepository organizationMemberRepository,
            final CreditBucketRepository creditBucketRepository,
            final JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.creditBucketRepository = creditBucketRepository;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * Loads the current authenticated account from an access JWT.
     *
     * @param accessToken signed access JWT
     * @return current account state
     */
    @Transactional(readOnly = true)
    public ProvisionedAccount currentAccount(final String accessToken) {
        return loadAccount(accessToken, JwtTokenType.ACCESS);
    }

    /**
     * Issues a fresh token pair from a valid refresh JWT.
     *
     * @param refreshToken signed refresh JWT
     * @return response containing fresh tokens and account state
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(final String refreshToken) {
        final ProvisionedAccount account = loadAccount(refreshToken, JwtTokenType.REFRESH);
        return toAuthResponse(account, jwtTokenService.issueTokenPair(account.user(), account.organization()));
    }

    /**
     * Converts account state and JWTs to the public auth response shape.
     *
     * @param account active account state
     * @param tokens token pair
     * @return auth response DTO
     */
    public AuthResponse toAuthResponse(final ProvisionedAccount account, final JwtTokenPair tokens) {
        return new AuthResponse(
                tokens.accessToken(),
                tokens.accessExpiresAt(),
                tokens.refreshToken(),
                tokens.refreshExpiresAt(),
                new AuthResponse.AuthUserDto(
                        account.user().getId(),
                        account.user().getEmail(),
                        account.user().getDisplayName(),
                        account.user().getAvatarUrl(),
                        account.user().isPlatformAdmin()),
                new AuthResponse.AuthOrganizationDto(
                        account.organization().getId(),
                        account.organization().getName(),
                        account.organization().getSlug(),
                        account.balanceCredits()),
                account.role());
    }

    private ProvisionedAccount loadAccount(final String token, final JwtTokenType tokenType) {
        try {
            final JwtTokenClaims claims = jwtTokenService.verify(token, tokenType);
            final var user = userRepository.findById(claims.userId())
                    .orElseThrow(() -> new InvalidCredentialsException("Session user no longer exists."));
            final var organization = organizationRepository.findById(claims.organizationId())
                    .orElseThrow(() -> new InvalidCredentialsException("Session organization no longer exists."));
            final var membership = organizationMemberRepository
                    .findByOrganizationIdAndUserId(organization.getId(), user.getId())
                    .orElseThrow(() -> new InvalidCredentialsException("Session organization membership no longer exists."));
            final long balanceCredits = creditBucketRepository.sumLiveCredits(organization.getId());
            return new ProvisionedAccount(user, organization, membership.getRole(), balanceCredits);
        } catch (final IllegalArgumentException exception) {
            throw new InvalidCredentialsException("Invalid session token.", exception);
        }
    }
}
