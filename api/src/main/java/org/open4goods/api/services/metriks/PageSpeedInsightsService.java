package org.open4goods.api.services.metriks;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.open4goods.api.dto.metriks.MetriksEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Weekly PageSpeed Insights (mobile) snapshot of a handful of representative nudger.fr URLs, exposed as Metriks
 * events by {@code GET /api/metriks/psi} (GOU-2703, audit SEO-39). PSI answers 429 without an API key, so the key
 * ({@code metriks.psi.api-key}) is required; without it the snapshot is an {@code error} event, never a zero.
 * <p>
 * The snapshot is computed by the weekly schedule (or {@code POST /api/metriks/psi/refresh}), not on read: about
 * 20 s per URL. It is kept in memory and, when {@code metriks.psi.snapshot-path} is set, in a JSON file so that a
 * restart does not lose it before the next run. Calls are sequential: moderate throughput towards Google.
 */
@Service
public class PageSpeedInsightsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageSpeedInsightsService.class);
    private static final String PSI_API = "https://www.googleapis.com/pagespeedonline/v5/runPagespeed";
    private static final int MAX_ERROR_BODY = 300;
    private static final List<String> GROUPS = List.of("seo", "performance");
    private static final List<String> TAGS = List.of("psi", "lighthouse", "core-web-vitals");

    private final String apiKey;
    private final String baseUrl;
    private final List<String> paths;
    private final Path snapshotPath;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile List<MetriksEvent> snapshot = List.of();

    @Autowired
    public PageSpeedInsightsService(
            @Value("${metriks.psi.api-key:}") String apiKey,
            @Value("${metriks.psi.base-url:https://nudger.fr}") String baseUrl,
            @Value("${metriks.psi.paths:/,/televiseurs,/televiseurs/televiseur-55-pouces,"
                    + "/televiseurs/8806097084532-tv-samsung-tq77s90fae-2025,/smartphones,/categories,/blog,"
                    + "/blog/quelle-taille-de-tv-choisir-pour-son-salon,"
                    + "/blog/black-friday-tv-reperer-fausses-promotions,/opendata}") List<String> paths,
            @Value("${metriks.psi.snapshot-path:}") String snapshotPath) {
        this(apiKey, baseUrl, paths, snapshotPath, HttpClient.newHttpClient());
    }

    /** Package-private constructor allowing tests to inject a fake {@link HttpClient}. */
    PageSpeedInsightsService(String apiKey, String baseUrl, List<String> paths, String snapshotPath,
                             HttpClient httpClient) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.paths = paths.stream().map(String::trim).filter(p -> !p.isEmpty()).toList();
        this.snapshotPath = StringUtils.hasText(snapshotPath) ? Path.of(snapshotPath) : null;
        this.httpClient = httpClient;
        this.snapshot = loadSnapshot();
    }

    /** Weekly run, Monday 04:15 Europe/Paris (outside the batch window). {@code "-"} disables it. */
    @Scheduled(cron = "${metriks.psi.cron:0 15 4 * * MON}", zone = "Europe/Paris")
    public void scheduledRefresh() {
        if (!StringUtils.hasText(apiKey)) {
            LOGGER.warn("PSI snapshot skipped: metriks.psi.api-key is not set");
            return;
        }
        refresh();
    }

    /**
     * Measures every configured URL and stores the result as the latest snapshot.
     *
     * @return the new snapshot events (one {@code error} event when the API key is missing)
     */
    public synchronized List<MetriksEvent> refresh() {
        if (!StringUtils.hasText(apiKey)) {
            return List.of(error("psi.snapshot", "Relevé PageSpeed Insights", "metriks.psi.api-key is not set"));
        }
        List<MetriksEvent> events = new ArrayList<>();
        String measuredAt = Instant.now().toString();
        for (String path : paths) {
            events.addAll(measure(path, measuredAt));
        }
        snapshot = List.copyOf(events);
        saveSnapshot(snapshot);
        return snapshot;
    }

    /** @return the last snapshot, or one {@code error} event when there is none yet */
    public List<MetriksEvent> latestEvents() {
        List<MetriksEvent> current = snapshot;
        if (current.isEmpty()) {
            return List.of(error("psi.snapshot", "Relevé PageSpeed Insights",
                    StringUtils.hasText(apiKey) ? "no snapshot yet (weekly run pending, POST /api/metriks/psi/refresh)"
                            : "metriks.psi.api-key is not set"));
        }
        return current;
    }

    private List<MetriksEvent> measure(String path, String measuredAt) {
        String key = urlId(path);
        String pageUrl = baseUrl + (path.startsWith("/") ? path : "/" + path);
        try {
            String uri = PSI_API + "?url=" + URLEncoder.encode(pageUrl, StandardCharsets.UTF_8)
                    + "&strategy=mobile&category=performance&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri)).timeout(Duration.ofSeconds(120))
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("PSI API HTTP " + response.statusCode() + ": "
                        + abbreviate(redact(response.body())));
            }
            return parse(key, pageUrl, measuredAt, objectMapper.readTree(response.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of(error("psi." + key + ".performance", "PSI " + path, "PSI request interrupted"));
        } catch (Exception e) {
            LOGGER.warn("PSI measure failed for {}: {}", pageUrl, redact(String.valueOf(e.getMessage())));
            return List.of(error("psi." + key + ".performance", "PSI " + path, redact(String.valueOf(e.getMessage()))));
        }
    }

    /** Extracts score, lab LCP / CLS / TBT and, when CrUX has data for the URL, the field p75 LCP / INP / CLS. */
    List<MetriksEvent> parse(String key, String pageUrl, String measuredAt, JsonNode root) {
        JsonNode lighthouse = root.path("lighthouseResult");
        JsonNode audits = lighthouse.path("audits");
        JsonNode field = root.path("loadingExperience").path("metrics");
        boolean hasField = field.isObject() && !field.isEmpty();
        List<MetriksEvent> events = new ArrayList<>();

        JsonNode score = lighthouse.path("categories").path("performance").path("score");
        events.add(ok("psi." + key + ".performance", "Score performance PSI mobile " + pageUrl, number(score, v -> v * 100),
                "percent", pageUrl, measuredAt, hasField));
        events.add(ok("psi." + key + ".lcp", "LCP labo PSI mobile " + pageUrl,
                number(audits.path("largest-contentful-paint").path("numericValue"), v -> v), "ms", pageUrl,
                measuredAt, hasField));
        events.add(ok("psi." + key + ".cls", "CLS labo PSI mobile " + pageUrl,
                number(audits.path("cumulative-layout-shift").path("numericValue"), v -> v), "ratio", pageUrl,
                measuredAt, hasField));
        events.add(ok("psi." + key + ".tbt", "TBT labo PSI mobile " + pageUrl,
                number(audits.path("total-blocking-time").path("numericValue"), v -> v), "ms", pageUrl, measuredAt,
                hasField));
        if (hasField) {
            addField(events, key, pageUrl, measuredAt, field, "LARGEST_CONTENTFUL_PAINT_MS", "field_lcp", "ms", 1.0);
            addField(events, key, pageUrl, measuredAt, field, "INTERACTION_TO_NEXT_PAINT", "field_inp", "ms", 1.0);
            // CrUX reports CLS multiplied by 100.
            addField(events, key, pageUrl, measuredAt, field, "CUMULATIVE_LAYOUT_SHIFT_SCORE", "field_cls", "ratio",
                    0.01);
        }
        return events;
    }

    private void addField(List<MetriksEvent> events, String key, String pageUrl, String measuredAt, JsonNode field,
                          String metric, String suffix, String unit, double factor) {
        JsonNode p75 = field.path(metric).path("percentile");
        if (p75.isNumber()) {
            events.add(ok("psi." + key + "." + suffix, "Terrain (CrUX p75) " + suffix + " " + pageUrl,
                    p75.asDouble() * factor, unit, pageUrl, measuredAt, true));
        }
    }

    private static Double number(JsonNode node, Function<Double, Double> map) {
        return node.isNumber() ? map.apply(node.asDouble()) : null;
    }

    private MetriksEvent ok(String id, String name, Double value, String unit, String pageUrl, String measuredAt,
                            boolean hasField) {
        if (value == null) {
            MetriksEvent missing = base(id, name, null, unit);
            missing.setStatus("error");
            missing.setErrorMessage("metric absent from the PSI response");
            missing.setUrl(pageUrl);
            return missing;
        }
        MetriksEvent event = base(id, name, Math.round(value * 100.0) / 100.0, unit);
        event.setUrl(pageUrl);
        event.setParams(Map.of("provider", "open4goods-api", "strategy", "mobile", "measuredAt", measuredAt,
                "fieldData", hasField));
        return event;
    }

    private MetriksEvent error(String id, String name, String message) {
        MetriksEvent event = base(id, name, null, "unknown");
        event.setStatus("error");
        event.setErrorMessage(message);
        return event;
    }

    private MetriksEvent base(String id, String name, Number value, String unit) {
        MetriksEvent event = new MetriksEvent();
        event.setId(id);
        event.setName(name);
        event.setValue(value);
        event.setUnit(unit);
        event.setStatus("ok");
        event.setGroups(GROUPS);
        event.setTags(TAGS);
        event.setParams(Map.of("provider", "open4goods-api"));
        return event;
    }

    /** {@code /televiseurs/x} gives {@code televiseurs_x}; the root path gives {@code home}. */
    static String urlId(String path) {
        String id = path.replaceAll("^/+|/+$", "").replaceAll("[^A-Za-z0-9-]+", "_");
        return id.isEmpty() ? "home" : id;
    }

    private String redact(String text) {
        String safe = text == null ? "" : text;
        return StringUtils.hasText(apiKey) ? safe.replace(apiKey, "[redacted]") : safe;
    }

    private static String abbreviate(String text) {
        return text.length() > MAX_ERROR_BODY ? text.substring(0, MAX_ERROR_BODY) + "..." : text;
    }

    private void saveSnapshot(List<MetriksEvent> events) {
        if (snapshotPath == null) {
            return;
        }
        try {
            Files.writeString(snapshotPath, objectMapper.writeValueAsString(events));
        } catch (Exception e) {
            LOGGER.warn("PSI snapshot not persisted to {}: {}", snapshotPath, e.getMessage());
        }
    }

    private List<MetriksEvent> loadSnapshot() {
        if (snapshotPath == null || !Files.isRegularFile(snapshotPath)) {
            return List.of();
        }
        try {
            return List.copyOf(objectMapper.readValue(Files.readString(snapshotPath),
                    new TypeReference<List<MetriksEvent>>() { }));
        } catch (Exception e) {
            LOGGER.warn("PSI snapshot unreadable at {}: {}", snapshotPath, e.getMessage());
            return List.of();
        }
    }
}
