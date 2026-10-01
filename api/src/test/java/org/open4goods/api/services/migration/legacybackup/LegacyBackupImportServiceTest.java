package org.open4goods.api.services.migration.legacybackup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.api.config.yml.LegacyBackupImportConfig;
import org.open4goods.pricehistory.service.LegacyPriceBackfillService;

class LegacyBackupImportServiceTest {

    @TempDir
    private Path root;

    private Path datasetRoot;
    private Path deadLetterFolder;
    private InMemorySourceRecordHeadStore sourceRecordStore;
    private InMemoryLegacyPriceBackfillStore priceStore;
    private InMemoryIngestionCheckpointStore checkpointStore;
    private LegacyBackupCancellationRegistry cancellationRegistry;
    private LegacyBackupImportService service;

    @BeforeEach
    void setUp() throws Exception {
        datasetRoot = Files.createDirectory(root.resolve("dataset"));
        deadLetterFolder = root.resolve("dead-letters");
        sourceRecordStore = new InMemorySourceRecordHeadStore();
        priceStore = new InMemoryLegacyPriceBackfillStore();
        checkpointStore = new InMemoryIngestionCheckpointStore();
        cancellationRegistry = new LegacyBackupCancellationRegistry();
        service = newService(2);
    }

    private LegacyBackupImportService newService(int batchSize) {
        LegacyBackupImportConfig config = new LegacyBackupImportConfig();
        config.setDatasetRoot(datasetRoot.toString());
        config.setDeadLetterFolder(deadLetterFolder.toString());
        config.setBatchSize(batchSize);
        return new LegacyBackupImportService(config, sourceRecordStore, new LegacyPriceBackfillService(priceStore),
                checkpointStore, cancellationRegistry);
    }

    private Path writeManifest(List<String> fileNames, List<Long> lineCounts) throws Exception {
        StringBuilder files = new StringBuilder();
        StringBuilder legacyFiles = new StringBuilder();
        for (int i = 0; i < fileNames.size(); i++) {
            Path file = datasetRoot.resolve(fileNames.get(i));
            String sha = GzipFixtures.sha256(file);
            if (i > 0) {
                files.append(',');
                legacyFiles.append(',');
            }
            files.append("{\"name\":\"").append(fileNames.get(i)).append("\",\"sha256\":\"").append(sha)
                    .append("\",\"bytes\":").append(Files.size(file)).append(",\"lineCount\":").append(lineCounts.get(i))
                    .append('}');
            legacyFiles.append('"').append(fileNames.get(i)).append('"');
        }
        String manifestJson = "{"
                + "\"schemaVersion\":\"open4goods.product-backup-input/v1\","
                + "\"legacyManifest\":{\"sha256\":\"" + "0".repeat(64) + "\","
                + "\"completedAt\":\"2026-09-01T00:00:00Z\",\"completedEpochMillis\":1,"
                + "\"expectedCount\":1,\"exportedCount\":1,\"files\":[" + legacyFiles + "]},"
                + "\"files\":[" + files + "],"
                + "\"conversionRules\":{\"mappingVersion\":\"legacy-product-fields/v1\"}"
                + "}";
        Path manifestPath = root.resolve("manifest.json");
        Files.writeString(manifestPath, manifestJson);
        return manifestPath;
    }

    private List<String> gtinLines(int count) {
        // Deterministic distinct valid GTIN-13s, each with a correctly computed check digit.
        return IntStream.range(0, count)
                .mapToObj(index -> "{\"gtin\":\"" + validGtin13(4006381333930L + index) + "\"}")
                .toList();
    }

    private String validGtin13(long base) {
        String twelve = String.format("%012d", base % 1_000_000_000_000L);
        int total = 0;
        for (int i = 0; i < 12; i++) {
            int digit = twelve.charAt(i) - '0';
            int weight = i % 2 == 0 ? 1 : 3;
            total += digit * weight;
        }
        int check = (10 - total % 10) % 10;
        return twelve + check;
    }

    @Test
    void inventoryValidatesManifestAndDigestsWithoutWritingAnything() throws Exception {
        GzipFixtures.writeGzip(datasetRoot, "products-backup-0.gz", gtinLines(3));
        Path manifestPath = writeManifest(List.of("products-backup-0.gz"), List.of(3L));

        LegacyBackupInventoryReport report = service.inventory(new LegacyBackupImportRequest("test-dataset", manifestPath));

        assertThat(report.totalLines()).isEqualTo(3);
        assertThat(sourceRecordStore.size()).isZero();
    }

    @Test
    void inventoryRejectsAChangedFile() throws Exception {
        GzipFixtures.writeGzip(datasetRoot, "products-backup-0.gz", gtinLines(3));
        Path manifestPath = writeManifest(List.of("products-backup-0.gz"), List.of(3L));
        // Mutate the archive after pinning.
        GzipFixtures.writeGzip(datasetRoot, "products-backup-0.gz", gtinLines(4));

        assertThatThrownBy(() -> service.inventory(new LegacyBackupImportRequest("test-dataset", manifestPath)))
                .isInstanceOf(LegacyBackupInputException.class);
    }

    @Test
    void applyWritesEveryDistinctGtinAndCheckpointsToCompletion() throws Exception {
        GzipFixtures.writeGzip(datasetRoot, "products-backup-0.gz", gtinLines(5));
        Path manifestPath = writeManifest(List.of("products-backup-0.gz"), List.of(5L));

        LegacyBackupApplyResult result = service.apply(new LegacyBackupImportRequest("test-dataset", manifestPath));

        assertThat(result.status()).isEqualTo(LegacyBackupApplyStatus.COMPLETED);
        assertThat(sourceRecordStore.size()).isEqualTo(5);
        assertThat(result.outcomeCounts().get(LegacyBackupApplyOutcome.APPLIED)).isEqualTo(5L);

        LegacyBackupStatusReport status = service.status("test-dataset");
        assertThat(status.hasCheckpoint()).isTrue();
        assertThat(status.cursor().fileIndex()).isEqualTo(1);
    }

