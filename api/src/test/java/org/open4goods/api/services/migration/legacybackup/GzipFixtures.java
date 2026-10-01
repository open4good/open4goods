package org.open4goods.api.services.migration.legacybackup;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/** Test-only helper to write gzip JSONL fixtures and compute their manifest coordinates. */
final class GzipFixtures {

    private GzipFixtures() {
    }

    static Path writeGzip(Path directory, String name, List<String> lines) {
        Path path = directory.resolve(name);
        try (OutputStream out = Files.newOutputStream(path); GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            for (String line : lines) {
                gzip.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException exception) {
            throw new UncheckedIOExceptionWrapper(exception);
        }
        return path;
    }

    static String sha256(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (IOException exception) {
            throw new UncheckedIOExceptionWrapper(exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class UncheckedIOExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = 1L;

        UncheckedIOExceptionWrapper(IOException cause) {
            super(cause);
        }
    }
}
