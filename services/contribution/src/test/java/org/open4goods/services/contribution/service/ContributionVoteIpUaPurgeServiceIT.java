package org.open4goods.services.contribution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.services.contribution.model.ContributionVote;
import org.open4goods.services.contribution.service.ContributionVoteIpUaPurgeService.Report;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.UpdateByQueryRequest;

/**
 * Integration coverage of {@link ContributionVoteIpUaPurgeService} against a real Elasticsearch and the mapping
 * generated from {@link ContributionVote} ({@code ip} and {@code ua} mapped {@code index: false}): {@code exists}
 * finds them through doc values, only these two keys leave {@code _source}, untouched documents keep their version,
 * nothing is deleted, a second run does nothing, and no log line quotes an IP or a User-Agent.
 */
@Testcontainers
class ContributionVoteIpUaPurgeServiceIT {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    private static final String INDEX = ContributionVote.INDEX_NAME;
    private static final List<String> PERSONAL_VALUES = List.of("203.0.113.7", "2001:db8::1", "198.51.100.23",
            "Mozilla/5.0 (X11; Linux x86_64)", "curl/8.4.0", "Googlebot/2.1");

    /** Documents holding an ip or a ua value: they must lose both keys and nothing else. */
    private static final Map<String, String> WITH_PERSONAL_DATA = new LinkedHashMap<>();
    /** Documents without ip/ua value (absent or null): they must not be touched at all. */
    private static final Map<String, String> WITHOUT_PERSONAL_DATA = new LinkedHashMap<>();

    static {
        WITH_PERSONAL_DATA.put("ip-and-ua", vote(1, "\"ip\":\"203.0.113.7\",\"ua\":\"Mozilla/5.0 (X11; Linux x86_64)\""));
        WITH_PERSONAL_DATA.put("ipv6-only", vote(2, "\"ip\":\"2001:db8::1\""));
        WITH_PERSONAL_DATA.put("ua-only", vote(3, "\"ua\":\"curl/8.4.0\""));
        WITH_PERSONAL_DATA.put("null-ip-with-ua", vote(4, "\"ip\":null,\"ua\":\"Googlebot/2.1\""));
        WITH_PERSONAL_DATA.put("ip-with-null-ua", vote(5, "\"ip\":\"198.51.100.23\",\"ua\":null"));
        WITH_PERSONAL_DATA.put("empty-ua", vote(6, "\"ua\":\"\""));
        WITHOUT_PERSONAL_DATA.put("no-ip-no-ua", vote(7, null));
        WITHOUT_PERSONAL_DATA.put("null-ip-null-ua", vote(8, "\"ip\":null,\"ua\":null"));
        WITHOUT_PERSONAL_DATA.put("null-ip", vote(9, "\"ip\":null"));
    }

    static ElasticsearchClient client;
    static ElasticsearchOperations operations;

    private ListAppender<ILoggingEvent> logs;

