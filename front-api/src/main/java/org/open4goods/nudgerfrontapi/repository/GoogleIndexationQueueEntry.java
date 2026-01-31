package org.open4goods.nudgerfrontapi.repository;

import java.time.Instant;

/**
 * In-memory entry representing a pending Google Indexation notification.
 */
public class GoogleIndexationQueueEntry {

    /**
     * Status of the indexation entry.
     */
    public enum Status {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        FAILED
    }

    private final String url;
    private final long gtin;
    private final Instant createdAt;
    private Status status;
    private int attempts;
    private Instant lastAttemptAt;
    private Instant lastSuccessAt;
    private String lastError;

    /**
     * Create a new queue entry.
     *
     * @param url product URL to index
     * @param gtin product identifier
     * @param createdAt creation timestamp
     */
    public GoogleIndexationQueueEntry(String url, long gtin, Instant createdAt) {
        this.url = url;
        this.gtin = gtin;
        this.createdAt = createdAt;
        this.status = Status.PENDING;
    }

    /**
     * Return the URL to index.
     *
     * @return product URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Return the product identifier.
     *
     * @return GTIN
     */
    public long getGtin() {
        return gtin;
    }

    /**
     * Return the creation timestamp.
     *
     * @return creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Return the current status.
     *
     * @return status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Update the status.
     *
     * @param status new status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Return the number of attempts.
     *
     * @return attempt count
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Set the number of attempts.
     *
     * @param attempts attempt count
     */
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    /**
     * Return the last attempt timestamp.
     *
     * @return last attempt time
     */
    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    /**
     * Set the last attempt timestamp.
     *
     * @param lastAttemptAt last attempt time
     */
    public void setLastAttemptAt(Instant lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    /**
     * Return the last success timestamp.
     *
     * @return last success time
     */
    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    /**
     * Set the last success timestamp.
     *
     * @param lastSuccessAt last success time
     */
    public void setLastSuccessAt(Instant lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    /**
     * Return the last error message.
     *
     * @return last error message
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Set the last error message.
     *
     * @param lastError last error message
     */
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
