package org.open4goods.api.repository.pricehistory;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.pricehistory.model.CompletedOfferFeed;
import org.open4goods.pricehistory.model.OfferAvailability;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.OfferHead;
import org.open4goods.pricehistory.model.OfferKey;
import org.open4goods.pricehistory.model.OfferObservation;
import org.open4goods.pricehistory.model.PriceChangeKind;
import org.open4goods.pricehistory.service.PriceObservationService;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/** Integration coverage for the versioned offer-head index's aliases and CAS mutations (GOU-201). */
@Testcontainers
class ElasticsearchOfferHeadStoreTest {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .waitingFor(Wait.forHttp("/").forPort(9200).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));

    static ElasticsearchClient client;
    private ElasticsearchOfferHeadStore store;

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
    void createsStrictVersionedAliasesAndRoundTripsAHead() throws Exception {
        store = new ElasticsearchOfferHeadStore(client);
        OfferObservation observation = observation(Instant.parse("2026-09-15T00:00:00Z"), "aa", "10.00");
        OfferHead head = OfferHead.first(observation, null);

        assertThat(store.compareAndSet(head, 0)).isTrue();
        assertThat(store.find(key())).contains(head);
        assertThat(client.indices().exists(exists -> exists.index(ElasticsearchOfferHeadStore.READ_ALIAS)).value()).isTrue();
    }

    @Test
    void aStaleRevisionIsRejectedByCompareAndSet() {
        store = new ElasticsearchOfferHeadStore(client);
        OfferObservation first = observation(Instant.parse("2026-09-15T00:00:00Z"), "aa", "10.00");
        OfferHead head = OfferHead.first(first, null);
        assertThat(store.compareAndSet(head, 0)).isTrue();

        OfferObservation second = observation(Instant.parse("2026-09-15T00:01:00Z"), "bb", "12.00");
        OfferHead replacement = head.replace(second, null);

        assertThat(store.compareAndSet(replacement, 0)).isFalse();
        assertThat(store.compareAndSet(replacement, 1)).isTrue();
        assertThat(store.find(key())).contains(replacement);
    }

    @Test
    void creatingTwiceAtRevisionZeroFailsTheSecondWriter() {
        store = new ElasticsearchOfferHeadStore(client);
        OfferHead head = OfferHead.first(observation(Instant.parse("2026-09-15T00:00:00Z"), "aa", "10.00"), null);

        assertThat(store.compareAndSet(head, 0)).isTrue();
        assertThat(store.compareAndSet(head, 0)).isFalse();
    }

    @Test
    void findByProviderStreamsOnlyThatProvidersHeads() {
        store = new ElasticsearchOfferHeadStore(client);
        SourceId provider = new SourceId("fixture");
        OfferKey keyA = new OfferKey(new Gtin("0123456789012"), provider, "offer-a");
        OfferKey keyB = new OfferKey(new Gtin("0123456789012"), provider, "offer-b");
        OfferKey keyOther = new OfferKey(new Gtin("0123456789012"), new SourceId("other"), "offer-c");
        store.compareAndSet(OfferHead.first(observationFor(keyA, "10.00"), null), 0);
        store.compareAndSet(OfferHead.first(observationFor(keyB, "11.00"), null), 0);
        store.compareAndSet(OfferHead.first(observationFor(keyOther, "12.00"), null), 0);

        try (var heads = store.findByProvider(provider)) {
            assertThat(heads.map(h -> h.observation().key())).containsExactlyInAnyOrder(keyA, keyB);
        }
    }

    @Test
    void offerHeadStoreBackedServiceIngestsAndReconciles() {
        store = new ElasticsearchOfferHeadStore(client);
        ElasticsearchPriceChangeEventStore events = new ElasticsearchPriceChangeEventStore(client);
        PriceObservationService service = new PriceObservationService(store, events);
        // Unlike the other fixtures in this file, a price-change event is written into a real
        // time-series data stream, whose backing index only accepts timestamps inside its current
        // writable window (a few hours around "now" by default); a fixed historical instant is
        // rejected with a timestamp_error.
        Instant start = Instant.now();

        assertThat(service.ingest(observation(start, "aa", "10.00"))).isEqualTo(PriceChangeKind.FIRST_SEEN);
        assertThat(service.reconcileCompletedFeed(
                new CompletedOfferFeed(new SourceId("fixture"), start.plusSeconds(60), Set.of()))).isEqualTo(1);
        assertThat(store.find(key()).orElseThrow().observation().availability()).isEqualTo(OfferAvailability.UNAVAILABLE);
    }

    private static OfferObservation observation(Instant observedAt, String hash, String amount) {
        return observationFor(key(), hash, amount, observedAt);
    }

    private static OfferObservation observationFor(OfferKey key, String amount) {
        return observationFor(key, "aa", amount, Instant.parse("2026-09-15T00:00:00Z"));
    }

    private static OfferObservation observationFor(OfferKey key, String hash, String amount, Instant observedAt) {
        return new OfferObservation(key, OfferCondition.NEW, Currency.getInstance("EUR"), new BigDecimal(amount),
                OfferAvailability.AVAILABLE, observedAt, observedAt, new PayloadHash("SHA-256", hash),
                new SourceUsagePolicyRef("fixture", "1"));
    }

    private static OfferKey key() {
        return new OfferKey(new Gtin("0123456789012"), new SourceId("fixture"), "offer-1");
    }
}
