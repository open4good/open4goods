package org.open4goods.nudgerfrontapi.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.open4goods.nudgerfrontapi.config.properties.CacheProperties;
import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexingProperties;
import org.open4goods.nudgerfrontapi.model.GoogleIndexingQueueItem;
import org.open4goods.nudgerfrontapi.model.GoogleIndexingQueueState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * File-backed repository storing pending Google Indexing URLs and submission history.
 */
@Repository
public class GoogleIndexingQueueRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleIndexingQueueRepository.class);

    private final ObjectMapper objectMapper;
    private final Path queueFilePath;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    private GoogleIndexingQueueState state;

    /**
     * Create the repository and load persisted queue state when available.
     *
     * @param cacheProperties cache configuration providing the base folder
     * @param properties      Google Indexing configuration
     * @param objectMapper    mapper used to serialize queue data
     * @param clock           clock used for timestamps
     */
    public GoogleIndexingQueueRepository(CacheProperties cacheProperties,
                                         GoogleIndexingProperties properties,
                                         ObjectMapper objectMapper,
                                         Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.queueFilePath = Path.of(cacheProperties.getPath(), properties.getQueueFileName());
        this.state = loadState().orElseGet(GoogleIndexingQueueState::new);
    }

    /**
     * Return a snapshot of the queue state.
     *
     * @return immutable snapshot of the current queue
     */
    public GoogleIndexingQueueSnapshot snapshot() {
        lock.lock();
        try {
            return new GoogleIndexingQueueSnapshot(
                    List.copyOf(state.getPendingItems()),
                    Map.copyOf(state.getIndexedUrls()),
                    Map.copyOf(state.getDeadLetterUrls()),
                    state.getLastSuccessAt(),
                    state.getLastFailureAt());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Enqueue a new URL if it is not already pending or recently indexed.
     *
     * @param url        absolute URL to enqueue
     * @param retention retention window for indexed history
     * @return {@code true} when the URL was added
     */
    public boolean enqueue(String url, Duration retention) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        lock.lock();
        try {
            Instant now = clock.instant();
            cleanupHistory(retention, now);
            if (state.getPendingItems().stream().anyMatch(item -> url.equals(item.url()))
                    || state.getIndexedUrls().containsKey(url)) {
                return false;
            }
            state.getPendingItems().add(new GoogleIndexingQueueItem(url, now, null, 0));
            persistState();
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reserve a batch of items that are ready for submission.
     *
     * @param batchSize  maximum number of items to return
     * @param retryDelay delay required between attempts
     * @param maxAttempts maximum number of attempts before discarding an entry
     * @param retention retention duration for history cleanup
     * @return list of reserved queue items
     */
    public List<GoogleIndexingQueueItem> reserveBatch(int batchSize,
                                                      Duration retryDelay,
                                                      int maxAttempts,
                                                      Duration retention) {
        lock.lock();
        try {
            Instant now = clock.instant();
            cleanupHistory(retention, now);
            List<GoogleIndexingQueueItem> eligible = state.getPendingItems().stream()
                    .filter(item -> item.attemptCount() < maxAttempts)
                    .filter(item -> item.lastAttemptAt() == null
                            || item.lastAttemptAt().plus(retryDelay).isBefore(now)
                            || item.lastAttemptAt().plus(retryDelay).equals(now))
                    .limit(batchSize)
                    .toList();
            if (eligible.isEmpty()) {
                return List.of();
            }
            Map<String, GoogleIndexingQueueItem> updated = eligible.stream()
                    .collect(Collectors.toMap(GoogleIndexingQueueItem::url, item -> new GoogleIndexingQueueItem(
                            item.url(),
                            item.createdAt(),
                            now,
                            item.attemptCount() + 1)));
            List<GoogleIndexingQueueItem> refreshed = new ArrayList<>(state.getPendingItems().size());
            for (GoogleIndexingQueueItem item : state.getPendingItems()) {
                refreshed.add(updated.getOrDefault(item.url(), item));
            }
            state.setPendingItems(refreshed);
            persistState();
            return eligible.stream()
                    .map(item -> updated.getOrDefault(item.url(), item))
                    .toList();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Mark a queue entry as successfully indexed.
     *
     * @param url URL that was indexed
     */
    public void markSuccess(String url) {
        lock.lock();
        try {
            Instant now = clock.instant();
            state.setLastSuccessAt(now);
            state.getPendingItems().removeIf(item -> url.equals(item.url()));
            state.getIndexedUrls().put(url, now);
            persistState();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Mark a queue entry as failed and move to dead-letter storage if needed.
     *
     * @param item        item that failed to index
     * @param maxAttempts maximum number of retry attempts
     */
    public void markFailure(GoogleIndexingQueueItem item, int maxAttempts) {
        lock.lock();
        try {
            Instant now = clock.instant();
            state.setLastFailureAt(now);
            if (item.attemptCount() >= maxAttempts) {
                state.getPendingItems().removeIf(entry -> entry.url().equals(item.url()));
                state.getDeadLetterUrls().put(item.url(), now);
            }
            persistState();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove expired history entries from indexed and dead-letter collections.
     *
     * @param retention retention window used to keep history
     * @param now       current timestamp
     */
    private void cleanupHistory(Duration retention, Instant now) {
        Instant cutoff = now.minus(retention);
        pruneHistoryMap(state.getIndexedUrls(), cutoff);
        pruneHistoryMap(state.getDeadLetterUrls(), cutoff);
    }

    /**
     * Remove map entries that are older than the cutoff time.
     *
     * @param map    map to prune
     * @param cutoff cutoff timestamp
     */
    private void pruneHistoryMap(Map<String, Instant> map, Instant cutoff) {
        Iterator<Map.Entry<String, Instant>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Instant> entry = iterator.next();
            if (entry.getValue() != null && entry.getValue().isBefore(cutoff)) {
                iterator.remove();
            }
        }
    }

    /**
     * Load the queue state from disk when present.
     *
     * @return optional loaded state
     */
    private Optional<GoogleIndexingQueueState> loadState() {
        if (!Files.exists(queueFilePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(queueFilePath.toFile(), GoogleIndexingQueueState.class));
        } catch (IOException ex) {
            LOGGER.warn("Failed to read Google indexing queue state: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Persist the queue state to disk.
     */
    private void persistState() {
        try {
            Files.createDirectories(queueFilePath.getParent());
            Path tmp = queueFilePath.resolveSibling(queueFilePath.getFileName() + ".tmp");
            objectMapper.writeValue(tmp.toFile(), state);
            try {
                Files.move(tmp, queueFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ex) {
                Files.move(tmp, queueFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            LOGGER.warn("Failed to persist Google indexing queue state: {}", ex.getMessage());
        }
    }

    /**
     * Snapshot object describing the current queue state.
     *
     * @param pendingItems  list of pending entries
     * @param indexedUrls   map of recently indexed URLs
     * @param deadLetterUrls map of URLs dropped after max attempts
     * @param lastSuccessAt timestamp of last success
     * @param lastFailureAt timestamp of last failure
     */
    public record GoogleIndexingQueueSnapshot(
            List<GoogleIndexingQueueItem> pendingItems,
            Map<String, Instant> indexedUrls,
            Map<String, Instant> deadLetterUrls,
            Instant lastSuccessAt,
            Instant lastFailureAt) {
    }
}
