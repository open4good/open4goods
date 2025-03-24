package org.open4goods.services.favicon.service;

import org.open4goods.services.favicon.dto.FaviconResponse;

/**
 * Service interface for retrieving favicons for a given URL.
 */
public interface FaviconService {
    /**
     * Checks if a favicon is available for the given URL.
     * @param url the URL to check.
     * @return true if a favicon exists, false otherwise.
     */
    boolean hasFavicon(String url);

    /**
     * Retrieves the favicon for the given URL.
     * @param url the URL for which to retrieve the favicon.
     * @return a FaviconResponse containing the favicon data.
     */
    FaviconResponse getFavicon(String url);

    /**
     * Clears the favicon cache.
     */
    void clearCache();
}
