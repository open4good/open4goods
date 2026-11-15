package org.open4goods.services.opendata.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.open4goods.services.opendata.model.DatasetManifest;
import org.open4goods.services.opendata.model.OpenDataManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads and writes the open data manifests as JSON files.
 */
public class OpenDataManifestStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataManifestStore.class);

    private final ObjectMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /**
     * Writes a manifest to {@code target}.
     */
    public void write(Object manifest, File target) throws IOException {
        try {
            Files.writeString(target.toPath(), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + "\n");
        } catch (JacksonException e) {
            throw new IOException("Unable to serialise the manifest " + target, e);
        }
    }

    /**
     * Moves a file written in the temporary folder over the published one, atomically.
     */
    public static void moveAtomically(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * @return the published manifest of a dataset, empty when absent or unreadable (first pass with manifests)
     */
    public Optional<DatasetManifest> readDataset(File file) {
        return read(file, DatasetManifest.class);
    }

    /**
     * @return the published manifest of all datasets, empty when absent or unreadable
     */
    public Optional<OpenDataManifest> readAll(File file) {
        return read(file, OpenDataManifest.class);
    }

    private <T> Optional<T> read(File file, Class<T> type) {
        if (file == null || !file.isFile()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(mapper.readValue(file, type));
        } catch (JacksonException e) {
            LOGGER.warn("Unreadable open data manifest {}: {}", file, e.getMessage());
            return Optional.empty();
        }
    }
}
