package org.open4goods.api.services.backfill;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.ShardFailure;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Batched, resumable update of the product index (GOU-2266), shared by the operations that rewrite a few fields of
 * many products: the Decitre purge (GOU-2266), the {@code nature} backfill (GOU-2234).
 *
 * <p>A run reads the products of {@link BackfillOperation#query()} through a point in time (PIT) and
 * {@code search_after} on {@code _shard_doc}, by pages of {@link BackfillOptions#batchSize()}, with only the
 * {@link BackfillOperation#sourceFields()}. For each page:
 * <ol>
 *   <li>each product gets its {@link BackfillOperation#plan(String, Map) planned change}, if any;</li>
 *   <li>outside dry run, the backup line of each change ({@code {"id":...,"data":...}}) is appended to the JSONL
 *       backup file and forced to disk <b>before</b> any update is sent;</li>
 *   <li>the changes go in one bulk of painless update scripts ({@code retry_on_conflict} 3, no refresh): only the
 *       fields of the operation are written, never the complete document;</li>
 *   <li>the cursor moves past the page, the counters are reported and logged, and the reading rate is capped.</li>
 * </ol>
 * The run stops cleanly when a stop is requested (between two pages) or when Elasticsearch answers 429 (search, shard
 * or bulk item): the report then carries the cursor of the last page fully handled, and the point in time stays open for
 * {@code keepAlive}, so a resumed run continues from there. A page with a rejected item is handled again on resume;
 * the operations are idempotent, so the products already changed are skipped.
 *
 * <p>{@link #restore} reads a backup file back and gives each line's {@code data} to the operation's
 * {@link BackfillOperation#restoreScript() restore script}.
 */
public class BackfillEngine {

	private static final Logger logger = LoggerFactory.getLogger(BackfillEngine.class);

	/** Number of product ids reported as examples. */
	static final int SAMPLE_SIZE = 20;

	/**
	 * A bulk fails the run when at least this many of its updates failed, and more than half of them: a single conflict
	 * in a small bulk is not a systematic failure.
	 */
	static final int MIN_FAILURES_TO_STOP = 10;

	/** Failed updates logged one by one per bulk; the others are only counted by status and type. */
	static final int LOGGED_FAILURES_PER_BULK = 5;

	/** Conflicts with a concurrent update of the same product (ingestion) are retried this many times. */
	static final int RETRY_ON_CONFLICT = 3;

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final ElasticsearchClient client;
	private final String index;
	private final Duration keepAlive;

	/**
	 * @param client    Elasticsearch client
	 * @param index     products index, for instance {@code products-moustik}
	 * @param keepAlive how long the point in time stays open after each page, and so after a stop
	 */
	public BackfillEngine(ElasticsearchClient client, String index, Duration keepAlive) {
		this.client = Objects.requireNonNull(client, "client");
		this.index = Objects.requireNonNull(index, "index");
		this.keepAlive = Objects.requireNonNull(keepAlive, "keepAlive");
	}

	/**
	 * Scans the products of the operation and applies their changes.
	 *
	 * @param operation  what to change
	 * @param options    mode, batch size and rate cap
	 * @param backupFile JSONL backup file, appended to; required outside dry run when the operation can be restored,
	 *                   ignored otherwise
	 * @param resumeFrom cursor of a stopped run to continue, null to start from the beginning
	 * @param stop       polled between two pages: true stops the run cleanly
	 * @param onBatch    receives the counters after each page
	 * @return the report of the run
	 * @throws IOException when the backup file cannot be written: nothing of the page was sent
	 */
	public BackfillReport run(BackfillOperation operation, BackfillOptions options, Path backupFile,
			BackfillCursor resumeFrom, BooleanSupplier stop, Consumer<BackfillReport> onBatch) throws IOException {
		boolean backedUp = !options.dryRun() && operation.restoreScript() != null;
		if (backedUp) {
			Objects.requireNonNull(backupFile, "backupFile is required outside dry run");
		}
		requirePaceWithinKeepAlive(options.batchSize(), options.maxProductsPerSecond());
		long start = System.currentTimeMillis();
		Counters counters = new Counters();
		String pitId = resumeFrom == null || resumeFrom.pitId() == null ? openPit() : resumeFrom.pitId();
		List<FieldValue> after = resumeFrom == null ? null : toFieldValues(resumeFrom.searchAfter());
		boolean resuming = resumeFrom != null;
		String stopReason = null;
		boolean completed = false;
		long batch = 0;
		logger.info("Backfill {} started (dryRun={}, batchSize={}, maxProductsPerSecond={}, resumeFrom={}, backup={})",
				operation.name(), options.dryRun(), options.batchSize(), options.maxProductsPerSecond(), resumeFrom,
				backedUp ? backupFile : null);

		try (BackupWriter backup = backedUp ? new BackupWriter(backupFile) : null) {
			while (true) {
				if (stop.getAsBoolean()) {
					stopReason = BackfillReport.STOP_REQUESTED;
					break;
				}
				long pageStart = System.currentTimeMillis();
				SearchResponse<Map<String, Object>> page;
				try {
					page = search(operation, options.batchSize(), pitId, after);
				} catch (ElasticsearchException e) {
					if (resuming && counters.scanned == 0 && e.status() == 404) {
						logger.warn("Backfill {}: the point in time of the resumed run has expired, scanning again from "
								+ "the beginning (products already changed are skipped)", operation.name());
						pitId = openPit();
						after = null;
						resuming = false;
						continue;
					}
					if (isBackpressure(e)) {
						logger.warn("Backfill {}: search rejected by Elasticsearch ({}), stopping", operation.name(),
								e.getMessage());
						stopReason = BackfillReport.BACKPRESSURE;
						break;
					}
					throw e;
				}
				resuming = false;
				if (page.pitId() != null) {
					pitId = page.pitId();
				}
				if (partial(operation, page)) {
					// A shard rejected the page: its products are not in it, so the cursor must not move past them
					stopReason = BackfillReport.BACKPRESSURE;
					break;
				}
				List<Hit<Map<String, Object>>> hits = page.hits().hits();
				if (hits.isEmpty()) {
					completed = true;
					break;
				}
				batch++;

				List<String> ids = new ArrayList<>();
				List<BackfillChange> changes = new ArrayList<>();
				for (Hit<Map<String, Object>> hit : hits) {
					counters.scanned++;
					Map<String, Object> source = hit.source() == null ? Map.of() : hit.source();
					operation.plan(hit.id(), source).ifPresent(change -> {
						counters.matched(hit.id(), change);
						ids.add(hit.id());
						changes.add(change);
					});
				}

				boolean rejected = false;
				if (!options.dryRun() && !changes.isEmpty()) {
					if (backup != null) {
						backup.append(ids, changes);
					}
					rejected = write(operation, ids, changes, counters);
				}
				if (rejected) {
					stopReason = BackfillReport.BACKPRESSURE;
					break;
				}

				after = hits.getLast().sort();
				BackfillReport progress = counters.report(operation, options.dryRun(), backedUp ? backupFile : null,
						new BackfillCursor(pitId, toLongs(after)), null, start);
				logger.info("Backfill {} (dryRun={}) batch {}: {} scanned, {} matched, {} written, {} unchanged, {} failed, "
						+ "labels {}", operation.name(), options.dryRun(), batch, counters.scanned, counters.matched,
						counters.written, counters.unchanged, counters.failed, counters.labels);
				onBatch.accept(progress);
				pace(options.maxProductsPerSecond(), hits.size(), pageStart, stop);
			}
		} finally {
			if (completed) {
				closePit(pitId);
			}
		}

		BackfillReport report = counters.report(operation, options.dryRun(), backedUp ? backupFile : null,
				new BackfillCursor(pitId, toLongs(after)), stopReason, start);
		logger.info("Backfill {} {}: {}", operation.name(), completed ? "done" : "stopped", report);
		return report;
	}

	/**
	 * Puts back, from a backup file, what a run removed or replaced, by bulks of {@code batchSize}.
	 *
	 * @param operation  operation that wrote the backup file
	 * @param backupFile JSONL backup file
	 * @param batchSize  lines restored per bulk
	 * @param maxProductsPerSecond restore rate cap, in lines per second; 0 means no cap
	 * @param stop       polled between two bulks: true stops the restore
	 * @param onBatch    receives the counters after each bulk
	 * @return the report of the restore
	 * @throws IOException                   when the backup file cannot be read
	 * @throws UnsupportedOperationException when the operation has no restore script
	 */
	public BackfillReport restore(BackfillOperation operation, Path backupFile, int batchSize,
			int maxProductsPerSecond, BooleanSupplier stop, Consumer<BackfillReport> onBatch) throws IOException {
		String script = operation.restoreScript();
		if (script == null) {
			throw new UnsupportedOperationException("Operation " + operation.name() + " cannot be restored");
		}
		long start = System.currentTimeMillis();
		Counters counters = new Counters();
		String stopReason = null;
		logger.info("Restore of backfill {} from {} started", operation.name(), backupFile);

		try (BufferedReader reader = Files.newBufferedReader(backupFile, StandardCharsets.UTF_8)) {
			List<String> ids = new ArrayList<>();
			List<BackfillChange> changes = new ArrayList<>();
			String line;
			long lineNumber = 0;
			long pageStart = System.currentTimeMillis();
			boolean more = true;
			while (more) {
				line = reader.readLine();
				more = line != null;
				if (more && !line.isBlank()) {
					lineNumber++;
					counters.scanned++;
					BackupLine parsed = parse(line);
					if (parsed == null) {
						// A truncated or corrupt line is skipped: the other lines are still restored
						counters.failed++;
						logger.error("Restore of backfill {}: unreadable line {} of {}, skipped", operation.name(),
								lineNumber, backupFile);
					} else {
						BackfillChange change = new BackfillChange(script, Map.of("data", parsed.data()), null,
								List.copyOf(parsed.data().keySet()));
						counters.matched(parsed.id(), change);
						ids.add(parsed.id());
						changes.add(change);
					}
				}
				if (!changes.isEmpty() && (changes.size() >= batchSize || !more)) {
					if (stop.getAsBoolean()) {
						stopReason = BackfillReport.STOP_REQUESTED;
						break;
					}
					int size = changes.size();
					if (write(operation, ids, changes, counters)) {
						stopReason = BackfillReport.BACKPRESSURE;
						break;
					}
					ids.clear();
					changes.clear();
					onBatch.accept(counters.restoreReport(operation, backupFile, null, start));
					pace(maxProductsPerSecond, size, pageStart, stop);
					pageStart = System.currentTimeMillis();
				}
			}
		}
		BackfillReport report = counters.restoreReport(operation, backupFile, stopReason, start);
		logger.info("Restore of backfill {} from {} {}: {}", operation.name(), backupFile,
				stopReason == null ? "done" : "stopped", report);
		return report;
	}

	/**
	 * Sends one bulk of scripted updates.
	 *
	 * @return true when Elasticsearch rejected the bulk or one of its items with a 429: the caller stops
	 */
	private boolean write(BackfillOperation operation, List<String> ids, List<BackfillChange> changes,
			Counters counters) throws IOException {
		List<BulkOperation> operations = new ArrayList<>(changes.size());
		for (int i = 0; i < changes.size(); i++) {
			String id = ids.get(i);
			BackfillChange change = changes.get(i);
			Map<String, JsonData> params = new LinkedHashMap<>();
			// JsonData refuses null: a missing parameter reads as null in painless
			change.params().forEach((k, v) -> {
				if (v != null) {
					params.put(k, JsonData.of(v));
				}
			});
			operations.add(BulkOperation.of(o -> o.update(u -> u
					.index(index)
					.id(id)
					.retryOnConflict(RETRY_ON_CONFLICT)
					.action(a -> a.script(s -> s
							.source(src -> src.scriptString(change.script()))
							.lang("painless")
							.params(params))))));
		}
		BulkResponse response;
		try {
			response = client.bulk(b -> b.index(index).refresh(Refresh.False).operations(operations));
		} catch (ElasticsearchException e) {
			counters.failed += changes.size();
			if (isBackpressure(e)) {
				logger.warn("Backfill {}: bulk of {} updates rejected by Elasticsearch ({}), stopping",
						operation.name(), changes.size(), e.getMessage());
				return true;
			}
			throw e;
		}
		boolean backpressure = false;
		int failures = 0;
		Map<String, Integer> failuresByType = new LinkedHashMap<>();
		for (BulkResponseItem item : response.items()) {
			if (item.error() == null) {
				if ("noop".equals(item.result())) {
					counters.unchanged++;
				} else {
					counters.written++;
				}
			} else if (item.status() == 404) {
				// Product deleted since the snapshot read it: nothing left to change
				counters.unchanged++;
			} else {
				counters.failed++;
				failures++;
				backpressure |= item.status() == 429;
				failuresByType.merge(item.status() + " " + item.error().type(), 1, Integer::sum);
				if (failures <= LOGGED_FAILURES_PER_BULK) {
					logger.error("Backfill {}: update of product {} failed ({}): {}", operation.name(), item.id(),
							item.status(), item.error().reason());
				}
			}
		}
		if (failures > 0) {
			logger.error("Backfill {}: {} of {} updates failed in this bulk, by status and type: {}", operation.name(),
					failures, changes.size(), failuresByType);
		}
		if (backpressure) {
			logger.warn("Backfill {}: Elasticsearch rejected updates with a 429, stopping", operation.name());
			return true;
		}
		if (failures >= MIN_FAILURES_TO_STOP && failures * 2 > changes.size()) {
			// Systematic failure (missing mapping, script error...): stop before failing millions of updates
			throw new IllegalStateException("Backfill " + operation.name() + ": " + failures + " of " + changes.size()
					+ " updates failed, stopping: " + failuresByType);
		}
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private SearchResponse<Map<String, Object>> search(BackfillOperation operation, int size, String pitId,
			List<FieldValue> after) throws IOException {
		SearchResponse response = client.search(s -> {
			s.pit(p -> p.id(pitId).keepAlive(t -> t.time(keepAliveTime())))
					.query(operation.query())
					.size(size)
					.trackTotalHits(t -> t.enabled(false))
					// A failed shard fails the page rather than leaving its products out of it
					.allowPartialSearchResults(false)
					.sort(o -> o.field(f -> f.field("_shard_doc").order(SortOrder.Asc)))
					.source(src -> src.filter(f -> f.includes(operation.sourceFields())));
			if (after != null && !after.isEmpty()) {
				s.searchAfter(after);
			}
			return s;
		}, Map.class);
		return response;
	}

	private String openPit() throws IOException {
		return client.openPointInTime(r -> r.index(index).keepAlive(t -> t.time(keepAliveTime()))).id();
	}

	private void closePit(String pitId) {
		try {
			client.closePointInTime(r -> r.id(pitId));
		} catch (IOException | ElasticsearchException e) {
			logger.warn("Cannot close point in time {}: it expires after {}", pitId, keepAlive, e);
		}
	}

	private String keepAliveTime() {
		return keepAlive.toSeconds() + "s";
	}

	/**
	 * Paces each page on its own: a page of {@code size} products takes at least {@code size / rate} seconds, so a slow
	 * period never builds up a credit spent at full speed afterwards. Sleeps by slices of at most 1 s and stops
	 * sleeping as soon as a stop is requested.
	 */
	private static void pace(int maxProductsPerSecond, int size, long pageStart, BooleanSupplier stop) {
		if (maxProductsPerSecond <= 0) {
			return;
		}
		long wakeUp = pageStart + (long) size * 1000 / maxProductsPerSecond;
		long now = System.currentTimeMillis();
		while (now < wakeUp && !stop.getAsBoolean()) {
			try {
				Thread.sleep(Math.min(1000, wakeUp - now));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			now = System.currentTimeMillis();
		}
	}

	/**
	 * Refuses a pace that would leave the point in time idle past half its keep alive: a page of {@code batchSize}
	 * products at {@code maxProductsPerSecond} must take less than that.
	 *
	 * @throws IllegalArgumentException when the page would take too long
	 */
	public void requirePaceWithinKeepAlive(int batchSize, int maxProductsPerSecond) {
		if (maxProductsPerSecond > 0 && (long) batchSize * 1000 / maxProductsPerSecond > keepAlive.toMillis() / 2) {
			throw new IllegalArgumentException("batchSize / maxProductsPerSecond must stay under " + keepAlive.dividedBy(2)
					+ ", or the point in time expires between two pages");
		}
	}

	/**
	 * Tells whether shards failed on this page. Partial results are refused in the request, so this is a safety net:
	 * a rejection (429) stops the run like a rejected search, any other failure fails it.
	 *
	 * @return true when a shard rejected the page (backpressure)
	 * @throws IllegalStateException when a shard failed for another reason
	 */
	private static boolean partial(BackfillOperation operation, SearchResponse<?> page) {
		if (page.shards() == null || page.shards().failed() == null || page.shards().failed().intValue() == 0) {
			return false;
		}
		List<ShardFailure> failures = page.shards().failures();
		boolean rejected = failures.stream().anyMatch(f -> "429".equals(f.status())
				|| (f.reason() != null && f.reason().type() != null && f.reason().type().contains("rejected")));
		if (rejected) {
			logger.warn("Backfill {}: {} shards rejected the page, stopping", operation.name(), page.shards().failed());
			return true;
		}
		throw new IllegalStateException("Backfill " + operation.name() + ": " + page.shards().failed()
				+ " shards failed: " + failures.stream().map(f -> f.reason() == null ? "?" : f.reason().reason()).toList());
	}

	/**
	 * A run killed in the middle of {@code BackupWriter.append} can leave a partial last line. Its page was never sent (the
	 * backup is forced to disk before the bulk), so the fragment is dropped before a resumed run appends after
	 * it, which would otherwise glue its first line to the fragment.
	 */
	static void dropPartialLastLine(Path file) throws IOException {
		if (!Files.isRegularFile(file) || Files.size(file) == 0) {
			return;
		}
		try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
			long end = raf.length();
			long position = end - 1;
			while (position >= 0) {
				raf.seek(position);
				if (raf.read() == '\n') {
					break;
				}
				position--;
			}
			long keep = position + 1;
			if (keep < end) {
				logger.warn("Backup {}: dropping a partial last line of {} bytes left by an interrupted run", file,
						end - keep);
				raf.setLength(keep);
			}
		}
	}

	private static BackupLine parse(String line) {
		try {
			BackupLine parsed = JSON.readValue(line, BackupLine.class);
			return parsed == null || parsed.id() == null || parsed.data() == null ? null : parsed;
		} catch (JacksonException e) {
			return null;
		}
	}

	/** 429: search or bulk queue full, circuit breaker. */
	static boolean isBackpressure(ElasticsearchException e) {
		return e.status() == 429;
	}

	private static List<Long> toLongs(List<FieldValue> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().map(FieldValue::longValue).toList();
	}

	private static List<FieldValue> toFieldValues(List<Long> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.stream().map(v -> FieldValue.of(v.longValue())).toList();
	}

	/** One line of a backup file. */
	record BackupLine(String id, Map<String, Object> data) {
	}

	/** JSONL backup file, appended to and forced to disk after each page. */
	private static final class BackupWriter implements AutoCloseable {

		private final FileOutputStream out;
		private final Writer writer;

		BackupWriter(Path file) throws IOException {
			Path folder = file.toAbsolutePath().getParent();
			if (folder != null) {
				Files.createDirectories(folder);
			}
			dropPartialLastLine(file);
			out = new FileOutputStream(file.toFile(), true);
			writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		}

		void append(List<String> ids, List<BackfillChange> changes) throws IOException {
			for (int i = 0; i < changes.size(); i++) {
				Map<String, Object> data = changes.get(i).backup();
				if (data == null) {
					continue;
				}
				writer.write(JSON.writeValueAsString(new BackupLine(ids.get(i), data)));
				writer.write('\n');
			}
			writer.flush();
			out.getFD().sync();
		}

		@Override
		public void close() throws IOException {
			writer.close();
		}
	}

	/** Mutable counters of a run. */
	private static final class Counters {
		long scanned;
		long matched;
		long written;
		long unchanged;
		long failed;
		final Map<String, Long> labels = new LinkedHashMap<>();
		final List<String> samples = new ArrayList<>();

		void matched(String id, BackfillChange change) {
			matched++;
			change.labels().forEach(label -> labels.merge(label, 1L, Long::sum));
			if (samples.size() < SAMPLE_SIZE) {
				samples.add(id);
			}
		}

		BackfillReport report(BackfillOperation operation, boolean dryRun, Path backupFile, BackfillCursor cursor,
				String stopReason, long start) {
			return new BackfillReport(operation.name(), dryRun, false, scanned, matched, written, unchanged, failed,
					Map.copyOf(labels), List.copyOf(samples),
					dryRun || backupFile == null ? null : backupFile.toString(), cursor, stopReason,
					System.currentTimeMillis() - start, Map.copyOf(operation.details()));
		}

		BackfillReport restoreReport(BackfillOperation operation, Path backupFile, String stopReason, long start) {
			return new BackfillReport(operation.name(), false, true, scanned, matched, written, unchanged, failed,
					Map.copyOf(labels), List.copyOf(samples), backupFile.toString(), null, stopReason,
					System.currentTimeMillis() - start, Map.of());
		}
	}
}
