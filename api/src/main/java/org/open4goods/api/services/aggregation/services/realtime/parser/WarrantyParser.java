package org.open4goods.api.services.aggregation.services.realtime.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * Parses warranty strings into a standardized duration in YEARS (Double).
 * Supports explicit years ("2 ans", "2 years"), months ("24 months"), and implicit numeric values.
 * <p>
 * Configuration (bounds) is managed via {@link VerticalConfig.WarrantyParserConfig}.
 */
public class WarrantyParser extends AttributeParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarrantyParser.class);
    
    // Default safe range if not configured: 0 to 10 years
    private static final double DEFAULT_MIN_YEARS = 0.0;
    private static final double DEFAULT_MAX_YEARS = 10.0;

    // Matches numbers with optional decimals
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(-?\\d+(?:[\\.,]\\d+)?)");

    // Matches explicit units
    private static final Pattern YEAR_PATTERN = Pattern.compile("(?i)(-?\\d+(?:[\\.,]\\d+)?)\\s*(ans?|years?|yrs?|a|y)\\b");
    private static final Pattern MONTH_PATTERN = Pattern.compile("(?i)(-?\\d+(?:[\\.,]\\d+)?)\\s*(mois?|months?|m)\\b");

    @Override
    public String parse(ProductAttribute attribute, AttributeConfig attributeConfig, VerticalConfig verticalConfig)
            throws ParseException {

        if (attribute == null || attribute.getSource() == null || attribute.getSource().isEmpty()) {
            return null;
        }

        List<Double> candidates = new ArrayList<>();

        for (SourcedAttribute src : attribute.getSource()) {
            String raw = safeGetString(src, "getValue");
            if (raw == null || raw.isBlank()) {
                continue;
            }

            Double val = parseInternal(raw, verticalConfig);
            if (val != null) {
                candidates.add(val);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Return the max warranty found (optimistic approach for beneficial attributes)
        // Or should we vote? For warranty, usually the "best" (longest) valid warranty mentioned is plausible,
        // often manufacturer warranty vs reseller.
        // Let's take the MAX valid warranty found.
        double best = candidates.stream().mapToDouble(d -> d).max().orElse(0.0);
        
        return formatYears(best);
    }
    
    /**
     * Convenience method for unit testing.
     */
    public String parse(String value, AttributeConfig attributeConfig, VerticalConfig verticalConfig) throws ParseException {
        Double d = parseInternal(value, verticalConfig);
        return d == null ? null : formatYears(d);
    }

    private Double parseInternal(String raw, VerticalConfig verticalConfig) {
        String normalized = normalize(raw);
        if (normalized.isBlank()) {
            return null;
        }

        // Resolve range
        Range validRange = resolveRange(verticalConfig);

        // Try explicit patterns first
        // If an explicit unit is present, we MUST respect it. If it's out of bounds, we reject it.
        // We do NOT fallback to implicit interpretation if an explicit unit was understood but rejected.
        
        Double yearsById = parseExplicitStrict(normalized, YEAR_PATTERN, 1.0, validRange);
        if (yearsById != null) {
            return Double.isNaN(yearsById) ? null : yearsById; 
        }

        Double yearsByMonth = parseExplicitStrict(normalized, MONTH_PATTERN, 1.0 / 12.0, validRange);
        if (yearsByMonth != null) {
            return Double.isNaN(yearsByMonth) ? null : yearsByMonth;
        }
        
        // Try implicit numeric
        Double number = extractFirstNumber(normalized);
        if (number != null) {
            // Hypothesis 1: It is years
            if (validRange.contains(number)) {
                return number;
            }
            // Hypothesis 2: It is months
            double converted = number / 12.0;
            if (validRange.contains(converted)) {
                return converted;
            }
        }
        
        LOGGER.warn("Could not parse warranty '{}' within range [{}, {}]", raw, validRange.min, validRange.max);
        return null;
    }

    /**
     * Returns:
     * - valid Double value if matched and in range
     * - Double.NaN if matched but out of range (signal to stop)
     * - null if no match found
     */
    private Double parseExplicitStrict(String normalized, Pattern pattern, double factorToYears, Range validRange) {
        Matcher m = pattern.matcher(normalized);
        if (m.find()) {
            String valStr = m.group(1).replace(',', '.');
            try {
                double val = Double.parseDouble(valStr);
                double years = val * factorToYears;
                if (validRange.contains(years)) {
                    return years;
                } else {
                    return Double.NaN; // Matched but rejected
                }
            } catch (NumberFormatException e) {
                // ignore, treat as no match or fall through? 
                // If it matched regex but failed strict parsing, maybe fallback? 
                // Regex guarantees digits, so ParseDouble shouldn't fail unless overflow.
            }
        }
        return null;
    }
    
    private Double extractFirstNumber(String normalized) {
        Matcher m = NUMBER_PATTERN.matcher(normalized);
        if (m.find()) {
             String valStr = m.group(1).replace(',', '.');
             try {
                 return Double.parseDouble(valStr);
             } catch (NumberFormatException e) {
                 return null;
             }
        }
        return null;
    }

    private Range resolveRange(VerticalConfig vc) {
        double min = DEFAULT_MIN_YEARS;
        double max = DEFAULT_MAX_YEARS;
        
        if (vc != null && vc.getParsers() != null && vc.getParsers().getWarranty() != null) {
            VerticalConfig.WarrantyParserConfig wpc = vc.getParsers().getWarranty();
            if (wpc.getMinYears() != null) min = wpc.getMinYears();
            if (wpc.getMaxYears() != null) max = wpc.getMaxYears();
        }
        return new Range(min, max);
    }

    private String normalize(String s) {
        if (s == null) return "";
        s = s.trim();
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        s = s.toLowerCase(Locale.ROOT);
        return s;
    }

    private String formatYears(double years) {
        BigDecimal bd = BigDecimal.valueOf(years).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.toPlainString();
    }
    
    private String safeGetString(SourcedAttribute src, String method) {
        try {
            return (String) src.getClass().getMethod(method).invoke(src);
        } catch (Exception e) {
            return null;
        }
    }

    private record Range(double min, double max) {
        boolean contains(double val) {
            return val >= min && val <= max;
        }
    }
}
