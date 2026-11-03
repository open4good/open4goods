package org.open4goods.api.services.metriks;

import java.io.ByteArrayInputStream;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads "pages with impressions" from the Google Search Console (Search Analytics) API.
 * <p>
 * The Search Console API does not expose a direct "total indexed pages" counter, and the Sitemaps
 * resource's {@code contents[].indexed} field is not populated by Google. We instead use the
 * Search Analytics query ({@code POST /webmasters/v3/sites/{siteUrl}/searchAnalytics/query}) with
 * the {@code page} dimension, over a rolling 28-day window, and count the distinct pages returned.
 * This is a proxy for indexation, not an exact measure: a page that is indexed but received no
 * impression in the window is not counted.
 * <p>
 * Credentials reuse the existing {@code google-indexation.service-account-json} /
 * {@code google-indexation.service-account-path} configuration (the same {@code google-api.json}
 * service account deployed on the server), only with the read-only Search Console scope. The service
 * account must be granted access to the Search Console property for the call to succeed.
 */
@Service
public class GoogleSearchConsoleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleSearchConsoleService.class);
    private static final String WEBMASTERS_READONLY_SCOPE = "https://www.googleapis.com/auth/webmasters.readonly";
    private static final String SEARCH_ANALYTICS_API = "https://www.googleapis.com/webmasters/v3/sites/%s/searchAnalytics/query";
    private static final int ROW_LIMIT = 25000;
    /** Number of rows kept in the top queries / top pages events. */
    private static final int TOP_LIMIT = 20;
    /** Maximum length of an upstream error body copied into an error message. */
    private static final int MAX_ERROR_BODY = 300;
    /** Row count at or above which we warn the caller that the real total may be higher. */
    private static final long TRUNCATION_WARNING_THRESHOLD = 50000L;

    private final String serviceAccountJson;
    private final String serviceAccountPath;
    /** Search Console property, e.g. {@code sc-domain:nudger.fr} or {@code https://nudger.fr/}. */
    private final String siteUrl;
    /** Size, in days, of the rolling Search Analytics window. */
    private final int windowDays;
    /** Number of days before "today" at which the window ends, to avoid Search Console's data lag. */
    private final int lagDays;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    /** Supplies the OAuth access token; overridable by tests to avoid real Google credentials. */
    private final TokenSupplier tokenSupplier;

    private volatile GoogleCredentials credentials;

    @Autowired
    public GoogleSearchConsoleService(
            @Value("${google-indexation.service-account-json:}") String serviceAccountJson,
            @Value("${google-indexation.service-account-path:}") String serviceAccountPath,
            @Value("${metriks.gsc.site-url:sc-domain:nudger.fr}") String siteUrl,
            @Value("${metriks.gsc.window-days:28}") int windowDays,
            @Value("${metriks.gsc.lag-days:3}") int lagDays) {
        this(serviceAccountJson, serviceAccountPath, siteUrl, windowDays, lagDays, HttpClient.newHttpClient());
    }

    /**
     * Package-private constructor allowing tests to inject a fake/mock {@link HttpClient} and avoid
     * any real network call.
     */
    GoogleSearchConsoleService(String serviceAccountJson, String serviceAccountPath, String siteUrl,
                                int windowDays, int lagDays, HttpClient httpClient) {
        this.serviceAccountJson = serviceAccountJson;
        this.serviceAccountPath = serviceAccountPath;
        this.siteUrl = siteUrl;
        this.windowDays = windowDays;
        this.lagDays = lagDays;
        this.httpClient = httpClient;
        this.tokenSupplier = this::accessToken;
    }

    /**
     * Package-private constructor for tests: injects both the {@link HttpClient} and the token source.
     */
    GoogleSearchConsoleService(String siteUrl, int windowDays, int lagDays, HttpClient httpClient,
                                TokenSupplier tokenSupplier) {
        this.serviceAccountJson = "";
        this.serviceAccountPath = "";
        this.siteUrl = siteUrl;
        this.windowDays = windowDays;
        this.lagDays = lagDays;
        this.httpClient = httpClient;
        this.tokenSupplier = tokenSupplier;
    }

    /**
     * Count distinct pages that received at least one impression in Search Console's Search
     * Analytics report, over the configured rolling window ({@code metriks.gsc.window-days} days,
     * ending {@code metriks.gsc.lag-days} days before today, UTC).
     * <p>
     * This is a proxy for indexation: a page that Google has indexed but which received no
     * impression during the window is not counted.
     *
     * @return the window result (count, window bounds, truncation flag)
     * @throws IOException          when credentials cannot be loaded/refreshed, or the API call fails
     * @throws InterruptedException when the HTTP call is interrupted
     */
    public PagesWithImpressionsResult countPagesWithImpressions() throws IOException, InterruptedException {
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC).minusDays(lagDays);
        LocalDate startDate = endDate.minusDays(windowDays - 1L);

        String token;
        try {
            token = tokenSupplier.get();
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
        String encodedSite = URLEncoder.encode(siteUrl, StandardCharsets.UTF_8);
        URI uri = URI.create(String.format(SEARCH_ANALYTICS_API, encodedSite));

        long total = paginate(startDate, endDate, startRow -> fetchPage(uri, token, startDate, endDate, startRow));

        boolean truncated = total >= TRUNCATION_WARNING_THRESHOLD;
        LOGGER.debug("Search Console reported {} pages with impressions for {} between {} and {}",
                total, siteUrl, startDate, endDate);

        LocalDate previousEnd = startDate.minusDays(1L);
        LocalDate previousStart = previousEnd.minusDays(windowDays - 1L);
        Long previousCount = null;
        String previousError = null;
        try {
            previousCount = paginate(previousStart, previousEnd,
                    startRow -> fetchPage(uri, token, previousStart, previousEnd, startRow));
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Search Console previous-window page count failed: {}", redact(e.getMessage(), token));
            previousError = abbreviate(redact(String.valueOf(e.getMessage()), token));
        }
        return new PagesWithImpressionsResult(total, startDate, endDate, truncated, previousCount,
                previousStart, previousEnd, previousError);
    }

    /**
     * Sums the row count of every page of the Search Analytics query, following the API's
     * pagination contract: keep requesting the next {@code startRow} while the previous page
     * returned exactly {@link #ROW_LIMIT} rows.
     */
    long paginate(LocalDate startDate, LocalDate endDate, PageFetcher fetcher)
            throws IOException, InterruptedException {
        long total = 0L;
        int startRow = 0;
        int rowsInPage;
        do {
            String body = fetcher.fetchPage(startRow);
            rowsInPage = countRows(body);
            total += rowsInPage;
            startRow += ROW_LIMIT;
        } while (rowsInPage == ROW_LIMIT);
        return total;
    }

    /**
     * Counts the {@code rows} entries of a Search Analytics response body; each row is one distinct
     * page. A response without a {@code rows} array (no impressions in the window) counts as zero.
     */
    int countRows(String responseBody) {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode rows = root.path("rows");
        return rows.isArray() ? rows.size() : 0;
    }

    private String fetchPage(URI uri, String token, LocalDate startDate, LocalDate endDate, int startRow)
            throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>(baseBody(startDate, endDate));
        body.put("dimensions", List.of("page"));
        body.put("rowLimit", ROW_LIMIT);
        body.put("startRow", startRow);
        return post(uri, token, body);
    }

    private Map<String, Object> baseBody(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("startDate", startDate.toString());
        body.put("endDate", endDate.toString());
        body.put("type", "web");
        body.put("dataState", "final");
        return body;
    }

    private String post(URI uri, String token, Map<String, Object> bodyMap) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(bodyMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Search Console API HTTP " + response.statusCode() + ": "
                    + abbreviate(redact(response.body(), token)));
        }
        return response.body();
    }

    private static String redact(String text, String token) {
        if (text == null) {
            return "";
        }
        return StringUtils.hasText(token) ? text.replace(token, "[redacted]") : text;
    }

    private static String abbreviate(String text) {
        return text.length() > MAX_ERROR_BODY ? text.substring(0, MAX_ERROR_BODY) + "..." : text;
    }

    /**
     * Fetches the Search Console SEO figures (totals and top queries/pages) for the current window and
     * the contiguous previous one. The access token is loaded once; each part succeeds or fails
     * independently, except that a credential failure fails every part.
     *
     * @return the SEO figures, with one {@link Outcome} per metric group
     */
    public SeoMetrics fetchSeoMetrics() {
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC).minusDays(lagDays);
        Window current = new Window(endDate.minusDays(windowDays - 1L), endDate);
        Window previous = new Window(current.start().minusDays(windowDays), current.start().minusDays(1L));

        String token;
        try {
            token = tokenSupplier.get();
        } catch (Exception e) {
            String message = "Search Console credentials unavailable: " + e.getMessage();
            return new SeoMetrics(current, previous, Outcome.failure(message), Outcome.failure(message),
                    Outcome.failure(message), Outcome.failure(message), Outcome.failure(message));
        }

        URI uri = URI.create(String.format(SEARCH_ANALYTICS_API, URLEncoder.encode(siteUrl, StandardCharsets.UTF_8)));
        Outcome<Totals> currentTotals = attempt(() -> fetchTotals(uri, token, current), token);
        Outcome<Totals> previousTotals = attempt(() -> fetchTotals(uri, token, previous), token);
        Outcome<TopRows> queries = attempt(() -> fetchTop(uri, token, current, previous, "query"), token);
        Outcome<TopRows> pages = attempt(() -> fetchTop(uri, token, current, previous, "page"), token);
        Outcome<List<DailyRow>> daily = attempt(() -> fetchDaily(uri, token, current, previous), token);
        return new SeoMetrics(current, previous, currentTotals, previousTotals, queries, pages, daily);
    }

    private <T> Outcome<T> attempt(Fetch<T> fetch, String token) {
        try {
            return Outcome.success(fetch.run());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Outcome.failure("Search Console request interrupted");
        } catch (Exception e) {
            LOGGER.warn("Search Console query failed: {}", redact(e.getMessage(), token));
            return Outcome.failure(abbreviate(redact(String.valueOf(e.getMessage()), token)));
        }
    }

    private Totals fetchTotals(URI uri, String token, Window window) throws IOException, InterruptedException {
        String body = post(uri, token, baseBody(window.start(), window.end()));
        JsonNode rows = objectMapper.readTree(body).path("rows");
        if (!rows.isArray() || rows.isEmpty()) {
            return new Totals(0L, 0L, 0.0, 0.0);
        }
        JsonNode row = rows.get(0);
        return new Totals(row.path("clicks").asLong(0L), row.path("impressions").asLong(0L),
                row.path("ctr").asDouble(0.0), row.path("position").asDouble(0.0));
    }

    private TopRows fetchTop(URI uri, String token, Window current, Window previous, String dimension)
            throws IOException, InterruptedException {
        List<Row> currentRows = fetchRows(uri, token, current, dimension);
        List<Row> previousRows = fetchRows(uri, token, previous, dimension);
        Map<String, Row> previousByKey = new HashMap<>();
        for (Row row : previousRows) {
            previousByKey.putIfAbsent(row.key(), row);
        }
        Map<String, Row> currentByKey = new HashMap<>();
        for (Row row : currentRows) {
            currentByKey.putIfAbsent(row.key(), row);
        }
        List<LossRow> losing = new ArrayList<>();
        for (Row prev : previousByKey.values()) {
            if (prev.clicks() <= 0) {
                continue;
            }
            Row cur = currentByKey.get(prev.key());
            long delta = (cur == null ? 0L : cur.clicks()) - prev.clicks();
            if (delta < 0) {
                losing.add(new LossRow(prev.key(), cur, prev, delta));
            }
        }
        losing.sort(Comparator.comparingLong(LossRow::clicksDelta)
                .thenComparing(Comparator.comparingLong((LossRow l) -> l.previous().clicks()).reversed())
                .thenComparing(LossRow::key));
        List<Row> top = currentRows.size() > TOP_LIMIT ? new ArrayList<>(currentRows.subList(0, TOP_LIMIT)) : currentRows;
        List<LossRow> topLosing = losing.size() > TOP_LIMIT ? new ArrayList<>(losing.subList(0, TOP_LIMIT)) : losing;
        boolean truncated = currentRows.size() >= ROW_LIMIT || previousRows.size() >= ROW_LIMIT;
        return new TopRows(top, previousByKey, topLosing, truncated);
    }

    /** One daily read over both windows, completed so that every day of the 56 has a row. */
    private List<DailyRow> fetchDaily(URI uri, String token, Window current, Window previous)
            throws IOException, InterruptedException {
        Map<String, Object> bodyMap = new HashMap<>(baseBody(previous.start(), current.end()));
        bodyMap.put("dimensions", List.of("date"));
        bodyMap.put("rowLimit", ROW_LIMIT);
        JsonNode rows = objectMapper.readTree(post(uri, token, bodyMap)).path("rows");
        Map<String, JsonNode> byDate = new HashMap<>();
        if (rows.isArray()) {
            for (JsonNode row : rows) {
                JsonNode keys = row.path("keys");
                if (keys.isArray() && !keys.isEmpty()) {
                    byDate.putIfAbsent(keys.get(0).asString(), row);
                }
            }
        }
        List<DailyRow> result = new ArrayList<>();
        for (LocalDate day = previous.start(); !day.isAfter(current.end()); day = day.plusDays(1L)) {
            String window = day.isBefore(current.start()) ? "previous" : "current";
            JsonNode row = byDate.get(day.toString());
            result.add(row == null
                    ? new DailyRow(day, 0L, 0L, null, null, window)
                    : new DailyRow(day, row.path("clicks").asLong(0L), row.path("impressions").asLong(0L),
                            row.path("ctr").asDouble(0.0), row.path("position").asDouble(0.0), window));
        }
        return result;
    }

    private List<Row> fetchRows(URI uri, String token, Window window, String dimension)
            throws IOException, InterruptedException {
        Map<String, Object> bodyMap = new HashMap<>(baseBody(window.start(), window.end()));
        bodyMap.put("dimensions", List.of(dimension));
        bodyMap.put("rowLimit", ROW_LIMIT);
        String body = post(uri, token, bodyMap);
        JsonNode rows = objectMapper.readTree(body).path("rows");
        List<Row> result = new ArrayList<>();
        if (rows.isArray()) {
            for (JsonNode row : rows) {
                JsonNode keys = row.path("keys");
                if (!keys.isArray() || keys.isEmpty()) {
                    continue;
                }
                result.add(new Row(keys.get(0).asString(), row.path("clicks").asLong(0L),
                        row.path("impressions").asLong(0L), row.path("ctr").asDouble(0.0),
                        row.path("position").asDouble(0.0)));
            }
        }
        result.sort(Comparator.comparingLong(Row::clicks).reversed());
        return result;
    }

    /**
     * The Search Console property this service queries.
     *
     * @return configured site URL
     */
    public String getSiteUrl() {
        return siteUrl;
    }

    private String accessToken() throws IOException {
        GoogleCredentials resolved = credentials;
        if (resolved == null) {
            resolved = loadCredentials();
            credentials = resolved;
        }
        resolved.refreshIfExpired();
        AccessToken accessToken = resolved.getAccessToken();
        if (accessToken == null) {
            throw new IOException("Google credentials did not return an access token");
        }
        return accessToken.getTokenValue();
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (StringUtils.hasText(serviceAccountJson)) {
            return GoogleCredentials.fromStream(new ByteArrayInputStream(
                    serviceAccountJson.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(List.of(WEBMASTERS_READONLY_SCOPE));
        }
        if (StringUtils.hasText(serviceAccountPath)) {
            byte[] content = Files.readAllBytes(Path.of(serviceAccountPath));
            return GoogleCredentials.fromStream(new ByteArrayInputStream(content))
                    .createScoped(List.of(WEBMASTERS_READONLY_SCOPE));
        }
        throw new IOException("Missing Google credentials (google-indexation.service-account-json or -path)");
    }

    /**
     * Result of a {@link #countPagesWithImpressions()} call.
     *
     * @param count      number of distinct pages with at least one impression in the window
     * @param startDate  inclusive start of the Search Analytics window (UTC)
     * @param endDate    inclusive end of the Search Analytics window (UTC)
     * @param truncated  {@code true} when the row count reached {@link #TRUNCATION_WARNING_THRESHOLD},
     *                   signalling the real total may be higher than reported
     * @param previousCount     same count over the contiguous previous window, {@code null} when it failed
     * @param previousStartDate inclusive start of the previous window (UTC)
     * @param previousEndDate   inclusive end of the previous window (UTC)
     * @param previousError     redacted message when only the previous count failed, else {@code null}
     */
    public record PagesWithImpressionsResult(long count, LocalDate startDate, LocalDate endDate, boolean truncated,
                                             Long previousCount, LocalDate previousStartDate,
                                             LocalDate previousEndDate, String previousError) {
        public PagesWithImpressionsResult(long count, LocalDate startDate, LocalDate endDate, boolean truncated) {
            this(count, startDate, endDate, truncated, null, null, null, null);
        }
    }

    /** Inclusive Search Analytics date range (UTC). */
    public record Window(LocalDate start, LocalDate end) {
    }

    /** Totals of a window; {@code ctr} is a fraction (0..1) as returned by Google. */
    public record Totals(long clicks, long impressions, double ctr, double position) {
    }

    /** One Search Analytics row for a {@code query} or {@code page} dimension; {@code ctr} is a fraction. */
    public record Row(String key, long clicks, long impressions, double ctr, double position) {
    }

    /**
     * Top rows of the current window, the previous window's rows indexed by key, the top losing keys
     * and whether one of the full reads reached the {@code rowLimit} (so the lists may be incomplete).
     */
    public record TopRows(List<Row> rows, Map<String, Row> previousByKey, List<LossRow> losing, boolean truncated) {
    }

    /**
     * A key that lost clicks between the previous and the current window.
     *
     * @param current     current-window row, {@code null} when the key is absent (0 click)
     * @param previous    previous-window row
     * @param clicksDelta current clicks (0 if absent) minus previous clicks, always negative
     */
    public record LossRow(String key, Row current, Row previous, long clicksDelta) {
    }

    /** One day of the daily series; {@code ctr} is a fraction, {@code ctr}/{@code position} are null for a day without data. */
    public record DailyRow(LocalDate date, long clicks, long impressions, Double ctr, Double position, String window) {
    }

    /** Value or error message of one metric group. */
    public record Outcome<T>(T value, String error) {
        static <T> Outcome<T> success(T value) {
            return new Outcome<>(value, null);
        }

        static <T> Outcome<T> failure(String error) {
            return new Outcome<>(null, error);
        }

        public boolean isFailure() {
            return error != null;
        }
    }

    /** Result of {@link #fetchSeoMetrics()}. */
    public record SeoMetrics(Window current, Window previous, Outcome<Totals> currentTotals,
                             Outcome<Totals> previousTotals, Outcome<TopRows> topQueries, Outcome<TopRows> topPages,
                             Outcome<List<DailyRow>> daily) {
    }

    @FunctionalInterface
    interface TokenSupplier {
        String get() throws Exception;
    }

    @FunctionalInterface
    private interface Fetch<T> {
        T run() throws Exception;
    }

    @FunctionalInterface
    interface PageFetcher {
        String fetchPage(int startRow) throws IOException, InterruptedException;
    }
}
