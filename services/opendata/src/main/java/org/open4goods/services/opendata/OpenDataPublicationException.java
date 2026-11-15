package org.open4goods.services.opendata;

/**
 * A publication on R2 is refused (budget guard, local file inconsistent with its manifest) or failed (upload, read
 * back, sha256). Before the copy to the public keys, the published objects stay untouched.
 */
public class OpenDataPublicationException extends Exception {

    private static final long serialVersionUID = 1L;

    public OpenDataPublicationException(String message) {
        super(message);
    }

    public OpenDataPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
