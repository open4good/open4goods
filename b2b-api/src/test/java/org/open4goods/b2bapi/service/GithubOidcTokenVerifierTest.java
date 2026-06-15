package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.model.OidcProvider;

/**
 * Unit tests for GitHub OAuth userinfo verification.
 */
class GithubOidcTokenVerifierTest {

    @Test
    void resolvesVerifiedPrimaryEmailWhenUserEmailIsPrivate() throws Exception {
        final HttpServer server = githubServer();
        try {
            final B2bApiProperties properties = new B2bApiProperties();
            properties.getSecurity().getOidc().getGithub()
                    .setApiBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            final GithubOidcTokenVerifier verifier = new GithubOidcTokenVerifier(properties);

            final OidcUserProfile profile = verifier.verify("github-token");

            assertThat(profile.provider()).isEqualTo(OidcProvider.GITHUB);
            assertThat(profile.subject()).isEqualTo("12345");
            assertThat(profile.email()).isEqualTo("primary@example.com");
            assertThat(profile.emailVerified()).isTrue();
            assertThat(profile.displayName()).isEqualTo("GitHub User");
            assertThat(profile.avatarUrl()).isEqualTo("https://example.com/avatar.png");
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer githubServer() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/user", exchange -> writeJson(exchange, """
                {
                  "login": "octocat",
                  "id": 12345,
                  "name": "GitHub User",
                  "email": null,
                  "avatar_url": "https://example.com/avatar.png"
                }
                """));
        server.createContext("/user/emails", exchange -> writeJson(exchange, """
                [
                  {"email": "secondary@example.com", "primary": false, "verified": true},
                  {"email": "primary@example.com", "primary": true, "verified": true}
                ]
                """));
        server.start();
        return server;
    }

    private static void writeJson(final com.sun.net.httpserver.HttpExchange exchange, final String json)
            throws java.io.IOException {
        final byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
