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

        String token = accessToken();
        String encodedSite = URLEncoder.encode(siteUrl, StandardCharsets.UTF_8);
        URI uri = URI.create(String.format(SEARCH_ANALYTICS_API, encodedSite));

        long total = paginate(startDate, endDate, startRow -> fetchPage(uri, token, startDate, endDate, startRow));

        boolean truncated = total >= TRUNCATION_WARNING_THRESHOLD;
        LOGGER.debug("Search Console reported {} pages with impressions for {} between {} and {}",
                total, siteUrl, startDate, endDate);
        return new PagesWithImpressionsResult(total, startDate, endDate, truncated);
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
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "dimensions", List.of("page"),
                "type", "web",
                "dataState", "final",
                "rowLimit", ROW_LIMIT,
                "startRow", startRow));

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
            throw new IOException("Search Console API HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
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
     */
    public record PagesWithImpressionsResult(long count, LocalDate startDate, LocalDate endDate, boolean truncated) {
    }

    @FunctionalInterface
    interface PageFetcher {
        String fetchPage(int startRow) throws IOException, InterruptedException;
    }
}
