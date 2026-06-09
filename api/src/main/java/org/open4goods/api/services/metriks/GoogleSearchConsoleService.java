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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads "indexed pages" from the Google Search Console (Sitemaps) API.
 * <p>
 * The Search Console API does not expose a direct "total indexed pages" counter. The most reliable
 * automatable proxy is the Sitemaps resource ({@code GET /webmasters/v3/sites/{siteUrl}/sitemaps}),
 * whose {@code contents[].indexed} fields report how many submitted URLs Google has indexed per
 * sitemap content type. We sum the {@code indexed} counts across every sitemap content of type
 * {@code web}.
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
    private static final String SITEMAPS_API = "https://www.googleapis.com/webmasters/v3/sites/%s/sitemaps";

    private final String serviceAccountJson;
    private final String serviceAccountPath;
    /** Search Console property, e.g. {@code sc-domain:nudger.fr} or {@code https://nudger.fr/}. */
    private final String siteUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private volatile GoogleCredentials credentials;

    public GoogleSearchConsoleService(
            @Value("${google-indexation.service-account-json:}") String serviceAccountJson,
            @Value("${google-indexation.service-account-path:}") String serviceAccountPath,
            @Value("${metriks.gsc.site-url:sc-domain:nudger.fr}") String siteUrl) {
        this.serviceAccountJson = serviceAccountJson;
        this.serviceAccountPath = serviceAccountPath;
        this.siteUrl = siteUrl;
    }

    /**
     * Count indexed pages reported by Search Console for the configured property.
     *
     * @return total number of indexed URLs across all submitted sitemaps
     * @throws IOException          when credentials cannot be loaded or refreshed
     * @throws InterruptedException when the HTTP call is interrupted
     */
    public long countIndexedPages() throws IOException, InterruptedException {
        String token = accessToken();
        String encodedSite = URLEncoder.encode(siteUrl, StandardCharsets.UTF_8);
        URI uri = URI.create(String.format(SITEMAPS_API, encodedSite));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Search Console API HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode sitemaps = root.path("sitemap");
        long totalIndexed = 0L;
        for (JsonNode sitemap : sitemaps) {
            for (JsonNode content : sitemap.path("contents")) {
                // "indexed" is returned as a string per the Search Console API.
                totalIndexed += content.path("indexed").asLong(0L);
            }
        }
        LOGGER.debug("Search Console reported {} indexed pages for {}", totalIndexed, siteUrl);
        return totalIndexed;
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
}
