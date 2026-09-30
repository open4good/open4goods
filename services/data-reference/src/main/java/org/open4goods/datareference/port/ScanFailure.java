package org.open4goods.datareference.port;

import java.util.Objects;

/**
 * One element a scan could not process, kept so the caller can act on it.
 *
 * @param elementId identifier of the failed element
 * @param reason operator-readable reason, carrying no provider payload
 */
public record ScanFailure(String elementId, String reason) {

    /**
     * Validates the failure record.
     */
    public ScanFailure {
        Objects.requireNonNull(elementId, "elementId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (elementId.isBlank() || reason.isBlank()) {
            throw new IllegalArgumentException("failure elementId and reason must not be blank");
        }
    }
}
