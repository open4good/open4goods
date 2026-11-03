package org.open4goods.api.services.metriks.affiliation;

/**
 * Failure of an affiliation network report call (HTTP error, timeout, unreadable or unexpected response).
 * <p>
 * The message is shown as the Metriks {@code errorMessage}: it names the network and the failing step and
 * never carries a credential, a request URL (the Effiliation key travels in the query string) or a raw
 * response body. No cause is attached, so that no lower-level message can leak through a stack trace.
 */
public class AffiliationReportException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @param message safe, human-readable message (no secret, no URL, no response body)
     */
    public AffiliationReportException(String message) {
        super(message);
    }
}
