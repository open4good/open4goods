package org.open4goods.b2bapi.service.pricehistory;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.pricehistory.model.DailyProviderRollup;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.model.PriceHistoryGranularity;
import org.open4goods.pricehistory.model.PriceHistoryPage;
import org.open4goods.pricehistory.model.PriceHistoryQuery;
import org.open4goods.pricehistory.service.PriceHistoryCursor;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * Integration coverage for {@link EsPriceHistoryQueryAdapter} against a real Elasticsearch.
 *
 * <p>This is the test that would have caught the two GOU-28 review defects that unit tests
 * (mocking {@link org.open4goods.pricehistory.port.PriceHistoryQueryPort}) structurally cannot see:
 * a {@code search_after} arity mismatch on the very first DAY-granularity second page, and a sort
 * on a {@code doc_values: false} field ({@code event_id}) that Elasticsearch rejects on every
 * CHANGE-granularity query. Both reproduce here against the exact field mappings the production
 * index templates declare (queried directly, never through the Product index - GOU-28 AC8).
 */
@Testcontainers
class EsPriceHistoryQueryAdapterIT {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    private static final String GTIN = "1234567890123";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    static ElasticsearchOperations operations;
    static EsPriceHistoryQueryAdapter adapter;
    static String esBaseUrl;

    @BeforeAll
    static void connect() throws Exception {
        esBaseUrl = "http://" + ELASTICSEARCH.getHttpHostAddress();
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress())
                .build();
        ElasticsearchClient client = ElasticsearchClients.createImperative(Rest5Clients.getRest5Client(clientConfiguration));
        operations = new ElasticsearchTemplate(client);
        adapter = new EsPriceHistoryQueryAdapter(operations);

