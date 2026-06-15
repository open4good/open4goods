package org.open4goods.b2bapi.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.exception.InvalidCredentialsException;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationMember;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.OrganizationMemberRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Provisions and updates dashboard users from verified OIDC identities.
 */
@Service
@ConditionalOnBean(name = "entityManagerFactory")
public class UserProvisioningService {

    private final B2bApiProperties properties;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final CreditBucketRepository creditBucketRepository;
    private final CreditGrantService creditGrantService;
    private final Clock clock;

    @Autowired
    public UserProvisioningService(
            final B2bApiProperties properties,
            final UserRepository userRepository,
            final OrganizationRepository organizationRepository,
            final OrganizationMemberRepository organizationMemberRepository,
            final CreditBucketRepository creditBucketRepository,
            final CreditGrantService creditGrantService) {
        this(properties, userRepository, organizationRepository, organizationMemberRepository,
                creditBucketRepository, creditGrantService, Clock.systemUTC());
    }

    UserProvisioningService(
            final B2bApiProperties properties,
            final UserRepository userRepository,
            final OrganizationRepository organizationRepository,
            final OrganizationMemberRepository organizationMemberRepository,
            final CreditBucketRepository creditBucketRepository,
            final CreditGrantService creditGrantService,
            final Clock clock) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.creditBucketRepository = creditBucketRepository;
        this.creditGrantService = creditGrantService;
        this.clock = clock;
    }

    /**
     * Applies first-login provisioning and profile updates for a verified provider identity.
     *
     * @param profile verified identity profile
     * @return active account state
     */
    @Transactional
    public ProvisionedAccount provision(final OidcUserProfile profile) {
        final String email = normalizedEmail(profile.email());
        if (!StringUtils.hasText(email) || !profile.emailVerified()) {
            throw new InvalidCredentialsException("Verified email is required.");
        }

        final Instant now = clock.instant();
        final User user = userRepository
                .findByOidcProviderAndOidcSubject(profile.provider(), profile.subject())
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> new User(email, profile.provider(), profile.subject()));

        user.setEmail(email);
        user.setOidcProvider(profile.provider());
        user.setOidcSubject(profile.subject());
        user.setDisplayName(profile.displayName());
        user.setAvatarUrl(profile.avatarUrl());
        user.setPlatformAdmin(isPlatformAdmin(email));
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        final User savedUser = userRepository.save(user);

        final OrganizationMember membership = organizationMemberRepository
                .findFirstByUserIdOrderByCreatedAtAsc(savedUser.getId())
                .orElseGet(() -> createDefaultOrganization(savedUser, now));

        final long balanceCredits = creditBucketRepository.sumLiveCredits(membership.getOrganization().getId());
        return new ProvisionedAccount(savedUser, membership.getOrganization(), membership.getRole(), balanceCredits);
    }

    private OrganizationMember createDefaultOrganization(final User user, final Instant now) {
        final Organization organization = new Organization(
                defaultOrganizationName(user),
                uniqueSlug(slugSource(user)));
        organization.setBillingEmail(user.getEmail());
        organization.setCreatedAt(now);
        organization.setUpdatedAt(now);
        final Organization savedOrganization = organizationRepository.save(organization);

        final OrganizationMember membership = organizationMemberRepository.save(
                new OrganizationMember(savedOrganization, user, OrganizationRole.OWNER));
        creditGrantService.grantFreeIfNeeded(savedOrganization.getId(), user);
        return membership;
    }

    private boolean isPlatformAdmin(final String email) {
        return properties.getSecurity().getAdminEmails().stream()
                .map(this::normalizedEmail)
                .anyMatch(email::equals);
    }

    private String normalizedEmail(final String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : "";
    }

    private String defaultOrganizationName(final User user) {
        if (StringUtils.hasText(user.getDisplayName())) {
            return user.getDisplayName().trim() + " workspace";
        }
        return user.getEmail() + " workspace";
    }

    private String slugSource(final User user) {
        if (StringUtils.hasText(user.getDisplayName())) {
            return user.getDisplayName();
        }
        return user.getEmail().split("@", 2)[0];
    }

    private String uniqueSlug(final String source) {
        final String base = source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        final String fallback = StringUtils.hasText(base) ? base : "workspace";
        String candidate = fallback;
        int suffix = 2;
        while (organizationRepository.findBySlug(candidate).isPresent()) {
            candidate = fallback + "-" + suffix;
            suffix++;
        }
        return candidate;
    }
}
