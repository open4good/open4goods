package org.open4goods.api.services.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import co.elastic.clients.elasticsearch.core.search.Hit;

/**
 * GOU-2266: the backfill stops cleanly when Elasticsearch answers 429, on a bulk item or on the search, and keeps the
 * cursor of the last page fully handled.
 */
class BackfillEngineBackpressureTest {

	private final ElasticsearchClient client = mock(ElasticsearchClient.class);

	@TempDir
	Path tmp;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void openPit() throws IOException {
		when(client.openPointInTime(any(Function.class)))
				.thenReturn(OpenPointInTimeResponse.of(r -> r.id("pit-1").shards(s -> s.total(1).successful(1).failed(0))));
	}

	@Test
	@SuppressWarnings("unchecked")
	void rejectedBulkItemStopsTheRunBeforeMovingTheCursor() throws IOException {
		when(client.search(any(Function.class), eq(Map.class))).thenReturn(page(hit("1", 10L), hit("2", 11L)));
		when(client.bulk(any(Function.class))).thenReturn(BulkResponse.of(b -> b.took(1).errors(true).items(
				BulkResponseItem.of(i -> i.operationType(OperationType.Update).index("products").id("1").status(200)),
				BulkResponseItem.of(i -> i.operationType(OperationType.Update).index("products").id("2").status(429)
						.error(e -> e.type("es_rejected_execution_exception").reason("queue full"))))));
		Path backup = tmp.resolve("backup.jsonl");

		BackfillReport report = engine().run(new DecitrePurgeOperation(), new BackfillOptions(false, 2, 0, false),
				backup, null, () -> false, r -> { });

		assertThat(report.stopReason()).isEqualTo(BackfillReport.BACKPRESSURE);
		assertThat(report.writtenProducts()).isEqualTo(1);
		assertThat(report.failedProducts()).isEqualTo(1);
		assertThat(report.cursor().searchAfter()).as("the rejected page is read again on resume").isEmpty();
		assertThat(Files.readAllLines(backup, StandardCharsets.UTF_8)).as("backup written before the bulk").hasSize(2);
		verify(client, never()).closePointInTime(any(Function.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void rejectedSearchStopsTheRun() throws IOException {
		when(client.search(any(Function.class), eq(Map.class))).thenThrow(new ElasticsearchException("search",
				ErrorResponse.of(e -> e.status(429).error(c -> c.type("es_rejected_execution_exception").reason("queue full")))));

		BackfillReport report = engine().run(new DecitrePurgeOperation(), new BackfillOptions(true, 2, 0, false), null,
				null, () -> false, r -> { });

		assertThat(report.stopReason()).isEqualTo(BackfillReport.BACKPRESSURE);
		assertThat(report.scannedProducts()).isZero();
	}

	@Test
	@SuppressWarnings("unchecked")
	void dryRunNeverSendsABulk() throws IOException {
		when(client.search(any(Function.class), eq(Map.class))).thenReturn(page(hit("1", 10L)), emptyPage());

		BackfillReport report = engine().run(new DecitrePurgeOperation(), new BackfillOptions(true, 2, 0, false), null,
				null, () -> false, r -> { });

		assertThat(report.matchedProducts()).isEqualTo(1);
		assertThat(report.stopReason()).isNull();
		verify(client, never()).bulk(any(Function.class));
		verify(client, never()).bulk(any(BulkRequest.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void shardRejectionStopsTheRunWithoutMovingTheCursor() throws IOException {
		when(client.search(any(Function.class), eq(Map.class))).thenReturn(SearchResponse.of(r -> r.took(1)
				.timedOut(false).pitId("pit-1")
				.shards(s -> s.total(10).successful(9).failed(1).failures(f -> f.shard(3).status("429")
						.reason(c -> c.type("es_rejected_execution_exception").reason("queue full"))))
				.hits(h -> h.hits(List.of(hit("1", 10L))))));

		BackfillReport report = engine().run(new DecitrePurgeOperation(), new BackfillOptions(true, 2, 0, false), null,
				null, () -> false, r -> { });

		assertThat(report.stopReason()).isEqualTo(BackfillReport.BACKPRESSURE);
		assertThat(report.scannedProducts()).as("the partial page is not used").isZero();
		assertThat(report.cursor().searchAfter()).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void otherShardFailureFailsTheRun() throws IOException {
		when(client.search(any(Function.class), eq(Map.class))).thenReturn(SearchResponse.of(r -> r.took(1)
				.timedOut(false).pitId("pit-1")
				.shards(s -> s.total(10).successful(9).failed(1).failures(f -> f.shard(3).status("500")
						.reason(c -> c.type("search_context_missing_exception").reason("lost"))))
				.hits(h -> h.hits(List.of(hit("1", 10L))))));

		assertThatThrownBy(() -> engine().run(new DecitrePurgeOperation(), new BackfillOptions(true, 2, 0, false), null,
				null, () -> false, r -> { })).isInstanceOf(IllegalStateException.class).hasMessageContaining("lost");
	}

	@Test
	@SuppressWarnings("unchecked")
	void noopIsCountedApart() throws IOException {
		when(client.search(any(Function.class), eq(Map.class))).thenReturn(page(hit("1", 10L), hit("2", 11L)),
				emptyPage());
		when(client.bulk(any(Function.class))).thenReturn(BulkResponse.of(b -> b.took(1).errors(false).items(
				BulkResponseItem.of(i -> i.operationType(OperationType.Update).index("products").id("1").status(200)
						.result("updated")),
				BulkResponseItem.of(i -> i.operationType(OperationType.Update).index("products").id("2").status(200)
						.result("noop")))));

		BackfillReport report = engine().run(new DecitrePurgeOperation(), new BackfillOptions(false, 2, 0, false),
				tmp.resolve("backup.jsonl"), null, () -> false, r -> { });

		assertThat(report.writtenProducts()).isEqualTo(1);
		assertThat(report.unchangedProducts()).isEqualTo(1);
	}

	@Test
	void paceThatOutlivesThePointInTimeIsRefused() {
		assertThatThrownBy(() -> engine().requirePaceWithinKeepAlive(5000, 1))
				.isInstanceOf(IllegalArgumentException.class);
		engine().requirePaceWithinKeepAlive(1000, 2000);
	}

	@Test
	void partialLastLineOfABackupIsDropped() throws IOException {
		Path backup = tmp.resolve("backup.jsonl");
		Files.writeString(backup, "{\"id\":\"1\",\"data\":{}}\n{\"id\":\"2\",\"da", StandardCharsets.UTF_8);

		BackfillEngine.dropPartialLastLine(backup);

		assertThat(Files.readString(backup, StandardCharsets.UTF_8)).isEqualTo("{\"id\":\"1\",\"data\":{}}\n");
	}

	private BackfillEngine engine() {
		return new BackfillEngine(client, "products", Duration.ofMinutes(1));
	}

	private static Hit<Object> hit(String id, long shardDoc) {
		Map<String, Object> source = Map.of("attributes",
				Map.of("all", Map.of("CLASSIFICATION DECITRE 1", Map.of("name", "CLASSIFICATION DECITRE 1"))));
		return Hit.of(h -> h.index("products").id(id).source(source).sort(List.of(FieldValue.of(shardDoc))));
	}

	@SafeVarargs
	private static SearchResponse<Object> page(Hit<Object>... hits) {
		return SearchResponse.of(r -> r.took(1).timedOut(false).pitId("pit-1")
				.shards(s -> s.total(1).successful(1).failed(0))
				.hits(h -> h.hits(List.of(hits))));
	}

	private static SearchResponse<Object> emptyPage() {
		return SearchResponse.of(r -> r.took(1).timedOut(false).pitId("pit-1")
				.shards(s -> s.total(1).successful(1).failed(0))
				.hits(h -> h.hits(List.of())));
	}
}
