package org.open4goods.icecat.client.exception;

/**
 * Base exception for all Icecat API errors.
 * Provides structured error information including HTTP status code and response body.
 */
public class IcecatApiException extends RuntimeException {

    private final int statusCode;
    private final String errorBody;
    private final String url;

    /**
     * Constructor for IcecatApiException.
     *
     * @param message    the error message
     * @param statusCode the HTTP status code
     * @param errorBody  the error response body
     * @param url        the URL that caused the error
     */
    public IcecatApiException(String message, int statusCode, String errorBody, String url) {
        super(message);
        this.statusCode = statusCode;
        this.errorBody = errorBody;
        this.url = url;
    }

    /**
     * Constructor for IcecatApiException with cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     * @param url     the URL that caused the error
     */
    public IcecatApiException(String message, Throwable cause, String url) {
        super(message, cause);
        this.statusCode = -1;
        this.errorBody = null;
        this.url = url;
    }

    /**
     * Constructor for IcecatApiException without URL.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public IcecatApiException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorBody() {
        return errorBody;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(": ").append(getMessage());
        if (statusCode > 0) {
            sb.append(" [status=").append(statusCode).append("]");
        }
        if (url != null) {
            sb.append(" [url=").append(url).append("]");
        }
        return sb.toString();
    }
}
