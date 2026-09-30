package org.open4goods.api.services.migration.legacybackup;

/**
 * Raised when the pinned dataset input cannot be proved safe to read: an unsafe file name, a
 * missing file, a digest or line-count mismatch, or a corrupt gzip stream.
 */
public class LegacyBackupInputException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LegacyBackupInputException(String message) {
        super(message);
    }

    public LegacyBackupInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
