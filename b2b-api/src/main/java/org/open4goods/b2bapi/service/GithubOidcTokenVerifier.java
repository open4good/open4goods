package org.open4goods.b2bapi.service;

import java.util.Arrays;
import java.util.Comparator;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.exception.InvalidCredentialsException;
import org.open4goods.b2bapi.model.OidcProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Verifies GitHub OAuth access tokens through GitHub userinfo APIs.
 */
@Service
public class GithubOidcTokenVerifier implements OidcTokenVerifier {

    private final B2bApiProperties properties;

    public GithubOidcTokenVerifier(final B2bApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public OidcProvider provider() {
        return OidcProvider.GITHUB;
    }

    @Override
    public OidcUserProfile verify(final String credential) {
        if (!StringUtils.hasText(credential)) {
            throw new InvalidCredentialsException("GitHub OAuth access token is missing");
        }

        final RestClient restClient = RestClient.builder()
                .baseUrl(properties.getSecurity().getOidc().getGithub().getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + credential)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
        try {
            final GithubUserResponse user = restClient.get()
                    .uri("/user")
                    .retrieve()
                    .body(GithubUserResponse.class);
            if (user == null) {
                throw new InvalidCredentialsException("GitHub userinfo response is empty");
            }
            final String email = verifiedEmail(restClient, user);
            return new OidcUserProfile(
                    OidcProvider.GITHUB,
                    String.valueOf(user.id()),
                    email,
                    StringUtils.hasText(email),
                    StringUtils.hasText(user.name()) ? user.name() : user.login(),
                    user.avatarUrl());
        } catch (final RestClientException exception) {
            throw new InvalidCredentialsException("GitHub OAuth token verification failed", exception);
        }
    }

    private static String verifiedEmail(final RestClient restClient, final GithubUserResponse user) {
        if (StringUtils.hasText(user.email())) {
            return user.email();
        }
        final GithubEmailResponse[] emails = restClient.get()
                .uri("/user/emails")
                .retrieve()
                .body(GithubEmailResponse[].class);
        if (emails == null) {
            return user.login() + "@users.noreply.github.com";
        }
        return Arrays.stream(emails)
                .filter(email -> email.verified() && StringUtils.hasText(email.email()))
                .max(Comparator.comparing(GithubEmailResponse::primary))
                .map(GithubEmailResponse::email)
                .orElse(user.login() + "@users.noreply.github.com");
    }

    private record GithubUserResponse(
            String login,
            long id,
            String name,
            String email,
            @com.fasterxml.jackson.annotation.JsonProperty("avatar_url") String avatarUrl) {
    }

    private record GithubEmailResponse(String email, boolean primary, boolean verified) {
    }
}
