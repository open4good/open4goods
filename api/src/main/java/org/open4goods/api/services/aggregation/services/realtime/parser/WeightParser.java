package org.open4goods.api.services.aggregation.services.realtime.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.exceptions.ParseException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributeParser;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Best-effort parser for product weights.
 * <p>
 * This parser tries to return a weight in <b>kilograms</b> from heterogeneous raw inputs coming
 * from multiple datasources (grams, kilograms, milligrams, sometimes imperial units, with or
 * without explicit unit suffix).
 * <p>
 * <h3>Rule levels</h3>
 * <ol>
 *   <li><b>Explicit unit rules</b>: if the raw string contains an explicit unit ("kg", "g", "mg",
 *       "lb", "oz" ...), the parser will trust it (with sanity checks against configured bounds).</li>
 *   <li><b>Implicit unit rules</b>: if the raw string is numeric (or contains numeric tokens),
 *       the parser will infer the most plausible unit by trying several interpretations
 *       (kg/g/mg/…) and selecting the one fitting best inside plausible bounds.</li>
 *   <li><b>Category (vertical) bounds</b>: plausible ranges (min/max in kg) can be configured per
 *       vertical id (e.g. TVs will rarely exceed 200kg). If a raw numeric value does not fit in
 *       range as kg but fits as grams, grams will be selected.</li>
 *   <li><b>Datasource default unit</b>: when no unit is present, a datasource-specific default
 *       unit can be configured (e.g. "amazon" values are in grams), which strongly influences
 *       inference.</li>
 * </ol>
 *
 * <p>
 * The configuration is exposed as a simple POJO ({@link WeightParserConfiguration}) so it can be
 * bound from YAML/JSON if desired.
 * TODO : Wears the "plausible range" and all specific verticals confuration in VerticalConfig.parsers.WEIGHT (to be created, will wears configuration for this kind of custom parser)
 * TODO : Update /home/goulven/git/open4goods/verticals/src/main/resources/verticals/* with plausible weights accordingly
 * TODO : Ensure it logs WARN if cannot resolve the weight.
 * TODO : Must ensure safety first. PRefers reejcting than too large acceptance
 */
public class WeightParser extends AttributeParser {

        private static final Logger LOGGER = LoggerFactory.getLogger(WeightParser.class);

        /**
         * Supported input units.
         */
        public enum WeightUnit {
                KILOGRAM(1.0),
                GRAM(1.0 / 1000.0),
                MILLIGRAM(1.0 / 1_000_000.0),
                POUND(0.45359237),
                OUNCE(0.028349523125);

                private final double toKgFactor;

                WeightUnit(double toKgFactor) {
                        this.toKgFactor = toKgFactor;
                }

                public double toKg(double value) {
                        return value * toKgFactor;
                }
        }

        /**
         * A plausible range for a weight (in kg).
         */
        public static final class WeightRangeKg {
                private double minKg = 0.0;
                private double maxKg = 10_000.0;

                public WeightRangeKg() {
                }

                public WeightRangeKg(double minKg, double maxKg) {
                        this.minKg = minKg;
                        this.maxKg = maxKg;
                }

                public double getMinKg() {
                        return minKg;
                }

                public void setMinKg(double minKg) {
                        this.minKg = minKg;
                }

                public double getMaxKg() {
                        return maxKg;
                }

                public void setMaxKg(double maxKg) {
                        this.maxKg = maxKg;
                }

                public boolean contains(double kg) {
                        return kg >= minKg && kg <= maxKg;
                }
        }

        /**
         * Parsed weight with a confidence score.
         */
        public record ParsedWeight(double kilograms, double confidence, WeightUnit chosenUnit, String reason,
                        String rawValue, String datasource) {
        }

        /**
         * Parser configuration (bindable from YAML/JSON).
         */
        public static class WeightParserConfiguration {
                /**
                 * Default unit used as a hint when unit is absent (still combined with range inference).
                 */
                private WeightUnit defaultUnitHint = WeightUnit.KILOGRAM;

