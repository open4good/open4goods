package org.open4goods.services.urlfetching.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for URL fetching.
 * <p>
 * This configuration holds a map of domain-specific configurations. Each domain
 * key maps to a DomainConfig which defines the user agent, fetching strategy,
 * custom headers, timeout, retry policy, and optional proxy settings.
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "urlfetcher")
public class UrlFetcherConfig {

    /**
     * Map of domain configurations.
     */
    private Map<String, DomainConfig> domains = new HashMap<>();

    public Map<String, DomainConfig> getDomains() {
        return domains;
    }

    public void setDomains(Map<String, DomainConfig> domains) {
        this.domains = domains;
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
         * The fetching strategy to apply (HTTP, PROXIFIED, SELENIUM).
         */
        private FetchStrategy strategy;

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
         * Proxy configuration details for proxified fetcher.
         */
        private ProxyConfig proxy;

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

        public ProxyConfig getProxy() {
            return proxy;
        }

        public void setProxy(ProxyConfig proxy) {
            this.proxy = proxy;
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
         * Proxy host.
         */
        private String host;
        /**
         * Proxy port.
         */
        private int port;

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
    }
}
