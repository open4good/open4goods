package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.exception.InvalidCredentialsException;
import org.open4goods.b2bapi.model.OidcProvider;

/**
 * Unit tests for JWKS-backed OIDC token verification.
 */
class JwksOidcTokenVerifierTest {

    private static final Instant NOW = Instant.now();

    @Test
    void verifiesSignedOidcTokenFromJwks() throws Exception {
        final RSAKey key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        final HttpServer server = jwksServer(key);
        try {
            final B2bApiProperties.OidcProvider properties = oidcProperties(server, "test-client");
            final TestVerifier verifier = new TestVerifier(properties);
            final String token = signedToken(key, properties.getIssuer(), "test-client", NOW.plusSeconds(300));

            final OidcUserProfile profile = verifier.verify(token);

            assertThat(profile.provider()).isEqualTo(OidcProvider.GOOGLE);
            assertThat(profile.subject()).isEqualTo("provider-subject");
            assertThat(profile.email()).isEqualTo("user@example.com");
            assertThat(profile.emailVerified()).isTrue();
            assertThat(profile.displayName()).isEqualTo("Test User");
            assertThat(profile.avatarUrl()).isEqualTo("https://example.com/avatar.png");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        final RSAKey key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        final HttpServer server = jwksServer(key);
        try {
            final B2bApiProperties.OidcProvider properties = oidcProperties(server, "expected-client");
            final TestVerifier verifier = new TestVerifier(properties);
            final String token = signedToken(key, properties.getIssuer(), "other-client", NOW.plusSeconds(300));

            assertThatThrownBy(() -> verifier.verify(token))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("audience mismatch");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void acceptsMicrosoftTenantIssuerWhenCommonIssuerIsConfigured() {
        final B2bApiProperties properties = new B2bApiProperties();
        final MicrosoftOidcTokenVerifier verifier = new MicrosoftOidcTokenVerifier(properties);

        assertThat(verifier.issuerMatches("https://login.microsoftonline.com/tenant-id/v2.0")).isTrue();
    }

    private static HttpServer jwksServer(final RSAKey key) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            final byte[] body = ("{\"keys\":[" + key.toPublicJWK().toJSONString() + "]}").getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static B2bApiProperties.OidcProvider oidcProperties(final HttpServer server, final String clientId) {
        final B2bApiProperties.OidcProvider properties = new B2bApiProperties.OidcProvider();
        properties.setClientId(clientId);
        properties.setIssuer("https://issuer.example.com");
        properties.setJwksUri("http://127.0.0.1:" + server.getAddress().getPort() + "/jwks");
        return properties;
    }

    private static String signedToken(final RSAKey key, final String issuer,
            final String audience, final Instant expiresAt) throws Exception {
        final JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("provider-subject")
                .audience(List.of(audience))
                .expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(NOW))
                .claim("email", "user@example.com")
                .claim("email_verified", true)
                .claim("name", "Test User")
                .claim("picture", "https://example.com/avatar.png")
                .build();
        final SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(key.getKeyID())
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static class TestVerifier extends JwksOidcTokenVerifier {

        TestVerifier(final B2bApiProperties.OidcProvider properties) {
            super(OidcProvider.GOOGLE, properties, Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }
}
