package org.open4goods.services.opendata.model;

import java.time.Instant;

import org.open4goods.services.opendata.model.OpenDataGenerationStatus.State;

/**
 * State of the publication on R2 in this process (not persisted: a restart resets it to {@link State#IDLE}).
 *
 * @param state         current state ({@link State#SUCCEEDED}: the last publication wrote {@code manifest.json})
 * @param startedAt     start of the current or last publication, null before the first one
 * @param finishedAt    end of the last publication, null while running or before the first one
 * @param lastSuccessAt end of the last successful publication, null when none succeeded in this process
 * @param error         cause of the last failure, null unless {@code state} is {@link State#FAILED}
 * @param bucketBytes   size of the bucket after the last successful publication, -1 when unknown
 */
public record OpenDataPublicationStatus(
        State state,
        Instant startedAt,
        Instant finishedAt,
        Instant lastSuccessAt,
        String error,
        long bucketBytes) {

    /**
     * @return the status of a process that has not published
     */
    public static OpenDataPublicationStatus idle() {
        return new OpenDataPublicationStatus(State.IDLE, null, null, null, null, -1);
    }
}
