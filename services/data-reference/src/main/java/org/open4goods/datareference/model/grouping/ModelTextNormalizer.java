package org.open4goods.datareference.model.grouping;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Normalizes a provider-written brand or model string for exact-tuple matching
 * and search tokenization.
 *
 * <p>Folds case and punctuation so that {@code "XR-500"}, {@code "xr 500"} and
 * {@code "XR500"} normalize identically, per ADR-0010's "exact normalized
 * tuple." This is intentionally simpler than the reviewed family-pattern
 * engine: it never strips a regional suffix or an embedded size, because doing
 * so would turn an exact match into a similarity heuristic.
 */
public final class ModelTextNormalizer {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private ModelTextNormalizer() {
    }

    /**
     * Normalizes a string into a single lower-case alphanumeric slug fragment.
     *
     * @param value raw provider text, possibly {@code null} or blank
     * @return normalized fragment, or empty when the input carries no
     *     alphanumeric content
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String folded = value.toLowerCase(Locale.ROOT);
        return NON_ALPHANUMERIC.matcher(folded).replaceAll("-").replaceAll("^-+|-+$", "");
    }

    /**
     * Splits a string into normalized, non-blank tokens.
     *
     * @param value raw provider text, possibly {@code null} or blank
     * @return ordered normalized tokens, without duplicates removed
     */
    public static List<String> tokenize(String value) {
        if (value == null) {
            return List.of();
        }
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split("-")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return List.copyOf(tokens);
    }

    /**
     * Joins already-normalized, non-blank fragments into a group slug.
     *
     * @param fragments fragments to join, every one of which must be non-blank
     * @return the joined slug
     */
    public static String joinSlug(String... fragments) {
        List<String> nonBlank = new ArrayList<>();
        for (String fragment : fragments) {
            Objects.requireNonNull(fragment, "fragment must not be null");
            if (fragment.isBlank()) {
                throw new IllegalArgumentException("slug fragment must not be blank");
            }
            nonBlank.add(fragment);
        }
        return String.join("-", nonBlank);
    }
}