    @BeforeAll
    static void connect() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress())
                .build();
        client = ElasticsearchClients.createImperative(Rest5Clients.getRest5Client(clientConfiguration));
        operations = new ElasticsearchTemplate(client);
    }

    @BeforeEach
    void captureLogs() {
        logs = new ListAppender<>();
        logs.start();
        serviceLogger().addAppender(logs);
    }

    @AfterEach
    void cleanUp() {
        serviceLogger().detachAppender(logs);
        operations.indexOps(IndexCoordinates.of(INDEX)).delete();
    }

    private static Logger serviceLogger() {
        return (Logger) LoggerFactory.getLogger(ContributionVoteIpUaPurgeService.class);
    }

    /**
     * Raw vote document (as written by front-api before v1.0.70), so that {@code "ip": null} really is in
     * {@code _source}: Spring Data does not write null properties.
     */
    private static String vote(int n, String personalFields) {
        return "{\"ts\":" + (1_700_000_000_000L + n) + ",\"gtin\":\"000000000000" + n + "\",\"price\":" + n
                + ".99,\"vote\":\"vote-" + n + "\",\"datasourceName\":\"shop" + n + ".example\",\"url\":"
                + "\"https://shop" + n + ".example/p/" + n + "?ref=a&b=c\"" + (personalFields == null ? "" : "," + personalFields)
                + "}";
    }

    private static void seed() throws IOException {
        operations.indexOps(ContributionVote.class).createWithMapping();
        Map<String, String> all = new LinkedHashMap<>(WITH_PERSONAL_DATA);
        all.putAll(WITHOUT_PERSONAL_DATA);
        for (Map.Entry<String, String> doc : all.entrySet()) {
            client.index(i -> i.index(INDEX).id(doc.getKey()).withJson(new StringReader(doc.getValue())));
        }
        // A vote saved through the entity itself, as the historical write path did.
        ContributionVote entityVote = new ContributionVote("entity.example", "https://entity.example/p/1");
        entityVote.setId("entity-with-ip-and-ua");
        entityVote.setGtin("0000000000010");
        entityVote.setPrice(10.5);
        entityVote.setVote("vote-entity");
        entityVote.setIp("203.0.113.7");
        entityVote.setUa("Mozilla/5.0 (X11; Linux x86_64)");
        operations.save(entityVote);
        operations.indexOps(IndexCoordinates.of(INDEX)).refresh();
    }

    private static Map<String, Snapshot> snapshot() throws IOException {
        Map<String, Snapshot> snapshot = new LinkedHashMap<>();
        List<String> ids = new ArrayList<>(WITH_PERSONAL_DATA.keySet());
        ids.addAll(WITHOUT_PERSONAL_DATA.keySet());
        ids.add("entity-with-ip-and-ua");
        for (String id : ids) {
            @SuppressWarnings("rawtypes")
            GetResponse<Map> response = client.get(g -> g.index(INDEX).id(id), Map.class);
            assertThat(response.found()).as(id).isTrue();
            @SuppressWarnings("unchecked")
            Map<String, Object> source = response.source();
            snapshot.put(id, new Snapshot(source, response.version()));
        }
        return snapshot;
    }

    private record Snapshot(Map<String, Object> source, Long version) {
    }

    private static long count(co.elastic.clients.elasticsearch._types.query_dsl.Query query) {
        return operations.count(NativeQuery.builder().withQuery(query).build(), IndexCoordinates.of(INDEX));
    }

    private static long total() {
        return operations.count(NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).build(),
                IndexCoordinates.of(INDEX));
    }

    @Test
    void mapping_keepsIpAndUaUnindexedSoExistsReliesOnDocValues() throws IOException {
        seed();

        Map<String, Object> mapping = operations.indexOps(IndexCoordinates.of(INDEX)).getMapping();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) mapping.get("properties");
        assertThat(properties.get("ip")).containsEntry("type", "ip").containsEntry("index", false)
                .doesNotContainKey("doc_values");
        assertThat(properties.get("ua")).containsEntry("type", "keyword").containsEntry("index", false)
                .doesNotContainKey("doc_values");
        assertThat(ContributionVoteIpUaPurgeService.checkMapping(mapping))
                .isEqualTo("ip={type=ip, index=false, doc_values=true}, ua={type=keyword, index=false, doc_values=true}");

        // exists on these unindexed fields does find the documents (null values excluded).
        assertThat(count(ContributionVoteIpUaPurgeService.withIpOrUa())).isEqualTo(WITH_PERSONAL_DATA.size() + 1);
        assertThat(count(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.exists(e -> e.field("ip")))))
                .isEqualTo(4);
        assertThat(count(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.exists(e -> e.field("ua")))))
                .isEqualTo(5);
    }

    @Test
    void updateQuery_proceedsOnConflictsRefreshesAndRunsOnlyTheRemovalScript() {
        ContributionVoteIpUaPurgeService service = new ContributionVoteIpUaPurgeService(operations);

        UpdateByQueryRequest request = service.updateByQueryRequest();

        assertThat(request.index()).containsExactly(INDEX);
        assertThat(request.conflicts()).isEqualTo(Conflicts.Proceed);
        assertThat(request.refresh()).isTrue();
        assertThat(request.maxDocs()).isEqualTo(ContributionVoteIpUaPurgeService.BATCH_SIZE);
        assertThat(request.waitForCompletion()).isNull();
        assertThat(request.script().lang()).isEqualTo("painless");
        assertThat(request.script().source().scriptString())
                .isEqualTo("ctx._source.remove(\"ip\"); ctx._source.remove(\"ua\")");
        assertThat(request.query().bool().minimumShouldMatch()).isEqualTo("1");
        assertThat(request.query().bool().should()).extracting(q -> q.exists().field()).containsExactly("ip", "ua");
        assertThat(request.query().bool().must()).isEmpty();
        assertThat(request.query().bool().filter()).isEmpty();
    }

    @Test
    void purge_removesOnlyIpAndUaThenDoesNothingOnTheSecondRun() throws IOException {
        seed();
        Map<String, Snapshot> before = snapshot();
        long totalBefore = total();
        // Batch of 2: 7 documents to purge need 4 passes.
        ContributionVoteIpUaPurgeService service = new ContributionVoteIpUaPurgeService(operations, 2);

        Report first = service.purge();

        assertThat(first).isEqualTo(new Report(true, totalBefore, 7, totalBefore, 0, 7, 0, 0, 4));
        // refresh=true: visible without any explicit refresh.
        assertThat(total()).isEqualTo(totalBefore);
        assertThat(count(ContributionVoteIpUaPurgeService.withIpOrUa())).isZero();

        Map<String, Snapshot> after = snapshot();
        for (String id : before.keySet()) {
            Map<String, Object> expected = new HashMap<>(before.get(id).source());
            if (WITHOUT_PERSONAL_DATA.containsKey(id)) {
                // Not selected: same source (null ip/ua kept as is) and same version.
                assertThat(after.get(id).source()).as(id).isEqualTo(expected);
                assertThat(after.get(id).version()).as(id).isEqualTo(before.get(id).version());
                continue;
            }
            expected.remove("ip");
            expected.remove("ua");
            assertThat(after.get(id).source()).as(id).isEqualTo(expected).doesNotContainKeys("ip", "ua")
                    .containsKeys("ts", "gtin", "price", "vote", "datasourceName", "url");
        }

        logs.list.clear();
        Report second = service.purge();

        assertThat(second).isEqualTo(new Report(false, totalBefore, 0, totalBefore, 0, 0, 0, 0, 0));
        assertThat(snapshot()).isEqualTo(after);
        assertThat(logs.list).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
                .startsWith("contribution-votes purge ip/ua : rien à purger (documents : " + totalBefore);
    }

    @Test
    void purge_logsCountsOnlyWithAStablePrefix() throws IOException {
        seed();

        new ContributionVoteIpUaPurgeService(operations).purge();

        List<String> messages = logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).startsWith("contribution-votes purge ip/ua : avant purge, documents : 10,"
                + " avec ip ou ua : 7 ;");
        assertThat(messages.get(1)).isEqualTo("contribution-votes purge ip/ua : après purge, documents : 10,"
                + " avec ip ou ua : 0, mis à jour : 7, conflits de version : 0, échecs : 0, passes : 1");
        assertThat(logs.list).allSatisfy(event -> assertThat(event.getLevel().toString()).isEqualTo("WARN"));
        for (String message : messages) {
            for (String value : PERSONAL_VALUES) {
                assertThat(message).doesNotContain(value);
            }
        }
    }

    @Test
    void purge_doesNothingWhenTheIndexIsMissing() {
        operations.indexOps(IndexCoordinates.of(INDEX)).delete();

        Report report = new ContributionVoteIpUaPurgeService(operations).purge();

        assertThat(report.executed()).isFalse();
        assertThat(operations.indexOps(IndexCoordinates.of(INDEX)).exists()).isFalse();
        assertThat(logs.list).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
                .isEqualTo("contribution-votes purge ip/ua : rien à purger, index contribution-votes absent");
    }

    @Test
    void purge_refusesAMappingWhereExistsCannotSeeTheField() throws IOException {
        client.indices().create(c -> c.index(INDEX).mappings(m -> m
                .properties("ip", p -> p.ip(ip -> ip.index(false).docValues(false)))
                .properties("ua", p -> p.keyword(k -> k.index(false)))));

        assertThatThrownBy(() -> new ContributionVoteIpUaPurgeService(operations).purge())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ip");
    }
}