        put("/" + EsPriceHistoryQueryAdapter.PRICE_CHANGE_INDEX, """
                {"mappings":{"properties":{
                  "event_id":{"type":"keyword"},
                  "gtin":{"type":"keyword"},
                  "provider_id":{"type":"keyword"},
                  "provider_offer_id":{"type":"keyword"},
                  "condition":{"type":"keyword"},
                  "currency":{"type":"keyword"},
                  "amount":{"type":"double"},
                  "availability":{"type":"keyword"},
                  "event_kind":{"type":"keyword"},
                  "observed_at":{"type":"date"},
                  "policy_ref":{"type":"keyword","index":false,"doc_values":false},
                  "content_hash":{"type":"keyword","index":false,"doc_values":false},
                  "@timestamp":{"type":"date_nanos"}
                }}}
                """);
        put("/" + EsPriceHistoryQueryAdapter.DAILY_ROLLUP_INDEX, """
                {"mappings":{"properties":{
                  "@timestamp":{"type":"date_nanos"},
                  "gtin":{"type":"keyword"},
                  "provider_id":{"type":"keyword"},
                  "condition":{"type":"keyword"},
                  "currency":{"type":"keyword"},
                  "minimum_amount":{"type":"double"},
                  "maximum_amount":{"type":"double"},
                  "close_amount":{"type":"double"},
                  "observed_offer_count":{"type":"long"},
                  "change_count":{"type":"long"},
                  "first_observed_at":{"type":"date"},
                  "last_observed_at":{"type":"date"}
                }}}
                """);
    }

    @AfterEach
    void cleanUp() throws Exception {
        HTTP.send(HttpRequest.newBuilder()
                        .uri(URI.create(esBaseUrl + "/" + EsPriceHistoryQueryAdapter.PRICE_CHANGE_INDEX + "/_delete_by_query?refresh=true"))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString("{\"query\":{\"match_all\":{}}}"))
                        .build(), HttpResponse.BodyHandlers.discarding());
        HTTP.send(HttpRequest.newBuilder()
                        .uri(URI.create(esBaseUrl + "/" + EsPriceHistoryQueryAdapter.DAILY_ROLLUP_INDEX + "/_delete_by_query?refresh=true"))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString("{\"query\":{\"match_all\":{}}}"))
                        .build(), HttpResponse.BodyHandlers.discarding());
    }

    /**
     * GOU-28 review defect 1: before the fix, {@code queryDaily} sorted on four fields
     * ({@code @timestamp}, {@code provider_id}, {@code condition}, {@code currency}) but the cursor
     * carried a single joined stable-id value, so Elasticsearch rejected the second page's
     * {@code search_after} for arity mismatch. Three rollups share one timestamp so a
     * one-item-per-page walk is forced to resume from a tiebreak position on every page.
     */
    @Test
    void dailyPaginationWalksAllPagesAtASharedTimestamp() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        indexDailyRollup(timestamp, "merchant-a", "NEW", "EUR");
        indexDailyRollup(timestamp, "merchant-b", "NEW", "EUR");
        indexDailyRollup(timestamp, "merchant-c", "NEW", "EUR");
        refresh(EsPriceHistoryQueryAdapter.DAILY_ROLLUP_INDEX);

        PriceHistoryQuery firstPage = dailyQuery(1, Optional.empty());
        PriceHistoryPage<DailyProviderRollup> page1 = adapter.queryDaily(firstPage);
        assertThat(page1.values()).hasSize(1);
        assertThat(page1.values().get(0).key().providerId().value()).isEqualTo("merchant-a");
        assertThat(page1.nextCursor()).isPresent();

        PriceHistoryQuery secondPage = dailyQuery(1, page1.nextCursor());
        PriceHistoryPage<DailyProviderRollup> page2 = adapter.queryDaily(secondPage);
        assertThat(page2.values()).hasSize(1);
        assertThat(page2.values().get(0).key().providerId().value()).isEqualTo("merchant-b");
        assertThat(page2.nextCursor()).isPresent();

        PriceHistoryQuery thirdPage = dailyQuery(1, page2.nextCursor());
        PriceHistoryPage<DailyProviderRollup> page3 = adapter.queryDaily(thirdPage);
        assertThat(page3.values()).hasSize(1);
        assertThat(page3.values().get(0).key().providerId().value()).isEqualTo("merchant-c");
        assertThat(page3.nextCursor()).isEmpty();
    }

    /**
     * GOU-28 review defect 2: {@code event_id} is mapped {@code index:false, doc_values:false} in
     * the production template (it is stored-only, never queried/sorted/aggregated on), so sorting
     * on it - the pre-fix behavior - fails on every CHANGE-granularity call, not just pagination.
     * The fixed adapter sorts on the sortable dimension fields instead.
     */
    @Test
    void changePaginationWalksAllPagesAtASharedTimestampWithoutSortingOnTheStoredOnlyEventId() {
        Instant observedAt = Instant.parse("2026-01-01T00:00:00Z");
        indexChangeEvent(observedAt, "offer-1");
        indexChangeEvent(observedAt, "offer-2");
        indexChangeEvent(observedAt, "offer-3");
        refresh(EsPriceHistoryQueryAdapter.PRICE_CHANGE_INDEX);

        PriceHistoryQuery firstPage = changeQuery(1, Optional.empty());
        PriceHistoryPage<PriceChangeEvent> page1 = adapter.queryChanges(firstPage);
        assertThat(page1.values()).hasSize(1);
        assertThat(page1.values().get(0).key().providerOfferId()).isEqualTo("offer-1");
        assertThat(page1.nextCursor()).isPresent();

        PriceHistoryQuery secondPage = changeQuery(1, page1.nextCursor());
        PriceHistoryPage<PriceChangeEvent> page2 = adapter.queryChanges(secondPage);
        assertThat(page2.values()).hasSize(1);
        assertThat(page2.values().get(0).key().providerOfferId()).isEqualTo("offer-2");

        PriceHistoryQuery thirdPage = changeQuery(1, page2.nextCursor());
        PriceHistoryPage<PriceChangeEvent> page3 = adapter.queryChanges(thirdPage);
        assertThat(page3.values()).hasSize(1);
        assertThat(page3.values().get(0).key().providerOfferId()).isEqualTo("offer-3");
        assertThat(page3.nextCursor()).isEmpty();
    }

    @Test
    void dailyQueryFiltersByProviderConditionAndCurrency() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        indexDailyRollup(timestamp, "merchant-a", "NEW", "EUR");
        indexDailyRollup(timestamp, "merchant-a", "OCCASION", "EUR");
        indexDailyRollup(timestamp, "merchant-a", "NEW", "USD");
        indexDailyRollup(timestamp, "merchant-b", "NEW", "EUR");
        refresh(EsPriceHistoryQueryAdapter.DAILY_ROLLUP_INDEX);

        PriceHistoryQuery query = new PriceHistoryQuery(
                timestamp.minusSeconds(3600), timestamp.plusSeconds(3600), Optional.of(PriceHistoryGranularity.DAY),
                Optional.of(new Gtin(GTIN)), Optional.of(new SourceId("merchant-a")),
                Optional.of(OfferCondition.NEW), Optional.of(Currency.getInstance("EUR")),
                Optional.empty(), 50, false);

        PriceHistoryPage<DailyProviderRollup> page = adapter.queryDaily(query);

        assertThat(page.values()).hasSize(1);
        assertThat(page.values().get(0).key().providerId().value()).isEqualTo("merchant-a");
        assertThat(page.values().get(0).key().condition()).isEqualTo(OfferCondition.NEW);
        assertThat(page.values().get(0).key().currency()).isEqualTo(Currency.getInstance("EUR"));
    }

    /**
     * A cursor that does not decode to the arity the adapter's sort clause expects must fail
     * predictably (caller maps this to a 400 {@code cursor-mismatch}), never silently misbehave.
     */
    @Test
    void aCursorWithTheWrongArityIsRejectedAsMalformed() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        indexDailyRollup(timestamp, "merchant-a", "NEW", "EUR");
        refresh(EsPriceHistoryQueryAdapter.DAILY_ROLLUP_INDEX);

        // A single-key cursor (as CHANGE granularity would carry) replayed against DAY, which
        // expects three sort keys, must not silently resume - it must fail fast.
        String mismatchedCursor = PriceHistoryCursor.after(timestamp, "only-one-key");
        PriceHistoryQuery query = dailyQuery(50, Optional.of(mismatchedCursor));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.queryDaily(query))
                .isInstanceOf(RuntimeException.class);
    }

    private PriceHistoryQuery dailyQuery(int pageSize, Optional<String> cursor) {
        return new PriceHistoryQuery(
                Instant.parse("2025-12-31T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"),
                Optional.of(PriceHistoryGranularity.DAY), Optional.of(new Gtin(GTIN)), Optional.empty(),
                Optional.empty(), Optional.empty(), cursor, pageSize, false);
    }

    private PriceHistoryQuery changeQuery(int pageSize, Optional<String> cursor) {
        return new PriceHistoryQuery(
                Instant.parse("2025-12-31T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"),
                Optional.of(PriceHistoryGranularity.CHANGE), Optional.of(new Gtin(GTIN)), Optional.empty(),
                Optional.empty(), Optional.empty(), cursor, pageSize, false);
    }

    private void indexDailyRollup(Instant timestamp, String providerId, String condition, String currency) {
        // Date fields are indexed as epoch millis: Spring Data Elasticsearch's default Instant
        // converter for @Field(type = Date/Date_Nanos) (no explicit `format`, matching the
        // production document classes) only reliably round-trips the `epoch_millis` representation.
        String body = """
                {"@timestamp":%d,"gtin":"%s","provider_id":"%s","condition":"%s","currency":"%s",
                 "minimum_amount":10.0,"maximum_amount":20.0,"close_amount":15.0,
                 "observed_offer_count":3,"change_count":1,
                 "first_observed_at":%d,"last_observed_at":%d}
                """.formatted(timestamp.toEpochMilli(), GTIN, providerId, condition, currency,
                timestamp.toEpochMilli(), timestamp.toEpochMilli());
        post("/" + EsPriceHistoryQueryAdapter.DAILY_ROLLUP_INDEX + "/_doc", body);
    }

    private void indexChangeEvent(Instant observedAt, String providerOfferId) {
        String eventId = "price-event:" + sha256Hex(providerOfferId);
        String body = """
                {"event_id":"%s","gtin":"%s","provider_id":"merchant-a","provider_offer_id":"%s",
                 "condition":"NEW","currency":"EUR","amount":19.99,"availability":"AVAILABLE",
                 "event_kind":"FIRST_SEEN","observed_at":%d,
                 "policy_ref":"merchant-feed:1","content_hash":"SHA-256:%s","@timestamp":%d}
                """.formatted(eventId, GTIN, providerOfferId, observedAt.toEpochMilli(), "b".repeat(64),
                observedAt.toEpochMilli());
        // The document's own event_id must also be the ES document _id: EsPriceChangeEventDocument
        // maps event_id with @Id, so Spring Data Elasticsearch fills it from ES's _id metadata on
        // read - an auto-generated _id would silently overwrite the event_id carried in _source.
        post("/" + EsPriceHistoryQueryAdapter.PRICE_CHANGE_INDEX + "/_doc/"
                + java.net.URLEncoder.encode(eventId, java.nio.charset.StandardCharsets.UTF_8), body);
    }

    private static void put(String path, String body) throws Exception {
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder()
                        .uri(URI.create(esBaseUrl + path))
                        .header("Content-Type", "application/json")
                        .PUT(BodyPublishers.ofString(body))
                        .build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("failed to PUT " + path + ": " + response.body());
        }
    }

    private static void post(String path, String body) {
        try {
            HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder()
                            .uri(URI.create(esBaseUrl + path))
                            .header("Content-Type", "application/json")
                            .POST(BodyPublishers.ofString(body))
                            .build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("failed to POST " + path + ": " + response.body());
            }
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (final java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void refresh(String index) {
        try {
            HTTP.send(HttpRequest.newBuilder()
                            .uri(URI.create(esBaseUrl + "/" + index + "/_refresh"))
                            .POST(BodyPublishers.noBody())
                            .build(), HttpResponse.BodyHandlers.discarding());
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
