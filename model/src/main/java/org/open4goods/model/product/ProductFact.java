package org.open4goods.model.product;

import java.util.Objects;

/**
 * Stores a web fact used as retrieval context for AI review generation.
 */
public class ProductFact {

    private String url;
    private String markdown;
    private String language;
    private long fetchedAt;
    private String fetchStrategy;
    private Integer tokenCount;
    private String contentHash;

    public ProductFact() {
    }

    public ProductFact(String url, String markdown, String language, long fetchedAt, String fetchStrategy,
            Integer tokenCount, String contentHash) {
        this.url = url;
        this.markdown = markdown;
        this.language = language;
        this.fetchedAt = fetchedAt;
        this.fetchStrategy = fetchStrategy;
        this.tokenCount = tokenCount;
        this.contentHash = contentHash;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductFact that)) {
            return false;
        }
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
