package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.b2bapi.exception.InvalidApiKeyException;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationStatus;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;

/**
 * Unit tests for external API-key authentication and hot-cache behavior.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");
    private static final String CLEAR_KEY = "pdapi_clear";
    private static final String KEY_HASH = "hash";

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiKeySecretGenerator secretGenerator;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private User user;
    private Organization organization;

    @BeforeEach
    void setUp() {
        user = new User("developer@example.com", OidcProvider.GITHUB, "developer");
        organization = new Organization("Test workspace", "test-workspace");
    }

    @Test
    void authenticatesActiveKeyFromPostgresWhenRedisIsUnavailable() {
        final ApiKey apiKey = apiKey(ApiKeyStatus.ACTIVE);
        when(secretGenerator.sha256Hex(CLEAR_KEY)).thenReturn(KEY_HASH);
        when(apiKeyRepository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(apiKey));
        final ApiKeyAuthenticationService service = service(null);

        final Authentication authentication = service.authenticate(CLEAR_KEY);

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(new ApiKeyPrincipal(organization.getId(), apiKey.getId()));
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly(ApiKeyAuthenticationService.API_KEY_AUTHORITY);
    }

    @Test
    void authenticatesFromRedisCacheAndDebouncesLastUsed() {
        final UUID apiKeyId = UUID.randomUUID();
        when(secretGenerator.sha256Hex(CLEAR_KEY)).thenReturn(KEY_HASH);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("b2b:apikey:" + KEY_HASH)).thenReturn(
                organization.getId() + ":" + apiKeyId + ":ACTIVE:ACTIVE");
        final ApiKeyAuthenticationService service = service(redisTemplate);

        final Authentication authentication = service.authenticate(CLEAR_KEY);

        assertThat(authentication.getPrincipal()).isEqualTo(new ApiKeyPrincipal(organization.getId(), apiKeyId));
        verify(apiKeyRepository, never()).findByKeyHash(KEY_HASH);
        verify(valueOperations).set("b2b:lastused:" + apiKeyId, NOW.toString());
    }

    @Test
    void cachesPostgresLookupAndDebouncesLastUsed() {
        final ApiKey apiKey = apiKey(ApiKeyStatus.ACTIVE);
        when(secretGenerator.sha256Hex(CLEAR_KEY)).thenReturn(KEY_HASH);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("b2b:apikey:" + KEY_HASH)).thenReturn(null);
        when(apiKeyRepository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(apiKey));
        final ApiKeyAuthenticationService service = service(redisTemplate);

        service.authenticate(CLEAR_KEY);

        verify(valueOperations).set(
                "b2b:apikey:" + KEY_HASH,
                organization.getId() + ":" + apiKey.getId() + ":ACTIVE:ACTIVE",
                java.time.Duration.ofMinutes(10));
        verify(valueOperations).set("b2b:lastused:" + apiKey.getId(), NOW.toString());
    }

    @Test
    void rejectsMalformedUnknownInactiveAndSuspendedOrganizationKeys() {
        final ApiKey revoked = apiKey(ApiKeyStatus.REVOKED);
        final ApiKey suspendedOrganization = apiKey(ApiKeyStatus.ACTIVE);
        suspendedOrganization.getOrganization().setStatus(OrganizationStatus.SUSPENDED);
        when(secretGenerator.sha256Hex("pdapi_unknown")).thenReturn("unknown");
        when(secretGenerator.sha256Hex("pdapi_revoked")).thenReturn("revoked");
        when(secretGenerator.sha256Hex("pdapi_suspended")).thenReturn("suspended");
        when(apiKeyRepository.findByKeyHash("unknown")).thenReturn(Optional.empty());
        when(apiKeyRepository.findByKeyHash("revoked")).thenReturn(Optional.of(revoked));
        when(apiKeyRepository.findByKeyHash("suspended")).thenReturn(Optional.of(suspendedOrganization));
        final ApiKeyAuthenticationService service = service(null);

        assertThatThrownBy(() -> service.authenticate("clear"))
                .isInstanceOf(InvalidApiKeyException.class);
        assertThatThrownBy(() -> service.authenticate("pdapi_unknown"))
                .isInstanceOf(InvalidApiKeyException.class);
        assertThatThrownBy(() -> service.authenticate("pdapi_revoked"))
                .isInstanceOf(InvalidApiKeyException.class);
        assertThatThrownBy(() -> service.authenticate("pdapi_suspended"))
                .isInstanceOf(InvalidApiKeyException.class);
    }

    @Test
    void evictsCacheByHash() {
        final ApiKeyAuthenticationService service = service(redisTemplate);

        service.evictHash(KEY_HASH);

        verify(redisTemplate).delete("b2b:apikey:" + KEY_HASH);
    }

    private ApiKeyAuthenticationService service(final StringRedisTemplate template) {
        return new ApiKeyAuthenticationService(
                apiKeyRepository,
                secretGenerator,
                template,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ApiKey apiKey(final ApiKeyStatus status) {
        final ApiKey apiKey = new ApiKey(organization, user, "Production", "pdapi_prefix", KEY_HASH);
        apiKey.setStatus(status);
        return apiKey;
    }
}
