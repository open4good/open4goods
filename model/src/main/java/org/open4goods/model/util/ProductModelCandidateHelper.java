package org.open4goods.model.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.open4goods.model.product.Product;

/**
 * Shared helpers for deriving and filtering product model identifiers.
 */
public final class ProductModelCandidateHelper {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{Alnum}]+");
    private static final int MAX_PERSISTED_MODEL_LENGTH = 48;
    private static final int MAX_PERSISTED_MODEL_SPACES = 4;
    private static final Pattern STORAGE_VARIANT_PATTERN = Pattern.compile(
            "(?i).*\\b\\d+\\s*(?:GO|GB|TO|TB)(?:\\s*/\\s*\\d+\\s*(?:GO|GB|TO|TB))*\\b.*");
    private static final Pattern CATEGORY_TITLE_PATTERN = Pattern.compile(
            "(?i).*(?:SMARTPHONE|TELEVISEUR|TV|REFRIGERATEUR|CONGELATEUR|LAVE\\s*-?\\s*LINGE|LAVE\\s*-?\\s*VAISSELLE|CLIMATISEUR|FOUR|CUISINIERE)\\b.*");
    private static final Pattern DEGENERATE_MODEL_PATTERN = Pattern.compile(
            "^\\d+$|^\\d+(?:[xX]\\d+){1,3}(?:[a-zA-Z]{0,3})?$|^[lL]?\\d+[xX][pP]?\\d+(?:[xX][hH]?\\d+)?(?:[a-zA-Z]{0,3})?$");
    private static final Pattern DIMENSION_OR_SPEC_PATTERN = Pattern.compile(
            "(?i).*\\b\\d+(?:[,.]\\d+)?\\s*(?:MM|CM|M|KG|G|L|W|HZ|KWH|MP|POUCES?|INCH|INCHES)\\b.*");
    private static final Pattern NAMED_MODEL_PATTERN = Pattern.compile(
            "(?=.*\\p{L})[\\p{Alnum}][\\p{Alnum} ./_-]{3,31}");
    private static final Pattern URL_MODEL_TOKEN_PATTERN = Pattern.compile(
            "[A-Za-z]{1,10}\\d[A-Za-z0-9]{2,}(?:[-_/][A-Za-z0-9]{1,10}){0,3}|\\d[A-Za-z]{1,10}[A-Za-z0-9]{2,}(?:[-_/][A-Za-z0-9]{1,10}){0,3}");
    private static final Set<String> GENERIC_MODEL_WORDS = Set.of("refrigerateur", "fridge", "glaciere",
            "dishwasher", "lave vaisselle", "lave", "vaisselle", "seche", "linge", "washing", "machine",
            "smartphone", "telephone", "mobile", "encastrable", "portable", "compact", "mini", "unknown");

    private ProductModelCandidateHelper() {
    }

    /**
     * Evidence source used when cleaning and ranking model candidates.
     */
    public enum ModelCandidateSource {
        /** EPREL model identifier or GTIN-authoritative EPREL evidence. */
        EPREL,
        /** Manufacturer metadata such as JSON-LD, itemprop, MPN, SKU, or model. */
        OFFICIAL_METADATA,
        /** Manufacturer page text explicitly labelling a model, MPN, SKU, or reference. */
        OFFICIAL_TEXT,
        /** Manufacturer URL or resource token that is also confirmed in official content. */
        OFFICIAL_URL_CONFIRMED,
        /** Structured catalogue identifiers such as Icecat, Amazon MPN, SKU, or brand part code. */
        STRUCTURED_DATA,
        /** Referential model from regular datasource ingestion. */
        DATASOURCE_REFERENTIAL,
        /** Candidate inferred from an offer or merchant title. */
        TITLE_INFERRED,
        /** Unknown or weak evidence. */
        WEAK
    }

    /**
     * Ranked, cleaned model candidate with its evidence source and score.
     *
     * @param value cleaned model value
     * @param source evidence source
     * @param score deterministic ranking score
     */
    public record RankedModelCandidate(String value, ModelCandidateSource source, int score) {
    }

    /**
     * Returns product model labels with common separator variants.
     *
     * @param product source product
     * @return ordered model candidates
     */
    public static List<String> expandedCandidates(Product product) {
        Set<String> candidates = new LinkedHashSet<>(hardenedCandidates(product));

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
     * Sanitises a model candidate for persistent storage as a canonical model or
     * alternate model.
     *
     * @param candidate raw model candidate
     * @return normalised storage form, or {@code null} when the candidate is too
     *         noisy or too weak
     */
    public static String cleanForStorage(String candidate) {
        return cleanForStorage(candidate, ModelCandidateSource.DATASOURCE_REFERENTIAL);
    }

    /**
     * Sanitises a model candidate for persistent storage using the evidence source.
     * Code-like identifiers are accepted from normal datasource ingestion; named
     * models require stronger evidence such as EPREL or official manufacturer data.
     *
     * @param candidate raw model candidate
     * @param source evidence source
     * @return normalised storage form, or {@code null} when the candidate is too
     *         noisy or too weak for its source
     */
    public static String cleanForStorage(String candidate, ModelCandidateSource source) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String cleaned = candidate.trim()
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");
        if (cleaned.isBlank()) {
            return null;
        }
        String code = cleaned.toUpperCase(Locale.ROOT);
        if (isPersistableModelCandidate(code)) {
            return code;
        }
        if (isStrongSource(source) && isNamedModel(cleaned)) {
            return cleaned;
        }
        return null;
    }

    /**
     * Returns whether a candidate is precise enough to persist on a product.
     *
     * @param candidate normalised or raw candidate
     * @return {@code true} when the candidate looks like a manufacturer model code
     */
    public static boolean isPersistableModelCandidate(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String trimmed = candidate.trim();
        if (trimmed.length() > MAX_PERSISTED_MODEL_LENGTH) {
            return false;
        }
        if (trimmed.chars().filter(Character::isWhitespace).count() > MAX_PERSISTED_MODEL_SPACES) {
            return false;
        }
        long alnum = trimmed.chars().filter(Character::isLetterOrDigit).count();
        if (alnum < 4) {
            return false;
        }
        String ascii = DIACRITICS.matcher(Normalizer.normalize(trimmed, Normalizer.Form.NFD)).replaceAll("");
        if (DEGENERATE_MODEL_PATTERN.matcher(trimmed).matches()
                || STORAGE_VARIANT_PATTERN.matcher(trimmed).matches()
                || CATEGORY_TITLE_PATTERN.matcher(ascii).matches()
                || DIMENSION_OR_SPEC_PATTERN.matcher(ascii).matches()) {
            return false;
        }
        String compact = compactModel(trimmed);
        if (compact == null || compact.length() < 4) {
            return false;
        }
        boolean hasLetter = compact.chars().anyMatch(Character::isLetter);
        boolean hasDigit = compact.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }

    /**
     * Checks whether a candidate is a readable named model instead of a compact
     * manufacturer code.
     *
     * @param candidate raw candidate
     * @return true when the value looks like a concise named model
     */
    public static boolean isNamedModel(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String trimmed = candidate.trim()
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ");
        String normalized = normalizePhrase(trimmed);
        if (normalized == null || GENERIC_MODEL_WORDS.contains(normalized)) {
            return false;
        }
        String ascii = DIACRITICS.matcher(Normalizer.normalize(trimmed, Normalizer.Form.NFD)).replaceAll("");
        return NAMED_MODEL_PATTERN.matcher(trimmed).matches()
                && trimmed.chars().filter(Character::isWhitespace).count() <= 3
                && !isCodeLikeModel(trimmed)
                && !STORAGE_VARIANT_PATTERN.matcher(trimmed).matches()
                && !CATEGORY_TITLE_PATTERN.matcher(ascii).matches()
                && !DIMENSION_OR_SPEC_PATTERN.matcher(ascii).matches()
                && !DEGENERATE_MODEL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Returns whether source evidence is strong enough to persist named models.
     *
     * @param source evidence source
     * @return true for authoritative model sources
     */
    public static boolean isStrongSource(ModelCandidateSource source) {
        return source == ModelCandidateSource.EPREL
                || source == ModelCandidateSource.OFFICIAL_METADATA
                || source == ModelCandidateSource.OFFICIAL_TEXT
                || source == ModelCandidateSource.OFFICIAL_URL_CONFIRMED
                || source == ModelCandidateSource.STRUCTURED_DATA;
    }

    /**
     * Cleans and deduplicates model candidates for storage.
     *
     * @param models raw candidates
     * @return ordered clean candidates
     */
    public static List<String> cleanForStorage(Collection<String> models) {
        if (models == null) {
            return List.of();
        }
        return models.stream()
                .map(ProductModelCandidateHelper::cleanForStorage)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * Ranks cleaned storage candidates by evidence source and candidate quality.
     *
     * @param candidates raw candidates keyed by source
     * @return cleaned, deduplicated candidates ordered from strongest to weakest
     */
    public static List<RankedModelCandidate> rankedStorageCandidates(Map<String, ModelCandidateSource> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Map<String, RankedModelCandidate> deduped = new LinkedHashMap<>();
        for (Map.Entry<String, ModelCandidateSource> entry : candidates.entrySet()) {
            ModelCandidateSource source = entry.getValue() == null ? ModelCandidateSource.WEAK : entry.getValue();
            String cleaned = cleanForStorage(entry.getKey(), source);
            if (cleaned == null) {
                continue;
            }
            int score = modelScore(cleaned, source);
            String key = compactModel(cleaned);
            RankedModelCandidate ranked = new RankedModelCandidate(cleaned, source, score);
            RankedModelCandidate existing = deduped.get(key);
            if (existing == null || ranked.score() > existing.score()) {
                deduped.put(key, ranked);
            }
        }
        return deduped.values().stream()
                .sorted(Comparator.comparingInt(RankedModelCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.value().length())
                        .thenComparing(RankedModelCandidate::value))
                .toList();
    }

    /**
     * Elects the best canonical model from ranked candidates.
     *
     * @param candidates raw candidates keyed by source
     * @return best cleaned candidate
     */
    public static Optional<String> electCanonicalModel(Map<String, ModelCandidateSource> candidates) {
        return rankedStorageCandidates(candidates).stream()
                .findFirst()
                .map(RankedModelCandidate::value);
    }

    /**
     * Builds ranked model candidates for pre-fetch searches and validation.
     * Conflicting sibling drift alternates are suppressed unless they are the
     * canonical value; this prevents broad searches for neighbouring variants.
     *
     * @param product source product
     * @return cleaned candidates ordered by confidence
     */
    public static List<String> hardenedCandidates(Product product) {
        if (product == null) {
            return List.of();
        }
        List<String> candidates = new ArrayList<>();
        String canonical = cleanForStorage(product.model(), ModelCandidateSource.DATASOURCE_REFERENTIAL);
        if (canonical != null) {
            candidates.add(canonical);
        }
        if (product.getAkaModels() != null) {
            product.getAkaModels().stream()
                    .map(value -> cleanForStorage(value, ModelCandidateSource.DATASOURCE_REFERENTIAL))
                    .filter(Objects::nonNull)
                    .forEach(candidates::add);
        }
        if (product.getExternalIds() != null) {
            if (product.getExternalIds().getMpn() != null) {
                product.getExternalIds().getMpn().stream()
                        .map(value -> cleanForStorage(value, ModelCandidateSource.STRUCTURED_DATA))
                        .filter(Objects::nonNull)
                        .forEach(candidates::add);
            }
            if (product.getExternalIds().getSku() != null) {
                product.getExternalIds().getSku().stream()
                        .map(value -> cleanForStorage(value, ModelCandidateSource.STRUCTURED_DATA))
                        .filter(Objects::nonNull)
                        .forEach(candidates::add);
            }
        }
        List<String> deduped = candidates.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProductModelCandidateHelper::compactModel,
                        value -> value,
                        (left, right) -> left,
                        LinkedHashMap::new))
                .values().stream()
                .toList();
        if (canonical == null) {
            return rejectAmbiguousSiblingFamilies(deduped);
        }
        return deduped.stream()
                .filter(candidate -> candidate.equals(canonical) || !isSiblingDrift(candidate, canonical))
                .toList();
    }

    /**
     * Checks if two candidates look like sibling variants from the same family.
     *
     * @param left first candidate
     * @param right second candidate
     * @return true when the values share a family prefix but differ in terminal
     *         numeric variant
     */
    public static boolean isSiblingDrift(String left, String right) {
        String leftNormalized = normalizePhrase(left);
        String rightNormalized = normalizePhrase(right);
        if (leftNormalized == null || rightNormalized == null || leftNormalized.equals(rightNormalized)) {
            return false;
        }
        String[] leftParts = leftNormalized.split(" ");
        String[] rightParts = rightNormalized.split(" ");
        if (leftParts.length == 0 || rightParts.length == 0) {
            return false;
        }
        String leftLast = leftParts[leftParts.length - 1];
        String rightLast = rightParts[rightParts.length - 1];
        if (!leftLast.matches("\\d{2,5}") || !rightLast.matches("\\d{2,5}") || leftLast.equals(rightLast)) {
            return false;
        }
        String leftPrefix = leftNormalized.substring(0, leftNormalized.length() - leftLast.length()).trim();
        String rightPrefix = rightNormalized.substring(0, rightNormalized.length() - rightLast.length()).trim();
        return !leftPrefix.isBlank() && leftPrefix.equals(rightPrefix);
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
     * Normalises a value for compact URL/resource matching.
     *
     * @param value raw value
     * @return lowercase alphanumeric value, or an empty string
     */
    public static String normalizeForUrlMatching(String value) {
        String compact = compactModel(value);
        return compact == null ? "" : compact;
    }

    /**
     * Checks whether a candidate appears as a whole model phrase or strong model
     * token in normalized text.
     *
     * @param model raw model
     * @param text raw or normalized text
     * @return true when the text contains model evidence
     */
    public static boolean modelMatchesTextZone(String model, String text) {
        if (model == null || model.isBlank() || text == null || text.isBlank()) {
            return false;
        }
        String normalizedZone = normalizePhrase(text);
        String normalizedModel = normalizePhrase(model);
        if (normalizedZone == null || normalizedModel == null) {
            return false;
        }
        if (normalizedModel.length() >= 4 && containsWholePhrase(normalizedZone, normalizedModel)) {
            return true;
        }
        for (String token : model.split("[\\s_\\-\\./\\\\]+")) {
            if (isStrongModelToken(token) && containsWholePhrase(normalizedZone, normalizePhrase(token))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts code-like candidates from URL, title, or free text.
     *
     * @param value raw value
     * @return extracted model-code candidates
     */
    public static List<String> extractModelCodeCandidates(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> candidates = URL_MODEL_TOKEN_PATTERN.matcher(value).results()
                .map(match -> match.group())
                .filter(candidate -> candidate != null && !candidate.isBlank())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Pattern.compile("(?i)\\b[A-Z]{2,8}\\s+\\d{3,5}\\b").matcher(value).results()
                .map(match -> match.group().trim())
                .forEach(candidates::add);
        return candidates;
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

    private static int modelScore(String candidate, ModelCandidateSource source) {
        int score = switch (source == null ? ModelCandidateSource.WEAK : source) {
            case EPREL -> 700;
            case OFFICIAL_METADATA, OFFICIAL_TEXT -> 600;
            case OFFICIAL_URL_CONFIRMED -> 520;
            case STRUCTURED_DATA -> 450;
            case DATASOURCE_REFERENTIAL -> 300;
            case TITLE_INFERRED -> 160;
            case WEAK -> 80;
        };
        if (isCodeLikeModel(candidate)) {
            score += 80;
        }
        if (isNamedModel(candidate)) {
            score += 40;
        }
        String compact = compactModel(candidate);
        if (compact != null) {
            score += Math.min(40, compact.length());
        }
        if (STORAGE_VARIANT_PATTERN.matcher(candidate).matches() || DIMENSION_OR_SPEC_PATTERN.matcher(candidate).matches()) {
            score -= 120;
        }
        score -= Math.max(0, candidate.length() - 32);
        return score;
    }

    private static boolean isCodeLikeModel(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.length() >= 4
                && trimmed.length() <= 32
                && trimmed.matches("(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9/_\\-. ]+")
                && trimmed.chars().filter(Character::isWhitespace).count() <= 2;
    }

    private static boolean isStrongModelToken(String token) {
        if (token == null || token.isBlank() || token.length() < 4) {
            return false;
        }
        boolean hasLetter = token.chars().anyMatch(Character::isLetter);
        boolean hasDigit = token.chars().anyMatch(Character::isDigit);
        return (hasLetter && hasDigit) || (hasDigit && token.length() >= 5);
    }

    private static boolean containsWholePhrase(String container, String contained) {
        return contained != null && (" " + container + " ").contains(" " + contained + " ");
    }

    private static List<String> rejectAmbiguousSiblingFamilies(List<String> candidates) {
        List<String> retained = new ArrayList<>();
        for (String candidate : candidates) {
            boolean conflicts = candidates.stream()
                    .anyMatch(other -> !candidate.equals(other) && isSiblingDrift(candidate, other));
            if (!conflicts) {
                retained.add(candidate);
            }
        }
        return retained;
    }

}
