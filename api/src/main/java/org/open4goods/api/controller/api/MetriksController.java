package org.open4goods.api.controller.api;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.open4goods.api.dto.metriks.MetriksEvent;
import org.open4goods.api.dto.metriks.MetriksPeriod;
import org.open4goods.api.dto.metriks.MetriksResponse;
import org.open4goods.api.services.metriks.GoogleSearchConsoleService;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Exposes the metrics consumed by the weekly Metriks report (the {@code open4goods-api} provider in
 * open4goods-config calls these endpoints). Each metric is emitted as a schema 2.0 event so it can
 * flow unchanged into the report pipeline and the {@code /metriks} dashboard.
 */
@RestController
@RequestMapping("/api/metriks")
@Tag(name = "Metriks", description = "Endpoints for Metriks monitoring system")
public class MetriksController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetriksController.class);
    /** Default locale used when counting products that carry an AI review. */
    private static final String DEFAULT_REVIEW_LOCALE = "fr";

    private final FeedService feedService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductRepository productRepository;
    private final GoogleSearchConsoleService searchConsoleService;

    public MetriksController(FeedService feedService,
                             ElasticsearchOperations elasticsearchOperations,
                             ProductRepository productRepository,
                             GoogleSearchConsoleService searchConsoleService) {
        this.feedService = feedService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.productRepository = productRepository;
        this.searchConsoleService = searchConsoleService;
    }

    @GetMapping("/system")
    @Operation(summary = "Get system metrics (Disk, Elasticsearch)")
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
    @Operation(summary = "Get functional metrics (Business KPIs)")
    public MetriksResponse getFunctionalMetrics(
            @RequestParam(name = "locale", required = false, defaultValue = DEFAULT_REVIEW_LOCALE) String locale) {
        MetriksResponse response = newResponse();
        List<MetriksEvent> events = new ArrayList<>();

        events.add(measure("business.products.total", "Produits en base", "count",
                List.of("products"), List.of("elasticsearch", "products"),
                productRepository::countMainIndex));

        events.add(measure("business.products.reviewed", "Produits avec AiReview", "count",
                List.of("products", "reviews"), List.of("elasticsearch", "products", "reviews"),
                () -> productRepository.countMainIndexValidAndReviewed(locale)));

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
    @Operation(summary = "Get SEO metrics (Google Search Console indexed pages)")
    public MetriksResponse getSeoMetrics() {
        MetriksResponse response = newResponse();
        List<MetriksEvent> events = new ArrayList<>();

        events.add(measure("seo.gsc.indexed_pages", "Pages indexées (Search Console)", "count",
                List.of("seo"), List.of("gsc", "google", "indexation"),
                searchConsoleService::countIndexedPages));

        response.setEvents(events);
        return response;
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
