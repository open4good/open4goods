package org.open4goods.icecat.services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.open4goods.icecat.client.IcecatRetailerApiClient;
import org.open4goods.icecat.model.retailer.RetailerCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing Icecat retailer categories.
 * Provides caching and lookup functionality for categories from the Retailer API.
 */
public class RetailerCategoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetailerCategoryService.class);

    /**
     * Default cache duration: 24 hours.
     */
    private static final Duration DEFAULT_CACHE_DURATION = Duration.ofHours(24);

    private final IcecatRetailerApiClient retailerApiClient;
    private final Duration cacheDuration;
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    // Cache
    private List<RetailerCategory> cachedCategories = new ArrayList<>();
    private Map<Long, RetailerCategory> categoriesById = new HashMap<>();
    private Map<Long, List<RetailerCategory>> categoriesByParentId = new HashMap<>();
    private Instant cacheExpiry = Instant.MIN;

    /**
     * Constructor with default cache duration.
     *
     * @param retailerApiClient the Icecat Retailer API client
     */
    public RetailerCategoryService(IcecatRetailerApiClient retailerApiClient) {
        this(retailerApiClient, DEFAULT_CACHE_DURATION);
    }

    /**
     * Constructor with custom cache duration.
     *
     * @param retailerApiClient the Icecat Retailer API client
     * @param cacheDuration     the cache duration
     */
    public RetailerCategoryService(IcecatRetailerApiClient retailerApiClient, Duration cacheDuration) {
        this.retailerApiClient = retailerApiClient;
        this.cacheDuration = cacheDuration;
    }

    /**
     * Gets all categories, using cache if available.
     *
     * @return list of all retailer categories
     */
    public List<RetailerCategory> getAllCategories() {
        ensureCacheValid();

        cacheLock.readLock().lock();
        try {
            return new ArrayList<>(cachedCategories);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Gets a category by its ID.
     *
     * @param categoryId the category ID
     * @return the category, or null if not found
     */
    public RetailerCategory getCategoryById(Long categoryId) {
        ensureCacheValid();

        cacheLock.readLock().lock();
        try {
            return categoriesById.get(categoryId);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Gets child categories for a given parent category ID.
     *
     * @param parentCategoryId the parent category ID (null for root categories)
     * @return list of child categories
     */
    public List<RetailerCategory> getChildCategories(Long parentCategoryId) {
        ensureCacheValid();

        cacheLock.readLock().lock();
        try {
            List<RetailerCategory> children = categoriesByParentId.get(parentCategoryId);
            return children != null ? new ArrayList<>(children) : new ArrayList<>();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Gets root categories (categories with no parent).
     *
     * @return list of root categories
     */
    public List<RetailerCategory> getRootCategories() {
        return getChildCategories(null);
    }

    /**
     * Gets categories at a specific level in the hierarchy.
     *
     * @param level the hierarchy level (0 = root)
     * @return list of categories at the specified level
     */
    public List<RetailerCategory> getCategoriesByLevel(int level) {
        ensureCacheValid();

        cacheLock.readLock().lock();
        try {
            return cachedCategories.stream()
                    .filter(c -> c.getLevel() != null && c.getLevel() == level)
                    .toList();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Searches categories by name (case-insensitive partial match).
     *
     * @param nameQuery the search query
     * @return list of matching categories
     */
    public List<RetailerCategory> searchByName(String nameQuery) {
        if (nameQuery == null || nameQuery.isBlank()) {
            return getAllCategories();
        }

        ensureCacheValid();
        String lowerQuery = nameQuery.toLowerCase();

        cacheLock.readLock().lock();
        try {
            return cachedCategories.stream()
                    .filter(c -> c.getCategoryName() != null
                            && c.getCategoryName().toLowerCase().contains(lowerQuery))
                    .toList();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Forces a refresh of the category cache.
     */
    public void refreshCache() {
        cacheLock.writeLock().lock();
        try {
            LOGGER.info("Refreshing retailer categories cache");
            loadCategories();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Clears the category cache.
     */
    public void clearCache() {
        cacheLock.writeLock().lock();
        try {
            cachedCategories.clear();
            categoriesById.clear();
            categoriesByParentId.clear();
            cacheExpiry = Instant.MIN;
            LOGGER.info("Retailer categories cache cleared");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Ensures the cache is valid, refreshing if necessary.
     */
    private void ensureCacheValid() {
        cacheLock.readLock().lock();
        boolean needsRefresh = Instant.now().isAfter(cacheExpiry);
        cacheLock.readLock().unlock();

        if (needsRefresh) {
            cacheLock.writeLock().lock();
            try {
                // Double-check after acquiring write lock
                if (Instant.now().isAfter(cacheExpiry)) {
                    loadCategories();
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
    }

    /**
     * Loads categories from the API and populates the cache.
     * Must be called with write lock held.
     */
    private void loadCategories() {
        try {
            List<RetailerCategory> categories = retailerApiClient.getCategories();

            cachedCategories = new ArrayList<>(categories);
            categoriesById.clear();
            categoriesByParentId.clear();

            for (RetailerCategory category : categories) {
                categoriesById.put(category.getCategoryId(), category);

                Long parentId = category.getParentCategoryId();
                categoriesByParentId
                        .computeIfAbsent(parentId, k -> new ArrayList<>())
                        .add(category);
            }

            cacheExpiry = Instant.now().plus(cacheDuration);

            LOGGER.info("Loaded {} retailer categories, cache expires at {}",
                    categories.size(), cacheExpiry);

        } catch (Exception e) {
            LOGGER.error("Failed to load retailer categories", e);
            // Keep existing cache if available, but mark as expired
            if (!cachedCategories.isEmpty()) {
                LOGGER.warn("Using stale category cache due to refresh failure");
            }
        }
    }

    /**
     * Gets the total number of cached categories.
     *
     * @return the category count
     */
    public int getCategoryCount() {
        cacheLock.readLock().lock();
        try {
            return cachedCategories.size();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Checks if the client is properly configured.
     *
     * @return true if the retailer API client is configured
     */
    public boolean isConfigured() {
        return retailerApiClient.isConfigured();
    }
}
