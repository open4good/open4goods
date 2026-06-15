package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.exception.InvalidCredentialsException;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationMember;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.OrganizationMemberRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;

/**
 * Unit tests for first-login and repeat-login account provisioning.
 */
@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private CreditBucketRepository creditBucketRepository;

    @Mock
    private CreditGrantService creditGrantService;

    private B2bApiProperties properties;
    private UserProvisioningService service;

    @BeforeEach
    void setUp() {
        properties = new B2bApiProperties();
        properties.getSecurity().setAdminEmails(List.of("Admin@Example.com"));
        service = new UserProvisioningService(
                properties,
                userRepository,
                organizationRepository,
                organizationMemberRepository,
                creditBucketRepository,
                creditGrantService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void provisionsNewUserDefaultOrganizationAndFreeGrant() {
        final OidcUserProfile profile = new OidcUserProfile(
                OidcProvider.GOOGLE,
                "provider-subject",
                "Admin@Example.com",
                true,
                "Test User",
                "https://example.com/avatar.png");

        when(userRepository.findByOidcProviderAndOidcSubject(OidcProvider.GOOGLE, "provider-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationMemberRepository.findFirstByUserIdOrderByCreatedAtAsc(any(UUID.class)))
                .thenReturn(Optional.empty());
        when(organizationRepository.findBySlug("test-user")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationMemberRepository.save(any(OrganizationMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(creditGrantService.grantFreeIfNeeded(any(UUID.class), any(User.class)))
                .thenReturn(new CreditGrantResult(UUID.randomUUID(), 2_500, 0, 2_500, false));
        when(creditBucketRepository.sumLiveCredits(any(UUID.class))).thenReturn(2_500L);

        final ProvisionedAccount account = service.provision(profile);

        assertThat(account.user().getEmail()).isEqualTo("admin@example.com");
        assertThat(account.user().isPlatformAdmin()).isTrue();
        assertThat(account.organization().getSlug()).isEqualTo("test-user");
        assertThat(account.role()).isEqualTo(OrganizationRole.OWNER);
        assertThat(account.balanceCredits()).isEqualTo(2_500L);
        verify(creditGrantService).grantFreeIfNeeded(account.organization().getId(), account.user());
    }

    @Test
    void updatesExistingUserWithoutApplyingAnotherFreeGrant() {
        final User user = new User("user@example.com", OidcProvider.GITHUB, "github-subject");
        final Organization organization = new Organization("Existing workspace", "existing");
        final OrganizationMember membership = new OrganizationMember(organization, user, OrganizationRole.ADMIN);
        final OidcUserProfile profile = new OidcUserProfile(
                OidcProvider.GITHUB,
                "github-subject",
                "USER@example.com",
                true,
                "Updated User",
                null);

        when(userRepository.findByOidcProviderAndOidcSubject(OidcProvider.GITHUB, "github-subject"))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(organizationMemberRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId()))
                .thenReturn(Optional.of(membership));
        when(creditBucketRepository.sumLiveCredits(organization.getId())).thenReturn(42L);

        final ProvisionedAccount account = service.provision(profile);

        assertThat(account.user().getDisplayName()).isEqualTo("Updated User");
        assertThat(account.user().getEmail()).isEqualTo("user@example.com");
        assertThat(account.role()).isEqualTo(OrganizationRole.ADMIN);
        assertThat(account.balanceCredits()).isEqualTo(42L);
        verify(creditGrantService, never()).grantFreeIfNeeded(any(), any());
    }

    @Test
    void rejectsProfileWithoutVerifiedEmail() {
        final OidcUserProfile profile = new OidcUserProfile(
                OidcProvider.APPLE,
                "apple-subject",
                "user@example.com",
                false,
                null,
                null);

        assertThatThrownBy(() -> service.provision(profile))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Verified email");
    }
}
