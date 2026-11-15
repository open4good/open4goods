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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.exceptions.ParseException;
import org.open4goods.model.helper.IdHelper;
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
 *   <li><b>Compound rule</b>: only a true compound is added up, i.e. an integer kilogram part
 *       immediately followed by a gram part below 1000 ("1 kg 250 g" = 1.25 kg). An equivalent
 *       given in parentheses ("3.5 kg (3500 g)") or a second value of the same magnitude
 *       ("3 kg 2 kg", "1 kg 1500 g") is not added: the explicit unit rule applies.</li>
 *   <li><b>Explicit unit rules</b>: if the raw string contains an explicit unit ("kg", "g", "mg",
 *       "lb", "oz" ...), the parser will trust it (with sanity checks against configured bounds).
 *       When several explicit values are found, a value inside the plausible range wins over one
 *       outside it, then the first one in the string wins.</li>
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
 * <h3>Normalization and rejections</h3>
 * <ul>
 *   <li>Thousands groups are joined before extraction: "1 200 g" and "1 200,5 g" (space, no-break
 *       space or thin space between groups of 3 digits) read as 1200 g and 1200.5 g.</li>
 *   <li>A dash between two digits, or glued in front of a digit, <b>anywhere</b> in the value
 *       rejects the whole value: "10-12 kg" and "10 - 12 kg" are ranges, not one measured weight,
 *       and "45 kg, 60-85-60 cm" or "45 kg (ref. WM-1234)" give {@code null} too. Another source of
 *       the same attribute may still provide a value.</li>
 *   <li>A range written in words keeps the value that carries the unit, through the explicit unit
 *       rule: "15 à 30 kg" gives 30, "+ de 100 kg" gives 100. This is the common
 *       format of the "TRANCHE DE POIDS" synonym, so it is deliberately not rejected.</li>
 *   <li>A negative value ("-5 kg") is rejected, and so is any result that is not strictly positive
 *       once rounded to the output scale ("0 kg"), on every parsing path.</li>
 *   <li>Lower plausibility bound: a weight under {@code minKg / 5} of the vertical is rejected,
 *       symmetric to the 5x max check. It catches misreads such as "1.200 g" (thousands dot read as
 *       a decimal, 0.0012 kg) or "0 à 100 g" (0.1 kg) on a TV (minKg 1, floor 0.2 kg), while a
 *       0.1 kg smartphone (minKg 0.08, floor 0.016 kg) stays accepted.</li>
 * </ul>
 *
 * <p>
 * The configuration is exposed as a simple POJO ({@link WeightParserConfiguration}) so it can be
 * bound from YAML/JSON if desired.
 * <p>
 * Specific vertical configurations (min/max range) are now managed in {@link VerticalConfig#getParsers()}.
 * Unresolvable or unsafe weights (exceeding 5x max range) are rejected with a WARN log.
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
         * Provides sensible defaults.
         */
        public static WeightParserConfiguration defaultConfiguration() {
                WeightParserConfiguration cfg = new WeightParserConfiguration();

                // Global plausible range: most product weights fall in [1g ; 2000kg]
                cfg.plausibleRangeByVerticalId.put("all", new WeightRangeKg(0.001, 2000.0));

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
                        final String raw = src.getValue();
                        if (raw == null || raw.isBlank()) {
                                continue;
                        }
                        final String datasource = safeGetDatasource(src);
                        ParsedWeight w = parseInternal(raw, verticalConfig, datasource);
                        if (w != null) {
                                // Safety check: if weight is wildly out of bounds (e.g. > 5x max), we might reject or warn.
                                // Logic moved inside parseInternal/inference, but we can double-check here against absolute safety.
                                WeightRangeKg range = resolveEffectiveRange(verticalConfig);
                                if (w.kilograms() > range.getMaxKg() * 5.0) {
                                	LOGGER.warn("Rejected weight {}kg for '{}' (datasource={}): exceeds 5x max configured ({}kg)", 
                                			formatKg(w.kilograms()), attribute.getName(), datasource, range.getMaxKg());
                                	continue;
                                }
                                
                                parsed.add(w);
                        } else {
                        	LOGGER.debug("Could not parse weight from '{}' (datasource={})", raw, datasource);
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
                                LOGGER.debug(
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
                if (w != null) {
                    WeightRangeKg range = resolveEffectiveRange(verticalConfig);
                    if (w.kilograms() > range.getMaxKg() * 5.0) {
                        return null;
                    }
                }
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
                if (w != null) {
                    WeightRangeKg range = resolveEffectiveRange(verticalConfig);
                    if (w.kilograms() > range.getMaxKg() * 5.0) {
                        return null;
                    }
                }
                return w == null ? null : formatKg(w.kilograms());
        }

        // ----------------------------
        // Internals
        // ----------------------------

        // Unsigned on purpose: negative values and ranges are rejected upfront (RANGE_OR_SIGNED_PATTERN).
        private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)");

        // Find explicit unit occurrences, possibly surrounded by spaces.
        private static final Pattern EXPLICIT_UNIT_PATTERN = Pattern.compile(
                        "(?i)(\\d+(?:[\\.,]\\d+)?)\\s*(kg|kgs|kilogrammes?|kilograms?|g|gr|grammes?|grams?|mg|milligrammes?|milligrams?|lb|lbs|pounds?|oz|ounces?)\\b");

        // True compound "1 kg 250 g": integer kg part, then only whitespace, then a gram part below 1000.
        private static final Pattern COMPOUND_PATTERN = Pattern.compile(
                        "(?i)(?<![\\d.])(\\d+)\\s*(?:kilogrammes?|kilograms?|kgs|kg)\\s*(\\d{1,3}(?:\\.\\d+)?)\\s*(?:grammes?|grams?|gr|g)\\b");

        // Groups of 3 digits separated by a space ("1 200", "12 345 678"), once Unicode spaces are normalized.
        private static final Pattern THOUSANDS_PATTERN = Pattern.compile("(?<![\\d.,])\\d{1,3}(?: \\d{3})+(?!\\d)");

        // A range ("10-12", "10 - 12", any dash) or a number with a dash or minus sign (U+2212) glued to it ("-5").
        // A weight below minKg / LOWER_BOUND_DIVISOR is rejected (mirror of the 5x max check).
        private static final double LOWER_BOUND_DIVISOR = 5.0;

        private static final Pattern RANGE_OR_SIGNED_PATTERN = Pattern.compile("\\d\\s*[\\p{Pd}\\u2212]\\s*\\d|[\\p{Pd}\\u2212]\\d");

        /**
         * Parses one raw value and keeps the result only when it is a strictly positive weight once
         * rounded to the output scale, so that no parsing path can yield a zero or negative weight.
         */
        private ParsedWeight parseInternal(String rawValue, VerticalConfig verticalConfig, String datasource) {
                final ParsedWeight weight = parseCandidate(rawValue, verticalConfig, datasource);
                if (weight == null || !isStrictlyPositive(weight.kilograms())) {
                        return null;
                }
                // Lower plausibility bound, symmetric to the 5x max check: below minKg / 5 the value is a misread.
                final WeightRangeKg range = resolveEffectiveRange(verticalConfig);
                if (weight.kilograms() < range.getMinKg() / LOWER_BOUND_DIVISOR) {
                        LOGGER.debug("Rejected weight '{}' (datasource={}): {}kg is below min/{} ({}kg)", rawValue, datasource,
                                        formatKg(weight.kilograms()), LOWER_BOUND_DIVISOR, range.getMinKg());
                        return null;
                }
                return weight;
        }

        private ParsedWeight parseCandidate(String rawValue, VerticalConfig verticalConfig, String datasource) {
                if (rawValue == null) {
                        return null;
                }

                final String normalized = normalize(rawValue);
                if (normalized.isBlank()) {
                        return null;
                }

                // A range or a negative value is never a single measured weight (see class Javadoc).
                if (RANGE_OR_SIGNED_PATTERN.matcher(normalized).find()) {
                        LOGGER.debug("Rejected weight '{}' (datasource={}): range or negative value", rawValue, datasource);
                        return null;
                }

                final WeightRangeKg range = resolveEffectiveRange(verticalConfig);
                final WeightUnit datasourceHint = GLOBAL_CONFIG.resolveDefaultUnitHintForDatasource(datasource);

                // Level 1: true compound forms like "1 kg 250 g".
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
                        if (!IdHelper.isPureDouble(token)) {
                                continue;
                        }
                        number = Double.parseDouble(token);
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
        
        private WeightRangeKg resolveEffectiveRange(VerticalConfig verticalConfig) {
        	if (verticalConfig != null && verticalConfig.getParsers() != null && verticalConfig.getParsers().getWeight() != null) {
        		VerticalConfig.WeightParserConfig wpc = verticalConfig.getParsers().getWeight();
        		if (wpc.getMinKg() != null && wpc.getMaxKg() != null) {
        			return new WeightRangeKg(wpc.getMinKg(), wpc.getMaxKg());
        		}
        	}
        	return GLOBAL_CONFIG.resolveRangeForVertical(verticalConfig);
        }

        private static String normalize(String raw) {
                String s = raw.trim();

                // Normalize accents and weird spaces.
                s = Normalizer.normalize(s, Normalizer.Form.NFKC);
                s = s.replace('\u00A0', ' '); // NBSP
                s = s.replace('\u202F', ' '); // narrow NBSP
                s = s.replace('\u2009', ' '); // thin space

                // Join thousands groups ("1 200" -> "1200") before the comma becomes a decimal point.
                s = THOUSANDS_PATTERN.matcher(s).replaceAll(m -> m.group().replace(" ", ""));

                // Common separators.
                s = s.replace(',', '.');

                // Fix typos like "43, 1" or "43. 1" where a space was incorrectly added after the decimal separator
                s = s.replaceAll("(?<=\\d)\\s*\\.\\s*(?=\\d)", ".");

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

        /**
         * Adds up a true compound ("1 kg 250 g", see COMPOUND_PATTERN). Returns {@code null}
         * otherwise, so that the explicit unit rule picks a single value.
         */
        private static ParsedWeight parseCompound(String normalized, WeightRangeKg range, String datasource) {
                Matcher m = COMPOUND_PATTERN.matcher(normalized);
                if (!m.find()) {
                        return null;
                }

                double kg = WeightUnit.KILOGRAM.toKg(Double.parseDouble(m.group(1)))
                                + WeightUnit.GRAM.toKg(Double.parseDouble(m.group(2)));

                double conf = range.contains(kg) ? 0.99 : 0.85;
                String reason = "compound-units";
                return new ParsedWeight(kg, conf, WeightUnit.KILOGRAM, reason, normalized, datasource);
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
                        String cleanNum = numberToken.replace(',', '.');
                        if (!IdHelper.isPureDouble(cleanNum)) {
                                continue;
                        }
                        value = Double.parseDouble(cleanNum);

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

                        // Prefer higher confidence (inside the plausible range); if tied, keep the first explicit value.
                        if (candidate.confidence() > best.confidence()) {
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

        private static boolean isStrictlyPositive(double kg) {
                return Double.isFinite(kg) && BigDecimal.valueOf(kg)
                                .setScale(GLOBAL_CONFIG.getOutputScale(), RoundingMode.HALF_UP).signum() > 0;
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
                String ds = src.getDataSourcename();
                return ds != null && !ds.isBlank() ? ds.trim().toLowerCase(Locale.ROOT) : "all";
        }

        private record Candidate(WeightUnit unit, double kg, double score) {
        }
}
