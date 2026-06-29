package org.open4goods.model.product;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/**
 * One canonical source URL tracked by the product enrichment pipeline.
 */
public class ProductSourceUrl {

    private String url;
    private String canonicalUrl;
    private String host;
    private String title;
    private String snippet;
    private Integer serpRank;
    private ProductSourceProvider provider = ProductSourceProvider.UNKNOWN;
    private ProductSourceQuery query;
    private ProductSourceUrlType type = ProductSourceUrlType.UNKNOWN;
    private ProductSourceUrlStatus status = ProductSourceUrlStatus.DISCOVERED;
    private long discoveredAt;
    private long fetchedAt;
    private String fetchStrategy;
    private Integer statusCode;
    private String markdown;
    private Integer tokenCount;
    private String contentHash;
    private String rejectionReason;

    public ProductSourceUrl() {
    }

    public ProductSourceUrl(String url) {
        setUrl(url);
        this.discoveredAt = System.currentTimeMillis();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        if (canonicalUrl == null || canonicalUrl.isBlank()) {
            this.canonicalUrl = canonicalize(url);
        }
        if (host == null || host.isBlank()) {
            this.host = extractHost(url);
        }
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public void setCanonicalUrl(String canonicalUrl) {
        this.canonicalUrl = canonicalize(canonicalUrl);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public Integer getSerpRank() {
        return serpRank;
    }

    public void setSerpRank(Integer serpRank) {
        this.serpRank = serpRank;
    }

    public ProductSourceProvider getProvider() {
        return provider;
    }

    public void setProvider(ProductSourceProvider provider) {
        this.provider = provider == null ? ProductSourceProvider.UNKNOWN : provider;
    }

    public ProductSourceQuery getQuery() {
        return query;
    }

    public void setQuery(ProductSourceQuery query) {
        this.query = query;
    }

    public ProductSourceUrlType getType() {
        return type;
    }

    public void setType(ProductSourceUrlType type) {
        this.type = type == null ? ProductSourceUrlType.UNKNOWN : type;
    }

    public ProductSourceUrlStatus getStatus() {
        return status;
    }

    public void setStatus(ProductSourceUrlStatus status) {
        this.status = status == null ? ProductSourceUrlStatus.DISCOVERED : status;
    }

    public long getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(long discoveredAt) {
        this.discoveredAt = discoveredAt;
    }

    public long getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(long fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public String getFetchStrategy() {
        return fetchStrategy;
    }

    public void setFetchStrategy(String fetchStrategy) {
        this.fetchStrategy = fetchStrategy;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String identityKey() {
        if (canonicalUrl != null && !canonicalUrl.isBlank()) {
            return canonicalUrl;
        }
        return canonicalize(url);
    }

    public void mergeFrom(ProductSourceUrl incoming) {
        if (incoming == null) {
            return;
        }
        if (isBlank(url)) setUrl(incoming.getUrl());
        if (isBlank(canonicalUrl)) setCanonicalUrl(incoming.getCanonicalUrl());
        if (isBlank(host)) host = incoming.getHost();
        if (isBlank(title)) title = incoming.getTitle();
        if (isBlank(snippet)) snippet = incoming.getSnippet();
        if (serpRank == null || (incoming.getSerpRank() != null && incoming.getSerpRank() < serpRank)) {
            serpRank = incoming.getSerpRank();
        }
        if (provider == ProductSourceProvider.UNKNOWN) provider = incoming.getProvider();
        if (query == null) query = incoming.getQuery();
        if (type == ProductSourceUrlType.UNKNOWN) type = incoming.getType();
        if (incoming.getStatus() != null && incoming.getStatus().ordinal() > status.ordinal()) {
            status = incoming.getStatus();
        }
        if (discoveredAt == 0L) discoveredAt = incoming.getDiscoveredAt();
        if (fetchedAt == 0L) fetchedAt = incoming.getFetchedAt();
        if (isBlank(fetchStrategy)) fetchStrategy = incoming.getFetchStrategy();
        if (statusCode == null) statusCode = incoming.getStatusCode();
        if (isBlank(markdown)) markdown = incoming.getMarkdown();
        if (tokenCount == null) tokenCount = incoming.getTokenCount();
        if (isBlank(contentHash)) contentHash = incoming.getContentHash();
        if (isBlank(rejectionReason)) rejectionReason = incoming.getRejectionReason();
    }

    private static String canonicalize(String value) {
        if (isBlank(value)) {
            return value;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
            return new URI(scheme, uri.getUserInfo(), host, uri.getPort(), path, null, null).toString();
        } catch (Exception e) {
            return value.trim();
        }
    }

    private static String extractHost(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            String host = URI.create(value.trim()).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProductSourceUrl that)) {
            return false;
        }
        return Objects.equals(identityKey(), that.identityKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityKey());
    }
}
