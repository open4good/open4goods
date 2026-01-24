package org.open4goods.nudgerfrontapi.service;

/**
 * Client interface used to publish URLs to the Google Indexing API.
 */
public interface GoogleIndexingClient {

    /**
     * Publish a URL to the Indexing API.
     *
     * @param url absolute URL to publish
     * @return {@code true} when the request succeeds
     */
    boolean publish(String url);
}
