package org.open4goods.services.contribution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.model.AffiliationClickStats;
import org.open4goods.services.contribution.model.AffiliationClickStats.MerchantClicks;
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
 * Integration coverage of {@link AffiliationClickStatsService} against a real Elasticsearch: the
 * {@link AffiliationClick} mapping (keyword fields, epoch-millis date), window bounds, per-network counts, top
 * merchants, and failure on a missing index (never zero).
 */
@Testcontainers
class AffiliationClickStatsServiceIT {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    static ElasticsearchOperations operations;

    private static final Instant END = Instant.parse("2026-10-21T22:00:00Z");
    private static final long LONG_START = END.minus(Duration.ofDays(28)).toEpochMilli();
    private static final long SHORT_START = END.minus(Duration.ofDays(7)).toEpochMilli();

    @BeforeAll
    static void connect() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress())
                .build();
        ElasticsearchClient client = ElasticsearchClients.createImperative(Rest5Clients.getRest5Client(clientConfiguration));
        operations = new ElasticsearchTemplate(client);
    }

    @AfterEach
    void cleanUp() {
        operations.indexOps(AffiliationClick.class).delete();
    }

    private static void createIndex() {
        IndexOperations indexOps = operations.indexOps(AffiliationClick.class);
        indexOps.createWithMapping();
    }

    private static AffiliationClick click(String merchant, String url, Instant ts) {
        return AffiliationClick.of(merchant, url, "0123456789012", "/fours/foo", ts.toEpochMilli());
    }

    @Test
    void mapping_indexesEveryFieldAsKeywordAndTsAsDate() {
        createIndex();

        Map<String, Object> mapping = operations.indexOps(AffiliationClick.class).getMapping();

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) mapping.get("properties");
        assertThat(properties.get("ts")).containsEntry("type", "date");
        for (String field : List.of("network", "merchant", "gtin", "originPath", "targetHost")) {
            assertThat(properties.get(field)).as(field).containsEntry("type", "keyword").doesNotContainKey("index");
        }
        assertThat(properties).doesNotContainKeys("ip", "ua", "url");
    }

    @Test
    void read_countsPerNetworkAndWindowAndRanksMerchants() {
        createIndex();
        List<AffiliationClick> clicks = new ArrayList<>();
        Instant inShort = END.minus(Duration.ofDays(2));
        Instant inLongOnly = END.minus(Duration.ofDays(20));
        // Short window: awin x3 (darty x2, fnac x1), effiliation x1, other x1.
        clicks.add(click("darty.com", "https://www.awin1.com/cread.php?a=1", inShort));
        clicks.add(click("darty.com", "https://www.awin1.com/cread.php?a=2", inShort));
        clicks.add(click("fnac.com", "https://www.awin1.com/cread.php?a=3", inShort));
        clicks.add(click("boulanger.com", "https://track.effiliation.com/servlet/effi.redir", inShort));
        clicks.add(click("shop.example", "https://shop.example/p/1", inShort));
        // Long window only: darty awin x1, effinity x2.
        clicks.add(click("darty.com", "https://www.awin1.com/cread.php?a=4", inLongOnly));
        clicks.add(click("boulanger.com", "https://track.effinity.fr/r", inLongOnly));
        clicks.add(click("boulanger.com", "https://track.effinity.fr/r", inLongOnly));
        // Bounds: exactly at the start of the long window (counted), at the end (excluded), before (excluded).
        clicks.add(click("edge.example", "https://edge.example/", Instant.ofEpochMilli(LONG_START)));
        clicks.add(click("future.example", "https://www.awin1.com/future", END));
        clicks.add(click("old.example", "https://www.awin1.com/old", Instant.ofEpochMilli(LONG_START - 1)));
        operations.save(clicks);
        operations.indexOps(AffiliationClick.class).refresh();

        AffiliationClickStats stats = new AffiliationClickStatsService(operations)
                .read(LONG_START, SHORT_START, END.toEpochMilli(), 20);

        assertThat(stats.clicksShort()).containsExactlyInAnyOrderEntriesOf(
                Map.of("awin", 3L, "effiliation", 1L, "kwanko", 0L, "tradetracker", 0L, "other", 1L));
        assertThat(stats.totalShort()).isEqualTo(5);
        assertThat(stats.clicksLong()).containsExactlyInAnyOrderEntriesOf(
                Map.of("awin", 4L, "effiliation", 3L, "kwanko", 0L, "tradetracker", 0L, "other", 2L));
        assertThat(stats.totalLong()).isEqualTo(9);
        // Most clicked first, ties ordered by key.
        assertThat(stats.topMerchantsLong()).containsExactly(
                new MerchantClicks("boulanger.com", "effiliation", 3),
                new MerchantClicks("darty.com", "awin", 3),
                new MerchantClicks("edge.example", "other", 1),
                new MerchantClicks("fnac.com", "awin", 1),
                new MerchantClicks("shop.example", "other", 1));
    }

    @Test
    void read_reportsZeroForANetworkWithoutClick() {
        createIndex();
        operations.save(click("shop.example", "https://shop.example/p/1", END.minus(Duration.ofDays(1))));
        operations.indexOps(AffiliationClick.class).refresh();

        AffiliationClickStats stats = new AffiliationClickStatsService(operations)
                .read(LONG_START, SHORT_START, END.toEpochMilli(), 1);

        assertThat(stats.clicksLong()).containsExactlyInAnyOrderEntriesOf(
                Map.of("awin", 0L, "effiliation", 0L, "kwanko", 0L, "tradetracker", 0L, "other", 1L));
        assertThat(stats.topMerchantsLong()).containsExactly(new MerchantClicks("shop.example", "other", 1));
    }

    @Test
    void read_failsWhenTheIndexDoesNotExist() {
        operations.indexOps(IndexCoordinates.of(AffiliationClick.INDEX_NAME)).delete();

        assertThatThrownBy(() -> new AffiliationClickStatsService(operations)
                .read(LONG_START, SHORT_START, END.toEpochMilli(), 20))
                .isInstanceOf(RuntimeException.class);
    }
}
