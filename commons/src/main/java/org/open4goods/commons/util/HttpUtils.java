package org.open4goods.commons.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility methods for HTTP query handling and safe logging.
 */
public final class HttpUtils {

    private HttpUtils() {
    }

    /**
     * Returns {@code true} when the input is {@code null}, empty, or whitespace-only.
     *
     * @param value input string
     * @return {@code true} when blank
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * URL-encodes a value using UTF-8.
     *
     * @param value input value
     * @return encoded value, never {@code null}
     */
    public static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * Masks the {@code key=} query parameter value in an URL for safe logging.
     *
     * @param endpoint endpoint URL
     * @return endpoint with masked key parameter
     */
    public static String safeEndpointForLogs(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        return endpoint.replaceAll("(key=)([^&]+)", "$1****");
    }

    /**
     * Normalizes URLs by adding a protocol when missing and trimming whitespace.
     *
     * @param url input URL
     * @return normalized URL or empty string for blank input
     */
    public static String normalizeUrl(String url) {
        if (isBlank(url)) {
            return "";
        }

        String normalized = url.trim();
        if (!normalized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            normalized = "https://" + normalized;
        }

        return normalized;
    }
}
