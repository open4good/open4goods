package org.open4goods.b2bapi.service;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.open4goods.b2bapi.exception.InvalidApiKeyException;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.OrganizationStatus;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Authenticates opaque {@code pdapi_} keys for external product endpoints.
 */
@Service
public class ApiKeyAuthenticationService {

    public static final String API_KEY_AUTHORITY = "PDAPI_KEY";
    private static final String CLEAR_PREFIX = "pdapi_";
    private static final String CACHE_PREFIX = "b2b:apikey:";
    private static final String LAST_USED_PREFIX = "b2b:lastused:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeySecretGenerator secretGenerator;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    @Autowired
    public ApiKeyAuthenticationService(
            final ObjectProvider<ApiKeyRepository> apiKeyRepository,
            final ApiKeySecretGenerator secretGenerator,
            final ObjectProvider<StringRedisTemplate> redisTemplate) {
        this(apiKeyRepository.getIfAvailable(), secretGenerator, redisTemplate.getIfAvailable(), Clock.systemUTC());
    }

    ApiKeyAuthenticationService(
            final ApiKeyRepository apiKeyRepository,
            final ApiKeySecretGenerator secretGenerator,
            final StringRedisTemplate redisTemplate,
            final Clock clock) {
        this.apiKeyRepository = apiKeyRepository;
        this.secretGenerator = secretGenerator;
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    /**
     * Authenticates a clear {@code pdapi_} key and returns a Spring Security authentication.
     *
     * @param clearKey clear API key from the bearer header
     * @return authenticated API key principal
     */
    @Transactional(readOnly = true)
    public Authentication authenticate(final String clearKey) {
        if (!StringUtils.hasText(clearKey) || !clearKey.startsWith(CLEAR_PREFIX)) {
            throw new InvalidApiKeyException("Malformed API key.");
        }
        final String keyHash = secretGenerator.sha256Hex(clearKey);
        final ApiKeyCacheEntry entry = resolve(keyHash);
        if (entry.status() != ApiKeyStatus.ACTIVE || entry.organizationStatus() != OrganizationStatus.ACTIVE) {
            throw new InvalidApiKeyException("Inactive API key.");
        }
        debounceLastUsed(entry.apiKeyId());
        return new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(entry.organizationId(), entry.apiKeyId()),
                clearKey,
                java.util.List.of(new SimpleGrantedAuthority(API_KEY_AUTHORITY)));
    }

    private ApiKeyCacheEntry resolve(final String keyHash) {
        return readCache(keyHash).orElseGet(() -> {
            if (apiKeyRepository == null) {
                throw new InvalidApiKeyException("API key authentication is unavailable.");
            }
            final ApiKey apiKey = apiKeyRepository.findByKeyHash(keyHash)
                    .orElseThrow(() -> new InvalidApiKeyException("Unknown API key."));
            final ApiKeyCacheEntry entry = new ApiKeyCacheEntry(
                    apiKey.getOrganization().getId(),
                    apiKey.getId(),
                    apiKey.getStatus(),
                    apiKey.getOrganization().getStatus());
            writeCache(keyHash, entry);
            return entry;
        });
    }

    private Optional<ApiKeyCacheEntry> readCache(final String keyHash) {
        if (redisTemplate == null) {
            return Optional.empty();
        }
        final String raw = redisTemplate.opsForValue().get(CACHE_PREFIX + keyHash);
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        final String[] parts = raw.split(":", 4);
        if (parts.length != 4) {
            return Optional.empty();
        }
        return Optional.of(new ApiKeyCacheEntry(
                UUID.fromString(parts[0]),
                UUID.fromString(parts[1]),
                ApiKeyStatus.valueOf(parts[2]),
                OrganizationStatus.valueOf(parts[3])));
    }

    private void writeCache(final String keyHash, final ApiKeyCacheEntry entry) {
        if (redisTemplate == null) {
            return;
        }
        final String raw = entry.organizationId() + ":" + entry.apiKeyId() + ":"
                + entry.status().name() + ":" + entry.organizationStatus().name();
        redisTemplate.opsForValue().set(CACHE_PREFIX + keyHash, raw, CACHE_TTL);
    }

    /**
     * Removes a hashed API key from the hot authentication cache.
     *
     * @param keyHash SHA-256 API key hash
     */
    public void evictHash(final String keyHash) {
        if (redisTemplate == null || !StringUtils.hasText(keyHash)) {
            return;
        }
        redisTemplate.delete(CACHE_PREFIX + keyHash);
    }

    private void debounceLastUsed(final UUID apiKeyId) {
        if (redisTemplate == null) {
            return;
        }
        redisTemplate.opsForValue().set(LAST_USED_PREFIX + apiKeyId, clock.instant().toString());
    }

    private record ApiKeyCacheEntry(
            UUID organizationId,
            UUID apiKeyId,
            ApiKeyStatus status,
            OrganizationStatus organizationStatus) {
    }
}
