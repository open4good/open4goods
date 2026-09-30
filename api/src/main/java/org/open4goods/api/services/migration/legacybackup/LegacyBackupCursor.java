package org.open4goods.api.services.migration.legacybackup;

import java.util.Optional;

import org.open4goods.datareference.port.ScanCursor;

/**
 * Durable progress within one dataset's manifest-ordered file list.
 *
 * @param fileIndex 0-based index of the file currently (or next) being imported
 * @param committedLines lines of {@code fileIndex} already reconciled into the target stores
 */
public record LegacyBackupCursor(int fileIndex, long committedLines) {

    /** Validates a non-negative position. */
    public LegacyBackupCursor {
        if (fileIndex < 0) {
            throw new IllegalArgumentException("fileIndex must not be negative");
        }
        if (committedLines < 0) {
            throw new IllegalArgumentException("committedLines must not be negative");
        }
    }

    /** Cursor at the very start of a dataset's import. */
    public static final LegacyBackupCursor START = new LegacyBackupCursor(0, 0);

    /**
     * Encodes this cursor as the opaque token an ingestion checkpoint carries.
     *
     * @return store-facing scan cursor
     */
    public ScanCursor toScanCursor() {
        return new ScanCursor(fileIndex + ":" + committedLines);
    }

    /**
     * Decodes a checkpoint's opaque cursor back into a dataset position.
     *
     * @param cursor optional stored cursor; empty reads as {@link #START}
     * @return decoded position
     */
    public static LegacyBackupCursor fromScanCursor(Optional<ScanCursor> cursor) {
        if (cursor.isEmpty()) {
            return START;
        }
        String token = cursor.orElseThrow().token();
        int separator = token.indexOf(':');
        if (separator < 0) {
            throw new LegacyBackupInputException("checkpoint cursor is malformed: " + token);
        }
        try {
            int fileIndex = Integer.parseInt(token.substring(0, separator));
            long committedLines = Long.parseLong(token.substring(separator + 1));
            return new LegacyBackupCursor(fileIndex, committedLines);
        } catch (NumberFormatException exception) {
            throw new LegacyBackupInputException("checkpoint cursor is malformed: " + token, exception);
        }
    }

    /**
     * Returns the cursor after committing {@code additionalLines} more lines of the current file.
     *
     * @param additionalLines lines newly reconciled in {@code fileIndex}
     * @return advanced cursor within the same file
     */
    public LegacyBackupCursor advance(long additionalLines) {
        return new LegacyBackupCursor(fileIndex, committedLines + additionalLines);
    }

    /**
     * Returns the cursor positioned at the start of the next file.
     *
     * @return cursor for {@code fileIndex + 1}
     */
    public LegacyBackupCursor nextFile() {
        return new LegacyBackupCursor(fileIndex + 1, 0);
    }
}
