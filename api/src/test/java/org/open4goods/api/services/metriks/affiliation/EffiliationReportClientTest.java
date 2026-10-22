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
 * Tests {@link EffiliationReportClient} against a anonymised Effinity API v3 conversions response.
 */
class EffiliationReportClientTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 24);
    private static final LocalDate END = LocalDate.of(2026, 10, 21);

    @Test
    void fetchTransactions_requestsTheWindowByTransactionDate() throws Exception {
        FakeHttp http = new FakeHttp(request -> response(200, "{\"pagination\":{\"lastPage\":1},\"conversions\":[]}"));
        EffiliationReportClient client = new EffiliationReportClient("effi-key-value", http.client);

        assertThat(client.fetchTransactions(START, END, AffiliationMetriksService.ZONE)).isEmpty();

        HttpRequest request = http.requests.get(0);
        assertThat(request.uri().getHost()).isEqualTo("api.effinity.fr");
        assertThat(request.uri().getPath()).isEqualTo("/apiv3/effi-key-value/publisher/conversions");
        assertThat(request.uri().getQuery()).contains("start=2026-09-24", "end=2026-10-21",
                "dateType=conversionDate", "conversionType=all", "status=all", "page=1", "perPage=1000");
        assertThat(request.uri().getQuery()).doesNotContain("effi-key-value");
    }

    @Test
    void fetchTransactions_readsEveryPage() throws Exception {
        FakeHttp http = new FakeHttp(request -> response(200, request.uri().getQuery().contains("page=1")
                ? "{\"pagination\":{\"lastPage\":2},\"conversions\":[{\"status\":\"validated\","
                        + "\"conversionDate\":\"2026-10-16T09:00:00\",\"commissionAmount\":1.5}]}"
                : "{\"pagination\":{\"lastPage\":2},\"conversions\":[{\"status\":\"pending\","
                        + "\"conversionDate\":\"2026-10-17T09:00:00\",\"commissionAmount\":2}]}"));
        EffiliationReportClient client = new EffiliationReportClient("effi-key-value", http.client);

        assertThat(client.fetchTransactions(START, END, AffiliationMetriksService.ZONE)).hasSize(2);
        assertThat(http.requests).hasSize(2);
    }

    @Test
    void fetchTransactions_mapsStatusesDatesAndCommissions() throws Exception {
        FakeHttp http = new FakeHttp(request -> response(200, fixture("effiliation-transactions.json")));
        EffiliationReportClient client = new EffiliationReportClient("effi-key-value", http.client);

        List<AffiliationReportTransaction> transactions = client.fetchTransactions(START, END, AffiliationMetriksService.ZONE);

        assertThat(transactions).containsExactly(
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 21), AffiliationReportStatus.VALIDATED, new BigDecimal("6.0"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 16), AffiliationReportStatus.PENDING, new BigDecimal("1.4"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 9, 30), AffiliationReportStatus.PENDING, new BigDecimal("2.6"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 2), AffiliationReportStatus.VALIDATED, new BigDecimal("8.15"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 19), AffiliationReportStatus.DECLINED, new BigDecimal("5.0"), "EUR"));
    }

    @Test
    void parseTransactions_keepsTheCurrencyAndAcceptsPlainDates() throws Exception {
        EffiliationReportClient client = new EffiliationReportClient("k", null);
        List<AffiliationReportTransaction> transactions = client.parseTransactions(new tools.jackson.databind.ObjectMapper().readTree(
                "{\"conversions\":[{\"status\":\"Pending\",\"conversionDate\":\"2026-10-15\",\"commissionAmount\":\"1\"},"
                        + "{\"status\":\"validated\",\"conversionDate\":\"2026-10-16T10:00:00\",\"commissionAmount\":2,"
                        + "\"currency\":\"USD\"}]}"));

        assertThat(transactions).containsExactly(
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 15), AffiliationReportStatus.PENDING, new BigDecimal("1"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 16), AffiliationReportStatus.VALIDATED, new BigDecimal("2"), "USD"));
    }

    @Test
    void fetchTransactions_failsOnAnUnknownStatus() {
        FakeHttp http = new FakeHttp(request -> response(200,
                "{\"conversions\":[{\"status\":\"Bloque\",\"conversionDate\":\"2026-10-16T09:00:00\",\"commissionAmount\":1}]}"));
        EffiliationReportClient client = new EffiliationReportClient("effi-key-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .isInstanceOf(AffiliationReportException.class)
                .hasMessage("Effiliation : statut inconnu : Bloque");
    }

    @Test
    void fetchTransactions_failsWhenTheResponseHasNoTransactionList() {
        FakeHttp http = new FakeHttp(request -> response(200, "{\"error\":\"Bad key\"}"));
        EffiliationReportClient client = new EffiliationReportClient("effi-key-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("Effiliation : réponse inattendue (liste de conversions absente)");
    }

    @Test
    void fetchTransactions_failsOnUnreadableJson() {
        FakeHttp http = new FakeHttp(request -> response(200, "<html>maintenance</html>"));
        EffiliationReportClient client = new EffiliationReportClient("effi-key-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("Effiliation : réponse JSON illisible (conversions)");
    }

    @Test
    void missingCredentials_namesTheApiKeyProperty() {
        assertThat(new EffiliationReportClient("", null).missingCredentials())
                .containsExactly("metriks.affiliation.effiliation.api-key");
    }
}
