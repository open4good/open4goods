package org.open4goods.services.feedservice.definition;

/**
 * Raised when a feed's {@code csvDatasource} configuration declares neither an explicit url
 * nor an explicit price column.
 *
 * <p>Both are required to build a {@link FeedDefinition} deterministically. Rather than guess
 * either from a label (AC2), the feed is reported and not indexed (fail-closed).
 */
public class MissingRequiredColumnsException extends RuntimeException {

    public MissingRequiredColumnsException(String message) {
        super(message);
    }
}
