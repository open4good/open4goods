package org.open4goods.api.services.backfill;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.model.product.Product;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;

/**
 * GOU-2266 against a real Elasticsearch, on the real products mapping ({@code strict}, {@code attributes.all}
 * {@code enabled:false}): the dry run counts, the write removes only the targeted keys (the rest of {@code _source}
 * is identical), the restore puts the document back as it was, and a stopped run resumes from its cursor.
 *
 * <p>Not picked up by surefire (IT suffix) and no failsafe execution is bound, so it is not part of the PR CI. Run it
 * with {@code mvn -pl api test -Dtest=BackfillEngineIT -Dsurefire.failIfNoSpecifiedTests=false} (Docker required,
 * one Docker test at a time on the shared host).
 */
@Testcontainers
class BackfillEngineIT {

	@Container
	static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
			DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
			.withEnv("xpack.security.enabled", "false")
			.withEnv("discovery.type", "single-node");

	private static final String INDEX = Product.DEFAULT_REPO;

	static ElasticsearchClient client;
	static ElasticsearchOperations operations;

	@TempDir
	Path tmp;

	/** Sources as indexed, by id. */
	private final Map<String, Map<String, Object>> originals = new LinkedHashMap<>();

	@BeforeAll
	static void connect() {
		ClientConfiguration clientConfiguration = ClientConfiguration.builder()
				.connectedTo(ELASTICSEARCH.getHttpHostAddress())
				.build();
		client = ElasticsearchClients.createImperative(Rest5Clients.getRest5Client(clientConfiguration));
		ElasticsearchCustomConversions conversions = new ElasticsearchCustomConversions(List.of());
		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
		converter.setConversions(conversions);
		converter.afterPropertiesSet();
		operations = new ElasticsearchTemplate(client, converter);
	}

	@BeforeEach
	void createIndex() throws IOException {
		IndexOperations productOperations = operations.indexOps(Product.class);
		productOperations.create(productOperations.createSettings(), productOperations.createMapping());

		// Books with Decitre keys, among other attributes
		for (int i = 0; i < 12; i++) {
			Map<String, Object> all = new LinkedHashMap<>();
			all.put("EDITEUR", attribute("EDITEUR", "Hachette " + i));
			all.put("CLASSIFICATION DECITRE 1", attribute("CLASSIFICATION DECITRE 1", "Littérature"));
			all.put("CLASSIFICATION DECITRE 2", attribute("CLASSIFICATION DECITRE 2", "Romans"));
			if (i % 2 == 0) {
				all.put("PROPRIÉTAIRE", attribute("PROPRIÉTAIRE", "Decitre"));
			}
			index(String.valueOf(9782000000000L + i), book("ISBN_13", all, 1_700_000_000_000L + i));
		}
		// A book without the keys, a book without attributes.all, and a non-book holding a key
		index("9782017148600", book("ISBN_13", Map.of("EDITEUR", attribute("EDITEUR", "Gallimard")), 1L));
		Map<String, Object> noAll = book("ISBN_13", null, 2L);
		index("9782017148601", noAll);
		index("3700000000001", book("GTIN_13",
				Map.of("CLASSIFICATION DECITRE 1", attribute("CLASSIFICATION DECITRE 1", "Hors livres")), 3L));
		client.indices().refresh(r -> r.index(INDEX));
	}

	@AfterEach
	void deleteIndex() throws IOException {
		client.indices().delete(r -> r.index(INDEX).ignoreUnavailable(true));
	}

	@Test
	void dryRunCountsWithoutWriting() throws IOException {
		BackfillReport report = engine().run(new DecitrePurgeOperation(), options(true, 5), null, null, () -> false,
				r -> { });

		assertThat(report.scannedProducts()).isEqualTo(14);
		assertThat(report.matchedProducts()).isEqualTo(12);
		assertThat(report.writtenProducts()).isZero();
		assertThat(report.labelCounts()).containsExactlyInAnyOrderEntriesOf(Map.of("CLASSIFICATION DECITRE 1", 12L,
				"CLASSIFICATION DECITRE 2", 12L, "PROPRIÉTAIRE", 6L));
		assertThat(report.backupFile()).isNull();
		assertThat(report.stopReason()).isNull();
		assertSources(originals);
	}

