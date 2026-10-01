package org.open4goods.api.services.migration.legacybackup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.open4goods.api.services.migration.legacybackup.LegacyBackupInputManifest.LegacyBackupFileEntry;

/**
 * Resolves and validates one manifest-listed archive file, never trusting the manifest's file
 * name blindly.
 *
 * <p>Rejects any name that is not a bare {@code products-backup-<n>.gz} file directly inside the
 * configured read-only dataset root (no path traversal, no subdirectories, no symlink escape),
 * and only accepts a file whose recomputed SHA-256 digest and decompressed line count both match
 * the immutable manifest.
 */
public final class LegacyBackupFileAccess {

    private static final Pattern SAFE_FILE_NAME = Pattern.compile("products-backup-\\d+\\.gz");

    private final Path datasetRoot;

    public LegacyBackupFileAccess(Path datasetRoot) {
        this.datasetRoot = datasetRoot.toAbsolutePath().normalize();
    }

    /**
     * Resolves a manifest-listed file name to a safe absolute path inside the dataset root.
     *
     * @param fileName manifest-listed file name
     * @return resolved, existing path
     * @throws LegacyBackupInputException when the name is unsafe or the file is missing
     */
    public Path resolveSafely(String fileName) {
        if (!SAFE_FILE_NAME.matcher(fileName).matches()) {
            throw new LegacyBackupInputException("unsafe archive file name: " + fileName);
        }
        Path resolved = datasetRoot.resolve(fileName).normalize();
        if (!resolved.getParent().equals(datasetRoot)) {
            throw new LegacyBackupInputException("archive file name escapes the dataset root: " + fileName);
        }
        if (!Files.isRegularFile(resolved)) {
            throw new LegacyBackupInputException("manifest-listed archive is missing: " + fileName);
        }
        return resolved;
    }

    /**
     * Verifies a resolved file's compressed digest and decompressed line count against the
     * pinned manifest entry.
     *
     * @param path resolved archive path
     * @param entry pinned manifest entry for this file
     * @throws LegacyBackupInputException when the digest or line count no longer matches
     */
    public void verify(Path path, LegacyBackupFileEntry entry) {
        String digest = sha256(path);
        if (!digest.equalsIgnoreCase(entry.sha256())) {
            throw new LegacyBackupInputException("archive changed since pinning (digest mismatch): " + entry.name());
        }
        long lineCount = countLines(path);
        if (lineCount != entry.lineCount()) {
            throw new LegacyBackupInputException("archive changed since pinning (line count mismatch): " + entry.name());
        }
    }

    private String sha256(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestStream = new DigestInputStream(input, digest)) {
                byte[] buffer = new byte[1 << 16];
                while (digestStream.read(buffer) != -1) {
                    // digest is updated as a side effect of reading
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException exception) {
            throw new LegacyBackupInputException("unable to read archive for digest: " + path.getFileName(), exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private long countLines(Path path) {
        try (InputStream input = Files.newInputStream(path);
                GZIPInputStream gzip = new GZIPInputStream(input)) {
            long count = 0;
            int previous = -1;
            int current;
            while ((current = gzip.read()) != -1) {
                if (current == '\n') {
                    count++;
                }
                previous = current;
            }
            if (previous != -1 && previous != '\n') {
                count++;
            }
            return count;
        } catch (IOException exception) {
            throw new LegacyBackupInputException("corrupt gzip archive: " + path.getFileName(), exception);
        }
    }

    /**
     * Opens a validated archive for JSONL decoding.
     *
     * @param path resolved archive path
     * @return a fresh decompressing reader positioned at the start of the file
     */
    public java.io.BufferedReader openForReading(Path path) {
        try {
            InputStream input = Files.newInputStream(path);
            GZIPInputStream gzip = new GZIPInputStream(input);
            return new java.io.BufferedReader(new java.io.InputStreamReader(gzip, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new LegacyBackupInputException("corrupt gzip archive: " + path.getFileName(), exception);
        }
    }
}
