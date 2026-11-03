package org.open4goods.nudgerfrontapi.utils;

/**
 * Masks e-mail addresses before they reach a log line.
 * <p>
 * Only the first character of the local part and the domain are kept
 * ({@code john.doe@example.com} becomes {@code j***@example.com}), which is
 * enough to tell two accounts apart while diagnosing a login without writing
 * the address in clear.
 * </p>
 */
public final class EmailMaskUtils {

    private static final String MASK = "***";

    private EmailMaskUtils() {
    }

    /**
     * Mask an e-mail address, or any identifier that should have been one.
     *
     * @param value e-mail address, possibly malformed or {@code null}
     * @return {@code j***@example.com} for an address, {@code j***} for a value
     *         without {@code @}, and {@code <empty>} for a null or blank value
     */
    public static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "<empty>";
        }
        String trimmed = value.strip();
        int at = trimmed.lastIndexOf('@');
        if (at < 0) {
            return trimmed.charAt(0) + MASK;
        }
        String local = trimmed.substring(0, at);
        String first = local.isEmpty() ? "" : String.valueOf(local.charAt(0));
        return first + MASK + trimmed.substring(at);
    }
}
