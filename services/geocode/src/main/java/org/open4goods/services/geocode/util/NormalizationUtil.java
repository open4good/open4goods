package org.open4goods.services.geocode.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility methods for normalizing input strings for geocoding.
 */
public final class NormalizationUtil
{
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern PUNCTUATION = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private NormalizationUtil()
    {
    }

    /**
     * Normalizes a string for case-insensitive matching and stable indexing.
     *
     * <p>The normalization lowers case, trims, removes diacritics, replaces punctuation
     * with spaces (while keeping hyphens), and collapses repeated whitespace. This
     * ensures consistent keys for GeoNames lookup while preserving common separators.</p>
     *
     * @param value input value
     * @return normalized value, or empty string when input is null
     */
    public static String normalize(String value)
    {
        if (value == null)
        {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        String withoutMarks = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        String withoutPunctuation = PUNCTUATION.matcher(withoutMarks).replaceAll(" ");
        return WHITESPACE.matcher(withoutPunctuation).replaceAll(" ").trim();
    }
}