    @Test
    void restartingApplyAfterFullCompletionDoesNotDuplicateRecords() throws Exception {
        GzipFixtures.writeGzip(datasetRoot, "products-backup-0.gz", gtinLines(4));
        Path manifestPath = writeManifest(List.of("products-backup-0.gz"), List.of(4L));
        LegacyBackupImportRequest request = new LegacyBackupImportRequest("test-dataset", manifestPath);

        service.apply(request);
        LegacyBackupApplyResult second = service.apply(request);

        // Nothing left to read past the completed file: the cursor's file index is out of range,
        // so the loop body never runs again and the store is untouched by this second call.
        assertThat(second.status()).isEqualTo(LegacyBackupApplyStatus.COMPLETED);
        assertThat(second.outcomeCounts()).isEmpty();
        assertThat(sourceRecordStore.size()).isEqualTo(4);
    }

    @Test
    void cancellingBetweenBatchesStopsCleanlyAndResumesWithoutLossOrDuplication() throws Exception {
        GzipFixtures.writeGzip(datasetRoot, "products-backup-0.gz", gtinLines(6));
        Path manifestPath = writeManifest(List.of("products-backup-0.gz"), List.of(6L));
        LegacyBackupImportRequest request = new LegacyBackupImportRequest("test-dataset", manifestPath);

        // Trigger cancellation from inside the store, deterministically, once 2 records (one
        // full batch) have already landed, rather than racing a background thread.
        CancelAfterNAppliesStore cancellingStore = new CancelAfterNAppliesStore(sourceRecordStore, 2, cancellationRegistry,
                "test-dataset");
        LegacyBackupImportService cancellingService = newService(2, cancellingStore);

        LegacyBackupApplyResult firstRun = cancellingService.apply(request);

        assertThat(firstRun.status()).isEqualTo(LegacyBackupApplyStatus.CANCELLED);
        int afterCancel = sourceRecordStore.size();
        assertThat(afterCancel).isPositive().isLessThan(6);

        LegacyBackupApplyResult secondRun = service.apply(request);

        assertThat(secondRun.status()).isEqualTo(LegacyBackupApplyStatus.COMPLETED);
        assertThat(sourceRecordStore.size()).isEqualTo(6);
    }

    @Test
    void resumingAfterASimulatedCrashMidFileDoesNotLoseOrDuplicateAcceptedRecords() throws Exception {
        GzipFixtures.writeGzip(datasetRoot, "products-backup-0.gz", gtinLines(7));
        Path manifestPath = writeManifest(List.of("products-backup-0.gz"), List.of(7L));
        LegacyBackupImportRequest request = new LegacyBackupImportRequest("test-dataset", manifestPath);

        // Simulate a hard crash: the store accepts writes but the run is cut short right after
        // the first checkpoint-advancing batch, before it can reach the end of the file.
        CancelAfterNAppliesStore crashingStore = new CancelAfterNAppliesStore(sourceRecordStore, 2, cancellationRegistry,
                "test-dataset");
        LegacyBackupImportService crashedRun = newService(2, crashingStore);
        crashedRun.apply(request);
        int afterFirstAttempt = sourceRecordStore.size();
        assertThat(afterFirstAttempt).isGreaterThan(0).isLessThan(7);

        // A fresh service instance with a clean cancellation registry simulates process restart.
        LegacyBackupImportService restarted = newService(2);
        LegacyBackupApplyResult result = restarted.apply(request);

        assertThat(result.status()).isEqualTo(LegacyBackupApplyStatus.COMPLETED);
        assertThat(sourceRecordStore.size()).isEqualTo(7);
    }

    @Test
    void repeatedPartitionsOfTheSameDatasetNeverDuplicateAnAlreadyAcceptedGtin() throws Exception {
        GzipFixtures.writeGzip(datasetRoot, "products-backup-0.gz", gtinLines(6));
        Path manifestPath = writeManifest(List.of("products-backup-0.gz"), List.of(6L));
        LegacyBackupImportRequest request = new LegacyBackupImportRequest("test-dataset", manifestPath);

        // Three restart cycles, each cut short after one batch, then a final unimpeded run.
        for (int cycle = 0; cycle < 3; cycle++) {
            CancelAfterNAppliesStore partitioningStore = new CancelAfterNAppliesStore(sourceRecordStore, 1,
                    cancellationRegistry, "test-dataset");
            newService(2, partitioningStore).apply(request);
        }
        LegacyBackupApplyResult finalResult = service.apply(request);

        assertThat(finalResult.status()).isEqualTo(LegacyBackupApplyStatus.COMPLETED);
        assertThat(sourceRecordStore.size()).isEqualTo(6);
    }

    private LegacyBackupImportService newService(int batchSize, org.open4goods.datareference.port.SourceRecordHeadStore store) {
        LegacyBackupImportConfig config = new LegacyBackupImportConfig();
        config.setDatasetRoot(datasetRoot.toString());
        config.setDeadLetterFolder(deadLetterFolder.toString());
        config.setBatchSize(batchSize);
        return new LegacyBackupImportService(config, store, new LegacyPriceBackfillService(priceStore), checkpointStore,
                cancellationRegistry);
    }
}
