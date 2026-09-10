package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.services.IcecatIndexVersionManager.IndexSwitchResult;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * Integration coverage for {@link IcecatIndexVersionManager} against a real Elasticsearch:
 * clean import, alias switch, rollback, tolerance of a pre-existing concrete index at the
 * alias name (migration), retry after a failed import, and tolerance of an unrelated stray
 * index left behind by an interrupted run.
 */
@Testcontainers
class IcecatIndexVersionManagerIT {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    static ElasticsearchOperations operations;

    @BeforeAll
    static void connect() throws Exception {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress())
                .build();
        ElasticsearchClient client = ElasticsearchClients.createImperative(Rest5Clients.getRest5Client(clientConfiguration));
        operations = new ElasticsearchTemplate(client);

        // Allow the wildcard index deletes this test class uses for between-test cleanup; ES
        // blocks them by default (action.destructive_requires_name) even on this throwaway container.
        java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
        http.send(java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://" + ELASTICSEARCH.getHttpHostAddress() + "/_cluster/settings"))
                        .header("Content-Type", "application/json")
                        .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(
                                "{\"persistent\":{\"action.destructive_requires_name\":false}}"))
                        .build(),
                java.net.http.HttpResponse.BodyHandlers.discarding());
    }

    /**
     * Every test targets the same alias ({@code icecat-features}); wipe every physical index
     * matching it after each test so tests don't see each other's indices/aliases.
     */
    @AfterEach
    void cleanUp() {
        operations.indexOps(IndexCoordinates.of("icecat-features*")).delete();
    }

    private static IcecatFeatureDocument feature(int id, String englishName) {
        IcecatFeatureDocument doc = new IcecatFeatureDocument();
        doc.setId(id);
        doc.setType("numerical");
        doc.setEnglishName(englishName);
        return doc;
    }

    @Test
    void cleanImportCreatesTheAliasAndIsReadableThroughIt() {
        IcecatIndexVersionManager manager = new IcecatIndexVersionManager(operations);
        String alias = operations.indexOps(IcecatFeatureDocument.class).getIndexCoordinates().getIndexName();

        IndexSwitchResult result = manager.reimport(IcecatFeatureDocument.class, List.of(feature(1, "Weight")));

        assertThat(result.documentCount()).isEqualTo(1);
        assertThat(manager.currentIndexesForAlias(alias)).containsExactly(result.indexName());
        assertThat(operations.count(org.springframework.data.elasticsearch.core.query.Query.findAll(),
                IcecatFeatureDocument.class, IndexCoordinates.of(alias))).isEqualTo(1);
    }

    @Test
    void secondImportSwitchesTheAliasAndRetainsThePreviousIndexForRollback() {
        IcecatIndexVersionManager manager = new IcecatIndexVersionManager(operations);
        String alias = operations.indexOps(IcecatFeatureDocument.class).getIndexCoordinates().getIndexName();

        IndexSwitchResult first = manager.reimport(IcecatFeatureDocument.class, List.of(feature(10, "Depth")));
        IndexSwitchResult second = manager.reimport(IcecatFeatureDocument.class,
                List.of(feature(10, "Depth"), feature(11, "Height")));

        Set<String> live = manager.currentIndexesForAlias(alias);
        assertThat(live).containsExactly(second.indexName());
        assertThat(live).doesNotContain(first.indexName());

        // The previous index is retained (not deleted) so it can be rolled back to.
        IndexOperations previousIndexOps = operations.indexOps(IndexCoordinates.of(first.indexName()));
        assertThat(previousIndexOps.exists()).isTrue();
    }

    @Test
    void rollbackPointsTheAliasBackAtARetainedPreviousVersion() {
        IcecatIndexVersionManager manager = new IcecatIndexVersionManager(operations);
        String alias = operations.indexOps(IcecatFeatureDocument.class).getIndexCoordinates().getIndexName();

        IndexSwitchResult first = manager.reimport(IcecatFeatureDocument.class, List.of(feature(20, "Width")));
        manager.reimport(IcecatFeatureDocument.class, List.of(feature(20, "Width"), feature(21, "Weight")));

        manager.rollback(alias, first.indexName());

        assertThat(manager.currentIndexesForAlias(alias)).containsExactly(first.indexName());
    }

    @Test
    void reimportMigratesAPreExistingConcreteIndexAtTheAliasName() {
        IcecatIndexVersionManager manager = new IcecatIndexVersionManager(operations);
        IndexOperations entityIndexOps = operations.indexOps(IcecatFeatureDocument.class);
        String alias = entityIndexOps.getIndexCoordinates().getIndexName();

        // Simulate a pre-migration deployment: a concrete index (not an alias) at the alias name.
        assertThat(entityIndexOps.exists()).isFalse();
        entityIndexOps.create(new java.util.HashMap<>(entityIndexOps.createSettings()), entityIndexOps.createMapping());
        assertThat(entityIndexOps.exists()).isTrue();
        assertThat(manager.currentIndexesForAlias(alias)).isEmpty();

        IndexSwitchResult result = manager.reimport(IcecatFeatureDocument.class, List.of(feature(30, "Diagonal")));

        assertThat(manager.currentIndexesForAlias(alias)).containsExactly(result.indexName());
    }

    @Test
    void aFailedImportCanBeRetriedWithoutLeavingAnOrphanIndexOrTouchingTheAlias() {
        IcecatIndexVersionManager manager = new IcecatIndexVersionManager(operations);
        String alias = operations.indexOps(IcecatFeatureDocument.class).getIndexCoordinates().getIndexName();

        IndexSwitchResult good = manager.reimport(IcecatFeatureDocument.class, List.of(feature(40, "Length")));

        // Two documents sharing the same @Id upsert to one, so the post-write count (1) will not
        // match the requested count (2) -- a real, non-mocked way to trigger validation failure.
        List<IcecatFeatureDocument> colliding = List.of(feature(41, "Duplicate"), feature(41, "Duplicate again"));

        assertThatThrownBy(() -> manager.reimport(IcecatFeatureDocument.class, colliding))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("validation failed");

        // The alias was never touched by the failed attempt.
        assertThat(manager.currentIndexesForAlias(alias)).containsExactly(good.indexName());

        // Retrying with valid data succeeds cleanly.
        IndexSwitchResult retried = manager.reimport(IcecatFeatureDocument.class, List.of(feature(42, "Retry ok")));
        assertThat(manager.currentIndexesForAlias(alias)).containsExactly(retried.indexName());
    }

    @Test
    void anUnrelatedStrayIndexDoesNotDisruptASubsequentImport() {
        IcecatIndexVersionManager manager = new IcecatIndexVersionManager(operations);
        String alias = operations.indexOps(IcecatFeatureDocument.class).getIndexCoordinates().getIndexName();

        IndexSwitchResult good = manager.reimport(IcecatFeatureDocument.class, List.of(feature(50, "Stray-safe")));

        // Simulate an interrupted run: a versioned-looking index exists but was never wired to the
        // alias (the process died between creating it and switching the alias).
        String strayIndexName = alias + "-1";
        IndexOperations strayOps = operations.indexOps(IndexCoordinates.of(strayIndexName));
        IndexOperations entityIndexOps = operations.indexOps(IcecatFeatureDocument.class);
        strayOps.create(new java.util.HashMap<>(entityIndexOps.createSettings()), entityIndexOps.createMapping());

        assertThat(manager.currentIndexesForAlias(alias)).containsExactly(good.indexName());

        IndexSwitchResult next = manager.reimport(IcecatFeatureDocument.class, List.of(feature(50, "Stray-safe-2")));

        assertThat(manager.currentIndexesForAlias(alias)).containsExactly(next.indexName());
        strayOps.delete();
    }
}
