package org.open4goods.nudgerfrontapi.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent snapshot of the Google Indexing queue and its historical metadata.
 */
public class GoogleIndexingQueueState {

    private List<GoogleIndexingQueueItem> pendingItems = new ArrayList<>();
    private Map<String, Instant> indexedUrls = new LinkedHashMap<>();
    private Map<String, Instant> deadLetterUrls = new LinkedHashMap<>();
    private Instant lastSuccessAt;
    private Instant lastFailureAt;

    /**
     * Return the list of pending URLs.
     *
     * @return pending queue entries
     */
    public List<GoogleIndexingQueueItem> getPendingItems() {
        return pendingItems;
    }

    /**
     * Replace the pending queue entries.
     *
     * @param pendingItems new pending queue entries
     */
    public void setPendingItems(List<GoogleIndexingQueueItem> pendingItems) {
        this.pendingItems = pendingItems;
    }

    /**
     * Return the URLs that were recently indexed.
     *
     * @return map of URL to last successful submission timestamp
     */
    public Map<String, Instant> getIndexedUrls() {
        return indexedUrls;
    }

    /**
     * Replace the map of indexed URLs.
     *
     * @param indexedUrls map of URL to timestamp
     */
    public void setIndexedUrls(Map<String, Instant> indexedUrls) {
        this.indexedUrls = indexedUrls;
    }

    /**
     * Return the URLs that reached the maximum retry count.
     *
     * @return map of URL to dead-letter timestamp
     */
    public Map<String, Instant> getDeadLetterUrls() {
        return deadLetterUrls;
    }

    /**
     * Replace the map of dead-letter URLs.
     *
     * @param deadLetterUrls map of URL to timestamp
     */
    public void setDeadLetterUrls(Map<String, Instant> deadLetterUrls) {
        this.deadLetterUrls = deadLetterUrls;
    }

    /**
     * Return the timestamp of the last successful submission.
     *
     * @return last success timestamp
     */
    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    /**
     * Set the last success timestamp.
     *
     * @param lastSuccessAt timestamp of the last success
     */
    public void setLastSuccessAt(Instant lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    /**
     * Return the timestamp of the last failed submission.
     *
     * @return last failure timestamp
     */
    public Instant getLastFailureAt() {
        return lastFailureAt;
    }

    /**
     * Set the last failure timestamp.
     *
     * @param lastFailureAt timestamp of the last failure
     */
    public void setLastFailureAt(Instant lastFailureAt) {
        this.lastFailureAt = lastFailureAt;
    }
}
