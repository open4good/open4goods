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
 * Tests {@link AwinReportClient} against a recorded, anonymised Awin transactions response.
 */
class AwinReportClientTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 24);
    private static final LocalDate END = LocalDate.of(2026, 10, 21);

    @Test
    void fetchTransactions_requestsTheUtcBoundsOfTheParisWindowWithABearerToken() throws Exception {
        FakeHttp http = new FakeHttp(request -> response(200, "[]"));
        AwinReportClient client = new AwinReportClient("100000", "awin-token-value", http.client);

        client.fetchTransactions(START, END, AffiliationMetriksService.ZONE);

        HttpRequest request = http.requests.get(0);
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.uri().getHost()).isEqualTo("api.awin.com");
        assertThat(request.uri().getPath()).isEqualTo("/publishers/100000/transactions/");
        // 2026-09-24 00:00 and 2026-10-21 23:59:59 in Paris (CEST, UTC+2).
        assertThat(request.uri().getQuery()).contains("startDate=2026-09-23T22:00:00", "endDate=2026-10-21T21:59:59",
                "timezone=UTC", "dateType=transaction");
        assertThat(request.uri().toString()).doesNotContain("awin-token-value");
        assertThat(request.headers().firstValue("Authorization")).contains("Bearer awin-token-value");
    }

    @Test
    void fetchTransactions_mapsStatusesDatesAndCommissions() throws Exception {
        FakeHttp http = new FakeHttp(request -> response(200, fixture("awin-transactions.json")));
        AwinReportClient client = new AwinReportClient("100000", "awin-token-value", http.client);

        List<AffiliationReportTransaction> transactions = client.fetchTransactions(START, END, AffiliationMetriksService.ZONE);

        assertThat(transactions).hasSize(7);
        assertThat(transactions.get(0)).isEqualTo(new AffiliationReportTransaction(LocalDate.of(2026, 10, 20),
                AffiliationReportStatus.VALIDATED, new BigDecimal("4.5"), "EUR"));
        // 2026-10-14 22:30 UTC is 2026-10-15 00:30 in Paris.
        assertThat(transactions.get(1).transactionDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(transactions.get(1).status()).isEqualTo(AffiliationReportStatus.PENDING);
        assertThat(transactions.get(4).status()).isEqualTo(AffiliationReportStatus.DECLINED);
        assertThat(transactions.get(5).status()).isEqualTo(AffiliationReportStatus.DECLINED);
        // 2026-09-23 21:59:59 UTC is still 2026-09-23 in Paris.
        assertThat(transactions.get(6).transactionDate()).isEqualTo(LocalDate.of(2026, 9, 23));
    }

    @Test
    void fetchTransactions_failsOnAnUnknownStatus() {
        FakeHttp http = new FakeHttp(request -> response(200,
                "[{\"commissionStatus\":\"onhold\",\"transactionDate\":\"2026-10-20T10:00:00\","
                        + "\"commissionAmount\":{\"amount\":1,\"currency\":\"EUR\"}}]"));
        AwinReportClient client = new AwinReportClient("100000", "awin-token-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .isInstanceOf(AffiliationReportException.class)
                .hasMessage("Awin : statut inconnu : onhold");
    }

    @Test
    void fetchTransactions_failsOnAnHttpErrorWithTheStatusOnly() {
        FakeHttp http = new FakeHttp(request -> response(401, "{\"error\":\"invalid token awin-token-value\"}"));
        AwinReportClient client = new AwinReportClient("100000", "awin-token-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .isInstanceOf(AffiliationReportException.class)
                .hasMessage("Awin : HTTP 401 (transactions)");
    }

    @Test
    void fetchTransactions_rejectsANonNumericPublisherIdWithoutCallingTheApi() {
        FakeHttp http = new FakeHttp(request -> response(200, "[]"));
        AwinReportClient client = new AwinReportClient("10/../x", "awin-token-value", http.client);

        assertThatThrownBy(() -> client.fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("Awin : metriks.affiliation.awin.publisher-id doit être numérique");
        assertThat(http.requests).isEmpty();
    }

    @Test
    void missingCredentials_listsTheBlankProperties() {
        assertThat(new AwinReportClient("", " ", null).missingCredentials()).containsExactly(
                "metriks.affiliation.awin.publisher-id", "metriks.affiliation.awin.api-token");
        assertThat(new AwinReportClient("100000", "", null).missingCredentials())
                .containsExactly("metriks.affiliation.awin.api-token");
        assertThat(new AwinReportClient("100000", "t", null).missingCredentials()).isEmpty();
    }
}
