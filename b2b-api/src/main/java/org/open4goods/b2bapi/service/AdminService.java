package org.open4goods.b2bapi.service;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.open4goods.b2bapi.dto.ApiKeyDto;
import org.open4goods.b2bapi.dto.admin.AdminAuditEventDto;
import org.open4goods.b2bapi.dto.admin.AdminCreditGrantResponseDto;
import org.open4goods.b2bapi.dto.admin.AdminManualGrantRequest;
import org.open4goods.b2bapi.dto.admin.AdminOrganizationDto;
import org.open4goods.b2bapi.dto.admin.AdminUsageEventDto;
import org.open4goods.b2bapi.dto.billing.B2bTransactionDto;
import org.open4goods.b2bapi.exception.ResourceNotFoundException;
import org.open4goods.b2bapi.model.AdminAuditEvent;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.CreditTransaction;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.model.UsageEvent;
import org.open4goods.b2bapi.repository.AdminAuditEventRepository;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UsageEventRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service providing administrative functions for platform oversight.
 */
@Service
@ConditionalOnBean(name = "entityManagerFactory")
public class AdminService {

    private final OrganizationRepository organizationRepository;
    private final CreditBucketRepository creditBucketRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CreditGrantService creditGrantService;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final AdminAuditEventRepository adminAuditEventRepository;
    private final UsageEventRepository usageEventRepository;
    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final Clock clock;

    @Autowired
    public AdminService(
            final OrganizationRepository organizationRepository,
            final CreditBucketRepository creditBucketRepository,
            final CreditTransactionRepository creditTransactionRepository,
            final CreditGrantService creditGrantService,
            final ApiKeyRepository apiKeyRepository,
            final UserRepository userRepository,
            final AdminAuditEventRepository adminAuditEventRepository,
            final UsageEventRepository usageEventRepository,
            final ApiKeyAuthenticationService apiKeyAuthenticationService) {
        this(
                organizationRepository,
                creditBucketRepository,
                creditTransactionRepository,
                creditGrantService,
                apiKeyRepository,
                userRepository,
                adminAuditEventRepository,
                usageEventRepository,
                apiKeyAuthenticationService,
                Clock.systemUTC()
        );
    }

    AdminService(
            final OrganizationRepository organizationRepository,
            final CreditBucketRepository creditBucketRepository,
            final CreditTransactionRepository creditTransactionRepository,
            final CreditGrantService creditGrantService,
            final ApiKeyRepository apiKeyRepository,
            final UserRepository userRepository,
            final AdminAuditEventRepository adminAuditEventRepository,
            final UsageEventRepository usageEventRepository,
            final ApiKeyAuthenticationService apiKeyAuthenticationService,
            final Clock clock) {
        this.organizationRepository = organizationRepository;
        this.creditBucketRepository = creditBucketRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.creditGrantService = creditGrantService;
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.adminAuditEventRepository = adminAuditEventRepository;
        this.usageEventRepository = usageEventRepository;
        this.apiKeyAuthenticationService = apiKeyAuthenticationService;
        this.clock = clock;
    }

    /**
     * Retrieve all billable organizations along with their current credit balances.
     */
    @Transactional(readOnly = true)
    public List<AdminOrganizationDto> listOrganizations() {
        return organizationRepository.findAll().stream()
                .map(this::toAdminOrgDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve details of a specific organization.
     */
    @Transactional(readOnly = true)
    public AdminOrganizationDto getOrganization(final UUID organizationId) {
        final Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));
        return toAdminOrgDto(org);
    }

