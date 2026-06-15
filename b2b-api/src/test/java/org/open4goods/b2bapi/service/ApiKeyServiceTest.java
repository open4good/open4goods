package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for customer API key lifecycle operations.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ApiKeySecretGenerator secretGenerator;

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    private ApiKeyService service;
    private User owner;
    private User developer;
    private Organization organization;

    @BeforeEach
    void setUp() {
        service = new ApiKeyService(
                apiKeyRepository,
                userRepository,
                organizationRepository,
                secretGenerator,
                apiKeyAuthenticationService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        owner = new User("owner@example.com", OidcProvider.GOOGLE, "owner");
        developer = new User("developer@example.com", OidcProvider.GITHUB, "developer");
        organization = new Organization("Test workspace", "test-workspace");
    }

    @Test
    void createStoresHashAndReturnsClearKeyOnce() {
        when(secretGenerator.generate()).thenReturn(new ApiKeySecret("pdapi_clear", "pdapi_clearpre", "hash"));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = service.create(principal(owner, OrganizationRole.OWNER), " Production ");

        assertThat(response.clearKey()).isEqualTo("pdapi_clear");
        assertThat(response.key().name()).isEqualTo("Production");
        assertThat(response.key().keyPrefix()).isEqualTo("pdapi_clearpre");
        final ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getKeyHash()).isEqualTo("hash");
        assertThat(captor.getValue().getCreatedBy()).isSameAs(owner);
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void developerListOnlyReturnsOwnKeys() {
        final ApiKey ownKey = apiKey(developer, "pdapi_own", ApiKeyStatus.ACTIVE);
        when(apiKeyRepository.findByOrganizationIdAndCreatedByIdOrderByCreatedAtDesc(organization.getId(), developer.getId()))
                .thenReturn(List.of(ownKey));

        final var keys = service.list(principal(developer, OrganizationRole.DEVELOPER));

        assertThat(keys).singleElement().extracting("keyPrefix").isEqualTo("pdapi_own");
    }

    @Test
    void ownerRotatesAnyActiveOrganizationKey() {
        final ApiKey existing = apiKey(developer, "pdapi_old", ApiKeyStatus.ACTIVE);
        when(apiKeyRepository.findByIdAndOrganizationId(existing.getId(), organization.getId()))
                .thenReturn(Optional.of(existing));
        when(secretGenerator.generate()).thenReturn(new ApiKeySecret("pdapi_newclear", "pdapi_new", "newhash"));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = service.rotate(principal(owner, OrganizationRole.OWNER), existing.getId());

        assertThat(existing.getStatus()).isEqualTo(ApiKeyStatus.ROTATED);
        assertThat(existing.getRevokedAt()).isEqualTo(NOW);
        assertThat(response.clearKey()).isEqualTo("pdapi_newclear");
        assertThat(response.key().keyPrefix()).isEqualTo("pdapi_new");
        verify(apiKeyAuthenticationService).evictHash("pdapi_old-hash");
    }

    @Test
    void developerCannotRevokeAnotherUsersKey() {
        final ApiKey ownerKey = apiKey(owner, "pdapi_owner", ApiKeyStatus.ACTIVE);
        when(apiKeyRepository.findByIdAndOrganizationId(ownerKey.getId(), organization.getId()))
                .thenReturn(Optional.of(ownerKey));

        assertThatThrownBy(() -> service.revoke(principal(developer, OrganizationRole.DEVELOPER), ownerKey.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void revokesActiveKey() {
        final ApiKey existing = apiKey(developer, "pdapi_dev", ApiKeyStatus.ACTIVE);
        when(apiKeyRepository.findByIdAndOrganizationId(existing.getId(), organization.getId()))
                .thenReturn(Optional.of(existing));
        when(apiKeyRepository.save(existing)).thenReturn(existing);

        final var response = service.revoke(principal(developer, OrganizationRole.DEVELOPER), existing.getId());

        assertThat(response.status()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(existing.getRevokedAt()).isEqualTo(NOW);
        verify(apiKeyAuthenticationService).evictHash("pdapi_dev-hash");
    }

    @Test
    void billingRoleCannotManageKeys() {
        assertThatThrownBy(() -> service.create(principal(owner, OrganizationRole.BILLING), "Billing key"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.list(principal(owner, OrganizationRole.BILLING)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private ApiKey apiKey(final User createdBy, final String keyPrefix, final ApiKeyStatus status) {
        final ApiKey apiKey = new ApiKey(organization, createdBy, "Test key", keyPrefix, keyPrefix + "-hash");
        apiKey.setStatus(status);
        apiKey.setCreatedAt(NOW);
        return apiKey;
    }

    private DashboardPrincipal principal(final User user, final OrganizationRole role) {
        return new DashboardPrincipal(user.getId(), organization.getId(), user.getEmail(), false, role);
    }
}
