package org.open4goods.api.services.metriks.affiliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.body;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.FakeHttp;

/**
 * Tests {@link TradeTrackerReportClient} against anonymised SOAP responses.
 */
class TradeTrackerReportClientTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 24);
    private static final LocalDate END = LocalDate.of(2026, 10, 21);
    private static final String SECRET = "tt-passphrase-value";

    private static String envelope(String content) {
        return "<?xml version=\"1.0\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<SOAP-ENV:Body>" + content + "</SOAP-ENV:Body></SOAP-ENV:Envelope>";
    }

    private static final String AUTH = envelope("<ns1:authenticateResponse xmlns:ns1=\"x\"/>");
    private static final String SITES = envelope("<ns1:getAffiliateSitesResponse xmlns:ns1=\"x\"><affiliateSites>"
            + "<item><ID>11</ID><name>site a</name></item><item><ID>12</ID><name>site b</name></item>"
            + "</affiliateSites></ns1:getAffiliateSitesResponse>");

    private static String conversion(String id, String status, String date, String commission, String currency) {
        return "<item><ID>" + id + "</ID><transactionStatus>" + status + "</transactionStatus><registrationDate>" + date
                + "</registrationDate><commission>" + commission + "</commission><orderAmount>50.00</orderAmount><currency>"
                + currency + "</currency></item>";
    }

    private static String conversions(String... items) {
        return envelope("<ns1:getConversionTransactionsResponse xmlns:ns1=\"x\"><conversionTransactions>"
                + String.join("", items) + "</conversionTransactions></ns1:getConversionTransactionsResponse>");
    }

    /** Answers by SOAP operation, as read from the SOAPAction header. */
    private static FakeHttp http(String sitesBody, java.util.function.Function<String, String> conversionsBySite) {
        return new FakeHttp(request -> {
            String action = request.headers().firstValue("SOAPAction").orElse("");
            if (action.endsWith("/authenticate")) {
                return response(200, AUTH);
            }
            if (action.endsWith("/getAffiliateSites")) {
                return response(200, sitesBody);
            }
            String body = body(request);
            return response(200, conversionsBySite.apply(body.contains("<affiliateSiteID>11<") ? "11" : "12"));
        });
    }

    @Test
    void fetchTransactions_authenticatesThenReadsEverySite() throws Exception {
        FakeHttp http = http(SITES, site -> "11".equals(site)
                ? conversions(conversion("1", "accepted", "2026-10-21T10:00:00+02:00", "6.00", "EUR"),
                        conversion("2", "pending", "2026-10-16T09:00:00+02:00", "1.40", "EUR"),
                        conversion("3", "rejected", "2026-10-19T09:00:00+02:00", "5.00", "EUR"))
                : conversions(conversion("4", "Accepted", "2026-10-20T09:00:00+02:00", "2,5", "EUR")));
        TradeTrackerReportClient client = new TradeTrackerReportClient("12345", SECRET, http.client);

        List<AffiliationReportTransaction> transactions = client.fetchTransactions(START, END, AffiliationMetriksService.ZONE);

        assertThat(transactions).containsExactly(
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 21), AffiliationReportStatus.VALIDATED, new BigDecimal("6.00"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 16), AffiliationReportStatus.PENDING, new BigDecimal("1.40"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 19), AffiliationReportStatus.DECLINED, new BigDecimal("5.00"), "EUR"),
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 20), AffiliationReportStatus.VALIDATED, new BigDecimal("2.5"), "EUR"));
        List<String> actions = http.requests.stream().map(r -> r.headers().firstValue("SOAPAction").orElse(""))
                .map(a -> a.substring(a.lastIndexOf('/') + 1)).toList();
        assertThat(actions).containsExactly("authenticate", "getAffiliateSites", "getConversionTransactions",
                "getConversionTransactions");
        assertThat(http.requests.get(0).uri().toString()).isEqualTo("https://ws.tradetracker.com/soap/affiliate");
        assertThat(body(http.requests.get(0))).contains("<customerID>12345</customerID>", "<passphrase>" + SECRET + "</passphrase>");
        assertThat(body(http.requests.get(2))).contains("<registrationDateFrom>2026-09-24<", "<registrationDateTo>2026-10-22<");
    }

    @Test
    void fetchTransactions_keepsTheCurrencyAndIgnoresDatesOutsideTheWindow() throws Exception {
        FakeHttp http = http(SITES.replace("<item><ID>12</ID><name>site b</name></item>", ""),
                site -> conversions(conversion("1", "accepted", "2026-10-16T10:00:00+02:00", "2.5", "USD"),
                        conversion("2", "accepted", "2026-10-22T10:00:00+02:00", "9", "EUR")));
        TradeTrackerReportClient client = new TradeTrackerReportClient("1", SECRET, http.client);

        assertThat(client.fetchTransactions(START, END, AffiliationMetriksService.ZONE)).containsExactly(
                new AffiliationReportTransaction(LocalDate.of(2026, 10, 16), AffiliationReportStatus.VALIDATED, new BigDecimal("2.5"), "USD"));
    }

    @Test
    void fetchTransactions_zeroConversionsIsValid() throws Exception {
        FakeHttp http = http(SITES, site -> conversions());
        assertThat(new TradeTrackerReportClient("1", SECRET, http.client)
                .fetchTransactions(START, END, AffiliationMetriksService.ZONE)).isEmpty();
    }

    @Test
    void fetchTransactions_failsOnAnUnknownStatus() {
        FakeHttp http = http(SITES, site -> conversions(conversion("1", "Bloque", "2026-10-16T10:00:00+02:00", "2", "EUR")));
        assertThatThrownBy(() -> new TradeTrackerReportClient("1", SECRET, http.client)
                .fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .isInstanceOf(AffiliationReportException.class).hasMessage("TradeTracker : statut inconnu : Bloque");
    }

    @Test
    void fetchTransactions_failsWhenTheCommissionIsMissing() {
        FakeHttp http = http(SITES, site -> conversions(
                "<item><ID>1</ID><transactionStatus>accepted</transactionStatus><registrationDate>2026-10-16T10:00:00+02:00</registrationDate></item>"));
        assertThatThrownBy(() -> new TradeTrackerReportClient("1", SECRET, http.client)
                .fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("TradeTracker : commission incomplète (montant ou devise absent)");
    }

    @Test
    void fetchTransactions_failsOnASoapFaultWithoutQuotingIt() {
        FakeHttp http = new FakeHttp(request -> response(200, envelope(
                "<SOAP-ENV:Fault><faultcode>1</faultcode><faultstring>bad " + SECRET + "</faultstring></SOAP-ENV:Fault>")));
        assertThatThrownBy(() -> new TradeTrackerReportClient("1", SECRET, http.client)
                .fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("TradeTracker : faute SOAP (authenticate)");
    }

    @Test
    void fetchTransactions_failsOnHttpErrorsAndUnreadableXml() {
        assertThatThrownBy(() -> new TradeTrackerReportClient("1", SECRET,
                new FakeHttp(request -> response(500, "denied " + SECRET)).client)
                .fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("TradeTracker : HTTP 500 (authenticate)");
        assertThatThrownBy(() -> new TradeTrackerReportClient("1", SECRET,
                new FakeHttp(request -> response(200, "<html>maintenance")).client)
                .fetchTransactions(START, END, AffiliationMetriksService.ZONE))
                .hasMessage("TradeTracker : réponse XML illisible (authenticate)");
    }

    @Test
    void fetchTransactions_readsFurtherPagesAndStopsWhenNothingNewComes() throws Exception {
        String[] full = new String[TradeTrackerReportClient.PAGE_SIZE];
        for (int i = 0; i < full.length; i++) {
            full[i] = conversion("p" + i, "accepted", "2026-10-16T10:00:00+02:00", "1", "EUR");
        }
        // an API ignoring offset answers the same page again: the duplicates must stop the loop
        FakeHttp http = http(SITES.replace("<item><ID>12</ID><name>site b</name></item>", ""), site -> conversions(full));
        TradeTrackerReportClient client = new TradeTrackerReportClient("1", SECRET, http.client);

        assertThat(client.fetchTransactions(START, END, AffiliationMetriksService.ZONE)).hasSize(TradeTrackerReportClient.PAGE_SIZE);
        assertThat(http.requests).hasSize(2 + 2);
    }

    @Test
    void missingCredentials_namesBothProperties() {
        assertThat(new TradeTrackerReportClient("", "", null).missingCredentials()).containsExactly(
                "metriks.affiliation.tradetracker.customer-id", "metriks.affiliation.tradetracker.passphrase");
        assertThat(new TradeTrackerReportClient("1", SECRET, null).missingCredentials()).isEmpty();
    }
}
