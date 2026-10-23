package org.open4goods.api.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.api.dto.metriks.MetriksEvent;
import org.open4goods.api.services.metriks.GoogleSearchConsoleService;
import org.open4goods.api.services.metriks.affiliation.AffiliationMetriksService;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MetriksController.class)
@ContextConfiguration(classes = MetriksController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MetriksControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private GoogleSearchConsoleService searchConsoleService;

    @MockitoBean
    private AffiliationMetriksService affiliationMetriksService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSystemMetrics() throws Exception {
        mockMvc.perform(get("/api/metriks/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("2.0"))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].id").value("system.disk.total"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetFunctionalMetrics() throws Exception {
        Set<AffiliationPartner> partners = new HashSet<>();
        AffiliationPartner p1 = new AffiliationPartner();
        p1.setId("p1");
        partners.add(p1);
        AffiliationPartner p2 = new AffiliationPartner();
        p2.setId("p2");
        partners.add(p2);

        when(productRepository.countMainIndex()).thenReturn(1234L);
        when(productRepository.countMainIndexValidAndRated()).thenReturn(99L);
        when(feedService.getFeedsUrl()).thenReturn(new HashSet<>());
        when(feedService.getPartners()).thenReturn(partners);

        mockMvc.perform(get("/api/metriks/functional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("2.0"))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].id").value("business.products.total"))
                .andExpect(jsonPath("$.events[0].value").value(1234))
                .andExpect(jsonPath("$.events[1].id").value("business.products.rated"))
                .andExpect(jsonPath("$.events[1].value").value(99));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSeoMetrics() throws Exception {
        when(searchConsoleService.fetchSeoMetrics()).thenThrow(new IllegalStateException("unused"));
        GoogleSearchConsoleService.PagesWithImpressionsResult result =
                new GoogleSearchConsoleService.PagesWithImpressionsResult(
                        5000L, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 8), false);
        when(searchConsoleService.countPagesWithImpressions()).thenReturn(result);

        mockMvc.perform(get("/api/metriks/seo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("2.0"))
                .andExpect(jsonPath("$.events[0].id").value("seo.gsc.pages_with_impressions"))
                .andExpect(jsonPath("$.events[0].value").value(5000))
                .andExpect(jsonPath("$.events[0].params.windowStartDate").value("2026-09-11"))
                .andExpect(jsonPath("$.events[0].params.windowEndDate").value("2026-10-08"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSeoMetricsOnErrorReturnsErrorEvent() throws Exception {
        when(searchConsoleService.fetchSeoMetrics()).thenThrow(new IllegalStateException("unused"));
        when(searchConsoleService.countPagesWithImpressions())
                .thenThrow(new java.io.IOException("Search Console API HTTP 500: boom"));

        mockMvc.perform(get("/api/metriks/seo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].id").value("seo.gsc.pages_with_impressions"))
                .andExpect(jsonPath("$.events[0].status").value("error"))
                .andExpect(jsonPath("$.events[0].errorMessage").value("Search Console API HTTP 500: boom"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetAffiliationMetrics() throws Exception {
        MetriksEvent ok = new MetriksEvent();
        ok.setId("affiliation.awin.commission.validated.7d");
        ok.setUnit("currency");
        ok.setValue(new java.math.BigDecimal("4.50"));
        ok.setParams(Map.of("provider", "open4goods-api", "network", "awin", "currency", "EUR"));
        MetriksEvent error = new MetriksEvent();
        error.setId("affiliation.effiliation.sales.7d");
        error.setUnit("count");
        error.setStatus("error");
        error.setErrorMessage("Credential manquant : metriks.affiliation.effiliation.api-key");
        when(affiliationMetriksService.collectEvents()).thenReturn(List.of(ok, error));

        mockMvc.perform(get("/api/metriks/affiliation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("2.0"))
                .andExpect(jsonPath("$.events[0].id").value("affiliation.awin.commission.validated.7d"))
                .andExpect(jsonPath("$.events[0].value").value(4.50))
                .andExpect(jsonPath("$.events[0].params.currency").value("EUR"))
                .andExpect(jsonPath("$.events[1].status").value("error"))
                .andExpect(jsonPath("$.events[1].value").doesNotExist())
                .andExpect(jsonPath("$.events[1].errorMessage").value("Credential manquant : metriks.affiliation.effiliation.api-key"));
    }

    private static GoogleSearchConsoleService.Outcome<GoogleSearchConsoleService.Totals> totals(
            long clicks, long impressions, double ctr, double position) {
        return new GoogleSearchConsoleService.Outcome<>(
                new GoogleSearchConsoleService.Totals(clicks, impressions, ctr, position), null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSeoMetricsReturnsTotalsAndTops() throws Exception {
        var current = new GoogleSearchConsoleService.Window(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 8));
        var previous = new GoogleSearchConsoleService.Window(LocalDate.of(2026, 8, 14), LocalDate.of(2026, 9, 10));
        var rows = new GoogleSearchConsoleService.TopRows(
                java.util.List.of(
                        new GoogleSearchConsoleService.Row("alpha", 50, 500, 0.1, 3.456),
                        new GoogleSearchConsoleService.Row("beta", 10, 400, 0.025, 8.0)),
                java.util.Map.of("alpha", new GoogleSearchConsoleService.Row("alpha", 40, 450, 0.09, 4.0)),
                java.util.List.of(), false);
        when(searchConsoleService.fetchSeoMetrics()).thenReturn(new GoogleSearchConsoleService.SeoMetrics(
                current, previous, totals(120, 3000, 0.04567, 12.345), totals(100, 2500, 0.04, 14.0),
                new GoogleSearchConsoleService.Outcome<>(rows, null),
                new GoogleSearchConsoleService.Outcome<>(rows, null),
                new GoogleSearchConsoleService.Outcome<>(java.util.List.of(), null)));

        mockMvc.perform(get("/api/metriks/seo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[1].id").value("seo.gsc.clicks"))
                .andExpect(jsonPath("$.events[1].value").value(120))
                .andExpect(jsonPath("$.events[1].unit").value("count"))
                .andExpect(jsonPath("$.events[1].params.previousValue").value(100))
                .andExpect(jsonPath("$.events[1].params.previousWindowEndDate").value("2026-09-10"))
                .andExpect(jsonPath("$.events[2].id").value("seo.gsc.impressions"))
                .andExpect(jsonPath("$.events[3].id").value("seo.gsc.ctr"))
                .andExpect(jsonPath("$.events[3].unit").value("percent"))
                .andExpect(jsonPath("$.events[3].value").value(4.57))
                .andExpect(jsonPath("$.events[4].id").value("seo.gsc.position"))
                .andExpect(jsonPath("$.events[4].value").value(12.35))
                .andExpect(jsonPath("$.events[5].id").value("seo.gsc.top_queries"))
                .andExpect(jsonPath("$.events[5].value").value(2))
                .andExpect(jsonPath("$.events[5].params.rows[0].rank").value(1))
                .andExpect(jsonPath("$.events[5].params.rows[0].ctr").value(10.0))
                .andExpect(jsonPath("$.events[5].params.rows[0].previousClicks").value(40))
                .andExpect(jsonPath("$.events[5].params.rows[1].rank").value(2))
                .andExpect(jsonPath("$.events[5].params.rows[1].previousClicks").doesNotExist())
                .andExpect(jsonPath("$.events[6].id").value("seo.gsc.top_pages"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSeoMetricsWithMissingCredentialReturnsErrorsWith200() throws Exception {
        var current = new GoogleSearchConsoleService.Window(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 8));
        var previous = new GoogleSearchConsoleService.Window(LocalDate.of(2026, 8, 14), LocalDate.of(2026, 9, 10));
        String message = "Search Console credentials unavailable: Missing Google credentials";
        var failed = new GoogleSearchConsoleService.Outcome<GoogleSearchConsoleService.Totals>(null, message);
        var failedTop = new GoogleSearchConsoleService.Outcome<GoogleSearchConsoleService.TopRows>(null, message);
        var failedDaily = new GoogleSearchConsoleService.Outcome<java.util.List<GoogleSearchConsoleService.DailyRow>>(null, message);
        when(searchConsoleService.fetchSeoMetrics()).thenReturn(
                new GoogleSearchConsoleService.SeoMetrics(current, previous, failed, failed, failedTop, failedTop, failedDaily));
        when(searchConsoleService.countPagesWithImpressions()).thenThrow(new java.io.IOException(message));

        mockMvc.perform(get("/api/metriks/seo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(10))
                .andExpect(jsonPath("$.events[?(@.status == 'error')].id").value(org.hamcrest.Matchers.hasSize(10)))
                .andExpect(jsonPath("$.events[1].value").doesNotExist())
                .andExpect(jsonPath("$.events[1].errorMessage").value(message));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSeoMetricsWhenServiceThrowsReturnsErrorsWith200() throws Exception {
        when(searchConsoleService.countPagesWithImpressions()).thenThrow(new java.io.IOException("HTTP 401"));
        when(searchConsoleService.fetchSeoMetrics()).thenThrow(new IllegalStateException("HTTP 401"));

        mockMvc.perform(get("/api/metriks/seo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(10))
                .andExpect(jsonPath("$.events[?(@.status == 'error')].id").value(org.hamcrest.Matchers.hasSize(10)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSeoMetricsExposesDailyLosingAndPreviousPagesCount() throws Exception {
        var current = new GoogleSearchConsoleService.Window(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 8));
        var previous = new GoogleSearchConsoleService.Window(LocalDate.of(2026, 8, 14), LocalDate.of(2026, 9, 10));
        var totals = new GoogleSearchConsoleService.Outcome<>(new GoogleSearchConsoleService.Totals(1, 2, 0.5, 3.0), null);
        var prevRow = new GoogleSearchConsoleService.Row("https://nudger.fr/a", 30, 300, 0.1, 4.0);
        var loss = new GoogleSearchConsoleService.LossRow("https://nudger.fr/a", null, prevRow, -30);
        var top = new GoogleSearchConsoleService.Outcome<>(new GoogleSearchConsoleService.TopRows(
                java.util.List.of(), java.util.Map.of(), java.util.List.of(loss), true), null);
        var daily = new GoogleSearchConsoleService.Outcome<>(java.util.List.of(
                new GoogleSearchConsoleService.DailyRow(LocalDate.of(2026, 8, 14), 3, 40, 0.075, 2.345, "previous"),
                new GoogleSearchConsoleService.DailyRow(LocalDate.of(2026, 8, 15), 0, 0, null, null, "previous")), null);
        when(searchConsoleService.fetchSeoMetrics()).thenReturn(
                new GoogleSearchConsoleService.SeoMetrics(current, previous, totals, totals, top, top, daily));
        when(searchConsoleService.countPagesWithImpressions()).thenReturn(
                new GoogleSearchConsoleService.PagesWithImpressionsResult(5000L, LocalDate.of(2026, 9, 11),
                        LocalDate.of(2026, 10, 8), false, 4800L, LocalDate.of(2026, 8, 14),
                        LocalDate.of(2026, 9, 10), null));

        mockMvc.perform(get("/api/metriks/seo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].params.previousValue").value(4800))
                .andExpect(jsonPath("$.events[0].params.previousWindowStartDate").value("2026-08-14"))
                .andExpect(jsonPath("$.events[0].params.previousWindowEndDate").value("2026-09-10"))
                .andExpect(jsonPath("$.events[7].id").value("seo.gsc.daily"))
                .andExpect(jsonPath("$.events[7].value").value(2))
                .andExpect(jsonPath("$.events[7].unit").value("count"))
                .andExpect(jsonPath("$.events[7].params.rows[0].date").value("2026-08-14"))
                .andExpect(jsonPath("$.events[7].params.rows[0].ctr").value(7.5))
                .andExpect(jsonPath("$.events[7].params.rows[0].position").value(2.35))
                .andExpect(jsonPath("$.events[7].params.rows[0].window").value("previous"))
                .andExpect(jsonPath("$.events[7].params.rows[1].ctr").doesNotExist())
                .andExpect(jsonPath("$.events[8].id").value("seo.gsc.top_losing_queries"))
                .andExpect(jsonPath("$.events[8].name").value("Top 20 requêtes en recul (Search Console, 28 j)"))
                .andExpect(jsonPath("$.events[8].params.rows[0].clicksDelta").value(-30))
                .andExpect(jsonPath("$.events[8].params.rows[0].clicks").value(0))
                .andExpect(jsonPath("$.events[8].params.rows[0].previousClicks").value(30))
                .andExpect(jsonPath("$.events[8].params.truncated").value("true"))
                .andExpect(jsonPath("$.events[9].id").value("seo.gsc.top_losing_pages"));
    }
}
