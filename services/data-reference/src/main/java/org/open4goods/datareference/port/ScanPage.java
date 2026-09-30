package org.open4goods.datareference.port;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One page of scan results.
 *
 * <p>An empty page with a cursor is not the end of a scan: a store that filters
 * server-side can legitimately return no element while advancing. Callers stop
 * when {@link #nextCursor()} is empty, never when the page is empty.
 *
 * @param elements elements of this page, in the requested order
 * @param nextCursor position of the next page, empty when the scan is exhausted
 * @param failures elements skipped under {@link ScanFailurePolicy#SKIP_AND_REPORT}
 * @param <T> element type
 */
public record ScanPage<T>(List<T> elements, Optional<ScanCursor> nextCursor, List<ScanFailure> failures) {

    /**
     * Copies the page content.
     */
    public ScanPage {
        elements = List.copyOf(Objects.requireNonNull(elements, "elements must not be null"));
        Objects.requireNonNull(nextCursor, "nextCursor must not be null");
        failures = List.copyOf(Objects.requireNonNull(failures, "failures must not be null"));
    }

    /**
     * Builds the final page of a scan.
     *
     * @param elements elements of the page
     * @param <T> element type
     * @return page with no next cursor
     */
    public static <T> ScanPage<T> last(List<T> elements) {
        return new ScanPage<>(elements, Optional.empty(), List.of());
    }

    /**
     * Reports whether the caller should request another page.
     *
     * @return {@code true} while a next cursor is present
     */
    public boolean hasMore() {
        return nextCursor.isPresent();
    }
}
