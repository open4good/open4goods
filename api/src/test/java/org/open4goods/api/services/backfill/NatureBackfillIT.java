package org.open4goods.api.services.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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

/**
 * GOU-2234 against a real Elasticsearch, on the real products mapping: the dry run counts per threshold and gives the
 * examples, the write sets or removes {@code nature} on the live products without vertical only, and leaves every
 * other field as it was; a second run writes nothing.
 *
 * <p>Not picked up by surefire (IT suffix): run it with
 * {@code mvn -pl api test -Dtest=NatureBackfillIT -Dsurefire.failIfNoSpecifiedTests=false} (Docker required).
 */
@Testcontainers
class NatureBackfillIT {

	@Container
	static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
			DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
			.withEnv("xpack.security.enabled", "false")
			.withEnv("discovery.type", "single-node");

	private static final String INDEX = Product.DEFAULT_REPO;
	private static final String SPARE = "PIECES DETACHEES LAVE-LINGE > JOINT HUBLOT";
	private static final long NOW = Instant.now().toEpochMilli();
	private static final long TEN_DAYS_AGO = Instant.now().minus(Duration.ofDays(10)).toEpochMilli();

	static ElasticsearchClient client;
	static ElasticsearchOperations operations;
	static VerticalsConfigService verticals;

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
		verticals = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
				new PathMatchingResourcePatternResolver());
	}

	@BeforeEach
	void createIndex() throws IOException {
		IndexOperations productOperations = operations.indexOps(Product.class);
		productOperations.create(productOperations.createSettings(), productOperations.createMapping());

		index("1", product(null, NOW, 1, List.of(SPARE), null));
		index("2", product(null, NOW, 2, List.of("ACCESSOIRES LAVE-LINGE"), null));
		index("3", product(null, NOW, 1, List.of("MAISON > DIVERS"), "ACCESSORY"));
		index("4", product("washing-machine", NOW, 1, List.of(SPARE), null));
		index("5", product(null, TEN_DAYS_AGO, 1, List.of(SPARE), null));
		index("6", product(null, NOW, 0, List.of(SPARE), null));
		index("7", product(null, NOW, 1,
				List.of(SPARE, "MAISON > DIVERS", "BRICOLAGE > OUTILLAGE", "LIVRES > ROMANS"), null));
		client.indices().refresh(r -> r.index(INDEX));
	}

	@AfterEach
	void deleteIndex() throws IOException {
		client.indices().delete(r -> r.index(INDEX).ignoreUnavailable(true));
	}

	@Test
	void dryRunCountsEachThresholdWithoutWriting() throws IOException {
		BackfillReport report = engine().run(operation(0), new BackfillOptions(true, 2, 0, false), null, null,
				() -> false, r -> { });

		assertThat(report.scannedProducts()).as("live products without vertical").isEqualTo(4);
		assertThat(report.matchedProducts()).isEqualTo(4);
		assertThat(report.labelCounts()).isEqualTo(Map.of("nature=SPARE_PART", 2L, "nature=ACCESSORY", 1L,
				"nature=absent", 1L));
		assertThat(report.details().get("markedByThreshold")).isEqualTo(Map.of(
				"0.0", Map.of("SPARE_PART", 2L, "ACCESSORY", 1L),
				"0.5", Map.of("SPARE_PART", 1L, "ACCESSORY", 1L)));
		assertThat(report.details().get("examplesMarkedOnlyAtLowestThreshold")).asList().singleElement()
				.extracting("gtin").isEqualTo("7");
		assertThat(report.backupFile()).isNull();
		assertSources(originals);
	}

	@Test
	void writeSetsOnlyTheNatureOfTheLiveProductsWithoutVerticalThenNothingMore() throws IOException {
		BackfillReport report = engine().run(operation(0), new BackfillOptions(false, 2, 0, false), null, null,
				() -> false, r -> { });

		assertThat(report.writtenProducts()).isEqualTo(4);
		assertThat(report.failedProducts()).isZero();
		assertThat(report.backupFile()).as("no backup: the nature is recomputable").isNull();

		Map<String, Map<String, Object>> expected = new LinkedHashMap<>(originals);
		expected.put("1", with(originals.get("1"), "SPARE_PART"));
		expected.put("2", with(originals.get("2"), "ACCESSORY"));
		expected.put("3", with(originals.get("3"), null));
		expected.put("7", with(originals.get("7"), "SPARE_PART"));
		assertSources(expected);

		client.indices().refresh(r -> r.index(INDEX));
		BackfillReport again = engine().run(operation(0), new BackfillOptions(false, 2, 0, false), null, null,
				() -> false, r -> { });
		assertThat(again.matchedProducts()).isZero();
		assertThat(again.writtenProducts()).isZero();
	}

	@Test
	void higherThresholdLeavesTheMinorityMarkedProductUnmarked() throws IOException {
		engine().run(operation(0.5), new BackfillOptions(false, 10, 0, false), null, null, () -> false, r -> { });

		assertThat(source("7")).doesNotContainKey("nature");
		assertThat(source("1")).containsEntry("nature", "SPARE_PART");
	}

	private static NatureBackfillOperation operation(double threshold) {
		return new NatureBackfillOperation(verticals, threshold, null, 2);
	}

	private static BackfillEngine engine() {
		return new BackfillEngine(client, INDEX, Duration.ofMinutes(1));
	}

	private static Map<String, Object> product(String vertical, long lastChange, int offersCount,
			List<String> categories, String nature) {
		Map<String, Object> source = new HashMap<>();
		if (vertical != null) {
			source.put("vertical", vertical);
		}
		source.put("lastChange", lastChange);
		source.put("offersCount", offersCount);
		source.put("datasourceCategories", categories);
		source.put("offerNames", List.of("Produit " + categories.getFirst()));
		if (nature != null) {
			source.put("nature", nature);
		}
		return source;
	}

	private void index(String id, Map<String, Object> source) throws IOException {
		source.put("id", Long.valueOf(id));
		originals.put(id, readBack(source));
		client.index(r -> r.index(INDEX).id(id).document(source));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readBack(Map<String, Object> source) {
		tools.jackson.databind.json.JsonMapper json = tools.jackson.databind.json.JsonMapper.builder().build();
		return json.readValue(json.writeValueAsString(source), Map.class);
	}

	private static Map<String, Object> with(Map<String, Object> source, String nature) {
		Map<String, Object> copy = readBack(source);
		if (nature == null) {
			copy.remove("nature");
		} else {
			copy.put("nature", nature);
		}
		return copy;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map<String, Object> source(String id) throws IOException {
		Map source = client.get(r -> r.index(INDEX).id(id), Map.class).source();
		return (Map<String, Object>) source;
	}

	private static void assertSources(Map<String, Map<String, Object>> expected) throws IOException {
		for (Map.Entry<String, Map<String, Object>> entry : expected.entrySet()) {
			assertThat(source(entry.getKey())).as(entry.getKey()).isEqualTo(entry.getValue());
		}
	}
}
