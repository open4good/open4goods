package org.open4goods.datareference.port;

import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.grouping.GroupType;
import org.open4goods.datareference.model.grouping.ModelTextNormalizer;

/**
 * One page of a prefix candidate search against the group index (GOU-199).
 *
 * @param prefix normalized, lower-case prefix to match against search tokens
 * @param type restricts the search to one group type
 * @param cursor position to resume from, empty to start
 * @param pageSize maximum elements in the page
 */
public record GroupSearchRequest(String prefix, GroupType type, Optional<ScanCursor> cursor, int pageSize) {

    /** Largest page a store is required to serve. */
    public static final int MAX_PAGE_SIZE = 1_000;

    /** Validates the search request. */
    public GroupSearchRequest {
        Objects.requireNonNull(prefix, "prefix must not be null");
        if (prefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        prefix = ModelTextNormalizer.normalize(prefix);
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(cursor, "cursor must not be null");
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    /**
     * Builds the first page of a prefix search.
     *
     * @param prefix prefix to match
     * @param type group type to restrict the search to
     * @param pageSize maximum elements in the page
     * @return search request positioned at the start
     */
    public static GroupSearchRequest first(String prefix, GroupType type, int pageSize) {
        return new GroupSearchRequest(prefix, type, ScanCursor.start(), pageSize);
    }

    /**
     * Builds the next page of this search.
     *
     * @param next cursor returned by the previous page
     * @return search request positioned after the previous page
     */
    public GroupSearchRequest resumeAt(ScanCursor next) {
        return new GroupSearchRequest(prefix, type, Optional.of(Objects.requireNonNull(next, "next must not be null")),
                pageSize);
    }
}
