package org.open4goods.b2bapi.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.exception.RateLimitExceededException;
import org.open4goods.b2bapi.exception.RedisUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Loads and invokes Redis Lua scripts for metered hot-path operations.
 */
@Service
public class RedisMeteringService {

    private static final String BALANCE_PREFIX = "b2b:org:";
    private static final String RATE_LIMIT_PREFIX = "b2b:ratelimit:";

    private static final String RESERVE_SCRIPT = """
            local bal = redis.call('GET', KEYS[1])
            if not bal then return -2 end
            bal = tonumber(bal)
            local cost = tonumber(ARGV[1])
            if bal < cost then return -1 end
            return redis.call('DECRBY', KEYS[1], cost)
            """;

    private static final String REFUND_SCRIPT = """
            if not redis.call('GET', KEYS[1]) then return -2 end
            return redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
            """;

    private static final String RECONCILE_SCRIPT = """
            redis.call('SET', KEYS[1], tonumber(ARGV[1]))
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            return 1
            """;

    private static final String RATE_LIMIT_SCRIPT = """
            local n = redis.call('INCR', KEYS[1])
            if n == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2])) end
            if n > tonumber(ARGV[1]) then return -1 end
            return n
            """;

    private final StringRedisTemplate redisTemplate;
    private final B2bApiProperties properties;
    private final Clock clock;

    private volatile LoadedScripts loadedScripts;

    @Autowired
    public RedisMeteringService(
            final ObjectProvider<StringRedisTemplate> redisTemplate,
            final B2bApiProperties properties) {
        this(redisTemplate.getIfAvailable(), properties, Clock.systemUTC());
    }

    RedisMeteringService(
            final StringRedisTemplate redisTemplate,
            final B2bApiProperties properties,
            final Clock clock) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Reserves the maximum request cost from the organization hot balance.
     *
     * @param organizationId organization id
     * @param maxCost maximum facet cost
     * @return reservation outcome
     */
    public RedisBalanceResult reserveCredits(final UUID organizationId, final long maxCost) {
        final long result = evalInteger(sha().reserveSha(), balanceKey(organizationId), Long.toString(maxCost));
        if (result == -2L) {
            return new RedisBalanceResult(RedisBalanceStatus.BALANCE_NOT_LOADED, result);
        }
        if (result == -1L) {
            return new RedisBalanceResult(RedisBalanceStatus.INSUFFICIENT_CREDITS, result);
        }
        return new RedisBalanceResult(RedisBalanceStatus.RESERVED, result);
    }

    /**
     * Refunds a reservation delta to the organization hot balance.
     *
     * @param organizationId organization id
     * @param amount amount to refund
     * @return refund outcome
     */
    public RedisBalanceResult refundCredits(final UUID organizationId, final long amount) {
        final long result = evalInteger(sha().refundSha(), balanceKey(organizationId), Long.toString(amount));
        if (result == -2L) {
            return new RedisBalanceResult(RedisBalanceStatus.BALANCE_NOT_LOADED, result);
        }
        return new RedisBalanceResult(RedisBalanceStatus.UPDATED, result);
    }

    /**
     * Replaces the organization hot balance with the authoritative Postgres balance.
     *
     * @param organizationId organization id
     * @param durableBalance authoritative balance
     */
    public void reconcileBalance(final UUID organizationId, final long durableBalance) {
        evalInteger(
                sha().reconcileSha(),
                balanceKey(organizationId),
                Long.toString(durableBalance),
                Long.toString(properties.getRedis().getBalanceTtl().toSeconds()));
    }

    /**
     * Applies the per-key request rate limit for the current configured window.
     *
     * @param apiKeyId API key id
     * @return current request count inside the window
     */
    public long checkRateLimit(final UUID apiKeyId) {
        final Duration window = properties.getRatelimit().getWindow();
        final long windowSeconds = window.toSeconds();
        final long windowIndex = Instant.now(clock).getEpochSecond() / windowSeconds;
        final long result = evalInteger(
                sha().rateLimitSha(),
                rateLimitKey(apiKeyId, windowIndex),
                Integer.toString(properties.getRatelimit().getRequestsPerMinute()),
                Long.toString(windowSeconds));
        if (result == -1L) {
            throw new RateLimitExceededException("Rate limit exceeded.");
        }
        return result;
    }

    public String balanceKey(final UUID organizationId) {
        return BALANCE_PREFIX + organizationId + ":balance";
    }

    private String rateLimitKey(final UUID apiKeyId, final long windowIndex) {
        return RATE_LIMIT_PREFIX + apiKeyId + ":" + windowIndex;
    }

    private LoadedScripts sha() {
        final LoadedScripts current = loadedScripts;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (loadedScripts == null) {
                loadedScripts = loadScripts();
            }
            return loadedScripts;
        }
    }

    private LoadedScripts loadScripts() {
        ensureRedisTemplate();
        try {
            return redisTemplate.execute((RedisCallback<LoadedScripts>) connection -> new LoadedScripts(
                    connection.scriptLoad(bytes(RESERVE_SCRIPT)),
                    connection.scriptLoad(bytes(REFUND_SCRIPT)),
                    connection.scriptLoad(bytes(RECONCILE_SCRIPT)),
                    connection.scriptLoad(bytes(RATE_LIMIT_SCRIPT))));
        } catch (final DataAccessException exception) {
            throw new RedisUnavailableException("Redis Lua scripts could not be loaded.");
        }
    }

    private long evalInteger(final String sha, final String key, final String... args) {
        ensureRedisTemplate();
        final byte[][] keysAndArgs = new byte[args.length + 1][];
        keysAndArgs[0] = bytes(key);
        for (int i = 0; i < args.length; i++) {
            keysAndArgs[i + 1] = bytes(args[i]);
        }
        try {
            final Long result = redisTemplate.execute((RedisCallback<Long>) connection ->
                    connection.evalSha(sha, ReturnType.INTEGER, 1, keysAndArgs));
            if (result == null) {
                throw new RedisUnavailableException("Redis Lua script returned no result.");
            }
            return result;
        } catch (final DataAccessException exception) {
            throw new RedisUnavailableException("Redis Lua script execution failed.");
        }
    }

    private void ensureRedisTemplate() {
        if (redisTemplate == null) {
            throw new RedisUnavailableException("Redis is unavailable.");
        }
    }

    private byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private record LoadedScripts(
            String reserveSha,
            String refundSha,
            String reconcileSha,
            String rateLimitSha) {
    }
}
