package org.open4goods.api.services.backfill;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs one {@link BackfillEngine} backfill or restore at a time in the background (GOU-2266), and keeps its status.
 *
 * <p>The status is written to a JSON file at the start, after each batch and at the end, through a temporary file
 * renamed over it. At startup the file is read back: a run left RUNNING by a stopped api becomes INTERRUPTED, with
 * its last counters and cursor, and can be resumed. Backup files are written under one folder, named
 * {@code <operation>-<UTC timestamp>.jsonl}; a restore only reads a file of that folder.
 */
public class BackfillService {

	private static final Logger logger = LoggerFactory.getLogger(BackfillService.class);

	private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
			.withZone(ZoneOffset.UTC);

	private static final JsonMapper JSON = JsonMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			// A status written by an older version may lack a counter: read it as 0 rather than going IDLE
			.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
			.build();

	private final BackfillEngine engine;
	private final Path statusFile;
	private final Path backupFolder;

	/** Single threaded: two runs must never overlap. */
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "backfill");
		t.setDaemon(true);
		return t;
	});

	private final AtomicReference<BackfillStatus> status = new AtomicReference<>(BackfillStatus.idle());
	private final AtomicBoolean stopRequested = new AtomicBoolean();

	/**
	 * Creates the service and reads back the status file: a run left RUNNING is reported (and rewritten) as
	 * INTERRUPTED.
	 *
	 * @param engine       batched update engine
	 * @param statusFile   JSON file of the status, its folder is created when missing
	 * @param backupFolder folder of the JSONL backup files, created when missing
	 */
	public BackfillService(BackfillEngine engine, Path statusFile, Path backupFolder) {
		this.engine = Objects.requireNonNull(engine, "engine");
		this.statusFile = Objects.requireNonNull(statusFile, "statusFile");
		this.backupFolder = Objects.requireNonNull(backupFolder, "backupFolder").toAbsolutePath().normalize();
		status.set(readPersistedStatus());
	}

	/**
	 * Starts a backfill in the background.
	 *
	 * @param operation what to change
	 * @param options   mode, batch size, rate cap and resume flag
	 * @return the RUNNING status of the new run, or the current status unchanged when a run is already in progress
	 * @throws IllegalStateException when {@code resume} is set but the last run is not a stopped, interrupted or failed
	 *                               run of the same operation and mode with a cursor
	 */
	public BackfillStatus start(BackfillOperation operation, BackfillOptions options) {
		Objects.requireNonNull(operation, "operation");
		Objects.requireNonNull(options, "options");
		BackfillCursor resumeFrom = null;
		Path backupFile = null;
		if (options.resume()) {
			BackfillStatus last = status.get();
			if (!resumable(last, operation, options)) {
				throw new IllegalStateException("Nothing to resume for " + operation.name() + " (dryRun="
						+ options.dryRun() + "): last run is " + last.state() + " of " + last.operation());
			}
			resumeFrom = last.report().cursor();
			backupFile = last.report().backupFile() == null ? null : Path.of(last.report().backupFile());
		}
		if (!options.dryRun() && backupFile == null) {
			backupFile = backupFolder.resolve(operation.name() + "-" + BACKUP_TIMESTAMP.format(Instant.now()) + ".jsonl");
		}
		BackfillCursor cursor = resumeFrom;
		Path backup = backupFile;
		Instant now = Instant.now();
		BackfillStatus running = BackfillStatus.running(operation.name(), options.dryRun(), false, now,
				emptyReport(operation, options.dryRun(), false, backup, cursor));
		if (!claim(running)) {
			return status.get();
		}
		executor.execute(() -> complete(() -> engine.run(operation, options, backup, cursor, stopRequested::get,
				this::progress)));
		return running;
	}

	/**
	 * Starts in the background the restore of a backup file of the backfill folder.
	 *
	 * @param operation  operation that wrote the file
	 * @param backupName file name, inside the backup folder
	 * @param batchSize  lines restored per bulk
	 * @return the RUNNING status of the restore, or the current status unchanged when a run is already in progress
	 * @throws IllegalArgumentException when the name leaves the backup folder or the file does not exist
	 */
	public BackfillStatus startRestore(BackfillOperation operation, String backupName, int batchSize) {
		Objects.requireNonNull(operation, "operation");
		Path backupFile = resolveBackup(backupName);
		BackfillStatus running = BackfillStatus.running(operation.name(), false, true, Instant.now(),
				emptyReport(operation, false, true, backupFile, null));
		if (!claim(running)) {
			return status.get();
		}
		executor.execute(() -> complete(() -> engine.restore(operation, backupFile, batchSize, stopRequested::get,
				this::progress)));
		return running;
	}

	/**
	 * Asks the current run to stop after its current batch.
	 *
	 * @return the current status
	 */
	public BackfillStatus requestStop() {
		if (status.get().state() == BackfillStatus.State.RUNNING) {
			stopRequested.set(true);
			logger.info("Stop of backfill {} requested", status.get().operation());
		}
		return status.get();
	}

	/** @return the status of the current or last run */
	public BackfillStatus getStatus() {
		return status.get();
	}

	/** @return the folder of the JSONL backup files */
	public Path getBackupFolder() {
		return backupFolder;
	}

	/**
	 * @param name file name of a backup
	 * @return its path inside the backup folder
	 * @throws IllegalArgumentException when the name leaves the folder or the file does not exist
	 */
	Path resolveBackup(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("backupFile is required");
		}
		Path file = backupFolder.resolve(name).normalize();
		if (!file.getParent().equals(backupFolder)) {
			throw new IllegalArgumentException("backupFile must be a file name of " + backupFolder);
		}
		if (!Files.isRegularFile(file)) {
			throw new IllegalArgumentException("No backup file " + file);
		}
		return file;
	}

	private static boolean resumable(BackfillStatus last, BackfillOperation operation, BackfillOptions options) {
		return last != null && !last.restore() && operation.name().equals(last.operation())
				&& last.dryRun() == options.dryRun()
				&& List.of(BackfillStatus.State.STOPPED, BackfillStatus.State.INTERRUPTED, BackfillStatus.State.FAILED)
						.contains(last.state())
				&& last.report() != null && last.report().cursor() != null;
	}

	/** Publishes the RUNNING status unless a run is already in progress. */
	private boolean claim(BackfillStatus running) {
		while (true) {
			BackfillStatus current = status.get();
			if (current.state() == BackfillStatus.State.RUNNING) {
				return false;
			}
			if (status.compareAndSet(current, running)) {
				stopRequested.set(false);
				persist(running);
				return true;
			}
		}
	}

	private void progress(BackfillReport report) {
		BackfillStatus step = status.get().withProgress(report, Instant.now());
		status.set(step);
		persist(step);
	}

	/** Body of a background run: records its final status, whatever happens. */
	private void complete(Run run) {
		BackfillStatus done;
		try {
			BackfillReport report = run.call();
			BackfillStatus.State state = report.stopReason() == null ? BackfillStatus.State.COMPLETED
					: BackfillStatus.State.STOPPED;
			done = status.get().finish(state, report, Instant.now(), null);
		} catch (Throwable t) {
			logger.error("Backfill {} failed", status.get().operation(), t);
			BackfillStatus last = status.get();
			done = last.finish(BackfillStatus.State.FAILED, last.report(), Instant.now(), String.valueOf(t));
		}
		// Written before it is published: whoever sees the final state can read it back from the file
		persist(done);
		status.set(done);
		logger.info("Backfill finished: {}", done);
	}

	private static BackfillReport emptyReport(BackfillOperation operation, boolean dryRun, boolean restore,
			Path backupFile, BackfillCursor cursor) {
		return new BackfillReport(operation.name(), dryRun, restore, 0, 0, 0, 0, Map.of(), List.of(),
				backupFile == null ? null : backupFile.toString(), cursor, null, 0);
	}

	/**
	 * Writes the status through a temporary file renamed over it. A write failure is logged and never stops the run.
	 */
	private synchronized void persist(BackfillStatus snapshot) {
		Path tmp = statusFile.resolveSibling(statusFile.getFileName() + ".tmp");
		try {
			Path folder = statusFile.toAbsolutePath().getParent();
			if (folder != null) {
				Files.createDirectories(folder);
			}
			Files.writeString(tmp, JSON.writeValueAsString(snapshot), StandardCharsets.UTF_8);
			Files.move(tmp, statusFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | JacksonException e) {
			logger.error("Cannot write the backfill status to {}", statusFile, e);
		}
	}

	/**
	 * Reads back the status file at startup. A run still RUNNING was stopped with the api: it becomes INTERRUPTED,
	 * and the file is rewritten so. A missing or unreadable file gives IDLE.
	 */
	private BackfillStatus readPersistedStatus() {
		BackfillStatus persisted;
		try {
			persisted = JSON.readValue(Files.readString(statusFile, StandardCharsets.UTF_8), BackfillStatus.class);
		} catch (NoSuchFileException e) {
			return BackfillStatus.idle();
		} catch (IOException | JacksonException e) {
			logger.warn("Cannot read the backfill status from {}, starting IDLE", statusFile, e);
			return BackfillStatus.idle();
		}
		if (persisted == null || persisted.state() == null) {
			return BackfillStatus.idle();
		}
		if (persisted.state() == BackfillStatus.State.RUNNING) {
			BackfillStatus interrupted = persisted.interrupted();
			logger.warn("Backfill {} was interrupted by a restart: {}", interrupted.operation(), interrupted);
			persist(interrupted);
			return interrupted;
		}
		logger.info("Last backfill read back from {}: {}", statusFile, persisted);
		return persisted;
	}

	/** A background run, which may throw. */
	@FunctionalInterface
	private interface Run {
		BackfillReport call() throws Exception;
	}
}