                /**
                 * Default unit hint by datasource name (lower-cased). Use "all" for global.
                 */
                private Map<String, WeightUnit> defaultUnitHintByDatasource = new HashMap<>();

                /**
                 * Plausible ranges (in kg) by vertical id (lower-cased). Use "all" for global.
                 */
                private Map<String, WeightRangeKg> plausibleRangeByVerticalId = new HashMap<>();

                /**
                 * If multiple sources disagree by more than this tolerance (in kg), a warning is logged.
                 */
                private double conflictToleranceKg = 0.25;

                /**
                 * Output scale used before stripping trailing zeros.
                 */
                private int outputScale = 6;

                public WeightParserConfiguration() {
                }

                public WeightUnit getDefaultUnitHint() {
                        return defaultUnitHint;
                }

                public void setDefaultUnitHint(WeightUnit defaultUnitHint) {
                        this.defaultUnitHint = defaultUnitHint;
                }

                public Map<String, WeightUnit> getDefaultUnitHintByDatasource() {
                        return defaultUnitHintByDatasource;
                }

                public void setDefaultUnitHintByDatasource(Map<String, WeightUnit> defaultUnitHintByDatasource) {
                        this.defaultUnitHintByDatasource = defaultUnitHintByDatasource == null ? new HashMap<>()
                                        : new HashMap<>(defaultUnitHintByDatasource);
                }

                public Map<String, WeightRangeKg> getPlausibleRangeByVerticalId() {
                        return plausibleRangeByVerticalId;
                }

                public void setPlausibleRangeByVerticalId(Map<String, WeightRangeKg> plausibleRangeByVerticalId) {
                        this.plausibleRangeByVerticalId = plausibleRangeByVerticalId == null ? new HashMap<>()
                                        : new HashMap<>(plausibleRangeByVerticalId);
                }

                public double getConflictToleranceKg() {
                        return conflictToleranceKg;
                }

                public void setConflictToleranceKg(double conflictToleranceKg) {
                        this.conflictToleranceKg = conflictToleranceKg;
                }

                public int getOutputScale() {
                        return outputScale;
                }

                public void setOutputScale(int outputScale) {
                        this.outputScale = outputScale;
                }

                public WeightRangeKg resolveRangeForVertical(VerticalConfig verticalConfig) {
                        final String vid = verticalConfig == null || verticalConfig.getId() == null ? "all"
                                        : verticalConfig.getId().trim().toLowerCase(Locale.ROOT);
                        if (plausibleRangeByVerticalId.containsKey(vid)) {
                                return plausibleRangeByVerticalId.get(vid);
                        }
                        return plausibleRangeByVerticalId.getOrDefault("all", new WeightRangeKg());
                }

                public WeightUnit resolveDefaultUnitHintForDatasource(String datasource) {
                        final String ds = datasource == null ? "all" : datasource.trim().toLowerCase(Locale.ROOT);
                        if (defaultUnitHintByDatasource.containsKey(ds)) {
                                return defaultUnitHintByDatasource.get(ds);
                        }
                        return defaultUnitHintByDatasource.getOrDefault("all", defaultUnitHint);
                }
        }

        /**
         * Global configuration, designed to be overridden by application bootstrap (YAML binding etc.).
         */
        private static volatile WeightParserConfiguration GLOBAL_CONFIG = defaultConfiguration();

        /**
         * Replaces the global configuration.
         *
         * @param configuration non-null
         */
        public static void setGlobalConfiguration(WeightParserConfiguration configuration) {
                GLOBAL_CONFIG = Objects.requireNonNull(configuration, "configuration");
        }

        /**
         * Returns the current global configuration.
         */
        public static WeightParserConfiguration getGlobalConfiguration() {
                return GLOBAL_CONFIG;
        }

