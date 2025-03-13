package org.open4goods.services.captcha;

/**
 * Exception thrown when hCaptcha verification fails.
 */
public class CaptchaVerificationException extends RuntimeException {

    /**
     * Constructs a new CaptchaVerificationException with the specified detail message.
     *
     * @param message the detail message
     */
    public CaptchaVerificationException(String message) {
        super(message);
    }

    /**
     * Constructs a new CaptchaVerificationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public CaptchaVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
