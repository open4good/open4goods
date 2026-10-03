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
 * <p>Folds case and collapses every run of non-alphanumeric characters into a
 * single {@code -}, so {@code "XR-500"} and {@code "xr 500"} normalize
 * identically. {@code "XR500"} deliberately does <em>not</em>: a separator is a
 * character of the model string, and dropping it would put {@code "XR500"} and
 * {@code "XR-500"} in one group without evidence that they are the same model.
 * That follows ADR-0010's "exact normalized tuple" for the same reason this
 * normalizer never strips a regional suffix or an embedded size — either would
 * turn an exact match into a similarity heuristic. Joining separated and
 * unseparated spellings is the reviewed family-pattern engine's job (AC3/AC4).
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
     * <p>Each fragment may itself contain a {@code -}, since {@link #normalize}
     * folds any punctuation to one. A plain {@code String.join("-", ...)} would
     * therefore not be injective: {@code ["tv", "sony-ericsson", "xr500"]} and
     * {@code ["tv", "sony", "ericsson-xr500"]} would both join to
     * {@code "tv-sony-ericsson-xr500"}. Each fragment is instead prefixed with
     * its own length, so the boundary between fragments is recoverable from the
     * encoded string alone: a reader consumes the decimal length, skips the
     * delimiter, then takes exactly that many characters as the fragment,
     * regardless of any {@code -} inside it.
     *
     * @param fragments fragments to join, every one of which must be non-blank
     * @return the joined slug, unique per distinct fragment sequence
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
        StringBuilder slug = new StringBuilder();
        for (int i = 0; i < nonBlank.size(); i++) {
            if (i > 0) {
                slug.append('-');
            }
            String fragment = nonBlank.get(i);
            slug.append(fragment.length()).append('-').append(fragment);
        }
        return slug.toString();
    }
}
