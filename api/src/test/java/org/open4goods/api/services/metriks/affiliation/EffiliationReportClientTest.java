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
 * Tests {@link EffiliationReportClient} against a recorded, anonymised Effiliation {@code transaction.json} response.
 */
class EffiliationReportClientTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 24);
    private static final LocalDate END = LocalDate.of(2026, 10, 21);

    @Test
    void fetchTransactions_requestsTheWindowByTransactionDate() throws Exception {
        FakeHttp http = new FakeHttp(request -> response(200, "{\"transactions\":[]}"));
        EffiliationReportClient client = new EffiliationReportClient("effi-key-value", http.client);

        assertThat(client.fetchTransactions(START, END, AffiliationMetriksService.ZONE)).isEmpty();

        HttpRequest request = http.requests.get(0);
        assertThat(request.uri().getHost()).isEqualTo("apiv2.effiliation.com");
        assertThat(request.uri().getPath()).isEqualTo("/apiv2/transaction.json");
        assertThat(request.uri().getQuery()).contains("key=effi-key-value", "start=24/09/2026", "end=21/10/2026",
                "type=datetran", "lg=fr");
    }

    @Test
    void fetchTransactions_mapsStatusesDatesAndCommissions() throws Exception {
        FakeHttp http = new FakeHttp(request -> response(200, fixture("effiliation-transactions.json")));
        EffiliationReportClient client = new EffiliationReportClient("effi-key-value", http.client);

        List<AffiliationReportTransaction> transactions = client.fetchTransactions(START, END, AffiliationMetriksService.ZONE);

        assertThat(transactions).containsExactly(
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 21), AffiliationReportStatus.VALIDATED, new BigDecimal("6.00"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 16), AffiliationReportStatus.PENDING, new BigDecimal("1.40"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 9, 30), AffiliationReportStatus.PENDING, new BigDecimal("2.60"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 2), AffiliationReportStatus.VALIDATED, new BigDecimal("8.15"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 19), AffiliationReportStatus.DECLINED, new BigDecimal("5.00"), "EUR"));
    }

    @Test
    void parseTransactions_acceptsEnglishLabelsAndFrenchDates() throws Exception {
        EffiliationReportClient client = new EffiliationReportClient("k", null);
        List<AffiliationReportTransaction> transactions = client.parseTransactions(new tools.jackson.databind.ObjectMapper().readTree(
                "{\"transactions\":[{\"etat\":\"Pending\",\"datetran\":\"15/10/2026 10:00:00\",\"commission\":\"1\"},"
                        + "{\"etat\":\"Valid\",\"datetran\":\"2026-10-16\",\"commission\":\"2\",\"devise\":\"USD\"}]}"));

        assertThat(transactions).containsExactly(
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 15), AffiliationReportStatus.PENDING, new BigDecimal("1"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 16), AffiliationReportStatus.VALIDATED, new BigDecimal("2"), "USD"));
    }

    @Test
    void fetchTransactions_failsOnAnUnknownStatus() {
        FakeHttp http = new FakeHttp(request -> response(200,
                "{\"transactions\":[{\"etat\":\"Bloque\",\"datetran\":\"2026-10-16 09:00:00\",\"commission\":\"1\"}]}"));
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
                .hasMessage("Effiliation : réponse inattendue (liste de transactions absente)");
    }

    @Test
    void fetchTransactions_failsOnUnreadableJson() {
        FakeHttp http = new FakeHttp(request -> response(200, "<html>maintenance</html>"));
        EffiliationReportClient client = new EffiliationReportClient("effi-key-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("Effiliation : réponse JSON illisible (transaction.json)");
    }

    @Test
    void missingCredentials_namesTheApiKeyProperty() {
        assertThat(new EffiliationReportClient("", null).missingCredentials())
                .containsExactly("metriks.affiliation.effiliation.api-key");
    }
}
