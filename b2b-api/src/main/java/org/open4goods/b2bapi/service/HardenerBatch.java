package org.open4goods.b2bapi.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.exception.RedisUnavailableException;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditTransaction;
import org.open4goods.b2bapi.model.CreditTransactionType;
import org.open4goods.b2bapi.model.UsageEvent;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UsageEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Periodic recovery and analytics batch for Redis/Postgres metering state.
 */
@Service
public class HardenerBatch {

    private static final String CONSUMER_GROUP = "hardener";
    private static final String CONSUMER_NAME = "hardener-1";
    private static final String LAST_USED_PREFIX = "b2b:lastused:";

    private final B2bApiProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final RedisMeteringService redisMeteringService;
    private final OrganizationRepository organizationRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final CreditBucketRepository creditBucketRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final UsageEventRepository usageEventRepository;
    private final Clock clock;

    @Autowired
    public HardenerBatch(
            final B2bApiProperties properties,
            final ObjectProvider<StringRedisTemplate> redisTemplate,
            final ObjectProvider<RedisMeteringService> redisMeteringService,
            final ObjectProvider<OrganizationRepository> organizationRepository,
            final ObjectProvider<ApiKeyRepository> apiKeyRepository,
            final ObjectProvider<CreditBucketRepository> creditBucketRepository,
            final ObjectProvider<CreditTransactionRepository> creditTransactionRepository,
            final ObjectProvider<UsageEventRepository> usageEventRepository) {
        this(
                properties,
                redisTemplate.getIfAvailable(),
                redisMeteringService.getIfAvailable(),
                organizationRepository.getIfAvailable(),
                apiKeyRepository.getIfAvailable(),
                creditBucketRepository.getIfAvailable(),
                creditTransactionRepository.getIfAvailable(),
                usageEventRepository.getIfAvailable(),
                Clock.systemUTC());
    }

