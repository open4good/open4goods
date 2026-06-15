package org.open4goods.b2bapi.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

/**
 * Generates opaque Product Data API keys and their storage derivations.
 */
@Service
public class ApiKeySecretGenerator {

    private static final String PREFIX = "pdapi_";
    private static final int RANDOM_BYTES = 32;
    private static final int DISPLAY_CHARS = 8;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a new clear key, display prefix, and SHA-256 hash.
     *
     * @return generated key material
     */
    public ApiKeySecret generate() {
        final byte[] random = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(random);
        final String suffix = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        final String clearKey = PREFIX + suffix;
        return new ApiKeySecret(clearKey, PREFIX + suffix.substring(0, DISPLAY_CHARS), sha256Hex(clearKey));
    }

    /**
     * Hashes a clear API key using SHA-256 hex.
     *
     * @param clearKey clear API key
     * @return lowercase SHA-256 hex
     */
    public String sha256Hex(final String clearKey) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(clearKey.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }
}
