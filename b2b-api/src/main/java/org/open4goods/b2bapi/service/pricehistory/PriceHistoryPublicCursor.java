package org.open4goods.b2bapi.service.pricehistory;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.open4goods.pricehistory.model.PriceHistoryGranularity;

/**
 * Opaque, client-facing price-history cursor. It authenticate-encrypts (AES-256-GCM, keyed by a
 * server-only secret) the internal Elasticsearch cursor together with a fingerprint of every query
 * filter it was issued for.
 *
 * <p>Two properties fall out of using AEAD rather than plain Base64: (1) the internal
 * {@code provider_id}/{@code event_id} coordinates carried by the inner cursor never appear in any
 * form a client can read (GOU-28 AC4), and (2) a client cannot construct a syntactically valid
 * opaque cursor without the server secret, so forged or corrupted input fails authentication and is
 * always rejected as {@code cursor-mismatch} - it can never reach the inner
 * {@link org.open4goods.pricehistory.service.PriceHistoryCursor} decoder and trigger an unhandled
 * exception (GOU-28 AC2).
 */
public final class PriceHistoryPublicCursor {

    private static final char SEPARATOR = '\u0000';
    private static final String CIPHER_TRANSFORM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PriceHistoryPublicCursor() {
    }

    /**
     * Encrypts the opaque internal cursor together with a fingerprint of the query it was issued for.
     *
     * @param cursorSecret server-only secret; never derived from request data
     * @param innerCursor opaque cursor returned by {@link org.open4goods.pricehistory.port.PriceHistoryQueryPort}
     * @param fingerprint query fingerprint from {@link #fingerprint}
     * @return opaque, URL-safe public cursor
     */
    public static String encode(final String cursorSecret, final String innerCursor, final String fingerprint) {
        Objects.requireNonNull(cursorSecret, "cursorSecret must not be null");
        Objects.requireNonNull(innerCursor, "innerCursor must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        final byte[] plaintext = (fingerprint + SEPARATOR + innerCursor).getBytes(StandardCharsets.UTF_8);
        final byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        RANDOM.nextBytes(iv);
        try {
            final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(cursorSecret), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            final byte[] ciphertext = cipher.doFinal(plaintext);
            final byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (final GeneralSecurityException ex) {
            throw new IllegalStateException("failed to encrypt price-history cursor", ex);
        }
    }

    /**
     * Decrypts a public cursor, verifying its authenticity and that it was issued for the exact same
     * query. Never throws on malformed or forged input - both are indistinguishable from a
     * fingerprint mismatch to the caller.
     *
     * @param cursorSecret server-only secret; never derived from request data
     * @param opaqueCursor client-supplied cursor
     * @param expectedFingerprint fingerprint of the request currently being served
     * @return decoded internal cursor, when the cursor authenticates and the fingerprint matches
     */
    public static Optional<String> decode(
            final String cursorSecret, final String opaqueCursor, final String expectedFingerprint) {
        Objects.requireNonNull(cursorSecret, "cursorSecret must not be null");
        if (opaqueCursor == null || opaqueCursor.isBlank()) {
            return Optional.empty();
        }
        final byte[] combined;
        try {
            combined = Base64.getUrlDecoder().decode(opaqueCursor);
        } catch (final IllegalArgumentException ex) {
            return Optional.empty();
        }
        if (combined.length <= GCM_IV_LENGTH_BYTES) {
            return Optional.empty();
        }
        final byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH_BYTES);
        final byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH_BYTES, combined.length);
        final String value;
        try {
            final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(cursorSecret), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            value = new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (final GeneralSecurityException | IllegalArgumentException ex) {
            return Optional.empty();
        }
        final int separator = value.indexOf(SEPARATOR);
        if (separator < 1) {
            return Optional.empty();
        }
        final String fingerprint = value.substring(0, separator);
        final String innerCursor = value.substring(separator + 1);
        if (!fingerprint.equals(expectedFingerprint) || innerCursor.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(innerCursor);
    }

    private static SecretKeySpec deriveKey(final String cursorSecret) {
        try {
            final byte[] key = MessageDigest.getInstance("SHA-256").digest(cursorSecret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("the JDK must provide SHA-256", ex);
        }
    }

    /**
     * Computes a stable fingerprint over every filter that changes the served result set.
     *
     * @param gtin normalized GTIN
     * @param granularity effective granularity
     * @param providerId optional provider filter, or {@code null}
     * @param condition optional condition filter, or {@code null}
     * @param currency optional currency filter, or {@code null}
     * @param from inclusive UTC start
     * @param to exclusive UTC end
     * @param limit requested page size
     * @return lower-case hexadecimal SHA-256 fingerprint
     */
    public static String fingerprint(
            final String gtin,
            final PriceHistoryGranularity granularity,
            final String providerId,
            final String condition,
            final String currency,
            final Instant from,
            final Instant to,
            final int limit) {
        final String material = String.join("|",
                nullToEmpty(gtin),
                String.valueOf(granularity),
                nullToEmpty(providerId),
                nullToEmpty(condition),
                nullToEmpty(currency),
                String.valueOf(from),
                String.valueOf(to),
                String.valueOf(limit));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("the JDK must provide SHA-256", ex);
        }
    }

    private static String nullToEmpty(final String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
