package org.open4goods.nudgerfrontapi.service.opendata;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.services.opendata.model.DatasetManifest;
import org.open4goods.services.opendata.model.OpenDataManifest;

/**
 * Result of the last read of the published {@code manifest.json} (GOU-2290).
 *
 * @param state    outcome of the last read
 * @param manifest last manifest read successfully, kept after a failed read; {@code null} when none was read
 * @param readAt   time of the last successful read; {@code null} when none
 * @param error    reason of the last failed read; {@code null} when the last read succeeded or nothing is configured
 */
public record OpenDataManifestSnapshot(State state, OpenDataManifest manifest, Instant readAt, String error) {

    /**
     * Outcome of the last read.
     */
    public enum State {
        /** {@code open-data-config.public-base-url} is empty: the manifest is never read. */
        NOT_CONFIGURED,
        /** The last read succeeded. */
        AVAILABLE,
        /** The last read failed (HTTP error, network, invalid JSON); {@link #manifest()} is the previous one, if any. */
        UNAVAILABLE
    }

    /** Snapshot returned while no base URL is configured. */
    public static final OpenDataManifestSnapshot NOT_CONFIGURED =
            new OpenDataManifestSnapshot(State.NOT_CONFIGURED, null, null, null);

    /**
     * @param id dataset identifier ({@code gtin}, {@code isbn}, {@code impact-...})
     * @return the entry of this dataset in the manifest, empty when there is no manifest or no such entry
     */
    public Optional<DatasetManifest> dataset(String id) {
        if (manifest == null || manifest.datasets() == null) {
            return Optional.empty();
        }
        return manifest.datasets().stream()
                .filter(Objects::nonNull)
                .filter(entry -> id.equals(entry.id()))
                .findFirst();
    }

    /**
     * @param dataset entry of the manifest
     * @return its {@code generatedAt}, empty when absent or not an ISO 8601 instant (never epoch 0)
     */
    public static Optional<Instant> generatedAt(DatasetManifest dataset) {
        if (dataset == null || dataset.generatedAt() == null || dataset.generatedAt().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(dataset.generatedAt().trim()));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
