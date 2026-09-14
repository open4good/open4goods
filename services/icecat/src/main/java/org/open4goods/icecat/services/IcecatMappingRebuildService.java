package org.open4goods.icecat.services;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.open4goods.datareference.model.registry.RegistryImportReport;
import org.open4goods.icecat.model.IcecatMappingRebuildJob;
import org.open4goods.icecat.model.IcecatMappingRebuildState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

/** Submits isolated, auditable rebuilds of the read-only Git registry projection. */
@Service
public class IcecatMappingRebuildService {

    private final IcecatRegistryProjectionService projectionService;
    private final ExecutorService executor;
    private final Clock clock;
    private final Map<UUID, IcecatMappingRebuildJob> jobs = new ConcurrentHashMap<>();

    @Autowired
    public IcecatMappingRebuildService(IcecatRegistryProjectionService projectionService) {
        this(projectionService, Executors.newVirtualThreadPerTaskExecutor(), Clock.systemUTC());
    }

    IcecatMappingRebuildService(
            IcecatRegistryProjectionService projectionService, ExecutorService executor, Clock clock) {
        this.projectionService = projectionService;
        this.executor = executor;
        this.clock = clock;
    }

    /**
     * Queues a rebuild after synchronously rejecting a stale operator precondition.
     *
     * @param expectedHash quoted or bare SHA-256 value read from the coverage endpoint
     * @return immutable job status with a stable job identifier
     */
    public IcecatMappingRebuildJob submit(String expectedHash) {
        String normalizedHash = normalizeExpectedHash(expectedHash);
        String installedHash = projectionService.current().contentHash();
        if (normalizedHash != null && !normalizedHash.equals(installedHash)) {
            throw new StaleRegistryProjectionException(normalizedHash, installedHash);
        }
        UUID jobId = UUID.randomUUID();
        IcecatMappingRebuildJob queued = new IcecatMappingRebuildJob(jobId, IcecatMappingRebuildState.QUEUED,
                installedHash, Instant.now(clock), null, null);
        jobs.put(jobId, queued);
        executor.submit(() -> rebuild(jobId, normalizedHash));
        return queued;
    }

    /** Finds an individual rebuild job without exposing any registry mutation endpoint. */
    public Optional<IcecatMappingRebuildJob> find(UUID jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    @PreDestroy
    void close() {
        executor.close();
    }

    private void rebuild(UUID jobId, String expectedHash) {
        jobs.computeIfPresent(jobId, (ignored, job) -> withState(job, IcecatMappingRebuildState.RUNNING, null, null));
        try {
            RegistryImportReport report = projectionService.rebuild(expectedHash);
            jobs.computeIfPresent(jobId, (ignored, job) -> withState(job, IcecatMappingRebuildState.SUCCEEDED,
                    report.contentHash(), null));
        } catch (RuntimeException exception) {
            jobs.computeIfPresent(jobId, (ignored, job) -> withState(job, IcecatMappingRebuildState.FAILED,
                    job.registryHash(), exception.getMessage()));
        }
    }

    private IcecatMappingRebuildJob withState(
            IcecatMappingRebuildJob job, IcecatMappingRebuildState state, String hash, String failure) {
        boolean complete = state == IcecatMappingRebuildState.SUCCEEDED || state == IcecatMappingRebuildState.FAILED;
        return new IcecatMappingRebuildJob(job.jobId(), state, hash == null ? job.registryHash() : hash,
                job.submittedAt(), complete ? Instant.now(clock) : null, failure);
    }

    private String normalizeExpectedHash(String expectedHash) {
        if (expectedHash == null || expectedHash.isBlank()) {
            return null;
        }
        String trimmed = expectedHash.trim();
        if (trimmed.length() == 66 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
