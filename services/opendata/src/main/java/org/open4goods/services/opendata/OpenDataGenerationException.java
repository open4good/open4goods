package org.open4goods.services.opendata;

/**
 * A generation pass is refused (disk space, row guard) or failed; the published files and manifests stay untouched.
 */
public class OpenDataGenerationException extends Exception {

    private static final long serialVersionUID = 1L;

    public OpenDataGenerationException(String message) {
        super(message);
    }

    public OpenDataGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
