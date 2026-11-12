package org.open4goods.api.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File present on disk while a full batch runs (GOU-2106).
 *
 * <p>The beta deployment ({@code publish-jars.sh} of open4goods-config) reads it before restarting the api: while it
 * exists and names a live process, the api restart waits for the end of the batch instead of killing it. The file
 * holds {@code pid=<pid>} and {@code started=<ISO-8601 instant>}; it is written at the start of the batch and deleted
 * at its end, failure included. A batch started while another one runs keeps the file until the last one ends.
 *
 * <p>Writing or deleting the file never fails the batch: an error is only logged. A blank path disables the marker.
 */
public class BatchRunningMarker {

	private static final Logger logger = LoggerFactory.getLogger(BatchRunningMarker.class);

	private final Path file;

	private int running;

	/**
	 * @param file path of the marker file, {@code null} or blank to disable it
	 */
	public BatchRunningMarker(String file) {
		this.file = file == null || file.isBlank() ? null : Path.of(file);
	}

	/**
	 * Writes the marker, unless a batch already holds it.
	 */
	public synchronized void begin() {
		running++;
		if (file == null || running > 1) {
			return;
		}
		String content = "pid=" + ProcessHandle.current().pid() + "\nstarted=" + Instant.now() + "\n";
		try {
			Path parent = file.toAbsolutePath().getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(tmp, content, StandardCharsets.UTF_8);
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			logger.warn("Batch running marker written: {}", file);
		} catch (IOException | RuntimeException e) {
			logger.error("Cannot write the batch running marker {}: a deployment may restart the api during this batch",
					file, e);
		}
	}

	/**
	 * Deletes the marker once the last running batch ends.
	 */
	public synchronized void end() {
		if (running > 0) {
			running--;
		}
		if (file == null || running > 0) {
			return;
		}
		try {
			Files.deleteIfExists(file);
			logger.warn("Batch running marker removed: {}", file);
		} catch (IOException | RuntimeException e) {
			logger.error("Cannot delete the batch running marker {}: deployments will wait until it is stale", file, e);
		}
	}

	/**
	 * @return the marker path, {@code null} when disabled
	 */
	public Path getFile() {
		return file;
	}
}
