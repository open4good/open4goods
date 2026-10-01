package org.open4goods.nudgerfrontapi.service.exception;

/**
 * Raised when the Google SSO endpoint is called while the feature flag is off.
 */
public class GoogleSsoDisabledException extends RuntimeException {

    public GoogleSsoDisabledException() {
        super("Google SSO is disabled");
    }
}
