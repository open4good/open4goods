package org.open4goods.nudgerfrontapi.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.nudgerfrontapi.repository.GoogleIndexationQueueEntry.Status;
import org.springframework.stereotype.Repository;

/**
 * In-memory repository storing URLs pending for Google Indexation.
 */
@Repository
public class GoogleIndexationQueueRepository {

    private final Map<String, GoogleIndexationQueueEntry> entries = new ConcurrentHashMap<>();

    /**
     * Insert or reuse an entry for the provided URL.
     *
     * @param url product URL
     * @param gtin product identifier
     * @return stored queue entry
     */
    public GoogleIndexationQueueEntry upsert(String url, long gtin) {
        return entries.compute(url, (key, existing) -> {
            if (existing == null) {
                return new GoogleIndexationQueueEntry(url, gtin, Instant.now());
            }
            return existing;
        });
    }

    /**
     * Return the queue entry for a URL, if present.
     *
     * @param url product URL
     * @return optional queue entry
     */
    public Optional<GoogleIndexationQueueEntry> find(String url) {
        return Optional.ofNullable(entries.get(url));
    }

    /**
     * Retrieve a snapshot of all entries sorted by creation time.
     *
     * @return list of entries
     */
    public List<GoogleIndexationQueueEntry> findAll() {
        return entries.values().stream()
                .sorted(Comparator.comparing(GoogleIndexationQueueEntry::getCreatedAt))
                .toList();
    }

    /**
     * Fetch a batch of entries that are pending or failed.
     *
     * @param max batch size
     * @param maxAttempts maximum allowed attempts
     * @return list of entries to process
     */
    public List<GoogleIndexationQueueEntry> findPending(int max, int maxAttempts) {
        List<GoogleIndexationQueueEntry> candidates = new ArrayList<>();
        for (GoogleIndexationQueueEntry entry : entries.values()) {
            if (entry.getStatus() == Status.PENDING || entry.getStatus() == Status.FAILED) {
                if (entry.getAttempts() < maxAttempts) {
                    candidates.add(entry);
                }
            }
        }
        candidates.sort(Comparator.comparing(GoogleIndexationQueueEntry::getCreatedAt));
        return candidates.stream().limit(max).toList();
    }

    /**
     * Return the current number of entries stored.
     *
     * @return entry count
     */
    public int size() {
        return entries.size();
    }
}