        /**
         * Provides sensible defaults, including a "tv" vertical max around 200kg.
         */
        public static WeightParserConfiguration defaultConfiguration() {
                WeightParserConfiguration cfg = new WeightParserConfiguration();



                // Global plausible range: most product weights fall in [1g ; 2000kg]
                cfg.plausibleRangeByVerticalId.put("all", new WeightRangeKg(0.001, 2000.0));
// TODO : Remove when weared at yaml vertical configuration level
                // Examples (can be overridden): TVs should not exceed ~200kg.
                cfg.plausibleRangeByVerticalId.put("tv", new WeightRangeKg(0.1, 200.0));
                cfg.plausibleRangeByVerticalId.put("television", new WeightRangeKg(0.1, 200.0));
                cfg.plausibleRangeByVerticalId.put("televisions", new WeightRangeKg(0.1, 200.0));
                cfg.plausibleRangeByVerticalId.put("dishwasher", new WeightRangeKg(1.0, 150.0));
                cfg.plausibleRangeByVerticalId.put("lave-vaisselle", new WeightRangeKg(1.0, 150.0));

                cfg.defaultUnitHint = WeightUnit.KILOGRAM;
                cfg.defaultUnitHintByDatasource.put("all", WeightUnit.KILOGRAM);

                cfg.conflictToleranceKg = 0.25;
                cfg.outputScale = 6;
                return cfg;
        }

        // ----------------------------
        // Parsing
        // ----------------------------

        @Override
        public String parse(ProductAttribute attribute, AttributeConfig attributeConfig, VerticalConfig verticalConfig)
                        throws ParseException {

                if (attribute == null || attribute.getSource() == null || attribute.getSource().isEmpty()) {
                        return null;
                }

                final List<ParsedWeight> parsed = new ArrayList<>();

                for (SourcedAttribute src : attribute.getSource()) {
                        final String raw = safeGetString(src, "getValue").orElse(null);
                        if (raw == null || raw.isBlank()) {
                                continue;
                        }
                        final String datasource = safeGetDatasource(src);
                        ParsedWeight w = parseInternal(raw, verticalConfig, datasource);
                        if (w != null) {
                                parsed.add(w);
                        }
                }

                if (parsed.isEmpty()) {
                        return null;
                }

                // Selecting best candidate by confidence, with a small tie-break on plausibility (already baked).
                parsed.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
                ParsedWeight best = parsed.get(0);

                // Conflict logging with other high-confidence candidates.
                for (int i = 1; i < parsed.size(); i++) {
                        ParsedWeight other = parsed.get(i);
                        if (other.confidence() < 0.80) {
                                break;
                        }
                        double diff = Math.abs(other.kilograms() - best.kilograms());
                        if (diff > GLOBAL_CONFIG.getConflictToleranceKg()) {
                                LOGGER.warn(
                                                "Weight conflict for attribute '{}': best={}kg ({}, conf={}) vs {}kg ({}, conf={})",
                                                attribute.getName(), formatKg(best.kilograms()), best.datasource(),
                                                best.confidence(), formatKg(other.kilograms()), other.datasource(),
                                                other.confidence());
                        }
                }

                return formatKg(best.kilograms());
        }

        /**
         * Convenience entry point (typically for unit tests).
         */
        public String parse(String value, AttributeConfig attributeConfig, VerticalConfig verticalConfig)
                        throws ParseException {
                ParsedWeight w = parseInternal(value, verticalConfig, "all");
                return w == null ? null : formatKg(w.kilograms());
        }

        /**
         * Convenience entry point allowing datasource-aware parsing when the unit is implicit.
         * <p>
         * This is especially useful when a datasource is known to provide raw numeric weights in
         * a given unit (for example grams).
         *
         * @param value         raw value
         * @param attributeConfig attribute configuration (currently not used but kept for API symmetry)
         * @param verticalConfig vertical configuration (used for plausible bounds)
         * @param datasource     datasource/store name (used for default unit hint)
         * @return the weight in kilograms (string), or {@code null} if not parsable
         * @throws ParseException unused for now (best-effort parsing)
         */
        public String parse(String value, AttributeConfig attributeConfig, VerticalConfig verticalConfig, String datasource)
                        throws ParseException {
                ParsedWeight w = parseInternal(value, verticalConfig, datasource);
                return w == null ? null : formatKg(w.kilograms());
        }

        // ----------------------------
        // Internals
        // ----------------------------

