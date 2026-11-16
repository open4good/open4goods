package org.open4goods.services.opendata.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Tells whether the raw value of a datasource is the published value of a criterion (GOU-2313).
 * <p>
 * The published WARRANTY (years) and WEIGHT (kilograms) come from the aggregation parsers, which live in the
 * {@code api} module and are not reachable from here. The raw value of a source is therefore converted to the unit of
 * the published value, then compared numerically at the parsers' output scale (2 decimals): "24 mois" is 2 years,
 * "52000 g" is 52 kg, "5,20 kg" is 5.2 kg. A value without unit is read in the published unit, or as months
 * (WARRANTY) or grams (WEIGHT). Other criteria keep the text comparison.
 */
final class SourceValueComparator {

    private static final Pattern MEASURE = Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\s*([a-zA-Zéû]*)\\.?$");
    private static final int SCALE = 2;

    private SourceValueComparator() {
    }

    static boolean matches(String criterion, String sourceValue, String published) {
        if (sourceValue == null || published == null) {
            return false;
        }
        if (StringUtils.normalizeSpace(sourceValue).equalsIgnoreCase(StringUtils.normalizeSpace(published))) {
            return true;
        }
        BigDecimal target = number(published);
        if (target == null) {
            return false;
        }
        List<BigDecimal> candidates = switch (criterion) {
            case "WARRANTY" -> warrantyYears(sourceValue);
            case "WEIGHT" -> weightKilograms(sourceValue);
            default -> List.of();
        };
        BigDecimal expected = target.setScale(SCALE, RoundingMode.HALF_UP);
        return candidates.stream().anyMatch(c -> c.setScale(SCALE, RoundingMode.HALF_UP).compareTo(expected) == 0);
    }

    private static BigDecimal number(String text) {
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<BigDecimal> warrantyYears(String raw) {
        Matcher m = MEASURE.matcher(StringUtils.normalizeSpace(raw).toLowerCase(Locale.ROOT));
        if (!m.matches()) {
            return List.of();
        }
        BigDecimal n = number(m.group(1));
        BigDecimal months = n.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        return switch (m.group(2)) {
            case "m", "mois", "month", "months" -> List.of(months);
            case "a", "an", "ans", "y", "yr", "yrs", "year", "years" -> List.of(n);
            case "" -> List.of(n, months);
            default -> List.of();
        };
    }

    private static List<BigDecimal> weightKilograms(String raw) {
        Matcher m = MEASURE.matcher(StringUtils.normalizeSpace(raw).toLowerCase(Locale.ROOT));
        if (!m.matches()) {
            return List.of();
        }
        BigDecimal n = number(m.group(1));
        BigDecimal grams = n.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        return switch (m.group(2)) {
            case "kg", "kgs", "kilo", "kilos" -> List.of(n);
            case "g", "gr", "gramme", "grammes" -> List.of(grams);
            case "mg" -> List.of(n.divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP));
            case "lb", "lbs" -> List.of(n.multiply(new BigDecimal("0.45359237")));
            case "oz" -> List.of(n.multiply(new BigDecimal("0.0283495231")));
            case "" -> List.of(n, grams);
            default -> List.of();
        };
    }
}
