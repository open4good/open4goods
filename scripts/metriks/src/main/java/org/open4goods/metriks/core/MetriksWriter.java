package org.open4goods.metriks.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writes metriks payloads to the history folder using atomic updates.
 */
public class MetriksWriter {

    private final ObjectMapper objectMapper;
    private final Path rootPath;

    public MetriksWriter(ObjectMapper objectMapper, Path rootPath) {
        this.objectMapper = objectMapper;
        this.rootPath = rootPath;
    }

    /**
     * Persist the payload to the history path.
     *
     * @param payload payload to write
     * @param dateKey date key YYYYMMDD
     * @throws IOException if writing fails
     */
    public void write(MetrikPayload payload, String dateKey) throws IOException {
        Path entryFolder = rootPath
                .resolve(payload.event_provider())
                .resolve(payload.event_id());
        Files.createDirectories(entryFolder);

        Path datedFile = entryFolder.resolve(dateKey + ".json");
        Path latestFile = entryFolder.resolve("latest.json");

        writeAtomically(datedFile, payload);
        writeAtomically(latestFile, payload);
    }

    private void writeAtomically(Path target, MetrikPayload payload) throws IOException {
        Path tempFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), payload);
        try {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