        private static final Pattern NUMBER_PATTERN = Pattern.compile("(-?\\d+(?:[\\.,]\\d+)?)");

        // Find explicit unit occurrences, possibly surrounded by spaces.
        private static final Pattern EXPLICIT_UNIT_PATTERN = Pattern.compile(
                        "(?i)(-?\\d+(?:[\\.,]\\d+)?)\\s*(kg|kgs|kilogrammes?|kilograms?|g|gr|grammes?|grams?|mg|milligrammes?|milligrams?|lb|lbs|pounds?|oz|ounces?)\\b");

        // Compound forms like "1 kg 250 g".
        private static final Pattern KG_PART_PATTERN = Pattern.compile("(?i)(-?\\d+(?:[\\.,]\\d+)?)\\s*(kg|kgs|kilogrammes?|kilograms?)\\b");
        private static final Pattern G_PART_PATTERN = Pattern.compile("(?i)(-?\\d+(?:[\\.,]\\d+)?)\\s*(g|gr|grammes?|grams?)\\b");
        private static final Pattern MG_PART_PATTERN = Pattern.compile("(?i)(-?\\d+(?:[\\.,]\\d+)?)\\s*(mg|milligrammes?|milligrams?)\\b");

        private ParsedWeight parseInternal(String rawValue, VerticalConfig verticalConfig, String datasource) {
                if (rawValue == null) {
                        return null;
                }

                final String normalized = normalize(rawValue);
                if (normalized.isBlank()) {
                        return null;
                }

                final WeightRangeKg range = GLOBAL_CONFIG.resolveRangeForVertical(verticalConfig);
                final WeightUnit datasourceHint = GLOBAL_CONFIG.resolveDefaultUnitHintForDatasource(datasource);

                // Level 1: compound forms like "1 kg 250 g" or "500 g 250 mg".
                ParsedWeight compound = parseCompound(normalized, range, datasource);
                if (compound != null) {
                        return compound;
                }

                // Level 1: explicit units (anywhere in the string).
                ParsedWeight explicit = parseExplicitUnit(normalized, range, datasource);
                if (explicit != null) {
                        return explicit;
                }

                // Level 2: numeric-only (or numeric tokens) with inference.
                List<String> numbers = extractNumbers(normalized);
                if (numbers.isEmpty()) {
                        return null;
                }

                // Evaluate each numeric token and keep the best.
                ParsedWeight best = null;
                for (String token : numbers) {
                        final boolean hasDecimal = token.contains(".");
                        final double number;
                        try {
                                number = Double.parseDouble(token);
                        } catch (NumberFormatException nfe) {
                                continue;
                        }
                        ParsedWeight candidate = inferUnit(number, hasDecimal, range, datasourceHint, rawValue, datasource);
                        if (candidate == null) {
                                continue;
                        }
                        if (best == null || candidate.confidence() > best.confidence()) {
                                best = candidate;
                        }
                }

                return best;
        }

        private static String normalize(String raw) {
                String s = raw.trim();

                // Normalize accents and weird spaces.
                s = Normalizer.normalize(s, Normalizer.Form.NFKC);
                s = s.replace('\u00A0', ' '); // NBSP
                s = s.replace('\u202F', ' '); // narrow NBSP

                // Common separators.
                s = s.replace(',', '.');

                // Keep it lower-cased for pattern matching.
                s = s.toLowerCase(Locale.ROOT);

                return s;
        }

        private static List<String> extractNumbers(String normalizedLower) {
                Matcher m = NUMBER_PATTERN.matcher(normalizedLower);
                List<String> numbers = new ArrayList<>();
                while (m.find()) {
                        String token = m.group(1);
                        if (token == null) {
                                continue;
                        }
                        token = token.trim();
                        if (token.isEmpty()) {
                                continue;
                        }
                        numbers.add(token);
                }
                return numbers;
        }

