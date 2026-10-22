package org.open4goods.api.controller.api;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.api.dto.metriks.MetriksEvent;
import org.open4goods.api.dto.metriks.MetriksPeriod;
import org.open4goods.api.dto.metriks.MetriksResponse;
import org.open4goods.api.services.metriks.GoogleSearchConsoleService;
import org.open4goods.api.services.metriks.GoogleSearchConsoleService.Outcome;
import org.open4goods.api.services.metriks.GoogleSearchConsoleService.Row;
import org.open4goods.api.services.metriks.GoogleSearchConsoleService.TopRows;
import org.open4goods.api.services.metriks.GoogleSearchConsoleService.Totals;
import org.open4goods.api.services.metriks.affiliation.AffiliationMetriksService;
import org.open4goods.model.RolesConstants;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Exposes the metrics consumed by the weekly Metriks report (the {@code open4goods-api} provider in
 * open4goods-config calls these endpoints). Each metric is emitted as a schema 2.0 event so it can
 * flow unchanged into the report pipeline and the {@code /metriks} dashboard.
 */
@RestController
@RequestMapping("/api/metriks")
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Metriks", description = "Structured metric endpoints consumed by the weekly Metriks report pipeline. "
        + "Each endpoint emits schema-2.0 events covering system health, business KPIs and SEO signals.")
