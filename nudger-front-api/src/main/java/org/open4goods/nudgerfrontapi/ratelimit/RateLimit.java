package org.open4goods.nudgerfrontapi.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare rate limited endpoints.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** Maximum bucket capacity */
    long capacity();

    /** Number of tokens refilled each interval (defaults to capacity). */
    long refillTokens() default -1;

    /** Interval in seconds after which tokens are refilled. */
    long refillSeconds();
}
