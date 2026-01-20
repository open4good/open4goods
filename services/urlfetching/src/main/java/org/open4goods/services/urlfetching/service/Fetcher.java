package org.open4goods.services.urlfetching.service;

import java.util.Map;

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
    
    /**
     * Asynchronously fetches the content from the given URL with custom headers.
     * 
     * @param url the URL to fetch
     * @param headers the custom headers to add to the request
     * @return a CompletableFuture of {@link FetchResponse}
     */
    default CompletableFuture<FetchResponse> fetchUrlAsync(String url, Map<String, String> headers) {
    	return fetchUrlAsync(url);
    }
    
    
    FetchResponse fetchUrlSync(String url) throws IOException, InterruptedException ;
    	
}