public class MetriksController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetriksController.class);
    private static final List<String> GSC_GROUPS = List.of("seo");
    private static final List<String> GSC_TAGS = List.of("gsc", "google", "search-analytics");
    /** Default locale used when counting products that carry an AI review. */
    private final FeedService feedService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductRepository productRepository;
    private final GoogleSearchConsoleService searchConsoleService;
    private final AffiliationMetriksService affiliationMetriksService;

    public MetriksController(FeedService feedService,
                             ElasticsearchOperations elasticsearchOperations,
                             ProductRepository productRepository,
                             GoogleSearchConsoleService searchConsoleService,
                             AffiliationMetriksService affiliationMetriksService) {
        this.feedService = feedService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.productRepository = productRepository;
        this.searchConsoleService = searchConsoleService;
        this.affiliationMetriksService = affiliationMetriksService;
    }

    @GetMapping("/system")
    @Operation(
            summary = "Get system metrics",
            description = "Returns disk usage (total, free, used) and the Elasticsearch product document count "
                    + "as Metriks schema-2.0 events. These events flow unchanged into the weekly report pipeline.")
    @ApiResponse(responseCode = "200", description = "MetriksResponse containing system metric events")
    public MetriksResponse getSystemMetrics() {
        MetriksResponse response = newResponse();
        List<MetriksEvent> events = new ArrayList<>();

        File root = new File("/");
        events.add(event("system.disk.total", "Total Disk Space", root.getTotalSpace(), "bytes",
                List.of("system"), List.of("disk")));
        events.add(event("system.disk.free", "Free Disk Space", root.getFreeSpace(), "bytes",
                List.of("system"), List.of("disk")));
        events.add(event("system.disk.used", "Used Disk Space", root.getTotalSpace() - root.getFreeSpace(), "bytes",
                List.of("system"), List.of("disk")));

        events.add(measure("infrastructure.elastic.docs", "Elasticsearch product documents", "count",
                List.of("system", "products"), List.of("elasticsearch"),
                () -> elasticsearchOperations.count(Query.findAll(), ProductRepository.CURRENT_INDEX)));

        response.setEvents(events);
        return response;
    }

    @GetMapping("/functional")
    @Operation(
            summary = "Get functional business KPI metrics",
            description = "Returns business KPIs as Metriks schema-2.0 events: total product count, "
                    + "products with an ImpactScore, number of active feeds and affiliation partner count.")
    @ApiResponse(responseCode = "200", description = "MetriksResponse containing business KPI events")
    public MetriksResponse getFunctionalMetrics() {
        MetriksResponse response = newResponse();
        List<MetriksEvent> events = new ArrayList<>();

        events.add(measure("business.products.total", "Produits en base", "count",
                List.of("products"), List.of("elasticsearch", "products"),
                productRepository::countMainIndex));

        events.add(measure("business.products.rated", "Produits notés (ImpactScore)", "count",
                List.of("products"), List.of("elasticsearch", "products"),
                productRepository::countMainIndexValidAndRated));

        events.add(measure("business.datasources.feeds", "Datasources / feeds", "count",
                List.of("business"), List.of("feeds", "datasources"),
                () -> (long) feedService.getFeedsUrl().size()));

        events.add(measure("business.partners.count", "Partenaires d'affiliation", "count",
                List.of("business"), List.of("partners"),
                () -> (long) feedService.getPartners().size()));

        response.setEvents(events);
        return response;
    }

    @GetMapping("/seo")
    @Operation(
            summary = "Get SEO metrics from Google Search Console",
            description = "Returns the number of distinct pages that received at least one impression in Search "
                    + "Console's Search Analytics report over a rolling 28-day window, as a Metriks schema-2.0 "
                    + "event. This is a proxy for indexation, not an exact measure: a page that Google has indexed "
                    + "but which received no impression in the window is not counted. Requires a valid Google "
                    + "Search Console credential to be configured. It also returns, for the last 28 days ending "
                    + "'metriks.gsc.lag-days' days ago (UTC) compared with the 28 contiguous days before: clicks, "
                    + "impressions, CTR (percent), average position (seo.gsc.clicks, impressions, ctr, position, each "
                    + "with 'previousValue' in params) and the top 20 queries and pages by clicks "
                    + "(seo.gsc.top_queries, seo.gsc.top_pages, 'params.rows' with previous-window values, null when "
                    + "the key is absent from the first 1000 previous rows). A failing metric is reported as an "
                    + "event with status 'error'; the endpoint still answers 200.")
    @ApiResponse(responseCode = "200", description = "MetriksResponse containing SEO metric events")
    public MetriksResponse getSeoMetrics() {
        MetriksResponse response = newResponse();
        List<MetriksEvent> events = new ArrayList<>();

        events.add(measurePagesWithImpressions());
        events.addAll(measureSearchAnalytics());

        response.setEvents(events);
        return response;
    }

    @GetMapping("/affiliation")
    @Operation(
            summary = "Get affiliation revenue metrics (Awin, Effiliation, Rakuten)",
            description = "Returns, for each affiliation network (awin, effiliation, rakuten) and for their total, "
                    + "the validated commissions, the pending commissions (EUR) and the sales count (validated + "
                    + "pending, declined excluded) over the last 7 days [D-7, D-1] and 28 days [D-28, D-1], "
                    + "Europe/Paris, by transaction date, as Metriks schema-2.0 events. A network without credentials "
                    + "or whose API fails has its events in error (never zero); the totals are then in error too. "
                    + "Always answers 200.")
    @ApiResponse(responseCode = "200", description = "MetriksResponse containing affiliation revenue events")
    public MetriksResponse getAffiliationMetrics() {
        MetriksResponse response = newResponse();
        response.setEvents(affiliationMetriksService.collectEvents());
        return response;
    }

    /**
     * Builds the {@code seo.gsc.pages_with_impressions} event, exposing the Search Analytics window
     * bounds and an optional truncation warning in {@code params}.
     */
    private MetriksEvent measurePagesWithImpressions() {
        String id = "seo.gsc.pages_with_impressions";
        String name = "Pages avec impressions (Search Console, 28 j)";
        String unit = "count";
        List<String> groups = List.of("seo");
        List<String> tags = List.of("gsc", "google", "search-analytics");

        try {
            GoogleSearchConsoleService.PagesWithImpressionsResult result = searchConsoleService.countPagesWithImpressions();
            MetriksEvent event = event(id, name, result.count(), unit, groups, tags);
            Map<String, Object> params = new HashMap<>(event.getParams());
            params.put("windowStartDate", result.startDate().toString());
            params.put("windowEndDate", result.endDate().toString());
            if (result.truncated()) {
                params.put("truncated", "true");
            }
            event.setParams(params);
            return event;
        } catch (Exception e) {
            LOGGER.warn("Metriks measure failed for {}: {}", id, e.getMessage());
            MetriksEvent error = event(id, name, null, unit, groups, tags);
            error.setStatus("error");
            error.setErrorMessage(e.getMessage());
            return error;
        }
    }

    /**
     * Builds the Search Analytics totals and top-20 events. A failure of the whole call or of one
     * metric group turns the affected events into {@code error} events.
     */
    private List<MetriksEvent> measureSearchAnalytics() {
        GoogleSearchConsoleService.SeoMetrics metrics;
        try {
            metrics = searchConsoleService.fetchSeoMetrics();
        } catch (Exception e) {
            LOGGER.warn("Metriks Search Console fetch failed: {}", e.getMessage());
            String message = String.valueOf(e.getMessage());
            return List.of(
                    gscError("seo.gsc.clicks", "Clics (Search Console, 28 j)", "count", message),
                    gscError("seo.gsc.impressions", "Impressions (Search Console, 28 j)", "count", message),
                    gscError("seo.gsc.ctr", "CTR (Search Console, 28 j)", "percent", message),
                    gscError("seo.gsc.position", "Position moyenne (Search Console, 28 j)", "ratio", message),
                    gscError("seo.gsc.top_queries", "Top 20 requêtes (Search Console, 28 j)", "count", message),
                    gscError("seo.gsc.top_pages", "Top 20 pages (Search Console, 28 j)", "count", message));
        }

        Outcome<Totals> cur = metrics.currentTotals();
        Outcome<Totals> prev = metrics.previousTotals();
        List<MetriksEvent> events = new ArrayList<>();
        events.add(totalsEvent("seo.gsc.clicks", "Clics (Search Console, 28 j)", "count", metrics, cur, prev,
                t -> BigDecimal.valueOf(t.clicks())));
        events.add(totalsEvent("seo.gsc.impressions", "Impressions (Search Console, 28 j)", "count", metrics, cur, prev,
                t -> BigDecimal.valueOf(t.impressions())));
        events.add(totalsEvent("seo.gsc.ctr", "CTR (Search Console, 28 j)", "percent", metrics, cur, prev,
                t -> round2(t.ctr() * 100)));
        events.add(totalsEvent("seo.gsc.position", "Position moyenne (Search Console, 28 j)", "ratio", metrics, cur, prev,
                t -> round2(t.position())));
        events.add(topEvent("seo.gsc.top_queries", "Top 20 requêtes (Search Console, 28 j)", metrics, metrics.topQueries()));
        events.add(topEvent("seo.gsc.top_pages", "Top 20 pages (Search Console, 28 j)", metrics, metrics.topPages()));
        return events;
    }

    private MetriksEvent totalsEvent(String id, String name, String unit, GoogleSearchConsoleService.SeoMetrics metrics,
                                     Outcome<Totals> cur, Outcome<Totals> prev, java.util.function.Function<Totals, BigDecimal> extractor) {
        if (cur.isFailure() || prev.isFailure()) {
            return gscError(id, name, unit, cur.isFailure() ? cur.error() : prev.error());
        }
        BigDecimal value = extractor.apply(cur.value());
        MetriksEvent event = event(id, name, value, unit, GSC_GROUPS, GSC_TAGS);
        Map<String, Object> params = windowParams(event.getParams(), metrics);
        params.put("previousValue", extractor.apply(prev.value()));
        event.setParams(params);
        return event;
    }

    private MetriksEvent topEvent(String id, String name, GoogleSearchConsoleService.SeoMetrics metrics,
                                  Outcome<TopRows> outcome) {
        if (outcome.isFailure()) {
            return gscError(id, name, "count", outcome.error());
        }
        TopRows top = outcome.value();
        List<Map<String, Object>> rows = new ArrayList<>();
        int rank = 1;
        for (Row row : top.rows()) {
            Row previous = top.previousByKey().get(row.key());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", rank++);
            entry.put("key", row.key());
            entry.put("clicks", row.clicks());
            entry.put("impressions", row.impressions());
            entry.put("ctr", round2(row.ctr() * 100));
            entry.put("position", round2(row.position()));
            entry.put("previousClicks", previous == null ? null : previous.clicks());
            entry.put("previousImpressions", previous == null ? null : previous.impressions());
            entry.put("previousPosition", previous == null ? null : round2(previous.position()));
            rows.add(entry);
        }
        MetriksEvent event = event(id, name, rows.size(), "count", GSC_GROUPS, GSC_TAGS);
        Map<String, Object> params = windowParams(event.getParams(), metrics);
        params.put("rows", rows);
        event.setParams(params);
        return event;
    }

    private Map<String, Object> windowParams(Map<String, Object> base, GoogleSearchConsoleService.SeoMetrics metrics) {
        Map<String, Object> params = new HashMap<>(base);
        params.put("windowStartDate", metrics.current().start().toString());
        params.put("windowEndDate", metrics.current().end().toString());
        params.put("previousWindowStartDate", metrics.previous().start().toString());
        params.put("previousWindowEndDate", metrics.previous().end().toString());
        return params;
    }

    private MetriksEvent gscError(String id, String name, String unit, String message) {
        MetriksEvent error = event(id, name, null, unit, GSC_GROUPS, GSC_TAGS);
        error.setStatus("error");
        error.setErrorMessage(message);
        return error;
    }

    private static BigDecimal round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private MetriksResponse newResponse() {
        MetriksResponse response = new MetriksResponse();
        response.setPeriod(new MetriksPeriod(LocalDate.now(), LocalDate.now()));
        return response;
    }

    /**
     * Run a value supplier and turn it into an {@code ok} event, or an {@code error} event when it throws.
     */
    private MetriksEvent measure(String id, String name, String unit, List<String> groups, List<String> tags,
                                 ValueSupplier supplier) {
        try {
            return event(id, name, supplier.get(), unit, groups, tags);
        } catch (Exception e) {
            LOGGER.warn("Metriks measure failed for {}: {}", id, e.getMessage());
            MetriksEvent error = event(id, name, null, unit, groups, tags);
            error.setStatus("error");
            error.setErrorMessage(e.getMessage());
            return error;
        }
    }

    private MetriksEvent event(String id, String name, Number value, String unit,
                               List<String> groups, List<String> tags) {
        MetriksEvent event = new MetriksEvent();
        event.setId(id);
        event.setName(name);
        event.setValue(value);
        event.setUnit(unit);
        event.setStatus("ok");
        event.setGroups(groups);
        event.setTags(tags);
        event.setParams(Map.of("provider", "open4goods-api"));
        return event;
    }

    /**
     * A supplier of a numeric metric value that may throw checked exceptions.
     */
    @FunctionalInterface
    private interface ValueSupplier {
        Number get() throws Exception;
    }
}
