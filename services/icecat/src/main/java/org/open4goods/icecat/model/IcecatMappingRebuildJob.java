package org.open4goods.icecat.model;

import java.time.Instant;
import java.util.UUID;

/** Immutable status view of an asynchronous registry projection rebuild. */
public record IcecatMappingRebuildJob(
        UUID jobId,
        IcecatMappingRebuildState state,
        String registryHash,
        Instant submittedAt,
        Instant completedAt,
        String failure) {
}
