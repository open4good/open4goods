package org.open4goods.nudgerfrontapi.service.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.open4goods.nudgerfrontapi.config.properties.GoogleSsoProperties;
import org.open4goods.nudgerfrontapi.config.properties.SecurityProperties;
import org.open4goods.nudgerfrontapi.service.exception.GoogleIdentityVerificationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Utility service to issue and validate JWT tokens for the frontend API.
 * <p>
 * The service wraps Spring Security's encoder/decoder beans and centralises claim construction so controllers
 * and filters keep a single source of truth for token lifetimes and payloads.
 * </p>
 */
@Service
public class JwtService {

    /** Authentication Method Reference claim identifying how a session's roles were established. */
    public static final String AMR_CLAIM = "amr";

    /**
     * Marks a session established through Google SSO. Its roles come from the allowlist rather than
     * from credentials we control, so every refresh re-checks {@link GoogleSsoProperties#rolesFor(String)}
     * instead of trusting the roles embedded at login time.
     */
    public static final String AMR_GOOGLE_SSO = "google-sso";

    private static final String AMR_PASSWORD = "pwd";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final SecurityProperties properties;
    private final GoogleSsoProperties googleSsoProperties;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, SecurityProperties properties,
            GoogleSsoProperties googleSsoProperties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
        this.googleSsoProperties = googleSsoProperties;
    }

    /**
     * Expose the current security properties (mainly used for configuration endpoints).
     */
    public SecurityProperties getProperties() {
        return properties;
    }

    /**
     * Generate an access token for the given authentication.
     *
     * @param auth authenticated principal
     * @return signed JWT containing the principal username and authorities
     */
    public String generateAccessToken(Authentication auth) {
        return issueToken(auth, properties.getAccessTokenExpiry());
    }

    /**
     * Generate a refresh token for the given authentication.
     *
     * @param auth authenticated principal
     * @return signed JWT usable to request a new access token
     */
    public String generateRefreshToken(Authentication auth) {
        return issueToken(auth, properties.getRefreshTokenExpiry());
    }

    private String issueToken(Authentication auth, Duration validity) {
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(() -> "HS256").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(auth.getName())
                .claim("roles", auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList())
                .claim(AMR_CLAIM, amrOf(auth))
                .issuedAt(now)
                .expiresAt(now.plus(validity))
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String amrOf(Authentication auth) {
        return AMR_GOOGLE_SSO.equals(auth.getDetails()) ? AMR_GOOGLE_SSO : AMR_PASSWORD;
    }

    /**
     * Validate a refresh token and rebuild the authentication it carries.
     * <p>
     * A session established through Google SSO ({@value #AMR_GOOGLE_SSO}, per the {@value #AMR_CLAIM}
     * claim) has its roles re-checked against {@link GoogleSsoProperties#rolesFor(String)} on every
     * refresh: an email removed from the allowlist revokes the session immediately instead of waiting
     * for the refresh token's own expiry, and a reduced allowlist only grants the remaining roles.
     * Password sessions are not allowlist-backed, so their roles claim is trusted as-is.
     * </p>
     *
     * @param token refresh token value
     * @return authentication rebuilt from the token's subject and (possibly narrowed) roles claim
     * @throws GoogleIdentityVerificationException if a Google SSO session's email is no longer allowlisted
     */
    public Authentication validateRefreshToken(String token) {
        Jwt jwt = decoder.decode(token);
        String subject = jwt.getSubject();
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<String> tokenRoles = roles == null ? List.of() : roles;
        String amr = jwt.getClaimAsString(AMR_CLAIM);

        List<String> effectiveRoles = tokenRoles;
        if (AMR_GOOGLE_SSO.equals(amr)) {
            List<String> allowlisted = googleSsoProperties.rolesFor(subject);
            if (allowlisted.isEmpty()) {
                throw new GoogleIdentityVerificationException(
                        "Email no longer present in the Google SSO role allowlist: " + subject);
            }
            effectiveRoles = tokenRoles.stream().filter(allowlisted::contains).toList();
        }

        List<GrantedAuthority> authorities = effectiveRoles.stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(subject, "N/A", authorities);
        auth.setDetails(amr);
        return auth;
    }
}
