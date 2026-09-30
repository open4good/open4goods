package org.open4goods.api.services.migration.legacybackup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResumableLegacyBackupReaderTest {

    @TempDir
    private Path root;

    @Test
    void readsEveryLineFromTheStart() {
        Path file = GzipFixtures.writeGzip(root, "products-backup-0.gz", List.of("a", "b", "c"));
        ResumableLegacyBackupReader reader = new ResumableLegacyBackupReader(new LegacyBackupFileAccess(root));
        List<String> seen = new ArrayList<>();

        long accepted = reader.resumeFrom(file, 0, (lineNumber, line) -> {
            seen.add(lineNumber + ":" + line);
            return true;
        });

        assertThat(seen).containsExactly("1:a", "2:b", "3:c");
        assertThat(accepted).isEqualTo(3);
    }

    @Test
    void skipsAlreadyCommittedLinesByDecompressingFromTheStart() {
        Path file = GzipFixtures.writeGzip(root, "products-backup-0.gz", List.of("a", "b", "c", "d"));
        ResumableLegacyBackupReader reader = new ResumableLegacyBackupReader(new LegacyBackupFileAccess(root));
        List<String> seen = new ArrayList<>();

        reader.resumeFrom(file, 2, (lineNumber, line) -> {
            seen.add(lineNumber + ":" + line);
            return true;
        });

        assertThat(seen).containsExactly("3:c", "4:d");
    }

    @Test
    void handlesAFinalShortBatchCorrectly() {
        Path file = GzipFixtures.writeGzip(root, "products-backup-0.gz", List.of("a", "b", "c"));
        ResumableLegacyBackupReader reader = new ResumableLegacyBackupReader(new LegacyBackupFileAccess(root));
        List<String> seen = new ArrayList<>();

        // Resume from a point leaving fewer lines than a typical batch size.
        long accepted = reader.resumeFrom(file, 2, (lineNumber, line) -> {
            seen.add(line);
            return true;
        });

        assertThat(seen).containsExactly("c");
        assertThat(accepted).isEqualTo(1);
    }

    @Test
    void stopsEarlyWhenConsumerReturnsFalse() {
        Path file = GzipFixtures.writeGzip(root, "products-backup-0.gz", List.of("a", "b", "c"));
        ResumableLegacyBackupReader reader = new ResumableLegacyBackupReader(new LegacyBackupFileAccess(root));
        List<String> seen = new ArrayList<>();

        reader.resumeFrom(file, 0, (lineNumber, line) -> {
            seen.add(line);
            return lineNumber < 2;
        });

        assertThat(seen).containsExactly("a", "b");
    }

    @Test
    void rejectsCorruptGzip() throws Exception {
        Path file = root.resolve("products-backup-0.gz");
        Files.writeString(file, "not a gzip file", StandardCharsets.UTF_8);
        ResumableLegacyBackupReader reader = new ResumableLegacyBackupReader(new LegacyBackupFileAccess(root));

        assertThatThrownBy(() -> reader.resumeFrom(file, 0, (lineNumber, line) -> true))
                .isInstanceOf(LegacyBackupInputException.class);
    }

    @Test
    void rejectsResumingPastTheEndOfTheFile() {
        Path file = GzipFixtures.writeGzip(root, "products-backup-0.gz", List.of("a"));
        ResumableLegacyBackupReader reader = new ResumableLegacyBackupReader(new LegacyBackupFileAccess(root));

        assertThatThrownBy(() -> reader.resumeFrom(file, 5, (lineNumber, line) -> true))
                .isInstanceOf(LegacyBackupInputException.class);
    }
}
