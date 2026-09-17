package org.open4goods.api.repository.datareference;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.GtinMatchMethod;
import org.open4goods.datareference.model.IngestionCheckpoint;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceFieldId;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceRecordTransition;
import org.open4goods.datareference.model.SourceRecordTransitionOutcome;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
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
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.search.Hit;

/** Integration coverage for aliases, CAS mutations and point-in-time source replay. */
@Testcontainers
class ElasticsearchSourceRecordStoreTest {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .waitingFor(Wait.forHttp("/").forPort(9200).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));

    static ElasticsearchClient client;
    private ElasticsearchSourceRecordStore store;

    @BeforeAll
    static void connect() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress())
                .build();
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
        client.indices().delete(delete -> delete.index("o4g-*").ignoreUnavailable(true));
    }

    @AfterAll
    static void closeClient() throws Exception {
        client._transport().close();
    }

    @Test
    void createsStrictVersionedAliasesAndDeliversAnAcceptedTransition() throws Exception {
        store = new ElasticsearchSourceRecordStore(client);
        SourceRecordHead head = head("one", "aa", SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                List.of(assertion("brand", 0, "Acme")), List.of(link("0123456789012")));

        var transition = store.apply(SourceRecordMutation.full(head));

        assertThat(transition.outcome()).isEqualTo(SourceRecordTransitionOutcome.ACCEPTED);
        assertThat(store.find(head.key())).contains(head);
        assertThat(client.indices().exists(exists -> exists.index(ElasticsearchSourceRecordStore.HEAD_READ_ALIAS)).value())
                .isTrue();
        assertThat(client.indices().exists(exists -> exists.index(ElasticsearchSourceRecordStore.JOURNAL_READ_ALIAS)).value())
                .isTrue();
        assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.JOURNAL_READ_ALIAS)).count()).isEqualTo(1);
        assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.RECOMPUTE_READ_ALIAS)).count()).isEqualTo(1);
        Map<String, Object> journal = client.get(get -> get.index(ElasticsearchSourceRecordStore.JOURNAL_READ_ALIAS)
                .id(transition.id()), Map.class).source();
        assertThat(journal).containsEntry("transitionId", transition.id()).containsEntry("outcome", "ACCEPTED")
                .doesNotContainKeys("affectedGtins", "headJson", "pendingJson", "assertions", "evidenceReference");
    }

    @Test
    void migratesALegacyReadWriteAliasBeforeServingTheHead() throws Exception {
        SourceRecordHead legacy = head("one", "aa", SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                List.of(assertion("brand", 0, "Acme")), List.of(link("0123456789012")));
        String legacyIndex = ElasticsearchSourceRecordStore.HEAD_READ_ALIAS + "-v1-000001";
        client.indices().create(create -> create.index(legacyIndex)
                .aliases(ElasticsearchSourceRecordStore.HEAD_READ_ALIAS, alias -> alias)
                .aliases(ElasticsearchSourceRecordStore.HEAD_WRITE_ALIAS, alias -> alias.isWriteIndex(true)));
        client.index(index -> index.index(ElasticsearchSourceRecordStore.HEAD_WRITE_ALIAS).id(headId(legacy.key()))
                .document(Map.of("revision", 1, "headJson", DataReferenceJson.mapper().writeValueAsString(legacy)))
                .refresh(Refresh.WaitFor));
        store = new ElasticsearchSourceRecordStore(client);

        assertThat(store.find(legacy.key())).contains(legacy);
        assertThat(client.indices().getAlias(get -> get.name(ElasticsearchSourceRecordStore.HEAD_READ_ALIAS)).aliases()
                .keySet()).containsExactly(ElasticsearchSourceRecordStore.HEAD_READ_ALIAS + "-v2-000001");
        assertThat(client.indices().getMapping(get -> get.index(ElasticsearchSourceRecordStore.HEAD_READ_ALIAS)).mappings()
                .get(ElasticsearchSourceRecordStore.HEAD_READ_ALIAS + "-v2-000001").mappings().dynamic())
                .isEqualTo(co.elastic.clients.elasticsearch._types.mapping.DynamicMapping.Strict);
    }

    @Test
    void mergesOnlyNamedPartialCoordinatesAndReplaysByGtinWithACursor() {
        store = new ElasticsearchSourceRecordStore(client);
        SourceRecordHead initial = head("one", "aa", SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                List.of(assertion("brand", 0, "Acme"), assertion("model", 0, "One")), List.of(link("0123456789012")));
        store.apply(SourceRecordMutation.full(initial));
        SourceRecordHead partial = head("one", "bb", SourceRecordCompleteness.PARTIAL, SourceRecordState.ACTIVE,
                List.of(assertion("model", 0, "Two")), List.of());

        var transition = store.apply(new SourceRecordMutation(partial, List.of(), List.of(), null));
        var page = store.scanByGtin(new Gtin("0123456789012"), ScanRequest.first(1));
        var lastPage = store.scanByGtin(new Gtin("0123456789012"),
                ScanRequest.first(1).resumeAt(page.nextCursor().orElseThrow()));

        assertThat(transition.outcome()).isEqualTo(SourceRecordTransitionOutcome.ACCEPTED);
        assertThat(store.find(initial.key()).orElseThrow().assertions())
                .extracting(assertion -> ((ScalarEvidence) assertion.evidence()).lexicalValue())
                .containsExactly("Acme", "Two");
        assertThat(page.elements()).containsExactly(store.find(initial.key()).orElseThrow());
        assertThat(lastPage.elements()).isEmpty();
        assertThat(lastPage.nextCursor()).isEmpty();
    }

    @Test
    void journalsUnavailableAttemptWithoutReplacingAUsableHead() throws Exception {
        store = new ElasticsearchSourceRecordStore(client);
        SourceRecordHead active = head("one", "aa", SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                List.of(assertion("brand", 0, "Acme")), List.of(link("0123456789012")));
        store.apply(SourceRecordMutation.full(active));
        SourceRecordHead unavailable = head("one", "bb", SourceRecordCompleteness.FULL, SourceRecordState.UNAVAILABLE,
                List.of(), List.of());

        var transition = store.apply(new SourceRecordMutation(unavailable, List.of(), List.of(), "HTTP_503"));

        assertThat(transition.outcome()).isEqualTo(SourceRecordTransitionOutcome.REJECTED);
        assertThat(store.find(active.key())).contains(active);
        assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.JOURNAL_READ_ALIAS)).count()).isEqualTo(2);
    }

    @Test
    void ordersDistinctPayloadsByObservationThenRetrievalAndLeavesDuplicatesAsNoOps() throws Exception {
        store = new ElasticsearchSourceRecordStore(client);
        Instant observed = Instant.parse("2026-09-15T00:00:00Z");
        SourceRecordHead first = headAt("one", "aa", observed, observed, SourceRecordCompleteness.FULL,
                SourceRecordState.ACTIVE, List.of(assertion("brand", 0, "Acme")), List.of(link("0123456789012")));
        SourceRecordHead laterRetrieval = headAt("one", "bb", observed, observed.plusSeconds(1),
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, List.of(assertion("brand", 0, "Acme 2")),
                List.of(link("0123456789012")));
        SourceRecordHead olderRetrieval = headAt("one", "cc", observed, observed,
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, List.of(assertion("brand", 0, "Old")),
                List.of(link("0123456789012")));
        SourceRecordHead collision = headAt("one", "dd", observed, observed.plusSeconds(1),
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, List.of(assertion("brand", 0, "Collision")),
                List.of(link("0123456789012")));

        store.apply(SourceRecordMutation.full(first));
        assertThat(store.apply(SourceRecordMutation.full(laterRetrieval)).outcome())
                .isEqualTo(SourceRecordTransitionOutcome.ACCEPTED);
        assertThat(store.apply(SourceRecordMutation.full(olderRetrieval)).outcome())
                .isEqualTo(SourceRecordTransitionOutcome.OUT_OF_ORDER);
        assertThat(store.apply(SourceRecordMutation.full(collision)).outcome())
                .isEqualTo(SourceRecordTransitionOutcome.COLLISION);
        assertThat(store.apply(SourceRecordMutation.full(laterRetrieval)).outcome())
                .isEqualTo(SourceRecordTransitionOutcome.DUPLICATE);
        assertThat(store.find(first.key())).contains(laterRetrieval);
        assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.JOURNAL_READ_ALIAS)).count()).isEqualTo(4);
    }

    @Test
    void recordsAnInitialUnavailableHeadButDoesNotMakeItEligible() throws Exception {
        store = new ElasticsearchSourceRecordStore(client);
        SourceRecordHead unavailable = head("one", "aa", SourceRecordCompleteness.FULL, SourceRecordState.UNAVAILABLE,
                List.of(), List.of());

        var transition = store.apply(new SourceRecordMutation(unavailable, List.of(), List.of(), "HTTP_503"));

        assertThat(transition.outcome()).isEqualTo(SourceRecordTransitionOutcome.REJECTED);
        assertThat(store.find(unavailable.key())).contains(unavailable);
        assertThat(store.find(unavailable.key()).orElseThrow().isUsableAt(Instant.now())).isFalse();
        assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.JOURNAL_READ_ALIAS)).count()).isEqualTo(1);
    }

    @Test
    void appliesExplicitPartialTombstonesAndFullReplacementDeletion() {
        store = new ElasticsearchSourceRecordStore(client);
        SourceRecordHead initial = head("one", "aa", SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                List.of(assertion("brand", 0, "Acme"), assertion("model", 0, "One")),
                List.of(link("0123456789012"), link("4006381333931")));
        store.apply(SourceRecordMutation.full(initial));
        SourceRecordHead partial = head("one", "bb", SourceRecordCompleteness.PARTIAL, SourceRecordState.ACTIVE,
                List.of(), List.of());

        var partialTransition = store.apply(new SourceRecordMutation(partial,
                List.of(new SourceAssertion.Coordinate(new SourceFieldId("fixture", "model", "1"), 0)),
                List.of(new Gtin("4006381333931")), null));
        SourceRecordHead afterPartial = store.find(initial.key()).orElseThrow();
        Instant fullObserved = Instant.parse("2026-09-15T00:02:00Z");
        SourceRecordHead fullEmpty = headAt("one", "cc", fullObserved, fullObserved,
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, List.of(), List.of());
        var fullTransition = store.apply(SourceRecordMutation.full(fullEmpty));

        assertThat(partialTransition.outcome()).isEqualTo(SourceRecordTransitionOutcome.ACCEPTED);
        assertThat(afterPartial.assertions()).extracting(assertion -> ((ScalarEvidence) assertion.evidence()).lexicalValue())
                .containsExactly("Acme");
        assertThat(afterPartial.gtinLinks()).extracting(GtinLink::gtin).containsExactly(new Gtin("0123456789012"));
        assertThat(fullTransition.outcome()).isEqualTo(SourceRecordTransitionOutcome.ACCEPTED);
        assertThat(store.find(initial.key()).orElseThrow().assertions()).isEmpty();
        assertThat(store.find(initial.key()).orElseThrow().gtinLinks()).isEmpty();
    }

    @Test
    void deletedStateClearsThePriorHeadAndQueuesBothFormerAttachments() throws Exception {
        store = new ElasticsearchSourceRecordStore(client);
        SourceRecordHead active = head("one", "aa", SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                List.of(assertion("brand", 0, "Acme")), List.of(link("0123456789012"), link("4006381333931")));
        store.apply(SourceRecordMutation.full(active));
        SourceRecordHead deleted = head("one", "bb", SourceRecordCompleteness.FULL, SourceRecordState.DELETED,
                List.of(), List.of());

        var transition = store.apply(SourceRecordMutation.full(deleted));

        assertThat(transition.outcome()).isEqualTo(SourceRecordTransitionOutcome.TOMBSTONED);
        assertThat(store.find(active.key())).contains(deleted);
        assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.RECOMPUTE_READ_ALIAS)).count()).isEqualTo(4);
    }

    @Test
    void expiryRetainsTheHeadAndJournalButIdempotentlyQueuesRemovalWork() throws Exception {
        store = new ElasticsearchSourceRecordStore(client);
        Instant observed = Instant.parse("2026-09-15T00:00:00Z");
        SourceRecordHead expired = headAt("one", "aa", observed, observed, SourceRecordCompleteness.FULL,
                SourceRecordState.ACTIVE, List.of(assertion("brand", 0, "Acme")), List.of(link("0123456789012")),
                observed.plusSeconds(30));
        store.apply(SourceRecordMutation.full(expired));

        assertThat(store.enqueueExpired(observed.plusSeconds(31))).isEqualTo(1);
        assertThat(store.enqueueExpired(observed.plusSeconds(31))).isZero();
        assertThat(store.find(expired.key())).contains(expired);
        assertThat(store.find(expired.key()).orElseThrow().isUsableAt(observed.plusSeconds(31))).isFalse();
        assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.JOURNAL_READ_ALIAS)).count()).isEqualTo(1);
        assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.RECOMPUTE_READ_ALIAS)).count()).isEqualTo(2);
        Map<String, Object> expiryWork = client.search(search -> search.index(ElasticsearchSourceRecordStore.RECOMPUTE_READ_ALIAS)
                .query(query -> query.term(term -> term.field("kind").value("EXPIRY"))), Map.class).hits().hits().getFirst()
                .source();
        assertThat(expiryWork).containsEntry("gtin", "0123456789012").containsEntry("kind", "EXPIRY");
    }

    @Test
    void identicalIngestionRunsProduceTheSameTransitionsAndReplayOrder() throws Exception {
        List<SourceRecordHead> input = List.of(
                head("one", "aa", SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                        List.of(assertion("brand", 0, "Acme")), List.of(link("0123456789012"))),
                head("two", "bb", SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                        List.of(assertion("brand", 0, "Bravo")), List.of(link("4006381333931"))));

        RunResult first = ingest(input);
        client.indices().delete(delete -> delete.index("o4g-*").ignoreUnavailable(true));
        RunResult second = ingest(input);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void recoversAndIdempotentlyDeliversAPendingTransitionAfterACrashWindow() throws Exception {
        store = new ElasticsearchSourceRecordStore(client);
        SourceRecordHead head = head("one", "aa", SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                List.of(assertion("brand", 0, "Acme")), List.of(link("0123456789012")));
        store.apply(SourceRecordMutation.full(head));
        SourceRecordTransition pending = new SourceRecordTransition(SourceRecordTransition.idFor(head.key(), 2), head.key(), 2,
                head.schemaVersion(), head.providerVersion(), head.payloadHash(), new PayloadHash("SHA-256", "bb"),
                head.observedAt().plusSeconds(1), head.retrievedAt().plusSeconds(1), SourceRecordState.ACTIVE,
                SourceRecordTransitionOutcome.ACCEPTED, null, List.of(new Gtin("0123456789012")));
        Hit<Map> stored = client.search(search -> search.index(ElasticsearchSourceRecordStore.HEAD_READ_ALIAS)
                .query(query -> query.term(term -> term.field("recordKey").value(head.key().externalForm()))), Map.class)
                .hits().hits().getFirst();
        Map<String, Object> crashedHead = new LinkedHashMap<>(stored.source());
        crashedHead.put("revision", pending.revision());
        crashedHead.put("hasPending", true);
        crashedHead.put("pendingJson", DataReferenceJson.mapper().writeValueAsString(pending));
        client.index(index -> index.index(ElasticsearchSourceRecordStore.HEAD_WRITE_ALIAS).id(stored.id())
                .document(crashedHead).refresh(Refresh.WaitFor));

        store.recoverPendingTransitions();
        store.recoverPendingTransitions();

        assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.JOURNAL_READ_ALIAS)).count()).isEqualTo(2);
        assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.RECOMPUTE_READ_ALIAS)).count()).isEqualTo(2);
        Map<String, Object> recovered = client.get(
                get -> get.index(ElasticsearchSourceRecordStore.HEAD_READ_ALIAS).id(stored.id()), Map.class).source();
        assertThat(recovered).containsEntry("hasPending", false);
        assertThat(recovered).doesNotContainKey("pendingJson");
    }

    @Test
    void recoversEveryCrashPointBetweenTheHeadCasAndPendingDelivery() throws Exception {
        for (int persistedPendingWrites = 0; persistedPendingWrites <= 3; persistedPendingWrites++) {
            client.indices().delete(delete -> delete.index("o4g-*").ignoreUnavailable(true));
            store = new ElasticsearchSourceRecordStore(client);
            PendingContext pending = installPendingTransition();

            if (persistedPendingWrites >= 1) {
                client.index(index -> index.index(ElasticsearchSourceRecordStore.JOURNAL_WRITE_ALIAS)
                        .id(pending.transition().id()).document(Map.of("transitionId", pending.transition().id()))
                        .refresh(Refresh.WaitFor));
            }
            for (int index = 0; index < persistedPendingWrites - 1; index++) {
                Gtin gtin = pending.transition().affectedGtins().get(index);
                client.index(write -> write.index(ElasticsearchSourceRecordStore.RECOMPUTE_WRITE_ALIAS)
                        .id(pending.transition().id() + ":" + gtin.value())
                        .document(Map.of("transitionId", pending.transition().id(), "gtin", gtin.value(),
                                "sourceId", pending.transition().key().sourceId().value(),
                                "revision", pending.transition().revision(), "kind", "TRANSITION"))
                        .refresh(Refresh.WaitFor));
            }

            store.recoverPendingTransitions();

            assertThat(store.find(pending.candidate().key())).contains(pending.candidate());
            assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.JOURNAL_READ_ALIAS)).count())
                    .isEqualTo(2);
            assertThat(client.count(count -> count.index(ElasticsearchSourceRecordStore.RECOMPUTE_READ_ALIAS)).count())
                    .isEqualTo(4);
            Map<String, Object> recovered = client.get(get -> get.index(ElasticsearchSourceRecordStore.HEAD_READ_ALIAS)
                    .id(headId(pending.candidate().key())), Map.class).source();
            assertThat(recovered).containsEntry("hasPending", false).doesNotContainKey("pendingJson");
        }
    }

    @Test
    void checkpointsAreOwnedAndCompareAndSetIndependentlyOfSourceHeads() {
        ElasticsearchIngestionCheckpointStore checkpoints = new ElasticsearchIngestionCheckpointStore(client);
        SourceId source = new SourceId("fixture");
        Instant updated = Instant.parse("2026-09-15T00:00:00Z");
        IngestionCheckpoint first = new IngestionCheckpoint("fixture-import", source, Optional.empty(), 0, Optional.empty(),
                updated);
        IngestionCheckpoint retry = new IngestionCheckpoint("fixture-import", source, Optional.empty(), 1,
                Optional.of(updated.plusSeconds(60)), updated.plusSeconds(1));

        assertThat(checkpoints.compareAndSet(first, 0)).isTrue();
        IngestionCheckpoint persistedFirst = new IngestionCheckpoint("fixture-import", source, Optional.empty(), 0,
                Optional.empty(), updated, 1);
        assertThat(checkpoints.find("fixture-import", source)).contains(persistedFirst);
        assertThat(checkpoints.compareAndSet(retry, 0)).isFalse();
        assertThat(checkpoints.compareAndSet(retry, 1)).isTrue();
        IngestionCheckpoint persistedRetry = new IngestionCheckpoint("fixture-import", source, Optional.empty(), 1,
                Optional.of(updated.plusSeconds(60)), updated.plusSeconds(1), 2);
        assertThat(checkpoints.find("fixture-import", source)).contains(persistedRetry);
    }

    @Test
    void concurrentReplacementLeavesTheMostRecentObservationAsTheHead() throws Exception {
        store = new ElasticsearchSourceRecordStore(client);
        Instant firstObserved = Instant.parse("2026-09-15T00:00:00Z");
        ExecutorService workers = Executors.newFixedThreadPool(4);
        try {
            List<java.util.concurrent.Callable<SourceRecordTransitionOutcome>> writes = java.util.stream.IntStream.range(0, 8)
                    .mapToObj(index -> (java.util.concurrent.Callable<SourceRecordTransitionOutcome>) () -> {
                        Instant observed = firstObserved.plusSeconds(index);
                        SourceRecordHead candidate = headAt("one", String.format("%02x", index), observed, observed,
                                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                                List.of(assertion("brand", 0, "Acme " + index)), List.of(link("0123456789012")));
                        return store.apply(SourceRecordMutation.full(candidate)).outcome();
                    }).toList();

            List<java.util.concurrent.Future<SourceRecordTransitionOutcome>> outcomes = workers.invokeAll(writes);

            for (java.util.concurrent.Future<SourceRecordTransitionOutcome> outcome : outcomes) {
                assertThat(outcome.get()).isIn(SourceRecordTransitionOutcome.ACCEPTED,
                        SourceRecordTransitionOutcome.OUT_OF_ORDER);
            }
            assertThat(store.find(SourceRecordKey.of("fixture", "one")).orElseThrow().payloadHash().hexadecimalValue())
                    .isEqualTo("07");
        } finally {
            workers.shutdown();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static SourceRecordHead head(String record, String hash, SourceRecordCompleteness completeness,
            SourceRecordState state, List<SourceAssertion> assertions, List<GtinLink> links) {
        Instant observed = hash.equals("aa") ? Instant.parse("2026-09-15T00:00:00Z")
                : Instant.parse("2026-09-15T00:01:00Z");
        return headAt(record, hash, observed, observed, completeness, state, assertions, links);
    }

    private static SourceRecordHead headAt(String record, String hash, Instant observed, Instant retrieved,
            SourceRecordCompleteness completeness, SourceRecordState state, List<SourceAssertion> assertions,
            List<GtinLink> links) {
        return headAt(record, hash, observed, retrieved, completeness, state, assertions, links, null);
    }

    private static SourceRecordHead headAt(String record, String hash, Instant observed, Instant retrieved,
            SourceRecordCompleteness completeness, SourceRecordState state, List<SourceAssertion> assertions,
            List<GtinLink> links, Instant expiresAt) {
        SourceRecordKey key = SourceRecordKey.of("fixture", record);
        return new SourceRecordHead(key, "1", null, observed, retrieved, expiresAt, completeness, state,
                new PayloadHash("SHA-256", hash), URI.create("urn:test:" + record), new SourceUsagePolicyRef("fixture", "1"),
                links, assertions.stream().map(assertion -> SourceAssertion.of(key, assertion.field(), assertion.ordinal(),
                        assertion.contentType(), assertion.evidence())).toList());
    }

    private static SourceAssertion assertion(String field, int ordinal, String value) {
        SourceRecordKey placeholder = SourceRecordKey.of("fixture", "placeholder");
        SourceFieldId sourceField = new SourceFieldId("fixture", field, "1");
        return SourceAssertion.of(placeholder, sourceField, ordinal, SourceContentType.IDENTITY, ScalarEvidence.of(value));
    }

    private static GtinLink link(String gtin) {
        return new GtinLink(new Gtin(gtin), GtinMatchConfidence.EXACT, GtinMatchMethod.DECLARED_IDENTIFIER,
                URI.create("urn:test:gtin"));
    }

    private static String headId(SourceRecordKey key) {
        try {
            return "source-head:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(key.externalForm().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private PendingContext installPendingTransition() throws Exception {
        Instant observed = Instant.parse("2026-09-15T00:00:00Z");
        SourceRecordHead initial = headAt("one", "aa", observed, observed, SourceRecordCompleteness.FULL,
                SourceRecordState.ACTIVE, List.of(assertion("brand", 0, "Acme")),
                List.of(link("0123456789012"), link("4006381333931")));
        store.apply(SourceRecordMutation.full(initial));
        SourceRecordHead candidate = headAt("one", "bb", observed.plusSeconds(1), observed.plusSeconds(1),
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, List.of(assertion("brand", 0, "Acme 2")),
                List.of(link("0123456789012"), link("4006381333931")));
        SourceRecordTransition transition = new SourceRecordTransition(
                SourceRecordTransition.idFor(candidate.key(), 2), candidate.key(), 2, candidate.schemaVersion(),
                candidate.providerVersion(), initial.payloadHash(), candidate.payloadHash(), candidate.observedAt(),
                candidate.retrievedAt(), candidate.state(), SourceRecordTransitionOutcome.ACCEPTED, null,
                List.of(new Gtin("0123456789012"), new Gtin("4006381333931")));
        String id = headId(candidate.key());
        Map<String, Object> crashed = new LinkedHashMap<>(client.get(get -> get
                .index(ElasticsearchSourceRecordStore.HEAD_READ_ALIAS).id(id), Map.class).source());
        crashed.put("observedAt", candidate.observedAt().toString());
        crashed.put("retrievedAt", candidate.retrievedAt().toString());
        crashed.put("payloadHash", candidate.payloadHash().algorithm() + ":" + candidate.payloadHash().hexadecimalValue());
        crashed.put("headJson", DataReferenceJson.mapper().writeValueAsString(candidate));
        crashed.put("revision", transition.revision());
        crashed.put("hasPending", true);
        crashed.put("pendingJson", DataReferenceJson.mapper().writeValueAsString(transition));
        client.index(index -> index.index(ElasticsearchSourceRecordStore.HEAD_WRITE_ALIAS).id(id).document(crashed)
                .refresh(Refresh.WaitFor));
        return new PendingContext(candidate, transition);
    }

    private RunResult ingest(List<SourceRecordHead> input) {
        ElasticsearchSourceRecordStore runStore = new ElasticsearchSourceRecordStore(client);
        List<String> transitions = input.stream().map(head -> runStore.apply(SourceRecordMutation.full(head)).id()).toList();
        List<String> replay = runStore.scanAll(ScanRequest.first(10)).elements().stream()
                .map(head -> head.key().externalForm() + ":" + head.payloadHash().hexadecimalValue()).toList();
        return new RunResult(transitions, replay);
    }

    private record RunResult(List<String> transitionIds, List<String> replay) {
    }

    private record PendingContext(SourceRecordHead candidate, SourceRecordTransition transition) {
    }
}
