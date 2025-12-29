package org.open4goods.icecat.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.open4goods.icecat.client.exception.IcecatApiException;
import org.open4goods.icecat.client.exception.IcecatAuthenticationException;
import org.open4goods.icecat.client.exception.IcecatRateLimitException;
import org.open4goods.icecat.client.exception.IcecatResourceNotFoundException;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.model.helper.IdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClient;

/**
 * Centralized HTTP client for all Icecat API interactions.
 * Handles Basic Auth for XML file downloads, retry logic, timeouts, and caching.
 */
public class IcecatHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatHttpClient.class);
    private static final int BUFFER_SIZE = 8192;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {1000, 2000, 4000};

    private final RestClient restClient;
    private final IcecatConfiguration config;
    private final String cacheDirectory;
    private final int maxRetries;

    /**
     * Constructor for IcecatHttpClient.
     *
     * @param config         the Icecat configuration
     * @param cacheDirectory the directory for caching downloaded files
     */
    public IcecatHttpClient(IcecatConfiguration config, String cacheDirectory) {
        this(config, cacheDirectory, DEFAULT_MAX_RETRIES);
    }

    /**
     * Constructor for IcecatHttpClient with custom retry count.
     *
     * @param config         the Icecat configuration
     * @param cacheDirectory the directory for caching downloaded files
     * @param maxRetries     the maximum number of retry attempts
     */
    public IcecatHttpClient(IcecatConfiguration config, String cacheDirectory, int maxRetries) {
        this.config = config;
        this.cacheDirectory = cacheDirectory;
        this.maxRetries = maxRetries;

        int connectTimeout = config.getConnectTimeoutMs() > 0 ? config.getConnectTimeoutMs() : 10000;
        int readTimeout = config.getReadTimeoutMs() > 0 ? config.getReadTimeoutMs() : 30000;

        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "Open4Goods/1.0")
                .build();

        File cacheDir = new File(cacheDirectory);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        LOGGER.info("IcecatHttpClient initialized with cache directory: {}", cacheDirectory);
    }

    /**
     * Downloads a gzipped file, decompresses it, and returns the cached file.
     * Uses Basic Authentication if credentials are provided.
     *
     * @param url      the URL to download from
     * @param cacheKey a unique key for caching (if null, derived from URL)
     * @return the decompressed cached file
     * @throws IcecatApiException if download fails
     */
    public File downloadAndDecompressGzip(String url, String cacheKey) {
        String key = cacheKey != null ? cacheKey : IdHelper.getHashedName(url);
        File destFile = new File(cacheDirectory + File.separator + key);

        if (destFile.exists()) {
            LOGGER.debug("File {} already cached", url);
            return destFile;
        }

        LOGGER.info("Downloading and decompressing: {}", url);
        File tmpFile = new File(cacheDirectory + File.separator + "tmp-" + key);

        try {
            downloadWithBasicAuth(url, tmpFile);
            decompressGzip(tmpFile, destFile);
            LOGGER.info("Successfully cached file from {}", url);
            return destFile;
        } catch (Exception e) {
            FileUtils.deleteQuietly(destFile);
            throw new IcecatApiException("Failed to download and decompress: " + url, e, url);
        } finally {
            FileUtils.deleteQuietly(tmpFile);
        }
    }

    /**
     * Downloads a file with Basic Authentication using retry logic.
     *
     * @param url      the URL to download from
     * @param destFile the destination file
     * @throws IcecatApiException if download fails after all retries
     */
    public void downloadWithBasicAuth(String url, File destFile) {
        String authHeader = createBasicAuthHeader(config.getUser(), config.getPassword());
        downloadWithRetry(url, destFile, authHeader);
    }

    /**
     * Performs a GET request and returns the response as a string.
     *
     * @param url the URL to fetch
     * @return the response body as a string
     * @throws IcecatApiException if the request fails
     */
    public String get(String url) {
        return get(url, null);
    }

    /**
     * Performs a GET request with custom headers and returns the response as a string.
     *
     * @param url     the URL to fetch
     * @param headers additional headers to include
     * @return the response body as a string
     * @throws IcecatApiException if the request fails
     */
    public String get(String url, Map<String, String> headers) {
        return executeWithRetry(() -> {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(URI.create(url));

            if (headers != null) {
                headers.forEach(request::header);
            }

            return request.exchange((req, res) -> {
                handleErrorResponse(res, url);
                return StreamUtils.copyToString(res.getBody(), StandardCharsets.UTF_8);
            });
        }, url);
    }

    /**
     * Performs a GET request and deserializes the response to the specified type.
     *
     * @param url          the URL to fetch
     * @param responseType the class of the response type
     * @param headers      additional headers to include
     * @param <T>          the response type
     * @return the deserialized response
     * @throws IcecatApiException if the request fails
     */
    public <T> T get(String url, Class<T> responseType, Map<String, String> headers) {
        return executeWithRetry(() -> {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(URI.create(url));

            if (headers != null) {
                headers.forEach(request::header);
            }

            return request.retrieve().body(responseType);
        }, url);
    }

    /**
     * Performs a POST request with form data and returns the response.
     *
     * @param url          the URL to post to
     * @param formData     the form data as a map
     * @param responseType the class of the response type
     * @param <T>          the response type
     * @return the deserialized response
     * @throws IcecatApiException if the request fails
     */
    public <T> T postForm(String url, Map<String, String> formData, Class<T> responseType) {
        return executeWithRetry(() -> {
            StringBuilder body = new StringBuilder();
            formData.forEach((k, v) -> {
                if (body.length() > 0) {
                    body.append("&");
                }
                body.append(k).append("=").append(v);
            });

            return restClient.post()
                    .uri(URI.create(url))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body.toString())
                    .retrieve()
                    .body(responseType);
        }, url);
    }

    /**
     * Creates a Basic Authentication header value.
     *
     * @param user     the username
     * @param password the password
     * @return the Basic Auth header value
     */
    private String createBasicAuthHeader(String user, String password) {
        if (user == null || password == null) {
            return null;
        }
        String credentials = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Downloads a file with retry logic.
     *
     * @param url        the URL to download from
     * @param destFile   the destination file
     * @param authHeader the authorization header (can be null)
     */
    private void downloadWithRetry(String url, File destFile, String authHeader) {
        executeWithRetry(() -> {
            restClient.method(HttpMethod.POST)
                    .uri(URI.create(url))
                    .headers(headers -> {
                        if (authHeader != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
                        }
                    })
                    .exchange((request, response) -> {
                        handleErrorResponse(response, url);
                        try (FileOutputStream fos = new FileOutputStream(destFile)) {
                            StreamUtils.copy(response.getBody(), fos);
                        }
                        return null;
                    });
            return null;
        }, url);
    }

    /**
     * Executes a request with retry logic and exponential backoff.
     *
     * @param operation the operation to execute
     * @param url       the URL for error reporting
     * @param <T>       the return type
     * @return the operation result
     * @throws IcecatApiException if all retries fail
     */
    private <T> T executeWithRetry(RequestOperation<T> operation, String url) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (IcecatRateLimitException e) {
                LOGGER.warn("Rate limited on attempt {} for {}, waiting {} seconds",
                        attempt + 1, url, e.getRetryAfter().getSeconds());
                sleep(e.getRetryAfter().toMillis());
                lastException = e;
            } catch (IcecatAuthenticationException | IcecatResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long delay = attempt < RETRY_DELAYS_MS.length ? RETRY_DELAYS_MS[attempt] : RETRY_DELAYS_MS[RETRY_DELAYS_MS.length - 1];
                    LOGGER.warn("Request failed on attempt {} for {}, retrying in {}ms: {}",
                            attempt + 1, url, delay, e.getMessage());
                    sleep(delay);
                }
            }
        }

        throw new IcecatApiException("Request failed after " + (maxRetries + 1) + " attempts: " + url,
                lastException, url);
    }

    /**
     * Handles error responses from the API.
     *
     * @param response the HTTP response
     * @param url      the URL for error reporting
     * @throws IOException if reading the response fails
     */
    private void handleErrorResponse(ClientHttpResponse response, String url) throws IOException {
        HttpStatusCode status = response.getStatusCode();

        if (status.is2xxSuccessful()) {
            return;
        }

        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        int statusCode = status.value();

        if (statusCode == 429) {
            String retryAfterHeader = response.getHeaders().getFirst("Retry-After");
            long retryAfterSeconds = retryAfterHeader != null ? Long.parseLong(retryAfterHeader) : 1;
            throw new IcecatRateLimitException(url, Duration.ofSeconds(retryAfterSeconds));
        }

        if (statusCode == 401 || statusCode == 403) {
            throw new IcecatAuthenticationException(
                    "Authentication failed: " + status,
                    statusCode, body, url);
        }

        if (statusCode == 400 || statusCode == 404) {
            throw new IcecatResourceNotFoundException(
                    "Resource not found: " + url,
                    null, url);
        }

        throw new IcecatApiException("HTTP error: " + status, statusCode, body, url);
    }

    /**
     * Decompresses a GZIP file.
     *
     * @param gzipFile the source GZIP file
     * @param destFile the destination file
     * @throws IOException if decompression fails
     */
    private void decompressGzip(File gzipFile, File destFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(gzipFile);
             GZIPInputStream gis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
        LOGGER.debug("Decompressed {} to {}", gzipFile.getName(), destFile.getName());
    }

    /**
     * Sleeps for the specified duration.
     *
     * @param millis the duration in milliseconds
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IcecatApiException("Interrupted while waiting for retry", e);
        }
    }

    /**
     * Clears the cached file for a given URL.
     *
     * @param url the URL whose cache to clear
     */
    public void clearCache(String url) {
        String key = IdHelper.getHashedName(url);
        File cachedFile = new File(cacheDirectory + File.separator + key);
        if (cachedFile.exists()) {
            FileUtils.deleteQuietly(cachedFile);
            LOGGER.info("Cleared cache for {}", url);
        }
    }

    /**
     * Functional interface for request operations.
     *
     * @param <T> the return type
     */
    @FunctionalInterface
    private interface RequestOperation<T> {
        T execute() throws Exception;
    }
}
