package org.open4goods.nudgerfrontapi.service.css;

/**
 * Failure of a call to the CSS API (GOU-2081), carrying a short reason used as metric tag and log field.
 */
public class CssClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String reason;

    /**
     * @param reason  short, low-cardinality reason (for example {@code RESOURCE_EXHAUSTED})
     * @param message detail for the logs
     * @param cause   original failure, may be null
     */
    public CssClientException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    /**
     * @return the short reason of the failure
     */
    public String reason() {
        return reason;
    }
}
