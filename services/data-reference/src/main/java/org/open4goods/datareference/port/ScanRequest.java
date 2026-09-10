package org.open4goods.datareference.port;

import java.util.Objects;
import java.util.Optional;

/**
 * One page of a batch scan, with everything a caller needs to resume it.
 *
 * @param cursor position to resume from, empty to start
 * @param pageSize maximum elements in the page
 * @param order ordering the store must guarantee
 * @param failurePolicy behavior when one element fails
 */
public record ScanRequest(
        Optional<ScanCursor> cursor,
        int pageSize,
        ScanOrder order,
        ScanFailurePolicy failurePolicy) {

    /** Largest page a store is required to serve. */
    public static final int MAX_PAGE_SIZE = 10_000;

    /**
     * Validates the scan request.
     */
    public ScanRequest {
        Objects.requireNonNull(cursor, "cursor must not be null");
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(failurePolicy, "failurePolicy must not be null");
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    /**
     * Builds a first page ordered by record key, failing fast.
     *
     * @param pageSize maximum elements in the page
     * @return scan request positioned at the start
     */
    public static ScanRequest first(int pageSize) {
        return new ScanRequest(ScanCursor.start(), pageSize, ScanOrder.RECORD_KEY, ScanFailurePolicy.FAIL_FAST);
    }

    /**
     * Builds the next page of this scan, keeping order and failure behavior.
     *
     * @param next cursor returned by the previous page
     * @return scan request positioned after the previous page
     */
    public ScanRequest resumeAt(ScanCursor next) {
        return new ScanRequest(Optional.of(Objects.requireNonNull(next, "next must not be null")),
                pageSize, order, failurePolicy);
    }
}
