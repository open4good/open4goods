package org.open4goods.datareference.port;

import java.util.Objects;
import java.util.Optional;

/**
 * Opaque position in a batch scan.
 *
 * <p>Opaque on purpose: the caller stores it in a checkpoint and hands it back,
 * but must not parse it. A store is free to change from a sort key to a
 * point-in-time identifier without breaking every caller that had learned to
 * read the old shape.
 *
 * @param token store-defined position token
 */
public record ScanCursor(String token) {

    /**
     * Validates the cursor token.
     */
    public ScanCursor {
        Objects.requireNonNull(token, "token must not be null");
        if (token.isBlank()) {
            throw new IllegalArgumentException("cursor token must not be blank");
        }
    }

    /**
     * Returns the cursor that starts a scan from the beginning.
     *
     * @return empty cursor
     */
    public static Optional<ScanCursor> start() {
        return Optional.empty();
    }
}
