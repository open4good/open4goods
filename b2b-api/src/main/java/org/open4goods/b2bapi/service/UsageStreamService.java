package org.open4goods.b2bapi.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.exception.RedisUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Emits finished-request analytics events to the Redis usage stream.
 */
@Service
public class UsageStreamService {

    public static final String USAGE_STREAM_KEY = "b2b:usage";

    private final StringRedisTemplate redisTemplate;
    private final B2bApiProperties properties;
    private final Clock clock;

    @Autowired
    public UsageStreamService(
            final ObjectProvider<StringRedisTemplate> redisTemplate,
            final B2bApiProperties properties) {
        this(redisTemplate.getIfAvailable(), properties, Clock.systemUTC());
    }

    UsageStreamService(
            final StringRedisTemplate redisTemplate,
            final B2bApiProperties properties,
            final Clock clock) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Emits one usage event to Redis and trims the stream approximately.
     *
     * @param event usage event
     */
    public void emit(final UsageStreamEvent event) {
        if (redisTemplate == null) {
            throw new RedisUnavailableException("Redis usage stream is unavailable.");
        }
        final Map<String, String> fields = fields(event);
        try {
            redisTemplate.opsForStream().add(MapRecord.create(USAGE_STREAM_KEY, fields));
            redisTemplate.opsForStream().trim(
                    USAGE_STREAM_KEY,
                    properties.getRedis().getUsageStreamMaxlen(),
                    true);
        } catch (final DataAccessException exception) {
            throw new RedisUnavailableException("Redis usage stream write failed.");
        }
    }

    private Map<String, String> fields(final UsageStreamEvent event) {
        final Instant timestamp = event.timestamp() == null ? clock.instant() : event.timestamp();
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("orgId", event.organizationId().toString());
        fields.put("keyId", event.apiKeyId() == null ? "" : event.apiKeyId().toString());
        fields.put("facetId", event.facetId());
        fields.put("gtin", nullToEmpty(event.gtin()));
        fields.put("requestId", event.requestId());
        fields.put("httpStatus", Integer.toString(event.httpStatus()));
        fields.put("billable", Boolean.toString(event.billable()));
        fields.put("creditsConsumed", Long.toString(event.creditsConsumed()));
        fields.put("noPayReason", nullToEmpty(event.noPayReason()));
        fields.put("responseTimeMs", event.responseTimeMs() == null ? "" : event.responseTimeMs().toString());
        fields.put("ts", timestamp.toString());
        return fields;
    }

    private String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }
}