    HardenerBatch(
            final B2bApiProperties properties,
            final StringRedisTemplate redisTemplate,
            final RedisMeteringService redisMeteringService,
            final OrganizationRepository organizationRepository,
            final ApiKeyRepository apiKeyRepository,
            final CreditBucketRepository creditBucketRepository,
            final CreditTransactionRepository creditTransactionRepository,
            final UsageEventRepository usageEventRepository,
            final Clock clock) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.redisMeteringService = redisMeteringService;
        this.organizationRepository = organizationRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.creditBucketRepository = creditBucketRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.usageEventRepository = usageEventRepository;
        this.clock = clock;
    }

    /**
     * Runs the Redis/Postgres hardener batch under a distributed scheduler lock.
     *
     * @return operation counts for observability and tests
     */
    @Scheduled(cron = "${b2b.hardener.cron:0 */5 * * * *}")
    @SchedulerLock(
            name = "HardenerBatch_run",
            lockAtLeastFor = "${b2b.hardener.lock-at-least-for:1m}",
            lockAtMostFor = "${b2b.hardener.lock-at-most-for:10m}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public HardenerBatchResult runBatch() {
        ensurePersistence();
        final int drained = drainUsageStream();
        final int expired = sweepExpiredBuckets();
        final int reconciled = reconcileBalances();
        final int lastUsed = flushLastUsed();
        return new HardenerBatchResult(drained, expired, reconciled, lastUsed);
    }

    private int drainUsageStream() {
        if (redisTemplate == null || !Boolean.TRUE.equals(redisTemplate.hasKey(UsageStreamService.USAGE_STREAM_KEY))) {
            return 0;
        }
        ensureConsumerGroup();
        final List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                StreamReadOptions.empty().count(properties.getHardener().getStreamBatchSize()),
                StreamOffset.create(UsageStreamService.USAGE_STREAM_KEY, ReadOffset.lastConsumed()));
        if (records == null || records.isEmpty()) {
            return 0;
        }
        for (final MapRecord<String, Object, Object> record : records) {
            usageEventRepository.save(toUsageEvent(record.getValue()));
            redisTemplate.opsForStream().acknowledge(UsageStreamService.USAGE_STREAM_KEY, CONSUMER_GROUP, record.getId());
        }
        return records.size();
    }

    private int sweepExpiredBuckets() {
        final List<CreditBucket> buckets = creditBucketRepository.findExpiredBucketsForSweep(clock.instant());
        for (final CreditBucket bucket : buckets) {
            final long expired = bucket.getCreditsRemaining();
            bucket.setCreditsRemaining(0);
            final CreditTransaction transaction = new CreditTransaction(
                    bucket.getOrganization(),
                    CreditTransactionType.EXPIRE,
                    -expired);
            transaction.setBucket(bucket);
            transaction.setNote("Bucket expiry sweep");
            transaction.setCreatedAt(clock.instant());
            creditTransactionRepository.insert(transaction);
        }
        return buckets.size();
    }

    private int reconcileBalances() {
        if (redisMeteringService == null) {
            return 0;
        }
        int count = 0;
        for (final UUID organizationId : creditBucketRepository.findOrganizationIdsWithBuckets()) {
            redisMeteringService.reconcileBalance(organizationId, creditBucketRepository.sumLiveCredits(organizationId));
            count++;
        }
        return count;
    }

    private int flushLastUsed() {
        if (redisTemplate == null) {
            return 0;
        }
        final Set<String> keys = redisTemplate.keys(LAST_USED_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (final String key : keys) {
            final String raw = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(raw)) {
                redisTemplate.delete(key);
                continue;
            }
            final UUID apiKeyId = UUID.fromString(key.substring(LAST_USED_PREFIX.length()));
            final Instant lastUsedAt = Instant.parse(raw);
            count += apiKeyRepository.updateLastUsedAtIfNewer(apiKeyId, lastUsedAt);
            redisTemplate.delete(key);
        }
        return count;
    }

    private void ensureConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(
                    UsageStreamService.USAGE_STREAM_KEY,
                    ReadOffset.from("0-0"),
                    CONSUMER_GROUP);
        } catch (final RedisSystemException exception) {
            if (!exception.getMessage().contains("BUSYGROUP")) {
                throw new RedisUnavailableException("Redis usage stream consumer group could not be created.");
            }
        } catch (final DataAccessException exception) {
            throw new RedisUnavailableException("Redis usage stream consumer group could not be created.");
        }
    }

    private UsageEvent toUsageEvent(final Map<Object, Object> fields) {
        final UUID organizationId = UUID.fromString(required(fields, "orgId"));
        final UsageEvent event = new UsageEvent(
                organizationRepository.getReferenceById(organizationId),
                required(fields, "facetId"),
                required(fields, "requestId"),
                Short.parseShort(required(fields, "httpStatus")),
                Boolean.parseBoolean(required(fields, "billable")));
        final String keyId = value(fields, "keyId");
        if (StringUtils.hasText(keyId)) {
            event.setApiKey(apiKeyRepository.getReferenceById(UUID.fromString(keyId)));
        }
        event.setGtin(emptyToNull(value(fields, "gtin")));
        event.setCreditsConsumed(Long.parseLong(required(fields, "creditsConsumed")));
        event.setNoPayReason(emptyToNull(value(fields, "noPayReason")));
        final String responseTimeMs = value(fields, "responseTimeMs");
        if (StringUtils.hasText(responseTimeMs)) {
            event.setResponseTimeMs(Integer.parseInt(responseTimeMs));
        }
        event.setCreatedAt(Instant.parse(required(fields, "ts")));
        return event;
    }

    private String required(final Map<Object, Object> fields, final String key) {
        final String value = value(fields, key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Missing usage stream field: " + key);
        }
        return value;
    }

    private String value(final Map<Object, Object> fields, final String key) {
        final Object value = fields.get(key);
        return value == null ? "" : value.toString();
    }

    private String emptyToNull(final String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private void ensurePersistence() {
        if (organizationRepository == null
                || apiKeyRepository == null
                || creditBucketRepository == null
                || creditTransactionRepository == null
                || usageEventRepository == null) {
            throw new IllegalStateException("Hardener persistence is unavailable.");
        }
    }
}
