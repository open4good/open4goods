package org.open4goods.services.urlfetching.service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.open4goods.services.urlfetching.dto.FetchResponse;

/**
 * Interface for URL fetching strategies.
 */
public interface Fetcher {

    /**
     * Asynchronously fetches the content from the given URL.
     *
     * @param url the URL to fetch
     * @return a CompletableFuture of {@link FetchResponse}
     */
    CompletableFuture<FetchResponse> fetchUrlAsync(String url);
    
    
    FetchResponse fetchUrlSync(String url) throws IOException, InterruptedException ;
    	
}
