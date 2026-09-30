package org.open4goods.api.repository.datareference;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.projection.EvaluationSummary;
import org.open4goods.datareference.model.projection.OfferSummary;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;
import org.open4goods.datareference.model.projection.ProductReferenceProjectionEnvelope;
import org.open4goods.datareference.model.projection.ProjectionReplayInputs;
import org.open4goods.datareference.model.projection.SearchSummary;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.port.ScanRequest;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/** Integration coverage for the strict, aliased GTIN projection store. */
@Testcontainers
class ElasticsearchProjectionStoreTest {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .waitingFor(Wait.forHttp("/").forPort(9200).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));

    static ElasticsearchClient client;
    private ElasticsearchProjectionStore store;

    @BeforeAll
    static void connect() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress()).build();
        client = ElasticsearchClients.createImperative(Rest5Clients.getRest5Client(configuration));
        java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("http://" + ELASTICSEARCH.getHttpHostAddress() + "/_cluster/settings"))
                .header("Content-Type", "application/json")
                .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"persistent\":{\"action.destructive_requires_name\":false}}"))
                .build(), java.net.http.HttpResponse.BodyHandlers.discarding());
    }

    @AfterEach
    void cleanUp() throws Exception {
        client.indices().delete(delete -> delete.index("o4g-product-reference-projections-*").ignoreUnavailable(true));
    }

    @AfterAll
    static void closeClient() throws Exception {
        client._transport().close();
    }

    @Test
    void storesOneSurfaceSeparatedEnvelopePerGtinAndScansItThroughAPit() throws Exception {
        store = new ElasticsearchProjectionStore(client);
        ProductReferenceProjectionEnvelope first = projection("4006381333931");
        ProductReferenceProjectionEnvelope second = projection("0123456789012");

        store.writeAll(List.of(second, first));
        var page = store.scan(ScanRequest.first(1));
        var last = store.scan(ScanRequest.first(1).resumeAt(page.nextCursor().orElseThrow()));
        Map<String, Object> raw = client.get(get -> get.index(ElasticsearchProjectionStore.READ_ALIAS)
                .id(first.gtin().value()), Map.class).source();

        assertThat(store.find(first.gtin())).contains(first);
        assertThat(page.elements()).extracting(value -> value.gtin().value()).containsExactly(second.gtin().value());
        assertThat(last.elements()).extracting(value -> value.gtin().value()).containsExactly(first.gtin().value());
        assertThat(last.nextCursor()).isEmpty();
        assertThat(raw).containsKeys("gtin", "projectionJson", "nudgerWebSearchTerms", "b2bApiSearchTerms",
                "odblExportSearchTerms").doesNotContainKeys("resolvedValues", "candidateAssertionIds", "assertions");
        assertThat(client.indices().getMapping(get -> get.index(ElasticsearchProjectionStore.READ_ALIAS)).mappings()
                .get(ElasticsearchProjectionStore.READ_ALIAS + "-v1-000001").mappings().dynamic())
                .isEqualTo(co.elastic.clients.elasticsearch._types.mapping.DynamicMapping.Strict);
    }

    @Test
    void migratesACompatibleLegacyAliasBeforeServingItsProjection() throws Exception {
        ProductReferenceProjectionEnvelope legacy = projection("4006381333931");
        String legacyIndex = ElasticsearchProjectionStore.READ_ALIAS + "-v0-000001";
        client.indices().create(create -> create.index(legacyIndex)
                .aliases(ElasticsearchProjectionStore.READ_ALIAS, alias -> alias)
                .aliases(ElasticsearchProjectionStore.WRITE_ALIAS, alias -> alias.isWriteIndex(true)));
        client.index(index -> index.index(ElasticsearchProjectionStore.WRITE_ALIAS).id(legacy.gtin().value())
                .document(Map.of("gtin", legacy.gtin().value(), "projectionJson",
                        DataReferenceJson.mapper().writeValueAsString(legacy))));

        store = new ElasticsearchProjectionStore(client);

        assertThat(store.find(legacy.gtin())).contains(legacy);
        assertThat(client.indices().getAlias(get -> get.name(ElasticsearchProjectionStore.READ_ALIAS)).aliases().keySet())
                .containsExactly(ElasticsearchProjectionStore.READ_ALIAS + "-v1-000001");
    }

    private static ProductReferenceProjectionEnvelope projection(String gtinValue) {
        Gtin gtin = new Gtin(gtinValue);
        Map<ProjectionSurface, ProductReferenceProjection> components = new EnumMap<>(ProjectionSurface.class);
        for (ProjectionSurface surface : ProjectionSurface.values()) {
            components.put(surface, new ProductReferenceProjection(gtin, surface, replayInputs(), Instant.EPOCH, List.of(),
                    new OfferSummary(0, false, null, null, Instant.EPOCH),
                    new EvaluationSummary(new RuleVersion("evaluation", 1), Instant.EPOCH, Map.of(), Map.of(), List.of()),
                    new SearchSummary(new RuleVersion("lexical-search", 1), List.of(surface.name().toLowerCase()))));
        }
        return new ProductReferenceProjectionEnvelope(gtin, components);
    }

    private static ProjectionReplayInputs replayInputs() {
        return new ProjectionReplayInputs(new RegistryVersion(1), new RuleVersion("normalization", 1),
                new RuleVersion("resolution", 1), List.of(new SourceUsagePolicyRef("policy", "1")), Instant.EPOCH);
    }
}
