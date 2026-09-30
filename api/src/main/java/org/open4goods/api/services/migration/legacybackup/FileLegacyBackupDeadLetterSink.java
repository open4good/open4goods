package org.open4goods.api.services.migration.legacybackup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Appends sanitized dead letters as NDJSON to a private, per-dataset file outside the
 * configured dataset root and never tracked in Git.
 *
 * <p>Single-writer per {@link LegacyBackupImportService} invocation: callers serialize their own
 * writes (the importer processes one dataset's batches sequentially), so no internal locking is
 * needed here beyond the atomic append the filesystem already provides for {@code O_APPEND}.
 */
public final class FileLegacyBackupDeadLetterSink implements LegacyBackupDeadLetterSink {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path deadLetterFile;

    public FileLegacyBackupDeadLetterSink(Path deadLetterFolder, String datasetId) {
        Objects.requireNonNull(deadLetterFolder, "deadLetterFolder must not be null");
        Objects.requireNonNull(datasetId, "datasetId must not be null");
        try {
            Files.createDirectories(deadLetterFolder);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        this.deadLetterFile = deadLetterFolder.resolve(datasetId + "-dead-letters.ndjson");
    }

    @Override
    public void record(LegacyBackupDeadLetter deadLetter) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("datasetId", deadLetter.datasetId());
        line.put("fileName", deadLetter.fileName());
        line.put("lineNumber", deadLetter.lineNumber());
        line.put("reason", deadLetter.reason().name());
        line.put("detail", deadLetter.detail());
        line.put("recordedAt", deadLetter.recordedAt().toString());
        try {
            String json = MAPPER.writeValueAsString(line);
            Files.writeString(deadLetterFile, json + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new UncheckedIOException("unable to write dead letter", exception);
        }
    }
}