	@Test
	void writeRemovesOnlyTheKeysThenRestorePutsThemBack() throws IOException {
		Path backup = tmp.resolve("decitre-purge.jsonl");
		BackfillReport report = engine().run(new DecitrePurgeOperation(), options(false, 5), backup, null,
				() -> false, r -> { });

		assertThat(report.matchedProducts()).isEqualTo(12);
		assertThat(report.writtenProducts()).isEqualTo(12);
		assertThat(report.failedProducts()).isZero();
		assertThat(Files.readAllLines(backup, StandardCharsets.UTF_8)).hasSize(12);

		// Every other field, lastChange included, is unchanged; the non-book keeps its key
		Map<String, Map<String, Object>> expected = new LinkedHashMap<>();
		originals.forEach((id, source) -> expected.put(id,
				id.startsWith("97820000") ? withoutKeys(source, DecitrePurgeOperation.DEFAULT_KEYS) : source));
		refresh();
		assertSources(expected);

		// A second dry run is the "after" count: nothing left
		BackfillReport after = engine().run(new DecitrePurgeOperation(), options(true, 5), null, null, () -> false,
				r -> { });
		assertThat(after.matchedProducts()).isZero();

		BackfillReport restored = engine().restore(new DecitrePurgeOperation(), backup, 5, () -> false, r -> { });
		assertThat(restored.writtenProducts()).isEqualTo(12);
		refresh();
		assertSources(originals);
	}

	@Test
	void stoppedRunResumesFromItsCursor() throws IOException {
		Path backup = tmp.resolve("decitre-purge.jsonl");
		AtomicBoolean stop = new AtomicBoolean();
		// Stops after the first page of 5 products
		BackfillReport stopped = engine().run(new DecitrePurgeOperation(), options(false, 5), backup, null, stop::get,
				r -> stop.set(true));

		assertThat(stopped.stopReason()).isEqualTo(BackfillReport.STOP_REQUESTED);
		assertThat(stopped.scannedProducts()).isEqualTo(5);
		assertThat(stopped.cursor().searchAfter()).isNotEmpty();

		BackfillReport resumed = engine().run(new DecitrePurgeOperation(), options(false, 5), backup,
				stopped.cursor(), () -> false, r -> { });

		assertThat(resumed.stopReason()).isNull();
		assertThat(resumed.scannedProducts()).isEqualTo(9);
		assertThat(stopped.writtenProducts() + resumed.writtenProducts()).isEqualTo(12);
		assertThat(Files.readAllLines(backup, StandardCharsets.UTF_8)).hasSize(12);
	}

	@Test
	void expiredPointInTimeRestartsFromTheBeginning() throws IOException {
		Path backup = tmp.resolve("decitre-purge.jsonl");
		// A point in time closed since the run stopped, as one expired after its keep alive
		String expiredPit = client.openPointInTime(r -> r.index(INDEX).keepAlive(t -> t.time("1m"))).id();
		client.closePointInTime(r -> r.id(expiredPit));

		BackfillReport resumed = engine().run(new DecitrePurgeOperation(), options(false, 5), backup,
				new BackfillCursor(expiredPit, List.of(3L)), () -> false, r -> { });

		assertThat(resumed.scannedProducts()).isEqualTo(14);
		assertThat(resumed.writtenProducts()).isEqualTo(12);
	}

	private BackfillEngine engine() {
		return new BackfillEngine(client, INDEX, Duration.ofMinutes(1));
	}

	private static BackfillOptions options(boolean dryRun, int batchSize) {
		return new BackfillOptions(dryRun, batchSize, 0, false);
	}

	private static Map<String, Object> attribute(String name, String value) {
		return Map.of("name", name, "value", value,
				"sources", List.of(Map.of("datasourceName", "rakuten.com", "value", value)));
	}

	private static Map<String, Object> book(String upcType, Map<String, Object> all, long lastChange) {
		Map<String, Object> source = new HashMap<>();
		source.put("gtinInfos", Map.of("upcType", upcType));
		source.put("lastChange", lastChange);
		source.put("offersCount", 1);
		if (all != null) {
			source.put("attributes", Map.of("all", all));
		}
		return source;
	}

	private void index(String id, Map<String, Object> source) throws IOException {
		source.put("id", Long.valueOf(id));
		originals.put(id, readBack(source));
		client.index(r -> r.index(INDEX).id(id).document(source).refresh(Refresh.False));
	}

	/** Normalises a source as Elasticsearch gives it back (numbers, nested maps). */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> readBack(Map<String, Object> source) {
		return tools.jackson.databind.json.JsonMapper.builder().build()
				.readValue(tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(source), Map.class);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> withoutKeys(Map<String, Object> source, List<String> keys) {
		Map<String, Object> copy = readBack(source);
		Map<String, Object> all = (Map<String, Object>) ((Map<String, Object>) copy.get("attributes")).get("all");
		keys.forEach(all::remove);
		return copy;
	}

	private static void refresh() throws IOException {
		client.indices().refresh(r -> r.index(INDEX));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void assertSources(Map<String, Map<String, Object>> expected) throws IOException {
		List<String> ids = new ArrayList<>(expected.keySet());
		for (String id : ids) {
			Map source = client.get(r -> r.index(INDEX).id(id), Map.class).source();
			assertThat((Map<String, Object>) source).as(id).isEqualTo(expected.get(id));
		}
	}
}
