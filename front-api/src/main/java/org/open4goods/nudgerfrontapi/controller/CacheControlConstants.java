package org.open4goods.nudgerfrontapi.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;

/**
 * Shared constants for cache control
 */
public class CacheControlConstants {
         public static final CacheControl FIFTEEN_MINUTES_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofMinutes(15)).cachePublic();
         public static final CacheControl ONE_HOUR_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();
         public static final CacheControl FIVE_MINUTES_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();
         public static final CacheControl PRIVATE_NO_STORE_CACHE = CacheControl.noStore().cachePrivate();

}
