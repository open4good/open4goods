package org.open4goods.b2bapi.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.open4goods.b2bapi.dto.ApiKeyDto;
import org.open4goods.b2bapi.dto.ApiKeySecretResponse;
import org.open4goods.b2bapi.exception.ResourceNotFoundException;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, lists, rotates, and revokes hashed Product Data API keys.
 */
@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ApiKeySecretGenerator secretGenerator;
    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final Clock clock;

    @Autowired
    public ApiKeyService(
            final ApiKeyRepository apiKeyRepository,
            final UserRepository userRepository,
            final OrganizationRepository organizationRepository,
            final ApiKeySecretGenerator secretGenerator,
            final ObjectProvider<ApiKeyAuthenticationService> apiKeyAuthenticationService) {
        this(
                apiKeyRepository,
                userRepository,
                organizationRepository,
                secretGenerator,
                apiKeyAuthenticationService.getIfAvailable(),
                Clock.systemUTC());
    }

    ApiKeyService(
            final ApiKeyRepository apiKeyRepository,
            final UserRepository userRepository,
            final OrganizationRepository organizationRepository,
            final ApiKeySecretGenerator secretGenerator,
            final ApiKeyAuthenticationService apiKeyAuthenticationService,
            final Clock clock) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.secretGenerator = secretGenerator;
        this.apiKeyAuthenticationService = apiKeyAuthenticationService;
        this.clock = clock;
    }

    @Transactional
    public ApiKeySecretResponse create(final DashboardPrincipal principal, final String name) {
        requireKeyManager(principal);
        final var user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        final var organization = organizationRepository.findById(principal.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));
        final ApiKeySecret secret = secretGenerator.generate();
        final ApiKey apiKey = new ApiKey(organization, user, name.trim(), secret.keyPrefix(), secret.keyHash());
        apiKey.setCreatedAt(clock.instant());
        final ApiKey saved = apiKeyRepository.save(apiKey);
        return new ApiKeySecretResponse(toDto(saved), secret.clearKey());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDto> list(final DashboardPrincipal principal) {
        requireKeyReader(principal);
        final List<ApiKey> apiKeys = switch (principal.role()) {
            case OWNER, ADMIN -> apiKeyRepository.findByOrganizationIdOrderByCreatedAtDesc(principal.organizationId());
            case DEVELOPER -> apiKeyRepository.findByOrganizationIdAndCreatedByIdOrderByCreatedAtDesc(
                    principal.organizationId(), principal.userId());
            case BILLING -> throw new AccessDeniedException("Billing members cannot list API keys.");
        };
        return apiKeys.stream().map(this::toDto).toList();
    }

    @Transactional
    public ApiKeySecretResponse rotate(final DashboardPrincipal principal, final UUID apiKeyId) {
        final ApiKey existing = findOrgKey(apiKeyId, principal.organizationId());
        requireCanMutate(principal, existing);
        if (existing.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new AccessDeniedException("Only active API keys can be rotated.");
        }
        final Instant now = clock.instant();
        existing.setStatus(ApiKeyStatus.ROTATED);
        existing.setRevokedAt(now);
        apiKeyRepository.save(existing);
        evictCachedKey(existing);

        final ApiKeySecret secret = secretGenerator.generate();
        final ApiKey replacement = new ApiKey(
                existing.getOrganization(),
                existing.getCreatedBy(),
                existing.getName(),
                secret.keyPrefix(),
                secret.keyHash());
        replacement.setRotatedFrom(existing);
        replacement.setCreatedAt(now);
        final ApiKey savedReplacement = apiKeyRepository.save(replacement);
        return new ApiKeySecretResponse(toDto(savedReplacement), secret.clearKey());
    }

    @Transactional
    public ApiKeyDto revoke(final DashboardPrincipal principal, final UUID apiKeyId) {
        final ApiKey apiKey = findOrgKey(apiKeyId, principal.organizationId());
        requireCanMutate(principal, apiKey);
        if (apiKey.getStatus() == ApiKeyStatus.ACTIVE) {
            apiKey.setStatus(ApiKeyStatus.REVOKED);
            apiKey.setRevokedAt(clock.instant());
            apiKeyRepository.save(apiKey);
            evictCachedKey(apiKey);
        }
        return toDto(apiKey);
    }

    private ApiKey findOrgKey(final UUID apiKeyId, final UUID organizationId) {
        return apiKeyRepository.findByIdAndOrganizationId(apiKeyId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found."));
    }

    private void requireKeyManager(final DashboardPrincipal principal) {
        if (principal.role() == OrganizationRole.BILLING) {
            throw new AccessDeniedException("Billing members cannot manage API keys.");
        }
    }

    private void requireKeyReader(final DashboardPrincipal principal) {
        if (principal.role() == OrganizationRole.BILLING) {
            throw new AccessDeniedException("Billing members cannot read API keys.");
        }
    }

    private void requireCanMutate(final DashboardPrincipal principal, final ApiKey apiKey) {
        if (principal.role() == OrganizationRole.OWNER || principal.role() == OrganizationRole.ADMIN) {
            return;
        }
        if (principal.role() == OrganizationRole.DEVELOPER && apiKey.getCreatedBy().getId().equals(principal.userId())) {
            return;
        }
        throw new AccessDeniedException("API key mutation is not allowed.");
    }

    private void evictCachedKey(final ApiKey apiKey) {
        if (apiKeyAuthenticationService != null) {
            apiKeyAuthenticationService.evictHash(apiKey.getKeyHash());
        }
    }

    private ApiKeyDto toDto(final ApiKey apiKey) {
        return new ApiKeyDto(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.getStatus(),
                apiKey.getCreatedBy().getId(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getRevokedAt());
    }
}
