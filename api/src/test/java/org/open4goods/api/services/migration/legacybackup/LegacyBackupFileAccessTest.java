package org.open4goods.api.services.migration.legacybackup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.api.services.migration.legacybackup.LegacyBackupInputManifest.LegacyBackupFileEntry;

class LegacyBackupFileAccessTest {

    @TempDir
    private Path root;

    @Test
    void rejectsPathTraversalFileName() {
        LegacyBackupFileAccess fileAccess = new LegacyBackupFileAccess(root);

        assertThatThrownBy(() -> fileAccess.resolveSafely("../products-backup-0.gz"))
                .isInstanceOf(LegacyBackupInputException.class);
    }

    @Test
    void rejectsFileNameNotMatchingSafePattern() {
        LegacyBackupFileAccess fileAccess = new LegacyBackupFileAccess(root);

        assertThatThrownBy(() -> fileAccess.resolveSafely("products-backup-0.gz.sh"))
                .isInstanceOf(LegacyBackupInputException.class);
    }

    @Test
    void rejectsMissingFile() {
        LegacyBackupFileAccess fileAccess = new LegacyBackupFileAccess(root);

        assertThatThrownBy(() -> fileAccess.resolveSafely("products-backup-0.gz"))
                .isInstanceOf(LegacyBackupInputException.class);
    }

    @Test
    void verifiesMatchingDigestAndLineCount() {
        Path file = GzipFixtures.writeGzip(root, "products-backup-0.gz", List.of("{\"gtin\":\"4006381333931\"}"));
        LegacyBackupFileEntry entry = new LegacyBackupFileEntry("products-backup-0.gz", GzipFixtures.sha256(file),
                fileSize(file), 1);
        LegacyBackupFileAccess fileAccess = new LegacyBackupFileAccess(root);

        fileAccess.verify(file, entry);
    }

    @Test
    void rejectsChangedInputByDigestMismatch() throws Exception {
        Path file = GzipFixtures.writeGzip(root, "products-backup-0.gz", List.of("{\"gtin\":\"4006381333931\"}"));
        LegacyBackupFileEntry entry = new LegacyBackupFileEntry("products-backup-0.gz", "0".repeat(64), fileSize(file), 1);
        LegacyBackupFileAccess fileAccess = new LegacyBackupFileAccess(root);

        assertThatThrownBy(() -> fileAccess.verify(file, entry)).isInstanceOf(LegacyBackupInputException.class)
                .hasMessageContaining("digest mismatch");
    }

    @Test
    void rejectsChangedInputByLineCountMismatch() {
        Path file = GzipFixtures.writeGzip(root, "products-backup-0.gz",
                List.of("{\"gtin\":\"4006381333931\"}", "{\"gtin\":\"96385074\"}"));
        LegacyBackupFileEntry entry = new LegacyBackupFileEntry("products-backup-0.gz", GzipFixtures.sha256(file),
                fileSize(file), 1);
        LegacyBackupFileAccess fileAccess = new LegacyBackupFileAccess(root);

        assertThatThrownBy(() -> fileAccess.verify(file, entry)).isInstanceOf(LegacyBackupInputException.class)
                .hasMessageContaining("line count mismatch");
    }

    @Test
    void rejectsCorruptGzip() throws Exception {
        Path file = root.resolve("products-backup-0.gz");
        Files.writeString(file, "not a gzip file", StandardCharsets.UTF_8);
        LegacyBackupFileEntry entry = new LegacyBackupFileEntry("products-backup-0.gz", "0".repeat(64), 10, 1);
        LegacyBackupFileAccess fileAccess = new LegacyBackupFileAccess(root);

        assertThatThrownBy(() -> fileAccess.verify(file, entry)).isInstanceOf(LegacyBackupInputException.class);
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
