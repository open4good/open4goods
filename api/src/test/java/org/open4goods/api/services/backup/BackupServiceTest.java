package org.open4goods.api.services.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.api.config.yml.BackupConfig;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class BackupServiceTest {

    @TempDir
    private Path tempDir;

    private BackupConfig backupConfig;
    private ProductRepository productRepository;
    private SerialisationService serialisationService;
    private BackupService backupService;
    private Path productBackupFolder;

    @BeforeEach
    void setUp() throws IOException {
        productBackupFolder = tempDir.resolve("products");
        Files.createDirectories(productBackupFolder);
        Path xwikiBackup = tempDir.resolve("xwiki.zip");
        Files.writeString(xwikiBackup, "xwiki", StandardCharsets.UTF_8);

        backupConfig = new BackupConfig();
        backupConfig.setXwikiBackupFile(xwikiBackup.toString());
        backupConfig.setDataBackupFolder(productBackupFolder.toString());
        backupConfig.setImportProductPath(tempDir.resolve("import").toString());
        backupConfig.setProductsExportThreads(2);
        backupConfig.setProductExportPageSize(1000);
        backupConfig.setMinXwikiBackupFileSizeInMb(0);
        backupConfig.setMinProductsBackupFolderSizeInMb(0);
        backupConfig.setMaxWikiBackupAgeInHours(24);
        backupConfig.setMaxProductsBackupAgeInHours(24);

        productRepository = mock(ProductRepository.class);
        serialisationService = mock(SerialisationService.class);
        backupService = new BackupService(null, productRepository, backupConfig, serialisationService, null);
    }

    @Test
    void healthIgnoresNonMatchingProductBackupFiles() throws IOException {
        writeGzip(productBackupFolder.resolve("products-backup-0.gz"), "product-0");
        writeGzip(productBackupFolder.resolve("products-backup-1.gz"), "product-1");
        Files.writeString(productBackupFolder.resolve("products-backup-0"), "stale", StandardCharsets.UTF_8);
        writeManifest(2, 2, 1000, List.of("products-backup-0.gz", "products-backup-1.gz"),
                System.currentTimeMillis());

        Health health = backupService.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).doesNotContainKey("product_backup_file_count");
    }

    @Test
    void healthFailsWhenProductBackupManifestIsMissing() throws IOException {
        writeGzip(productBackupFolder.resolve("products-backup-0.gz"), "product-0");
        writeGzip(productBackupFolder.resolve("products-backup-1.gz"), "product-1");

        Health health = backupService.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("product_backup_manifest_missing");
    }

    @Test
    void healthFailsWhenProductBackupManifestExportedCountIsTooLow() throws IOException {
        writeGzip(productBackupFolder.resolve("products-backup-0.gz"), "product-0");
        writeGzip(productBackupFolder.resolve("products-backup-1.gz"), "product-1");
        writeManifest(3, 2, 1000, List.of("products-backup-0.gz", "products-backup-1.gz"),
                System.currentTimeMillis());

        Health health = backupService.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("product_backup_manifest_exported_items_too_low");
    }

    @Test
    void healthFailsWhenProductBackupManifestIsStale() throws IOException {
        backupConfig.setMaxProductsBackupAgeInHours(1);
        writeGzip(productBackupFolder.resolve("products-backup-0.gz"), "product-0");
        writeGzip(productBackupFolder.resolve("products-backup-1.gz"), "product-1");
        writeManifest(2, 2, 1000, List.of("products-backup-0.gz", "products-backup-1.gz"),
                System.currentTimeMillis() - 2L * 3600L * 1000L);

        Health health = backupService.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("product_backup_manifest_too_old");
    }

    @Test
    void successfulProductBackupClearsPreviousExportExceptionAfterPublishingFiles() throws Exception {
        when(productRepository.countMainIndex()).thenReturn(1L);
        when(productRepository.exportAll(1000)).thenThrow(new RuntimeException("stream failed"))
                .thenReturn(Stream.of(new Product()));
        when(serialisationService.toJson(any(Product.class))).thenReturn("{\"id\":1}");

        backupService.backupProducts();
        assertThat(backupService.health().getDetails()).containsKey("product_export_exception");

        backupService.backupProducts();

        Health health = backupService.health();
        assertThat(health.getDetails()).doesNotContainKey("product_export_exception");
        assertThat(Files.exists(productBackupFolder.resolve("products-backup-manifest.json"))).isTrue();
        assertThat(health.getDetails()).doesNotContainKey("product_backup_manifest_missing");
        verify(productRepository, times(2)).exportAll(1000);
    }

    @Test
    void failedProductStreamLeavesExistingFinalFilesUntouched() throws Exception {
        writeExistingBackup();
        when(productRepository.countMainIndex()).thenReturn(1L);
        when(productRepository.exportAll(1000)).thenThrow(new RuntimeException("stream failed"));

        backupService.backupProducts();

        assertThat(readGzip(productBackupFolder.resolve("products-backup-0.gz"))).isEqualTo("old-0");
        assertThat(readGzip(productBackupFolder.resolve("products-backup-1.gz"))).isEqualTo("old-1");
        assertThat(backupService.health().getDetails()).containsKey("product_export_exception");
    }

    @Test
    void failedProductWorkerLeavesExistingFinalFilesUntouched() throws Exception {
        writeExistingBackup();
        when(productRepository.countMainIndex()).thenReturn(1L);
        when(productRepository.exportAll(1000)).thenReturn(Stream.of(new Product()));
        when(serialisationService.toJson(any(Product.class))).thenThrow(new IllegalStateException("json failed"));

        backupService.backupProducts();

        assertThat(readGzip(productBackupFolder.resolve("products-backup-0.gz"))).isEqualTo("old-0");
        assertThat(readGzip(productBackupFolder.resolve("products-backup-1.gz"))).isEqualTo("old-1");
        assertThat(backupService.health().getDetails()).containsKey("product_export_exception");
    }

    private void writeExistingBackup() throws IOException {
        writeGzip(productBackupFolder.resolve("products-backup-0.gz"), "old-0");
        writeGzip(productBackupFolder.resolve("products-backup-1.gz"), "old-1");
        writeManifest(2, 2, 1000, List.of("products-backup-0.gz", "products-backup-1.gz"),
                System.currentTimeMillis());
    }

    private void writeManifest(long expectedCount, long exportedCount, int pageSize, List<String> files,
            long completedEpochMillis) throws IOException {
        String json = """
                {
                  "completedAt": "%s",
                  "completedEpochMillis": %d,
                  "expectedCount": %d,
                  "exportedCount": %d,
                  "pageSize": %d,
                  "files": %s
                }
                """.formatted(Instant.ofEpochMilli(completedEpochMillis), completedEpochMillis, expectedCount,
                exportedCount, pageSize, toJsonArray(files));
        Files.writeString(productBackupFolder.resolve("products-backup-manifest.json"), json, StandardCharsets.UTF_8);
    }

    private String toJsonArray(List<String> files) {
        return files.stream()
                .map(file -> "\"" + file + "\"")
                .toList()
                .toString();
    }

    private void writeGzip(Path path, String content) throws IOException {
        try (GZIPOutputStream outputStream = new GZIPOutputStream(Files.newOutputStream(path))) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readGzip(Path path) throws IOException {
        try (GZIPInputStream inputStream = new GZIPInputStream(Files.newInputStream(path));
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.readLine();
        }
    }
}
