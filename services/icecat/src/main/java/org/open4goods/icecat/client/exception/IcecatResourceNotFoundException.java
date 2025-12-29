package org.open4goods.icecat.client.exception;

/**
 * Exception thrown when a requested resource is not found in Icecat (HTTP 404 or 400 for products).
 */
public class IcecatResourceNotFoundException extends IcecatApiException {

    private final String resourceId;

    /**
     * Constructor for IcecatResourceNotFoundException.
     *
     * @param message    the error message
     * @param resourceId the identifier of the resource that was not found
     * @param url        the URL that returned 404
     */
    public IcecatResourceNotFoundException(String message, String resourceId, String url) {
        super(message, 404, null, url);
        this.resourceId = resourceId;
    }

    /**
     * Returns the identifier of the resource that was not found.
     *
     * @return the resource identifier
     */
    public String getResourceId() {
        return resourceId;
    }
}
