package org.open4goods.api.services.migration.legacybackup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Reopens one pinned gzip JSONL file from the start and skips already-committed lines by
 * decompressing and discarding them, never seeking a compressed byte offset directly (AC3).
 *
 * <p>A crash or restart therefore always resumes by re-decompressing from byte zero up to the
 * last committed line, which is correct but not free; callers checkpoint often enough (one
 * importer batch) that the redo cost stays bounded.
 */
public final class ResumableLegacyBackupReader {

    private final LegacyBackupFileAccess fileAccess;

    public ResumableLegacyBackupReader(LegacyBackupFileAccess fileAccess) {
        this.fileAccess = fileAccess;
    }

    /**
     * Streams every line after {@code alreadyCommittedLines}, calling {@code consumer} with the
     * 1-based line number and raw line text until it returns {@code false} or input is exhausted.
     *
     * @param path validated archive path
     * @param alreadyCommittedLines number of leading lines to skip without invoking the consumer
     * @param consumer receives (lineNumber, rawLine); returning {@code false} stops the scan early
     *     (used for cooperative cancellation)
     * @return the number of lines the consumer accepted ({@code true}) in this call
     */
    public long resumeFrom(Path path, long alreadyCommittedLines, LineConsumer consumer) {
        if (alreadyCommittedLines < 0) {
            throw new IllegalArgumentException("alreadyCommittedLines must not be negative");
        }
        long accepted = 0;
        try (BufferedReader reader = fileAccess.openForReading(path)) {
            long skipped = 0;
            while (skipped < alreadyCommittedLines) {
                if (reader.readLine() == null) {
                    throw new LegacyBackupInputException(
                            "archive has fewer lines than already committed: " + path.getFileName());
                }
                skipped++;
            }
            long lineNumber = alreadyCommittedLines;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                boolean shouldContinue = consumer.accept(lineNumber, line);
                accepted++;
                if (!shouldContinue) {
                    break;
                }
            }
        } catch (IOException exception) {
            throw new LegacyBackupInputException("corrupt gzip archive: " + path.getFileName(), exception);
        } catch (UncheckedIOException exception) {
            throw new LegacyBackupInputException("corrupt gzip archive: " + path.getFileName(), exception.getCause());
        }
        return accepted;
    }

    /** Receives one decoded JSONL line at its 1-based position in the file. */
    @FunctionalInterface
    public interface LineConsumer {

        /**
         * Consumes one line.
         *
         * @param lineNumber 1-based position of this line in the decompressed file
         * @param rawLine raw JSONL line text
         * @return {@code true} to continue reading, {@code false} to stop after this line
         */
        boolean accept(long lineNumber, String rawLine);
    }
}