        private static ParsedWeight parseCompound(String normalized, WeightRangeKg range, String datasource) {
                Double kgPart = extractSingleUnit(normalized, KG_PART_PATTERN);
                Double gPart = extractSingleUnit(normalized, G_PART_PATTERN);
                Double mgPart = extractSingleUnit(normalized, MG_PART_PATTERN);

                if (kgPart == null && gPart == null && mgPart == null) {
                        return null;
                }

                double kg = 0.0;
                if (kgPart != null) {
                        kg += WeightUnit.KILOGRAM.toKg(kgPart);
                }
                if (gPart != null) {
                        kg += WeightUnit.GRAM.toKg(gPart);
                }
                if (mgPart != null) {
                        kg += WeightUnit.MILLIGRAM.toKg(mgPart);
                }

                double conf = range.contains(kg) ? 0.99 : 0.85;
                String reason = "compound-units";
                return new ParsedWeight(kg, conf, WeightUnit.KILOGRAM, reason, normalized, datasource);
        }

        private static Double extractSingleUnit(String normalized, Pattern p) {
                Matcher m = p.matcher(normalized);
                if (!m.find()) {
                        return null;
                }
                String token = m.group(1);
                if (token == null) {
                        return null;
                }
                token = token.trim().replace(',', '.');
                try {
                        return Double.parseDouble(token);
                } catch (NumberFormatException nfe) {
                        return null;
                }
        }

        private static ParsedWeight parseExplicitUnit(String normalized, WeightRangeKg range, String datasource) {
                Matcher m = EXPLICIT_UNIT_PATTERN.matcher(normalized);
                ParsedWeight best = null;
                while (m.find()) {
                        final String numberToken = m.group(1);
                        final String unitToken = m.group(2);
                        if (numberToken == null || unitToken == null) {
                                continue;
                        }

                        final double value;
                        try {
                                value = Double.parseDouble(numberToken.replace(',', '.'));
                        } catch (NumberFormatException nfe) {
                                continue;
                        }

                        WeightUnit unit = unitFromToken(unitToken);
                        if (unit == null) {
                                continue;
                        }

                        double kg = unit.toKg(value);
                        double conf = range.contains(kg) ? 0.98 : 0.80;
                        String reason = "explicit-unit:" + unit;
                        ParsedWeight candidate = new ParsedWeight(kg, conf, unit, reason, normalized, datasource);

                        if (best == null) {
                                best = candidate;
                                continue;
                        }

                        // Prefer higher confidence; if tied, prefer the smallest plausible weight.
                        if (candidate.confidence() > best.confidence()) {
                                best = candidate;
                        } else if (Double.compare(candidate.confidence(), best.confidence()) == 0
                                        && candidate.kilograms() < best.kilograms()) {
                                best = candidate;
                        }
                }

                return best;
        }

        private static WeightUnit unitFromToken(String token) {
                if (token == null) {
                        return null;
                }
                String u = token.trim().toLowerCase(Locale.ROOT);
                return switch (u) {
                case "kg", "kgs", "kilogramme", "kilogrammes", "kilogram", "kilograms" -> WeightUnit.KILOGRAM;
                case "g", "gr", "gramme", "grammes", "gram", "grams" -> WeightUnit.GRAM;
                case "mg", "milligramme", "milligrammes", "milligram", "milligrams" -> WeightUnit.MILLIGRAM;
                case "lb", "lbs", "pound", "pounds" -> WeightUnit.POUND;
                case "oz", "ounce", "ounces" -> WeightUnit.OUNCE;
                default -> null;
                };
        }

        private static ParsedWeight inferUnit(double number, boolean hasDecimal, WeightRangeKg range, WeightUnit hint,
                        String rawValue, String datasource) {
                if (!Double.isFinite(number)) {
                        return null;
                }
                if (number < 0) {
                        return null;
                }
                if (number == 0.0) {
                        return null;
                }

                List<WeightUnit> unitsToTry = new ArrayList<>();
                unitsToTry.add(WeightUnit.KILOGRAM);
                unitsToTry.add(WeightUnit.GRAM);
                unitsToTry.add(WeightUnit.MILLIGRAM);
                unitsToTry.add(WeightUnit.POUND);
                unitsToTry.add(WeightUnit.OUNCE);

                Candidate best = null;
                for (WeightUnit unit : unitsToTry) {
                        double kg = unit.toKg(number);
                        double score = scoreCandidate(unit, kg, number, hasDecimal, range, hint);
                        if (best == null || score > best.score) {
                                best = new Candidate(unit, kg, score);
                        }
                }

                if (best == null || best.score < 0.25) {
                        return null;
                }

                double confidence = Math.min(0.97, best.score);
                String reason = "inferred:" + best.unit + ",hint=" + hint + ",score=" + round2(best.score);
                return new ParsedWeight(best.kg, confidence, best.unit, reason, rawValue, datasource);
        }

