package org.open4goods.services.urlfetching.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for URL fetching.
 * <p>
 * This configuration holds global URL fetching options and a map of
 * domain-specific configurations. Each domain key maps to a {@link DomainConfig}
 * which defines the user agent, fetching strategy, custom headers, timeout, and
 * retry policy.
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "urlfetcher")
public class UrlFetcherConfig {

    /**
     * Map of domain configurations.
     */
    private Map<String, DomainConfig> domains = new HashMap<>();

    /**
     * Thread pool size for asynchronous fetching.
     */
    private int threadPoolSize = 10;

    /**
     * Global proxy configuration used by proxified fetches and Playwright proxy replay.
     */
    private ProxyConfig proxy;

    /**
     * When true, a failed or empty Playwright fetch is replayed with the configured proxy.
     */
    private boolean playwrightProxyFallbackEnabled = false;

    /**
     * When true, Playwright starts every fetch through the configured proxy.
     */
    private boolean playwrightProxyRequired = false;

    /**
     * Configuration for recording mode.
     * <p>
     * Note: This configuration is intended for test purposes only. Recorded responses will
     * only be played back via the {@code UrlFetchingServiceMock} and are not used in production.
     * </p>
     */
    private RecordConfig record = new RecordConfig();

    public Map<String, DomainConfig> getDomains() {
        return domains;
    }

    public void setDomains(Map<String, DomainConfig> domains) {
        this.domains = domains == null ? new HashMap<>() : domains;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public ProxyConfig getProxy() {
        return proxy;
    }

    public void setProxy(ProxyConfig proxy) {
        this.proxy = proxy;
    }

    public boolean isPlaywrightProxyFallbackEnabled() {
        return playwrightProxyFallbackEnabled;
    }

    public void setPlaywrightProxyFallbackEnabled(boolean playwrightProxyFallbackEnabled) {
        this.playwrightProxyFallbackEnabled = playwrightProxyFallbackEnabled;
    }

    public boolean isPlaywrightProxyRequired() {
        return playwrightProxyRequired;
    }

    public void setPlaywrightProxyRequired(boolean playwrightProxyRequired) {
        this.playwrightProxyRequired = playwrightProxyRequired;
    }

    public RecordConfig getRecord() {
        return record;
    }

    public void setRecord(RecordConfig record) {
        this.record = record;
    }

    /**
     * Domain-specific configuration options.
     */
    public static class DomainConfig {

        /**
         * The user agent string to be used when fetching URLs.
         */
        private String userAgent;

        /**
         * The fetching strategy to apply (HTTP, PROXIFIED, PLAYWRIGHT).
         */
        private FetchStrategy strategy = FetchStrategy.PLAYWRIGHT;

        /**
         * Custom headers to be added to the request.
         */
        private Map<String, String> customHeaders;

        /**
         * Request timeout duration in milliseconds.
         */
        private long timeout = 5000;

        /**
         * Retry policy configuration.
         */
        private RetryPolicy retryPolicy;

        /**
         * Playwright browser channel (e.g. "chrome", "chrome-beta", "msedge").
         * <p>
         * Defaults to "chrome" so Playwright launches installed branded Chrome instead of
         * bundled Chromium — branded Chrome has a different TLS / JS fingerprint that is
         * harder for anti-bot WAFs (Datadome, Akamai, PerimeterX) to flag. Requires the
         * channel to be installed on the host (e.g. {@code npx playwright install chrome}).
         * When the channel is missing at launch time, the fetcher falls back to bundled
         * Chromium and logs a warning once per JVM.
         * </p>
         * <p>Set to an empty string or {@code null} to force bundled Chromium.</p>
         */
        private String browserChannel = "chrome";

        // Getters and setters

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public FetchStrategy getStrategy() {
            return strategy;
        }

        public void setStrategy(FetchStrategy strategy) {
            this.strategy = strategy;
        }

        public Map<String, String> getCustomHeaders() {
            return customHeaders;
        }

        public void setCustomHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public RetryPolicy getRetryPolicy() {
            return retryPolicy;
        }

        public void setRetryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
        }

        public String getBrowserChannel() {
            return browserChannel;
        }

        public void setBrowserChannel(String browserChannel) {
            this.browserChannel = browserChannel;
        }
    }

    /**
     * Configuration for retry policies.
     */
    public static class RetryPolicy {
        /**
         * Maximum number of retries.
         */
        private int maxRetries = 0;
        /**
         * Delay between retries in milliseconds.
         */
        private long delayBetweenRetries = 1000;

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getDelayBetweenRetries() {
            return delayBetweenRetries;
        }

        public void setDelayBetweenRetries(long delayBetweenRetries) {
            this.delayBetweenRetries = delayBetweenRetries;
        }
    }

    /**
     * Configuration for proxy settings.
     */
    public static class ProxyConfig {
        /**
         * Proxy scheme.
         */
        private String scheme = "http";
        /**
         * Proxy host.
         */
        private String host;
        /**
         * Proxy port.
         */
        private int port;
        /**
         * Optional proxy username.
         */
        private String username;
        /**
         * Optional proxy password.
         */
        private String password;

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme == null || scheme.isBlank() ? "http" : scheme;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Configuration for recording mode.
     * <p>
     * Note: These properties are used exclusively in test configurations (e.g., via {@code UrlFetchingServiceMock})
     * and are not applied in production.
     * </p>
     */
    public static class RecordConfig {
        /**
         * Flag to enable or disable recording mode.
         */
        private boolean enabled = false;
        /**
         * Destination folder for storing/retrieving recorded responses.
         */
        private String destinationFolder = "src/test/resources/urlfetching/mocks";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDestinationFolder() {
            return destinationFolder;
        }

        public void setDestinationFolder(String destinationFolder) {
            this.destinationFolder = destinationFolder;
        }
    }

}
