package org.open4goods.icecat.client.exception;

/**
 * Exception thrown when authentication to Icecat API fails.
 * This can happen with invalid credentials (401) or forbidden access (403).
 */
public class IcecatAuthenticationException extends IcecatApiException {

    /**
     * Constructor for IcecatAuthenticationException.
     *
     * @param message    the error message
     * @param statusCode the HTTP status code (401 or 403)
     * @param url        the URL that caused the authentication error
     */
    public IcecatAuthenticationException(String message, int statusCode, String url) {
        super(message, statusCode, null, url);
    }

    /**
     * Constructor for IcecatAuthenticationException with error body.
     *
     * @param message    the error message
     * @param statusCode the HTTP status code (401 or 403)
     * @param errorBody  the error response body
     * @param url        the URL that caused the authentication error
     */
    public IcecatAuthenticationException(String message, int statusCode, String errorBody, String url) {
        super(message, statusCode, errorBody, url);
    }
}
