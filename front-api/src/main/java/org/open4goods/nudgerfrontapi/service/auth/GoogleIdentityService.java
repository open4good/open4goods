package org.open4goods.nudgerfrontapi.service.auth;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.config.properties.SecurityProperties;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Verifies a Google OpenID Connect ID token and resolves its current, explicit
 * application roles. An email domain never grants access.
 */
@Service
public class GoogleIdentityService {

    private static final Set<String> ASSIGNABLE_ROLES = Set.of(RolesConstants.ROLE_ADMIN, RolesConstants.ROLE_EDITOR);

    private final SecurityProperties securityProperties;
    private final Function<SecurityProperties.Google, JwtDecoder> decoderFactory;

    @Autowired
    public GoogleIdentityService(SecurityProperties securityProperties) {
        this(securityProperties, google -> JwtDecoders.fromIssuerLocation(google.getIssuer()));
    }

    GoogleIdentityService(SecurityProperties securityProperties,
                          Function<SecurityProperties.Google, JwtDecoder> decoderFactory) {
        this.securityProperties = securityProperties;
        this.decoderFactory = decoderFactory;
    }

    /**
     * Validates a provider token and returns the corresponding current identity.
     *
     * @param idToken Google ID token received only from the Nuxt BFF
     * @return verified email and explicitly assigned roles
     * @throws BadCredentialsException if the token or assignment is invalid
     */
    public VerifiedIdentity verify(String idToken) {
        SecurityProperties.Google google = securityProperties.getGoogle();
        if (google.getAudience() == null || google.getAudience().isBlank()) {
            throw new BadCredentialsException("Google OIDC is not configured");
        }

        Jwt jwt = decoder(google).decode(idToken);
        if (!google.getIssuer().equals(jwt.getIssuer().toString())) {
            throw new BadCredentialsException("Google token has an unexpected issuer");
        }
        if (!jwt.getAudience().contains(google.getAudience())) {
            throw new BadCredentialsException("Google token has an unexpected audience");
        }
        if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {
            throw new BadCredentialsException("Google token has expired");
        }
        if (!Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            throw new BadCredentialsException("Google token email is not verified");
        }
        String email = jwt.getClaimAsString("email");
        return identityFor(email);
    }

    /**
     * Resolves roles again at refresh time so removed assignments take effect
     * without waiting for an old session to expire.
     */
    public VerifiedIdentity identityFor(String email) {
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Google token has no email");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return securityProperties.getGoogle().getAllowedRoles().stream()
                .filter(entry -> normalizedEmail.equals(normalize(entry.getEmail())))
                .map(entry -> new VerifiedIdentity(normalizedEmail, entry.getRoles().stream().distinct().toList()))
                .filter(identity -> ASSIGNABLE_ROLES.containsAll(identity.roles()))
                .findFirst()
                .filter(identity -> !identity.roles().isEmpty())
                .orElseThrow(() -> new BadCredentialsException("Email has no application role"));
    }

    private JwtDecoder decoder(SecurityProperties.Google google) {
        return decoderFactory.apply(google);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Verified, allowlisted identity used to issue an application session. */
    public record VerifiedIdentity(String email, List<String> roles) { }
}