        /**
         * Scoring heuristic for unit inference.
         * <p>
         * The score is in [0..~1.5]. It is later capped into a confidence.
         */
        private static double scoreCandidate(WeightUnit unit, double kg, double rawNumber, boolean hasDecimal,
                        WeightRangeKg range, WeightUnit hint) {

                // Base plausibility: in range is strongly preferred.
                double score = range.contains(kg) ? 1.0 : 0.0;

                // Distance-based weak plausibility (helps in edge cases).
                if (score == 0.0) {
                        double dist = distanceToRange(kg, range);
                        // If within 20% of bounds, keep some score.
                        if (dist <= 0.20) {
                                score = 0.35;
                        }
                }

                // Datasource hint: strong preference when no unit is present.
                if (hint != null && unit == hint) {
                        score += 0.45;
                }

                // Decimal numbers are very often expressed in kilograms.
                if (hasDecimal && unit == WeightUnit.KILOGRAM) {
                        score += 0.15;
                }

                // Large integers are often grams.
                if (!hasDecimal && unit == WeightUnit.GRAM && rawNumber >= 500) {
                        score += 0.10;
                }

                // Very large integers can be milligrams (less common).
                if (!hasDecimal && unit == WeightUnit.MILLIGRAM && rawNumber >= 50_000) {
                        score += 0.05;
                }

                // Penalize obviously absurd weights.
                if (kg > range.getMaxKg() * 5) {
                        score -= 0.75;
                }

                if (kg < range.getMinKg() / 100) {
                        score -= 0.50;
                }

                return score;
        }

        private static double distanceToRange(double kg, WeightRangeKg range) {
                if (range.contains(kg)) {
                        return 0.0;
                }
                double nearest = kg < range.getMinKg() ? range.getMinKg() : range.getMaxKg();
                double denom = Math.max(1e-9, nearest);
                return Math.abs(kg - nearest) / denom;
        }

        private static String formatKg(double kg) {
                if (!Double.isFinite(kg)) {
                        return null;
                }
                BigDecimal bd = BigDecimal.valueOf(kg).setScale(GLOBAL_CONFIG.getOutputScale(), RoundingMode.HALF_UP)
                                .stripTrailingZeros();
                return bd.toPlainString();
        }

        private static String round2(double v) {
                BigDecimal bd = BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
                return bd.toPlainString();
        }

        /**
         * Attempts to extract a datasource/store name from a {@link SourcedAttribute} without
         * binding to a specific method name (the model may evolve).
         */
        private static String safeGetDatasource(SourcedAttribute src) {
                if (src == null) {
                        return "all";
                }
                Optional<String> ds = safeGetString(src, "getDatasource");
                if (ds.isEmpty()) {
                        ds = safeGetString(src, "getDatasourceName");
                }
                if (ds.isEmpty()) {
                        ds = safeGetString(src, "getStore");
                }
                if (ds.isEmpty()) {
                        ds = safeGetString(src, "getSource");
                }
                return ds.map(d -> d.trim().toLowerCase(Locale.ROOT)).filter(d -> !d.isBlank()).orElse("all");
        }

        private static Optional<String> safeGetString(Object target, String method) {
                try {
                        Object res = target.getClass().getMethod(method).invoke(target);
                        if (res instanceof String s) {
                                return Optional.ofNullable(s);
                        }
                        return Optional.empty();
                } catch (ReflectiveOperationException e) {
                        return Optional.empty();
                }
        }

        private record Candidate(WeightUnit unit, double kg, double score) {
        }
}
