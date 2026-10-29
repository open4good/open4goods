package org.open4goods.api.services.metriks.affiliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.fixture;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.response;

import java.math.BigDecimal;
import java.net.http.HttpRequest;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.FakeHttp;

/**
 * Tests {@link KwankoReportClient} against an anonymised Kwanko conversions response.
 */
class KwankoReportClientTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 24);
    private static final LocalDate END = LocalDate.of(2026, 10, 21);
    private static final String ZERO = "{\"conversions\":[]}";

    @Test
    void fetchTransactions_requestsTheWindowWithABearerToken() throws Exception {
        FakeHttp http = new FakeHttp(request -> response(200, ZERO));
        KwankoReportClient client = new KwankoReportClient("kwanko-token-value", http.client);

        assertThat(client.fetchTransactions(START, END, AffiliationMetriksService.ZONE)).isEmpty();

        HttpRequest request = http.requests.get(0);
        assertThat(request.uri().getHost()).isEqualTo("api.kwanko.com");
        assertThat(request.uri().getPath()).isEqualTo("/publishers/conversions");
        assertThat(request.uri().getQuery()).contains("completion_date_from=2026-09-23T22:00:00+00:00",
                "completion_date_to=2026-10-21T21:59:59+00:00");
        assertThat(request.uri().toString()).doesNotContain("kwanko-token-value");
        assertThat(request.headers().firstValue("Authorization")).contains("Bearer kwanko-token-value");
    }

    @Test
    void fetchTransactions_mapsStatusesDatesAndCommissions() throws Exception {
        FakeHttp http = new FakeHttp(request -> response(200, fixture("kwanko-conversions.json")));
        KwankoReportClient client = new KwankoReportClient("kwanko-token-value", http.client);

        List<AffiliationReportTransaction> transactions = client.fetchTransactions(START, END, AffiliationMetriksService.ZONE);

        assertThat(transactions).containsExactly(
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 21), AffiliationReportStatus.VALIDATED, new BigDecimal("6.00"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 16), AffiliationReportStatus.PENDING, new BigDecimal("1.4"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 19), AffiliationReportStatus.DECLINED, new BigDecimal("5.0"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 22), AffiliationReportStatus.VALIDATED, new BigDecimal("2.5"), "EUR"));
    }

    @Test
    void fetchTransactions_readsEveryPageWhenThePaginationIsAnnounced() throws Exception {
        FakeHttp http = new FakeHttp(request -> response(200, request.uri().getQuery().contains("&page=2")
                ? "{\"pagination\":{\"total_pages\":2},\"conversions\":[{\"state\":\"pending\","
                        + "\"completed_at\":\"2026-10-17T09:00:00+00:00\",\"earnings\":{\"value\":2},\"countervalue_currency\":\"EUR\"}]}"
                : "{\"pagination\":{\"total_pages\":2},\"conversions\":[{\"state\":\"approved\","
                        + "\"completed_at\":\"2026-10-16T09:00:00+00:00\",\"earnings\":{\"value\":1.5},\"countervalue_currency\":\"EUR\"}]}"));
        KwankoReportClient client = new KwankoReportClient("kwanko-token-value", http.client);

        assertThat(client.fetchTransactions(START, END, AffiliationMetriksService.ZONE)).hasSize(2);
        assertThat(http.requests).hasSize(2);
    }

    @Test
    void parseTransactions_keepsTheCurrency() throws Exception {
        KwankoReportClient client = new KwankoReportClient("k", null);
        List<AffiliationReportTransaction> transactions = client.parseTransactions(new tools.jackson.databind.ObjectMapper().readTree(
                "[{\"state\":\"Approved\",\"completed_at\":\"2026-10-16T10:00:00+00:00\",\"earnings\":{\"value\":\"2,5\"},"
                        + "\"campaign\":{\"currency\":\"USD\"}}]"), AffiliationMetriksService.ZONE);

        assertThat(transactions).containsExactly(
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 16), AffiliationReportStatus.VALIDATED, new BigDecimal("2.5"), "USD"));
    }

    @Test
    void fetchTransactions_failsOnAnUnknownState() {
        FakeHttp http = new FakeHttp(request -> response(200,
                "{\"conversions\":[{\"state\":\"Bloque\",\"completed_at\":\"2026-10-16T09:00:00+00:00\"}]}"));
        KwankoReportClient client = new KwankoReportClient("kwanko-token-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .isInstanceOf(AffiliationReportException.class)
                .hasMessage("Kwanko : statut inconnu : Bloque");
    }

    @Test
    void fetchTransactions_failsWhenTheCommissionIsMissing() {
        FakeHttp http = new FakeHttp(request -> response(200,
                "{\"conversions\":[{\"state\":\"approved\",\"completed_at\":\"2026-10-16T09:00:00+00:00\"}]}"));
        KwankoReportClient client = new KwankoReportClient("kwanko-token-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("Kwanko : commission incomplète (montant ou devise absent)");
    }

    @Test
    void fetchTransactions_failsOnHttpErrorsWithoutTheBody() {
        for (int status : new int[] {401, 403, 429, 500}) {
            FakeHttp http = new FakeHttp(request -> response(status, "denied kwanko-token-value"));
            KwankoReportClient client = new KwankoReportClient("kwanko-token-value", http.client);

            assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                    .hasMessage("Kwanko : HTTP " + status + " (conversions)");
        }
    }

    @Test
    void fetchTransactions_failsWhenTheResponseHasNoConversionList() {
        FakeHttp http = new FakeHttp(request -> response(200, "{\"error\":\"Bad token\"}"));
        KwankoReportClient client = new KwankoReportClient("kwanko-token-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("Kwanko : réponse inattendue (tableau de conversions attendu)");
    }

    @Test
    void fetchTransactions_failsOnUnreadableJson() {
        FakeHttp http = new FakeHttp(request -> response(200, "<html>maintenance</html>"));
        KwankoReportClient client = new KwankoReportClient("kwanko-token-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("Kwanko : réponse JSON illisible (conversions)");
    }

    @Test
    void missingCredentials_namesTheTokenProperty() {
        assertThat(new KwankoReportClient("", null).missingCredentials())
                .containsExactly("metriks.affiliation.kwanko.api-token");
    }
}
