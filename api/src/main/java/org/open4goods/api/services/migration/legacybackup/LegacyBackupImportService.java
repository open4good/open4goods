package org.open4goods.api.services.migration.legacybackup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.open4goods.api.services.migration.legacybackup.LegacyBackupInputManifest.LegacyBackupFileEntry;
import org.open4goods.datareference.port.IngestionCheckpointStore;
import org.open4goods.datareference.port.SourceRecordHeadStore;
import org.open4goods.pricehistory.service.LegacyPriceBackfillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Orchestrates the resumable legacy backup to neutral data importer.
 *
 * <p>No method here is ever invoked automatically: nothing is {@code @Scheduled}, and nothing
 * runs from an {@code ApplicationRunner}. Every operation is triggered explicitly by an operator
 * through {@link LegacyBackupImportController} (AC1). This service does not call
 * {@code BackupService.importProducts} or write to the active legacy {@code ProductRepository}
 * (AC2): it only writes to {@link SourceRecordHeadStore} and the legacy price backfill store.
 */
public final class LegacyBackupImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyBackupImportService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final org.open4goods.api.config.yml.LegacyBackupImportConfig config;
    private final SourceRecordHeadStore sourceRecordStore;
    private final LegacyPriceBackfillService priceBackfillService;
    private final LegacyBackupCheckpointCoordinator checkpointCoordinator;
    private final LegacyBackupCancellationRegistry cancellationRegistry;

    public LegacyBackupImportService(
            org.open4goods.api.config.yml.LegacyBackupImportConfig config,
            SourceRecordHeadStore sourceRecordStore,
            LegacyPriceBackfillService priceBackfillService,
            IngestionCheckpointStore checkpointStore,
            LegacyBackupCancellationRegistry cancellationRegistry) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.sourceRecordStore = Objects.requireNonNull(sourceRecordStore, "sourceRecordStore must not be null");
        this.priceBackfillService = Objects.requireNonNull(priceBackfillService, "priceBackfillService must not be null");
        this.checkpointCoordinator = new LegacyBackupCheckpointCoordinator(
                Objects.requireNonNull(checkpointStore, "checkpointStore must not be null"), config.getCheckpointOwner());
        this.cancellationRegistry = Objects.requireNonNull(cancellationRegistry, "cancellationRegistry must not be null");
    }

    /**
     * Validates the pinned manifest and every listed file's digest and line count. Writes
     * nothing.
     *
     * @param request explicit dataset id and manifest path
     * @return inventory report
     */
    public LegacyBackupInventoryReport inventory(LegacyBackupImportRequest request) {
        LegacyBackupInputManifest manifest = loadManifest(request.manifestPath());
        LegacyBackupFileAccess fileAccess = fileAccess();
        List<LegacyBackupInventoryReport.VerifiedFile> verified = new ArrayList<>();
        long totalLines = 0;
        for (LegacyBackupFileEntry entry : manifest.files()) {
            Path resolved = fileAccess.resolveSafely(entry.name());
            fileAccess.verify(resolved, entry);
            verified.add(new LegacyBackupInventoryReport.VerifiedFile(entry.name(), entry.bytes(), entry.lineCount()));
            totalLines += entry.lineCount();
        }
        return new LegacyBackupInventoryReport(
                request.datasetId(), manifest.schemaVersion(), manifest.conversionRules().mappingVersion(),
                LegacyBackupUsagePolicy.POLICY.policyId(), LegacyBackupUsagePolicy.POLICY.version(), verified, totalLines);
    }

    /**
     * Runs {@link #inventory} plus a dry-run classification of a bounded sample of the first
     * manifest file's lines. Writes nothing.
     *
     * @param request explicit dataset id and manifest path
     * @param sampleLines number of leading lines of the first file to classify
     * @return sample report
     */
    public LegacyBackupSampleReport sample(LegacyBackupImportRequest request, long sampleLines) {
        if (sampleLines < 1) {
            throw new IllegalArgumentException("sampleLines must be positive");
        }
        LegacyBackupInventoryReport inventoryReport = inventory(request);
        LegacyBackupInputManifest manifest = loadManifest(request.manifestPath());
        LegacyBackupFileAccess fileAccess = fileAccess();
        LegacyBackupRecordConverter converter = new LegacyBackupRecordConverter(manifest.conversionRules());
        Instant observedAt = Instant.parse(manifest.legacyManifest().completedAt());

        LegacyBackupFileEntry firstFile = manifest.files().get(0);
        Path resolved = fileAccess.resolveSafely(firstFile.name());

        AtomicLong sampled = new AtomicLong();
        AtomicLong convertible = new AtomicLong();
        Map<LegacyBackupDeadLetterReason, Long> deadLetterCounts = new EnumMap<>(LegacyBackupDeadLetterReason.class);

        new ResumableLegacyBackupReader(fileAccess).resumeFrom(resolved, 0, (lineNumber, rawLine) -> {
            LegacyBackupRecordConversion conversion = converter.convert(
                    request.datasetId(), firstFile.name(), lineNumber, rawLine, observedAt);
            sampled.incrementAndGet();
            if (conversion.mutation().isPresent()) {
                convertible.incrementAndGet();
            }
            for (LegacyBackupDeadLetter deadLetter : conversion.deadLetters()) {
                deadLetterCounts.merge(deadLetter.reason(), 1L, Long::sum);
            }
            return sampled.get() < sampleLines;
        });

        return new LegacyBackupSampleReport(inventoryReport, sampled.get(), convertible.get(), deadLetterCounts);
    }

    /**
     * Resumable write of source, legacy baseline and price stores from the committed checkpoint
     * position, batching checkpoint advances and stopping cleanly on cancellation.
     *
     * @param request explicit dataset id and manifest path
     * @return the outcome of this invocation
     */
    public LegacyBackupApplyResult apply(LegacyBackupImportRequest request) {
        LegacyBackupInputManifest manifest = loadManifest(request.manifestPath());
        LegacyBackupFileAccess fileAccess = fileAccess();
        LegacyBackupRecordConverter converter = new LegacyBackupRecordConverter(manifest.conversionRules());
        Instant observedAt = Instant.parse(manifest.legacyManifest().completedAt());
        LegacyBackupDeadLetterSink deadLetterSink = new FileLegacyBackupDeadLetterSink(
                Path.of(config.getDeadLetterFolder()), request.datasetId());
        LegacyBackupDedupCoordinator dedupCoordinator = new LegacyBackupDedupCoordinator(
                sourceRecordStore, priceBackfillService, deadLetterSink);

        cancellationRegistry.clear(request.datasetId());
        List<LegacyBackupFileEntry> files = manifest.files();
        LegacyBackupCursor cursor = checkpointCoordinator.find(request.datasetId());
        Map<LegacyBackupApplyOutcome, Long> outcomeCounts = new EnumMap<>(LegacyBackupApplyOutcome.class);

        for (int fileIndex = cursor.fileIndex(); fileIndex < files.size(); fileIndex++) {
            LegacyBackupFileEntry entry = files.get(fileIndex);
            Path resolved = fileAccess.resolveSafely(entry.name());
            fileAccess.verify(resolved, entry);

            long startLine = fileIndex == cursor.fileIndex() ? cursor.committedLines() : 0;
            BatchState state = new BatchState(new LegacyBackupCursor(fileIndex, startLine));

            try {
                new ResumableLegacyBackupReader(fileAccess).resumeFrom(resolved, startLine, (lineNumber, rawLine) -> {
                    if (cancellationRegistry.isCancellationRequested(request.datasetId())) {
                        return false;
                    }
                    LegacyBackupRecordConversion conversion = converter.convert(
                            request.datasetId(), entry.name(), lineNumber, rawLine, observedAt);
                    LegacyBackupApplyOutcome outcome = dedupCoordinator.apply(
                            request.datasetId(), entry.name(), lineNumber, conversion, observedAt);
                    outcomeCounts.merge(outcome, 1L, Long::sum);
                    state.linesSinceCheckpoint++;
                    if (state.linesSinceCheckpoint >= config.getBatchSize()) {
                        LegacyBackupCursor advanced = state.cursor.advance(state.linesSinceCheckpoint);
                        checkpointCoordinator.advance(request.datasetId(), advanced);
                        state.cursor = advanced;
                        state.linesSinceCheckpoint = 0;
                    }
                    return !cancellationRegistry.isCancellationRequested(request.datasetId());
                });
            } catch (RuntimeException exception) {
                if (state.linesSinceCheckpoint > 0) {
                    checkpointCoordinator.advance(request.datasetId(), state.cursor.advance(state.linesSinceCheckpoint));
                    state.cursor = state.cursor.advance(state.linesSinceCheckpoint);
                    state.linesSinceCheckpoint = 0;
                }
                LOGGER.error("Legacy backup APPLY failed for dataset {}", request.datasetId(), exception);
                return new LegacyBackupApplyResult(request.datasetId(), LegacyBackupApplyStatus.FAILED, outcomeCounts,
                        state.cursor, java.util.Optional.of(safeMessage(exception)));
            }

            if (state.linesSinceCheckpoint > 0) {
                LegacyBackupCursor advanced = state.cursor.advance(state.linesSinceCheckpoint);
                checkpointCoordinator.advance(request.datasetId(), advanced);
                state.cursor = advanced;
                state.linesSinceCheckpoint = 0;
            }

            if (cancellationRegistry.isCancellationRequested(request.datasetId())) {
                return new LegacyBackupApplyResult(request.datasetId(), LegacyBackupApplyStatus.CANCELLED, outcomeCounts,
                        state.cursor, java.util.Optional.empty());
            }

            LegacyBackupCursor endOfFile = state.cursor.nextFile();
            checkpointCoordinator.advance(request.datasetId(), endOfFile);
            cursor = endOfFile;
        }

        return new LegacyBackupApplyResult(request.datasetId(), LegacyBackupApplyStatus.COMPLETED, outcomeCounts, cursor,
                java.util.Optional.empty());
    }

    /**
     * Reports current checkpoint progress without touching dataset files.
     *
     * @param datasetId dataset to report on
     * @return status report
     */
    public LegacyBackupStatusReport status(String datasetId) {
        boolean hasCheckpoint = checkpointCoordinator.findRaw(datasetId).isPresent();
        LegacyBackupCursor cursor = checkpointCoordinator.find(datasetId);
        return new LegacyBackupStatusReport(
                datasetId, hasCheckpoint, cursor, cancellationRegistry.isCancellationRequested(datasetId));
    }

    /**
     * Requests that a running {@code APPLY} for {@code datasetId} stop after its current batch.
     *
     * @param datasetId dataset to cancel
     */
    public void cancel(String datasetId) {
        cancellationRegistry.requestCancellation(datasetId);
    }

    private LegacyBackupInputManifest loadManifest(Path manifestPath) {
        try {
            LegacyBackupInputManifest manifest = MAPPER.readValue(
                    manifestPath.toFile(), LegacyBackupInputManifest.class);
            if (!LegacyBackupInputManifest.SCHEMA_VERSION.equals(manifest.schemaVersion())) {
                throw new LegacyBackupInputException("unsupported manifest schema version: " + manifest.schemaVersion());
            }
            if (!LegacyBackupInputManifest.ConversionRules.MAPPING_VERSION.equals(manifest.conversionRules().mappingVersion())) {
                throw new LegacyBackupInputException(
                        "unsupported conversion mapping version: " + manifest.conversionRules().mappingVersion());
            }
            return manifest;
        } catch (IOException exception) {
            throw new LegacyBackupInputException("unable to read pinned manifest: " + manifestPath, exception);
        }
    }

    private LegacyBackupFileAccess fileAccess() {
        return new LegacyBackupFileAccess(new File(config.getDatasetRoot()).toPath());
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message;
    }

    private static final class BatchState {
        private LegacyBackupCursor cursor;
        private long linesSinceCheckpoint;

        private BatchState(LegacyBackupCursor cursor) {
            this.cursor = cursor;
        }
    }
}
