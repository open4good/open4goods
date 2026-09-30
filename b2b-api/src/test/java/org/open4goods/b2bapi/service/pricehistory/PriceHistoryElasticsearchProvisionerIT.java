package org.open4goods.b2bapi.service.pricehistory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch.indices.DataStream;
import co.elastic.clients.elasticsearch.indices.IndexMode;

/**
 * Integration coverage for GOU-144 against a real Elasticsearch: the three classpath resources
 * ({@link PriceHistoryElasticsearchProvisioner}) register templates and ILM policies that then let
 * a hand-indexed document create a genuine time-series data stream, and the guard-rail health
 * indicator reflects both the missing and the provisioned state.
 */
@Testcontainers
class PriceHistoryElasticsearchProvisionerIT {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    static ElasticsearchClient client;

    @BeforeAll
    static void connect() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress())
                .build();
        client = ElasticsearchClients.createImperative(Rest5Clients.getRest5Client(clientConfiguration));
    }

    @Test
    void healthIsDownBeforeProvisioningAndUpAfter() throws Exception {
        PriceHistoryProvisioningHealthIndicator healthIndicator = new PriceHistoryProvisioningHealthIndicator(client);

        Health before = healthIndicator.health();
        assertThat(before.getStatus()).isEqualTo(Status.DOWN);
        @SuppressWarnings("unchecked")
        var missingBefore = (Iterable<String>) before.getDetails().get("missing");
        assertThat(missingBefore).contains(
                "index-template:" + PriceHistoryElasticsearchProvisioner.PRICE_CHANGE_TEMPLATE,
                "index-template:" + PriceHistoryElasticsearchProvisioner.DAILY_ROLLUP_TEMPLATE,
                "ilm-policy:" + PriceHistoryElasticsearchProvisioner.PRICE_CHANGE_POLICY,
                "ilm-policy:" + PriceHistoryElasticsearchProvisioner.DAILY_ROLLUP_POLICY);

        new PriceHistoryElasticsearchProvisioner(client).provision();

        Health after = healthIndicator.health();
        assertThat(after.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void provisionedTemplatesAndPoliciesAreRegistered() throws Exception {
        new PriceHistoryElasticsearchProvisioner(client).provision();

        assertThat(client.indices()
                .existsIndexTemplate(b -> b.name(PriceHistoryElasticsearchProvisioner.PRICE_CHANGE_TEMPLATE))
                .value()).isTrue();
        assertThat(client.indices()
                .existsIndexTemplate(b -> b.name(PriceHistoryElasticsearchProvisioner.DAILY_ROLLUP_TEMPLATE))
                .value()).isTrue();
        assertThat(client.ilm().getLifecycle(b -> b.name(PriceHistoryElasticsearchProvisioner.PRICE_CHANGE_POLICY))
                .lifecycles()).isNotEmpty();
        assertThat(client.ilm().getLifecycle(b -> b.name(PriceHistoryElasticsearchProvisioner.DAILY_ROLLUP_POLICY))
                .lifecycles()).isNotEmpty();
    }

    @Test
    void handIndexingADocumentCreatesATimeSeriesDataStreamWithDateNanosTimestamp() throws Exception {
        new PriceHistoryElasticsearchProvisioner(client).provision();
        String dataStreamName = "o4g-price-change-it";

        client.index(idx -> idx.index(dataStreamName).opType(OpType.Create).document(priceChangeDocument()));
        client.indices().refresh(r -> r.index(dataStreamName));

        var dataStreams = client.indices().getDataStream(r -> r.name(dataStreamName)).dataStreams();
        assertThat(dataStreams).hasSize(1);
        DataStream dataStream = dataStreams.get(0);
        assertThat(dataStream.indexMode()).isEqualTo(IndexMode.TimeSeries);

        var mapping = client.indices().getMapping(r -> r.index(dataStreamName)).mappings();
        boolean timestampIsDateNanos = mapping.values().stream()
                .anyMatch(record -> record.mappings().properties().get("@timestamp").isDateNanos());
        assertThat(timestampIsDateNanos).isTrue();
    }

    private static Map<String, Object> priceChangeDocument() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("@timestamp", Instant.now().toString());
        document.put("gtin", "0885909950805");
        document.put("provider_id", "it-provider");
        document.put("provider_offer_id", "it-offer-1");
        document.put("condition", "NEW");
        document.put("currency", "EUR");
        document.put("amount", 42.0);
        document.put("availability", "IN_STOCK");
        document.put("event_kind", "FIRST_SEEN");
        document.put("event_id", "it-event-1");
        document.put("observed_at", Instant.now().toString());
        return document;
    }
}
