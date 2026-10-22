package org.open4goods.api.services.metriks.affiliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.body;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.fixture;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.response;

import java.math.BigDecimal;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.StringJoiner;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.FakeHttp;

/**
 * Tests {@link RakutenReportClient} against a recorded, anonymised Rakuten Events API response.
 */
class RakutenReportClientTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 24);
    private static final LocalDate END = LocalDate.of(2026, 10, 21);
    private static final String TOKEN_RESPONSE =
            "{\"token_type\":\"bearer\",\"expires_in\":3600,\"refresh_token\":\"r\",\"access_token\":\"rk-access-value\"}";

    private static FakeHttp http(String eventsBody) {
        return new FakeHttp(request -> request.uri().getPath().equals("/token")
                ? response(200, TOKEN_RESPONSE)
                : response(200, eventsBody));
    }

    @Test
    void fetchTransactions_obtainsATokenThenReadsTheEventsOfTheWindow() throws Exception {
        FakeHttp http = http("[]");
        RakutenReportClient client = new RakutenReportClient("rk-client-id", "rk-client-secret", "1234567", http.client);

        client.fetchTransactions(START, END, AffiliationMetriksService.ZONE);

        HttpRequest token = http.requests.get(0);
        assertThat(token.method()).isEqualTo("POST");
        assertThat(token.uri().toString()).isEqualTo("https://api.linksynergy.com/token");
        assertThat(token.headers().firstValue("Authorization")).contains("Bearer " + Base64.getEncoder()
                .encodeToString("rk-client-id:rk-client-secret".getBytes(StandardCharsets.UTF_8)));
        assertThat(body(token)).isEqualTo("scope=1234567");

        HttpRequest events = http.requests.get(1);
        assertThat(events.uri().getPath()).isEqualTo("/events/1.0/transactions");
        // 2026-09-24 00:00 and 2026-10-21 23:59:59 in Paris (CEST, UTC+2), sent in GMT.
        assertThat(events.uri().getQuery()).contains("transaction_date_start=2026-09-23 22:00:00",
                "transaction_date_end=2026-10-21 21:59:59", "limit=100", "page=1");
        assertThat(events.headers().firstValue("Authorization")).contains("Bearer rk-access-value");
        assertThat(events.uri().toString()).doesNotContain("rk-client-secret", "rk-access-value");
    }

    @Test
    void fetchTransactions_groupsRecordsByOrder() throws Exception {
        RakutenReportClient client = new RakutenReportClient("id", "secret", "1234567", http(fixture("rakuten-events.json")).client);

        List<AffiliationReportTransaction> transactions = client.fetchTransactions(START, END, AffiliationMetriksService.ZONE);

        assertThat(transactions).containsExactly(
                // ORDER-A: the real-time event is superseded by the batch transaction.
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 19), AffiliationReportStatus.VALIDATED, new BigDecimal("1.0"), "EUR"),
                // ORDER-B: real-time event only.
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 17), AffiliationReportStatus.PENDING, new BigDecimal("0.8"), "EUR"),
                // ORDER-C: sale then cancellation.
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 5), AffiliationReportStatus.DECLINED, new BigDecimal("0.0"), "EUR"),
                // ORDER-D: two items of one order, one sale.
                new AffiliationReportTransaction(LocalDate.of(2026, 9, 28), AffiliationReportStatus.VALIDATED, new BigDecimal("3.3"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 9, 25), AffiliationReportStatus.PENDING, new BigDecimal("0.5"), "EUR"));
    }

    @Test
    void fetchTransactions_readsEveryPage() throws Exception {
        StringJoiner fullPage = new StringJoiner(",", "[", "]");
        for (int i = 0; i < RakutenReportClient.PAGE_SIZE; i++) {
            fullPage.add("{\"order_id\":\"O" + i + "\",\"commissions\":1,\"currency\":\"EUR\",\"is_event\":\"N\","
                    + "\"transaction_date\":\"2026-10-01 10:00:00\"}");
        }
        String lastPage = "[{\"order_id\":\"LAST\",\"commissions\":1,\"currency\":\"EUR\",\"is_event\":\"Y\","
                + "\"transaction_date\":\"2026-10-02T10:00:00Z\"}]";
        FakeHttp http = new FakeHttp(request -> {
            if (request.uri().getPath().equals("/token")) {
                return response(200, TOKEN_RESPONSE);
            }
            return response(200, request.uri().getQuery().contains("page=1") ? fullPage.toString() : lastPage);
        });
        RakutenReportClient client = new RakutenReportClient("id", "secret", "1234567", http.client);

        List<AffiliationReportTransaction> transactions = client.fetchTransactions(START, END, AffiliationMetriksService.ZONE);

        assertThat(transactions).hasSize(RakutenReportClient.PAGE_SIZE + 1);
        assertThat(http.requests).hasSize(3);
        assertThat(http.requests.get(2).uri().getQuery()).contains("page=2");
    }

    @Test
    void fetchTransactions_reportsATokenFailure() {
        FakeHttp http = new FakeHttp(request -> response(401, "{\"error\":\"invalid_client\"}"));
        RakutenReportClient client = new RakutenReportClient("id", "secret", "1234567", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .isInstanceOf(AffiliationReportException.class)
                .hasMessage("Rakuten : HTTP 401 (jeton OAuth)");
        assertThat(http.requests).hasSize(1);
    }

    @Test
    void fetchTransactions_failsOnAnUnknownIsEventValue() {
        RakutenReportClient client = new RakutenReportClient("id", "secret", "1234567", http(
                "[{\"order_id\":\"O\",\"commissions\":1,\"currency\":\"EUR\",\"is_event\":\"maybe\","
                        + "\"transaction_date\":\"2026-10-01 10:00:00\"}]").client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("Rakuten : is_event inconnu : maybe");
    }

    @Test
    void missingCredentials_listsTheBlankProperties() {
        assertThat(new RakutenReportClient("", "", "", null).missingCredentials()).containsExactly(
                "metriks.affiliation.rakuten.client-id", "metriks.affiliation.rakuten.client-secret",
                "metriks.affiliation.rakuten.sid");
        assertThat(new RakutenReportClient("id", "secret", "", null).missingCredentials())
                .containsExactly("metriks.affiliation.rakuten.sid");
    }
}