    /**
     * Retrieve recent transactions ledger for a specific organization.
     */
    @Transactional(readOnly = true)
    public List<B2bTransactionDto> getOrganizationTransactions(final UUID organizationId, final int limit) {
        // Ensure organization exists
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found.");
        }
        final List<CreditTransaction> txs = creditTransactionRepository.findByOrganizationId(organizationId, limit);
        return txs.stream()
                .map(t -> new B2bTransactionDto(
                        t.getId(),
                        t.getType() != null ? t.getType().name() : null,
                        t.getCredits(),
                        t.getFacetId(),
                        t.getGtin(),
                        t.getRequestId(),
                        t.getNote(),
                        t.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Grant manual credits to a customer organization.
     */
    @Transactional
    public AdminCreditGrantResponseDto grantManualCredits(
            final UUID organizationId,
            final DashboardPrincipal actor,
            final AdminManualGrantRequest request) {
        final CreditGrantResult result = creditGrantService.grantManual(
                organizationId,
                actor.userId(),
                request.credits(),
                request.expiresAt(),
                request.note()
        );
        return new AdminCreditGrantResponseDto(
                result.bucketId(),
                result.creditsGranted(),
                result.durableBalance()
        );
    }

    /**
     * Retrieve all API keys across all organizations.
     */
    @Transactional(readOnly = true)
    public List<ApiKeyDto> listApiKeys() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toApiKeyDto)
                .collect(Collectors.toList());
    }

    /**
     * Revoke any API key as a platform admin.
     */
    @Transactional
    public ApiKeyDto revokeApiKey(final DashboardPrincipal actor, final UUID apiKeyId) {
        final ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found."));

        if (apiKey.getStatus() == ApiKeyStatus.ACTIVE) {
            apiKey.setStatus(ApiKeyStatus.REVOKED);
            apiKey.setRevokedAt(clock.instant());
            apiKeyRepository.save(apiKey);

            if (apiKeyAuthenticationService != null) {
                apiKeyAuthenticationService.evictHash(apiKey.getKeyHash());
            }

            final User actorUser = userRepository.findById(actor.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Actor user not found."));

            final AdminAuditEvent audit = new AdminAuditEvent(actorUser, "API_KEY_REVOKE");
            audit.setTargetOrganization(apiKey.getOrganization());
            audit.setTargetRef(apiKey.getId().toString());
            final Map<String, Object> details = new LinkedHashMap<>();
            details.put("apiKeyId", apiKey.getId().toString());
            details.put("name", apiKey.getName());
            details.put("keyPrefix", apiKey.getKeyPrefix());
            audit.setDetail(details);
            audit.setCreatedAt(clock.instant());
            adminAuditEventRepository.insert(audit);
        }

        return toApiKeyDto(apiKey);
    }

    /**
     * Retrieve recent global request usage events.
     */
    @Transactional(readOnly = true)
    public List<AdminUsageEventDto> listUsage(final int limit) {
        final List<UsageEvent> events = usageEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        return events.stream()
                .map(e -> new AdminUsageEventDto(
                        e.getId(),
                        e.getOrganization().getId(),
                        e.getOrganization().getName(),
                        e.getApiKey() != null ? e.getApiKey().getId() : null,
                        e.getApiKey() != null ? e.getApiKey().getKeyPrefix() : null,
                        e.getFacetId(),
                        e.getGtin(),
                        e.getRequestId(),
                        e.getHttpStatus(),
                        e.isBillable(),
                        e.getCreditsConsumed(),
                        e.getNoPayReason(),
                        e.getResponseTimeMs(),
                        e.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve recent platform audit events.
     */
    @Transactional(readOnly = true)
    public List<AdminAuditEventDto> listAuditEvents(final int limit) {
        final List<AdminAuditEvent> events = adminAuditEventRepository.findAllRecent(limit);
        return events.stream()
                .map(e -> new AdminAuditEventDto(
                        e.getId(),
                        e.getActorUser().getId(),
                        e.getActorUser().getEmail(),
                        e.getAction(),
                        e.getTargetOrganization() != null ? e.getTargetOrganization().getId() : null,
                        e.getTargetOrganization() != null ? e.getTargetOrganization().getName() : null,
                        e.getTargetRef(),
                        e.getDetail(),
                        e.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private AdminOrganizationDto toAdminOrgDto(final Organization org) {
        final long balance = creditBucketRepository.sumLiveCredits(org.getId());
        return new AdminOrganizationDto(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getBillingEmail(),
                org.getDefaultLanguage(),
                org.getStatus(),
                org.isFreeGrantApplied(),
                balance,
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }

    private ApiKeyDto toApiKeyDto(final ApiKey apiKey) {
        return new ApiKeyDto(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.getStatus(),
                apiKey.getCreatedBy().getId(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getRevokedAt()
        );
    }
}
