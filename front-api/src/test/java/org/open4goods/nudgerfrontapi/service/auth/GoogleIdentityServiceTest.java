package org.open4goods.nudgerfrontapi.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.GoogleSsoProperties;
import org.open4goods.nudgerfrontapi.service.exception.GoogleIdentityVerificationException;
import org.open4goods.nudgerfrontapi.service.exception.GoogleSsoDisabledException;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

/**
 * Unit tests for {@link GoogleIdentityService}, backed by a local JWKS stub so
 * no network call ever reaches Google. The stub binds to 127.0.0.1 within the
 * 4100-4109 loopback range reserved for dev/test servers on the shared build
 * host.
 */
class GoogleIdentityServiceTest {

    private static final Instant NOW = Instant.now();
    private static final String ISSUER = "https://accounts.google.com";
    private static final String CLIENT_ID = "test-client-id";
    private static final String ALLOWED_EMAIL = "owner@example.com";

    private HttpServer server;
    private RSAKey key;
    private GoogleSsoProperties properties;
    private GoogleIdentityService service;

    @BeforeEach
    void startJwksStub() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        server = startOnFirstAvailablePort(key);

        properties = new GoogleSsoProperties();
        properties.setEnabled(true);
        properties.setClientId(CLIENT_ID);
        properties.setIssuer(ISSUER);
        properties.setJwksUri("http://127.0.0.1:" + server.getAddress().getPort() + "/jwks");
        properties.setRoleAllowlist(Map.of(ALLOWED_EMAIL, List.of("ROLE_SITEEDITOR")));

        service = new GoogleIdentityService(properties);
    }

    @AfterEach
    void stopJwksStub() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void verifiesTokenAndResolvesAllowlistedRole() throws Exception {
        String token = signedToken(ALLOWED_EMAIL, true, "expected-nonce", NOW.plusSeconds(300), CLIENT_ID, ISSUER);

        GoogleIdentity identity = service.verify(token, "expected-nonce");

        assertThat(identity.email()).isEqualTo(ALLOWED_EMAIL);
        assertThat(identity.roles()).containsExactly("ROLE_SITEEDITOR");
    }

    @Test
    void rejectsWhenFeatureFlagDisabled() throws Exception {
        properties.setEnabled(false);
        String token = signedToken(ALLOWED_EMAIL, true, "n", NOW.plusSeconds(300), CLIENT_ID, ISSUER);

        assertThatThrownBy(() -> service.verify(token, "n"))
                .isInstanceOf(GoogleSsoDisabledException.class);
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        String token = signedToken(ALLOWED_EMAIL, true, "n", NOW.plusSeconds(300), CLIENT_ID, "https://evil.example.com");

        assertThatThrownBy(() -> service.verify(token, "n"))
                .isInstanceOf(GoogleIdentityVerificationException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        String token = signedToken(ALLOWED_EMAIL, true, "n", NOW.plusSeconds(300), "other-client", ISSUER);

        assertThatThrownBy(() -> service.verify(token, "n"))
                .isInstanceOf(GoogleIdentityVerificationException.class)
                .hasMessageContaining("audience");
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String token = signedToken(ALLOWED_EMAIL, true, "n", NOW.minusSeconds(300), CLIENT_ID, ISSUER);

        assertThatThrownBy(() -> service.verify(token, "n"))
                .isInstanceOf(GoogleIdentityVerificationException.class);
    }

    @Test
    void rejectsUnverifiedEmail() throws Exception {
        String token = signedToken(ALLOWED_EMAIL, false, "n", NOW.plusSeconds(300), CLIENT_ID, ISSUER);

        assertThatThrownBy(() -> service.verify(token, "n"))
                .isInstanceOf(GoogleIdentityVerificationException.class)
                .hasMessageContaining("not verified");
    }

    @Test
    void rejectsNonceMismatchReplay() throws Exception {
        String token = signedToken(ALLOWED_EMAIL, true, "original-nonce", NOW.plusSeconds(300), CLIENT_ID, ISSUER);

        assertThatThrownBy(() -> service.verify(token, "different-nonce"))
                .isInstanceOf(GoogleIdentityVerificationException.class)
                .hasMessageContaining("nonce");
    }

    @Test
    void rejectsUnicodeNonceWithoutRangeError() throws Exception {
        // A multi-byte UTF-8 nonce must never crash the comparison (UTF-16 length
        // vs UTF-8 byte length divergence was the root cause of a prior RangeError).
        String unicodeNonce = "n-éèê-😀";
        String token = signedToken(ALLOWED_EMAIL, true, unicodeNonce, NOW.plusSeconds(300), CLIENT_ID, ISSUER);

        GoogleIdentity identity = service.verify(token, unicodeNonce);

        assertThat(identity.email()).isEqualTo(ALLOWED_EMAIL);
    }

    @Test
    void rejectsEmailAbsentFromAllowlist() throws Exception {
        String token = signedToken("stranger@example.com", true, "n", NOW.plusSeconds(300), CLIENT_ID, ISSUER);

        assertThatThrownBy(() -> service.verify(token, "n"))
                .isInstanceOf(GoogleIdentityVerificationException.class)
                .hasMessageContaining("allowlist");
    }

    private static HttpServer startOnFirstAvailablePort(RSAKey key) throws Exception {
        Exception last = null;
        for (int port = 4100; port <= 4109; port++) {
            try {
                HttpServer candidate = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
                candidate.createContext("/jwks", exchange -> {
                    byte[] body = ("{\"keys\":[" + key.toPublicJWK().toJSONString() + "]}")
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
                candidate.start();
                return candidate;
            } catch (Exception ex) {
                last = ex;
            }
        }
        throw new IllegalStateException("No free port in 4100-4109 for the JWKS stub", last);
    }

    private String signedToken(String email, boolean emailVerified, String nonce, Instant expiresAt,
            String audience, String issuer) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("google-subject")
                .audience(List.of(audience))
                .expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(NOW))
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .claim("nonce", nonce)
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(key.getKeyID())
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
