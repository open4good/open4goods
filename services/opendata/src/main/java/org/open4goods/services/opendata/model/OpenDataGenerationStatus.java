package org.open4goods.services.opendata.model;

import java.time.Instant;
import java.util.Map;

/**
 * State of the open data generation in this process (not persisted: a restart resets it to {@link State#IDLE}).
 *
 * @param state         current state
 * @param startedAt     start of the current or last pass, null before the first one
 * @param finishedAt    end of the last pass, null while running or before the first one
 * @param lastSuccessAt end of the last successful pass, null when none succeeded in this process
 * @param error         cause of the last failure, null unless {@code state} is {@link State#FAILED}
 * @param rows          rows per dataset id of the last successful pass
 */
public record OpenDataGenerationStatus(
        State state,
        Instant startedAt,
        Instant finishedAt,
        Instant lastSuccessAt,
        String error,
        Map<String, Long> rows) {

    /** Generation state. */
    public enum State {
        /** No pass since the start of the process. */
        IDLE,
        /** A pass is running. */
        RUNNING,
        /** The last pass published new files. */
        SUCCEEDED,
        /** The last pass failed; the previous files and manifests stay published. */
        FAILED
    }

    /**
     * @return the status of a process that has not run any pass
     */
    public static OpenDataGenerationStatus idle() {
        return new OpenDataGenerationStatus(State.IDLE, null, null, null, null, Map.of());
    }
}
