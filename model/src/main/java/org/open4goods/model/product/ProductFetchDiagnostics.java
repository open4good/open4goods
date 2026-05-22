package org.open4goods.model.product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the latest review-source fetching diagnostics for a product.
 */
public class ProductFetchDiagnostics {

    private long fetchedAt;
    private int sourceCount;
    private int totalTokens;
    private String resultQuality;
    private List<String> searchedQueries = new ArrayList<>();
    private List<String> acceptedUrls = new ArrayList<>();
    private Map<String, String> rejectedUrls = new LinkedHashMap<>();
    private Map<String, String> enrichmentStatus = new LinkedHashMap<>();

    public long getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(long fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getResultQuality() {
        return resultQuality;
    }

    public void setResultQuality(String resultQuality) {
        this.resultQuality = resultQuality;
    }

    public List<String> getSearchedQueries() {
        return searchedQueries;
    }

    public void setSearchedQueries(List<String> searchedQueries) {
        this.searchedQueries = searchedQueries == null ? new ArrayList<>() : new ArrayList<>(searchedQueries);
    }

    public List<String> getAcceptedUrls() {
        return acceptedUrls;
    }

    public void setAcceptedUrls(List<String> acceptedUrls) {
        this.acceptedUrls = acceptedUrls == null ? new ArrayList<>() : new ArrayList<>(acceptedUrls);
    }

    public Map<String, String> getRejectedUrls() {
        return rejectedUrls;
    }

    public void setRejectedUrls(Map<String, String> rejectedUrls) {
        this.rejectedUrls = rejectedUrls == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rejectedUrls);
    }

    public Map<String, String> getEnrichmentStatus() {
        return enrichmentStatus;
    }

    public void setEnrichmentStatus(Map<String, String> enrichmentStatus) {
        this.enrichmentStatus = enrichmentStatus == null ? new LinkedHashMap<>() : new LinkedHashMap<>(enrichmentStatus);
    }
}
