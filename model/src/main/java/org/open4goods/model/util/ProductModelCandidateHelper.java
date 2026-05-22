package org.open4goods.model.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.open4goods.model.product.Product;

/**
 * Shared helpers for deriving and filtering product model identifiers.
 */
public final class ProductModelCandidateHelper {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{Alnum}]+");
    private static final Pattern DEGENERATE_MODEL_PATTERN = Pattern.compile(
            "^\\d+$|^\\d+(?:[xX]\\d+){1,3}(?:[a-zA-Z]{0,3})?$|^[lL]?\\d+[xX][pP]?\\d+(?:[xX][hH]?\\d+)?(?:[a-zA-Z]{0,3})?$");

    private ProductModelCandidateHelper() {
    }

    /**
     * Returns product model labels with common separator variants.
     *
     * @param product source product
     * @return ordered model candidates
     */
    public static List<String> expandedCandidates(Product product) {
        Set<String> candidates = new LinkedHashSet<>();
        if (product != null && product.model() != null && !product.model().isBlank()) {
            candidates.add(product.model());
        }
        if (product != null && product.getAkaModels() != null) {
            product.getAkaModels().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(candidates::add);
        }

        List<String> baseCandidates = new ArrayList<>(candidates);
        for (String base : baseCandidates) {
            String hyphenated = base.replace(' ', '-').replaceAll("-+", "-");
            if (!hyphenated.equalsIgnoreCase(base)) {
                candidates.add(hyphenated);
            }
            String spaced = NON_ALNUM.matcher(base).replaceAll(" ").replaceAll(" +", " ").trim();
            if (!spaced.equalsIgnoreCase(base) && !spaced.isEmpty()) {
                candidates.add(spaced);
            }
            String compact = NON_ALNUM.matcher(base).replaceAll("");
            if (!compact.equalsIgnoreCase(base) && !compact.isEmpty()) {
                candidates.add(compact);
            }
        }
        return new ArrayList<>(candidates);
    }

    /**
     * Sanitises model candidates for exact/reference searches.
     *
     * @param models candidate labels
     * @param maxSpaces maximum allowed whitespace count
     * @param minAlnum minimum alphanumeric character count
     * @return filtered candidates, longest first
     */
    public static List<String> sanitise(Collection<String> models, int maxSpaces, int minAlnum) {
        if (models == null) {
            return List.of();
        }
        return models.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(candidate -> !candidate.isEmpty())
                .filter(candidate -> !shouldExcludeCandidate(candidate, maxSpaces, minAlnum))
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    /**
     * Normalises a phrase to lowercase alphanumeric words.
     *
     * @param value raw value
     * @return normalised phrase, or {@code null} when empty
     */
    public static String normalizePhrase(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String ascii = DIACRITICS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
        String normalized = NON_ALNUM.matcher(ascii.toLowerCase()).replaceAll(" ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Returns a compact alphanumeric model label.
     *
     * @param value raw value
     * @return compact label, or {@code null} when empty
     */
    public static String compactModel(String value) {
        String normalized = normalizePhrase(value);
        if (normalized == null) {
            return null;
        }
        String compact = normalized.replace(" ", "");
        return compact.isEmpty() ? null : compact;
    }

    /**
     * Checks whether a candidate is useful for broad user-facing web searches.
     *
     * @param candidate model candidate
     * @return true when the value is not a bare internal numeric reference
     */
    public static boolean isHumanSearchCandidate(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String trimmed = candidate.trim();
        long alnum = trimmed.chars().filter(Character::isLetterOrDigit).count();
        return alnum >= 3 && !DEGENERATE_MODEL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Indicates if a model candidate is likely an internal reference or dimensions.
     *
     * @param candidate candidate to inspect
     * @param maxSpaces maximum allowed whitespace count
     * @param minAlnum minimum alphanumeric character count
     * @return true when the candidate should be excluded
     */
    public static boolean shouldExcludeCandidate(String candidate, int maxSpaces, int minAlnum) {
        long spaceCount = candidate.chars().filter(Character::isWhitespace).count();
        if (spaceCount > maxSpaces) {
            return true;
        }
        long alnumCount = candidate.chars().filter(Character::isLetterOrDigit).count();
        if (alnumCount < minAlnum) {
            return true;
        }
        return DEGENERATE_MODEL_PATTERN.matcher(candidate).matches();
    }
}
