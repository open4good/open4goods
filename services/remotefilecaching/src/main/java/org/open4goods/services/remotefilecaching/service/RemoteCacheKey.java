package org.open4goods.services.remotefilecaching.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Produces opaque, deterministic names for remotely retrieved cache entries.
 *
 * <p>A cache key is derived from the complete request URL so that signed URLs and
 * distinct query parameters do not collide. The URL itself must never become a
 * filename because query values can contain credentials.</p>
 */
public final class RemoteCacheKey {

    private static final String SHA_256 = "SHA-256";

    private RemoteCacheKey() {
    }

    /**
     * Returns a lowercase hexadecimal SHA-256 digest for a remote resource URL.
     *
     * @param url complete remote resource URL
     * @return opaque filename-safe cache key
     */
    public static String fromUrl(final String url) {
        Objects.requireNonNull(url, "url must not be null");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(SHA_256)
                    .digest(url.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available on the Java runtime", exception);
        }
    }

    /**
     * Returns the opaque temporary filename for a remote resource URL.
     *
     * @param url complete remote resource URL
     * @return temporary cache filename with no URL-derived text
     */
    public static String temporaryFromUrl(final String url) {
        return "tmp-" + fromUrl(url);
    }
}
