package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.exception.RateLimitExceededException;
import org.open4goods.b2bapi.exception.RedisUnavailableException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for Redis Lua metering scripts.
 */
class RedisMeteringServiceTest {

    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private RedisMeteringService service;
    private B2bApiProperties properties;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        REDIS.stop();
    }

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        properties = new B2bApiProperties();
        properties.getRedis().setBalanceTtl(Duration.ofSeconds(60));
        properties.getRatelimit().setRequestsPerMinute(2);
        properties.getRatelimit().setWindow(Duration.ofSeconds(60));
        service = new RedisMeteringService(
                redisTemplate,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void reserveReturnsBalanceNotLoadedInsufficientAndReservedStates() {
        final UUID organizationId = UUID.randomUUID();
        final String balanceKey = service.balanceKey(organizationId);

        assertThat(service.reserveCredits(organizationId, 5).status())
                .isEqualTo(RedisBalanceStatus.BALANCE_NOT_LOADED);

        redisTemplate.opsForValue().set(balanceKey, "10");

        final RedisBalanceResult reserved = service.reserveCredits(organizationId, 4);
        assertThat(reserved.status()).isEqualTo(RedisBalanceStatus.RESERVED);
        assertThat(reserved.balance()).isEqualTo(6);
        assertThat(redisTemplate.opsForValue().get(balanceKey)).isEqualTo("6");

        final RedisBalanceResult insufficient = service.reserveCredits(organizationId, 7);
        assertThat(insufficient.status()).isEqualTo(RedisBalanceStatus.INSUFFICIENT_CREDITS);
        assertThat(redisTemplate.opsForValue().get(balanceKey)).isEqualTo("6");
    }

    @Test
    void refundRestoresReservedCreditsWhenBalanceExists() {
        final UUID organizationId = UUID.randomUUID();
        final String balanceKey = service.balanceKey(organizationId);

        assertThat(service.refundCredits(organizationId, 5).status())
                .isEqualTo(RedisBalanceStatus.BALANCE_NOT_LOADED);

        redisTemplate.opsForValue().set(balanceKey, "6");

        final RedisBalanceResult refund = service.refundCredits(organizationId, 4);

        assertThat(refund.status()).isEqualTo(RedisBalanceStatus.UPDATED);
        assertThat(refund.balance()).isEqualTo(10);
        assertThat(redisTemplate.opsForValue().get(balanceKey)).isEqualTo("10");
    }

    @Test
    void reconcileSetsAuthoritativeBalanceWithConfiguredTtl() {
        final UUID organizationId = UUID.randomUUID();
        final String balanceKey = service.balanceKey(organizationId);

        service.reconcileBalance(organizationId, 42);

        assertThat(redisTemplate.opsForValue().get(balanceKey)).isEqualTo("42");
        assertThat(redisTemplate.getExpire(balanceKey)).isBetween(1L, 60L);
    }

    @Test
    void rateLimitCountsWithinWindowAndRejectsOverLimit() {
        final UUID apiKeyId = UUID.randomUUID();

        assertThat(service.checkRateLimit(apiKeyId)).isEqualTo(1);
        assertThat(service.checkRateLimit(apiKeyId)).isEqualTo(2);
        assertThatThrownBy(() -> service.checkRateLimit(apiKeyId))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void failsClosedWhenRedisTemplateIsUnavailable() {
        final RedisMeteringService unavailable = new RedisMeteringService(
                null,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> unavailable.reserveCredits(UUID.randomUUID(), 1))
                .isInstanceOf(RedisUnavailableException.class);
    }
}
