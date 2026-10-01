package org.open4goods.api.services.migration.legacybackup;

/** Sink for sanitized dead letters produced while importing one dataset. */
public interface LegacyBackupDeadLetterSink {

    /**
     * Records one dead letter.
     *
     * @param deadLetter sanitized dead-letter record
     */
    void record(LegacyBackupDeadLetter deadLetter);
}
