package org.open4goods.services.contribution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.service.AffiliationClickMappingService.Outcome;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * Integration coverage of {@link AffiliationClickMappingService} against a real Elasticsearch: an index created
 * before GOU-1828 gets {@code contentPath} as a {@code keyword}, its documents untouched; the update is idempotent,
 * creates a missing index, and fails on a {@code contentPath} already mapped dynamically.
 */
@Testcontainers
class AffiliationClickMappingServiceIT {

    /** Mapping of {@code affiliation-clicks} as created before GOU-1828 (no {@code contentPath}). */
    static final String MAPPING_BEFORE_CONTENT_PATH = """
            {"properties": {
              "_class": {"type": "keyword", "index": false, "doc_values": false},
              "id": {"type": "keyword"},
              "ts": {"type": "date", "format": "epoch_millis"},
              "network": {"type": "keyword"},
              "merchant": {"type": "keyword"},
              "gtin": {"type": "keyword"},
              "originPath": {"type": "keyword"},
              "targetHost": {"type": "keyword"}
            }}""";

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    static ElasticsearchOperations operations;

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
        operations.indexOps(IndexCoordinates.of(AffiliationClick.INDEX_NAME)).delete();
    }

    private static IndexOperations rawIndex() {
        return operations.indexOps(IndexCoordinates.of(AffiliationClick.INDEX_NAME));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> properties() {
        return (Map<String, Map<String, Object>>) rawIndex().getMapping().get("properties");
    }

    @Test
    void update_addsContentPathAsKeywordToAnIndexCreatedWithoutIt() {
        rawIndex().create();
        rawIndex().putMapping(Document.parse(MAPPING_BEFORE_CONTENT_PATH));
        AffiliationClick old = AffiliationClick.of("darty.com", "https://www.awin1.com/cread.php", "0123456789012",
                "/fours/foo", 1_760_000_000_000L);
        operations.save(old);
        rawIndex().refresh();
        assertThat(properties()).doesNotContainKey("contentPath");

        Outcome outcome = new AffiliationClickMappingService(operations).update();

        assertThat(outcome).isEqualTo(Outcome.UPDATED);
        Map<String, Map<String, Object>> properties = properties();
        assertThat(properties.get("contentPath")).containsEntry("type", "keyword").doesNotContainKey("fields");
        assertThat(properties.get("originPath")).containsEntry("type", "keyword");
        assertThat(properties.get("ts")).containsEntry("type", "date");
        // The existing document is not rewritten.
        AffiliationClick stored = operations.get(old.getId(), AffiliationClick.class);
        assertThat(stored.getOriginPath()).isEqualTo("/fours/foo");
        assertThat(stored.getContentPath()).isNull();

        // A new click is stored with its contentPath, the mapping stays a keyword.
        operations.save(AffiliationClick.of("darty.com", "https://www.awin1.com/cread.php", null, "/fours/foo",
                "/blog/choisir-un-four", 1_760_000_000_001L));
        rawIndex().refresh();
        assertThat(properties().get("contentPath")).containsEntry("type", "keyword").doesNotContainKey("fields");
    }

    @Test
    void update_isIdempotent() {
        AffiliationClickMappingService service = new AffiliationClickMappingService(operations);
        operations.indexOps(AffiliationClick.class).createWithMapping();
        Map<String, Object> before = rawIndex().getMapping();

        assertThat(service.update()).isEqualTo(Outcome.UPDATED);
        assertThat(service.update()).isEqualTo(Outcome.UPDATED);

        assertThat(rawIndex().getMapping()).isEqualTo(before);
    }

    @Test
    void update_createsAMissingIndexWithTheFullMapping() {
        assertThat(rawIndex().exists()).isFalse();

        assertThat(new AffiliationClickMappingService(operations).update()).isEqualTo(Outcome.CREATED);

        assertThat(properties().get("contentPath")).containsEntry("type", "keyword");
        assertThat(properties().get("merchant")).containsEntry("type", "keyword");
    }

    @Test
    void update_failsWhenContentPathWasAlreadyMappedDynamically() {
        rawIndex().create();
        rawIndex().putMapping(Document.parse(MAPPING_BEFORE_CONTENT_PATH));
        // A click written before the update: dynamic mapping turns contentPath into text + .keyword.
        operations.save(Document.parse("{\"id\": \"x\", \"contentPath\": \"/blog/x\"}"),
                IndexCoordinates.of(AffiliationClick.INDEX_NAME));
        rawIndex().refresh();
        assertThat(properties().get("contentPath")).containsEntry("type", "text");

        assertThatThrownBy(() -> new AffiliationClickMappingService(operations).update())
                .isInstanceOf(RuntimeException.class);
    }
}
