package org.open4goods.urlfetching.service;

import java.util.concurrent.CompletableFuture;
import org.open4goods.urlfetching.dto.FetchResponse;

/**
 * Interface for URL fetching strategies.
 */
public interface Fetcher {

    /**
     * Asynchronously fetches the content from the given URL.
     *
     * @param url the URL to fetch
     * @return a CompletableFuture of FetchResponse
     */
    CompletableFuture<FetchResponse> fetchUrl(String url);
}
