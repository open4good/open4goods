package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.b2bapi.dto.ApiKeyDto;
import org.open4goods.b2bapi.dto.admin.AdminManualGrantRequest;
import org.open4goods.b2bapi.exception.ResourceNotFoundException;
import org.open4goods.b2bapi.model.AdminAuditEvent;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.CreditTransaction;
import org.open4goods.b2bapi.model.CreditTransactionType;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.model.OrganizationStatus;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.model.UsageEvent;
import org.open4goods.b2bapi.repository.AdminAuditEventRepository;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UsageEventRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private CreditBucketRepository creditBucketRepository;

    @Mock
    private CreditTransactionRepository creditTransactionRepository;

    @Mock
    private CreditGrantService creditGrantService;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminAuditEventRepository adminAuditEventRepository;

    @Mock
    private UsageEventRepository usageEventRepository;

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    private AdminService adminService;
    private Organization organization;
    private User actorUser;
    private DashboardPrincipal actorPrincipal;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                organizationRepository,
                creditBucketRepository,
                creditTransactionRepository,
                creditGrantService,
                apiKeyRepository,
                userRepository,
                adminAuditEventRepository,
                usageEventRepository,
                apiKeyAuthenticationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        organization = new Organization("Target Org", "target-org");
        organization.setId(UUID.randomUUID());
        organization.setStatus(OrganizationStatus.ACTIVE);

        actorUser = new User("admin@platform.com", OidcProvider.GOOGLE, "admin_sub");
        actorUser.setId(UUID.randomUUID());

        actorPrincipal = new DashboardPrincipal(
                actorUser.getId(),
                organization.getId(),
                actorUser.getEmail(),
                true,
                OrganizationRole.OWNER
        );
    }

    @Test
    void listOrganizationsReturnsMappedDtos() {
        when(organizationRepository.findAll()).thenReturn(List.of(organization));
        when(creditBucketRepository.sumLiveCredits(organization.getId())).thenReturn(3500L);

        final var list = adminService.listOrganizations();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).name()).isEqualTo("Target Org");
        assertThat(list.get(0).creditBalance()).isEqualTo(3500L);
    }

    @Test
    void getOrganizationReturnsDetails() {
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(creditBucketRepository.sumLiveCredits(organization.getId())).thenReturn(100L);

        final var dto = adminService.getOrganization(organization.getId());

        assertThat(dto.name()).isEqualTo("Target Org");
        assertThat(dto.creditBalance()).isEqualTo(100L);
    }

    @Test
    void getOrganizationThrowsWhenNotFound() {
        final UUID randomId = UUID.randomUUID();
        when(organizationRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getOrganization(randomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrganizationTransactionsReturnsList() {
        when(organizationRepository.existsById(organization.getId())).thenReturn(true);
        final CreditTransaction tx = new CreditTransaction(organization, CreditTransactionType.GRANT, 1000);
        tx.setId(UUID.randomUUID());
        tx.setCreatedAt(NOW);

        when(creditTransactionRepository.findByOrganizationId(organization.getId(), 50))
                .thenReturn(List.of(tx));

        final var txs = adminService.getOrganizationTransactions(organization.getId(), 50);

        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).type()).isEqualTo("GRANT");
        assertThat(txs.get(0).credits()).isEqualTo(1000);
    }

    @Test
    void getOrganizationTransactionsThrowsWhenOrgNotFound() {
        final UUID randomId = UUID.randomUUID();
        when(organizationRepository.existsById(randomId)).thenReturn(false);

        assertThatThrownBy(() -> adminService.getOrganizationTransactions(randomId, 50))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void grantManualCreditsDelegatesToCreditGrantService() {
        final AdminManualGrantRequest request = new AdminManualGrantRequest(5000L, "Test grant", null);
        final CreditGrantResult result = new CreditGrantResult(UUID.randomUUID(), 5000L, 0L, 7500L, false);

        when(creditGrantService.grantManual(organization.getId(), actorUser.getId(), 5000L, null, "Test grant"))
                .thenReturn(result);

        final var response = adminService.grantManualCredits(organization.getId(), actorPrincipal, request);

        assertThat(response.bucketId()).isEqualTo(result.bucketId());
        assertThat(response.creditsGranted()).isEqualTo(5000L);
        assertThat(response.durableBalance()).isEqualTo(7500L);
    }

    @Test
    void listApiKeysReturnsSortedKeys() {
        final ApiKey apiKey = new ApiKey(organization, actorUser, "Key name", "pdapi_abc", "hash");
        apiKey.setId(UUID.randomUUID());
        apiKey.setCreatedAt(NOW);

        when(apiKeyRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(apiKey));

        final var keys = adminService.listApiKeys();

        assertThat(keys).hasSize(1);
        assertThat(keys.get(0).name()).isEqualTo("Key name");
        assertThat(keys.get(0).keyPrefix()).isEqualTo("pdapi_abc");
    }

    @Test
    void revokeApiKeyChangesStatusToRevokedAndAudits() {
        final ApiKey apiKey = new ApiKey(organization, actorUser, "Key name", "pdapi_abc", "hash_to_evict");
        apiKey.setId(UUID.randomUUID());
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKey.setCreatedAt(NOW);

        when(apiKeyRepository.findById(apiKey.getId())).thenReturn(Optional.of(apiKey));
        when(userRepository.findById(actorUser.getId())).thenReturn(Optional.of(actorUser));

        final ApiKeyDto result = adminService.revokeApiKey(actorPrincipal, apiKey.getId());

        assertThat(result.status()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(apiKey.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(apiKey.getRevokedAt()).isEqualTo(NOW);

        verify(apiKeyRepository).save(apiKey);
        verify(apiKeyAuthenticationService).evictHash("hash_to_evict");

        final ArgumentCaptor<AdminAuditEvent> auditCaptor = ArgumentCaptor.forClass(AdminAuditEvent.class);
        verify(adminAuditEventRepository).insert(auditCaptor.capture());
        final AdminAuditEvent audit = auditCaptor.getValue();
        assertThat(audit.getAction()).isEqualTo("API_KEY_REVOKE");
        assertThat(audit.getActorUser()).isEqualTo(actorUser);
        assertThat(audit.getTargetOrganization()).isEqualTo(organization);
        assertThat(audit.getDetail()).containsEntry("apiKeyId", apiKey.getId().toString());
    }

    @Test
    void revokeApiKeyDoesNothingIfAlreadyRevoked() {
        final ApiKey apiKey = new ApiKey(organization, actorUser, "Key name", "pdapi_abc", "hash");
        apiKey.setId(UUID.randomUUID());
        apiKey.setStatus(ApiKeyStatus.REVOKED);
        apiKey.setCreatedAt(NOW);

        when(apiKeyRepository.findById(apiKey.getId())).thenReturn(Optional.of(apiKey));

        final ApiKeyDto result = adminService.revokeApiKey(actorPrincipal, apiKey.getId());

        assertThat(result.status()).isEqualTo(ApiKeyStatus.REVOKED);
        verify(apiKeyRepository, never()).save(any());
        verify(adminAuditEventRepository, never()).insert(any());
    }

    @Test
    void listUsageReturnsMappedUsageEvents() {
        final UsageEvent usageEvent = new UsageEvent(organization, "product.price", "pdreq_123", (short) 200, true);
        usageEvent.setId(UUID.randomUUID());
        usageEvent.setCreatedAt(NOW);
        usageEvent.setCreditsConsumed(5L);

        when(usageEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50)))
                .thenReturn(List.of(usageEvent));

        final var list = adminService.listUsage(50);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).requestId()).isEqualTo("pdreq_123");
        assertThat(list.get(0).creditsConsumed()).isEqualTo(5L);
        assertThat(list.get(0).organizationName()).isEqualTo("Target Org");
    }

    @Test
    void listAuditEventsReturnsMappedAuditLogs() {
        final AdminAuditEvent audit = new AdminAuditEvent(actorUser, "CREDIT_MANUAL_GRANT");
        audit.setId(UUID.randomUUID());
        audit.setTargetOrganization(organization);
        audit.setCreatedAt(NOW);

        when(adminAuditEventRepository.findAllRecent(50)).thenReturn(List.of(audit));

        final var list = adminService.listAuditEvents(50);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).action()).isEqualTo("CREDIT_MANUAL_GRANT");
        assertThat(list.get(0).actorUserEmail()).isEqualTo(actorUser.getEmail());
    }
}
