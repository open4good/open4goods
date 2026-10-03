package org.open4goods.api.repository.pricehistory;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.pricehistory.model.OfferAvailability;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.OfferKey;
import org.open4goods.pricehistory.model.OfferObservation;
import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.model.PriceChangeKind;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.search.Hit;

/**
 * Integration coverage for the price-change-event data stream: idempotent append, deterministic
 * document shape and reuse of the real production index template (GOU-201).
 */
@Testcontainers
class ElasticsearchPriceChangeEventStoreTest {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .waitingFor(Wait.forHttp("/").forPort(9200).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));

    static ElasticsearchClient client;
    private ElasticsearchPriceChangeEventStore store;

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
        try {
            client.indices().deleteDataStream(delete -> delete.name("o4g-*"));
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException exception) {
            if (exception.status() != 404) {
                throw exception;
            }
        }
    }

    @AfterAll
    static void closeClient() throws Exception {
        client._transport().close();
    }

    @Test
    void appendsAnEventIntoTheRealProductionDataStreamAndTemplate() throws Exception {
        store = new ElasticsearchPriceChangeEventStore(client);
        PriceChangeEvent event = PriceChangeEvent.of(observation(), PriceChangeKind.FIRST_SEEN);

        assertThat(store.append(event)).isTrue();
        client.indices().refresh(refresh -> refresh.index(ElasticsearchPriceChangeEventStore.DATA_STREAM));

        var response = client.search(search -> search.index(ElasticsearchPriceChangeEventStore.DATA_STREAM), Map.class);
        Hit<Map> hit = response.hits().hits().getFirst();
        Map<String, Object> source = hit.source();
        assertThat(source).containsEntry("event_id", event.id()).containsEntry("gtin", "0123456789012")
                .containsEntry("provider_id", "fixture").containsEntry("provider_offer_id", "offer-1")
                .containsEntry("condition", "NEW").containsEntry("currency", "EUR").containsEntry("amount", 10.0)
                .containsEntry("availability", "AVAILABLE").containsEntry("event_kind", "FIRST_SEEN")
                .containsEntry("policy_ref", "fixture:1").containsEntry("content_hash", "SHA-256:aa");
        assertThat(client.indices().getDataStream(get -> get.name(ElasticsearchPriceChangeEventStore.DATA_STREAM))
                .dataStreams()).hasSize(1);
    }

    @Test
    void appendingTheSameLogicalEventTwiceIsIdempotent() {
        store = new ElasticsearchPriceChangeEventStore(client);
        PriceChangeEvent event = PriceChangeEvent.of(observation(), PriceChangeKind.FIRST_SEEN);

        assertThat(store.append(event)).isTrue();
        assertThat(store.append(event)).isFalse();
    }

    private static OfferObservation observation() {
        OfferKey key = new OfferKey(new Gtin("0123456789012"), new SourceId("fixture"), "offer-1");
        // The price-change data stream's backing index only accepts timestamps inside its current
        // writable window (a few hours around "now" by default), so this fixture cannot use a
        // fixed historical instant.
        Instant observedAt = Instant.now();
        return new OfferObservation(key, OfferCondition.NEW, Currency.getInstance("EUR"), new BigDecimal("10.00"),
                OfferAvailability.AVAILABLE, observedAt, observedAt, new PayloadHash("SHA-256", "aa"),
                new SourceUsagePolicyRef("fixture", "1"));
    }
}
