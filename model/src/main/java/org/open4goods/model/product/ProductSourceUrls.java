package org.open4goods.model.product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Durable source URL inventory for product enrichment.
 */
public class ProductSourceUrls {

    public static final int DEFAULT_MAX_STORED_URLS = 20;

    private List<ProductSourceUrl> urls = new ArrayList<>();
    private int maxStoredUrls = DEFAULT_MAX_STORED_URLS;

    public ProductSourceUrls() {
    }

    public List<ProductSourceUrl> getUrls() {
        return urls;
    }

    public void setUrls(List<ProductSourceUrl> urls) {
        this.urls = urls == null ? new ArrayList<>() : deduplicate(urls, maxStoredUrls);
    }

    public int getMaxStoredUrls() {
        return maxStoredUrls;
    }

    public void setMaxStoredUrls(int maxStoredUrls) {
        this.maxStoredUrls = Math.max(1, maxStoredUrls);
        this.urls = deduplicate(this.urls, this.maxStoredUrls);
    }

    public void add(ProductSourceUrl sourceUrl) {
        if (sourceUrl == null || sourceUrl.identityKey() == null || sourceUrl.identityKey().isBlank()) {
            return;
        }
        List<ProductSourceUrl> merged = new ArrayList<>(urls);
        merged.add(sourceUrl);
        urls = deduplicate(merged, maxStoredUrls);
    }

    public List<ProductSourceUrl> fetched() {
        return urls.stream()
                .filter(sourceUrl -> sourceUrl.getStatus() == ProductSourceUrlStatus.FETCHED)
                .filter(sourceUrl -> sourceUrl.getMarkdown() != null && !sourceUrl.getMarkdown().isBlank())
                .toList();
    }

    private static List<ProductSourceUrl> deduplicate(List<ProductSourceUrl> input, int limit) {
        Map<String, ProductSourceUrl> byKey = new LinkedHashMap<>();
        for (ProductSourceUrl sourceUrl : input) {
            if (sourceUrl == null || sourceUrl.identityKey() == null || sourceUrl.identityKey().isBlank()) {
                continue;
            }
            byKey.compute(sourceUrl.identityKey(), (ignored, existing) -> {
                if (existing == null) {
                    return sourceUrl;
                }
                existing.mergeFrom(sourceUrl);
                return existing;
            });
        }
        return byKey.values().stream()
                .sorted(Comparator
                        .comparing((ProductSourceUrl url) -> url.getSerpRank() == null ? Integer.MAX_VALUE : url.getSerpRank())
                        .thenComparing(ProductSourceUrl::identityKey))
                .limit(limit)
                .toList();
    }
}
