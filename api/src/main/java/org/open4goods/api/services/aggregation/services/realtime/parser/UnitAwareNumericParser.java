package org.open4goods.api.services.aggregation.services.realtime.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.open4goods.api.services.uudc.UUDCRegistry;
import org.open4goods.api.services.uudc.UnitDefinition;
import org.open4goods.api.util.SpringContextHolder;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcableAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.exceptions.ParseException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributeParser;
import org.open4goods.model.vertical.AttributeParserConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalConfig.NumericBoundsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic numeric parser that converts raw attribute values to a dimension's base unit
 * using the {@link UUDCRegistry}.
 * <p>
 * Configuration in the attribute YAML:
 * <pre>
 * parser:
 *   clazz: org.open4goods.api.services.aggregation.services.realtime.parser.UnitAwareNumericParser
 *   dimension: "LENGTH"       # required: physical dimension name
 *   defaultUnitHint: "cm"    # applied when the raw value carries no unit
 *   unitlessHints: ["cm", "mm"]  # optional: units tried, in order, for a value without unit
 *   targetUnit: "cm"         # optional: unit of the stored value (base unit otherwise)
 *   normalize: true
 * </pre>
 * <p>
 * Parsing strategy per sourced value:
 * <ol>
 *   <li>Normalize whitespace and Unicode.</li>
 *   <li>Extract numeric part and optional unit token using a regex.</li>
 *   <li>Resolve the unit token via {@link UUDCRegistry}. A value without a unit is read
 *       with the first of {@code unitlessHints} that gives a value inside the vertical
 *       bounds of the attribute ({@code parsers.bounds}), and with {@code defaultUnitHint}
 *       otherwise: Icecat sends "600" (mm) and EPREL "60" (cm) for the same width.</li>
 *   <li>Convert to the base unit, then to {@code targetUnit} when set.</li>
 *   <li>Elect one valid conversion by legacy trusted-source rank, then datasource
 *       name, then value, and return it as a {@link String}. Conflicting sources are
 *       logged rather than blended: averaging two sources produced a value neither
 *       of them ever asserted.</li>
 * </ol>
 */
public class UnitAwareNumericParser extends AttributeParser
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitAwareNumericParser.class);

    /**
     * Matches: optional sign, integer or decimal number, optional whitespace,
     * optional unit token (letters, degree symbol, superscripts, slash, micro-sign).
     */
    private static final Pattern VALUE_UNIT_PATTERN = Pattern.compile(
            "(?i)^(-?\\d+(?:[.,]\\d+)?)\\s*([a-z°²³/μ\"]+)?$");

    private static final int OUTPUT_SCALE = 6;

    private final UUDCRegistry uudcRegistry;

    /**
     * Builds the parser on a given registry, without a Spring context. Used by the unit tests.
     *
     * @param uudcRegistry unit registry used to resolve and convert units
     */
    UnitAwareNumericParser(UUDCRegistry uudcRegistry)
    {
        this.uudcRegistry = uudcRegistry;
    }

    /**
     * Constructor used in production: {@link AttributeConfig#getParserInstance()} instantiates the
     * parser reflectively, and the registry is the Spring bean read through {@link SpringContextHolder}.
     */
    public UnitAwareNumericParser()
    {
        this.uudcRegistry = SpringContextHolder.getBean(UUDCRegistry.class);
    }

    @Override
    public String parse(ProductAttribute attr, AttributeConfig attributeConfig, VerticalConfig verticalConfig)
            throws ParseException
    {
        if (attr == null || attr.getSource() == null || attr.getSource().isEmpty())
        {
            return null;
        }

        String dimension = attributeConfig.getParser().getDimension();
        if (dimension == null || dimension.isBlank())
        {
            LOGGER.warn("UnitAwareNumericParser invoked for attribute '{}' without a dimension configured",
                    attributeConfig.getKey());
            return null;
        }
        List<Candidate> candidates = new ArrayList<>();
        for (SourcedAttribute src : attr.getSource())
        {
            String raw = getStringValue(src);
            if (raw == null || raw.isBlank())
            {
                continue;
            }
            try
            {
                Double baseValue = parseToTarget(raw, attributeConfig, verticalConfig);
                if (baseValue != null && Double.isFinite(baseValue) && baseValue >= 0)
                {
                    candidates.add(new Candidate(src.getDataSourcename(), baseValue));
                }
            }
            catch (Exception e)
            {
                LOGGER.debug("Could not parse '{}' for attribute '{}': {}", raw, attributeConfig.getKey(),
                        e.getMessage());
            }
        }

        if (candidates.isEmpty())
        {
            return null;
        }

        Candidate elected = candidates.stream().min(candidateOrder(attributeConfig.getPreferredSources())).orElseThrow();

        long distinctValues = candidates.stream().map(candidate -> candidate.baseValue).distinct().count();
        if (distinctValues > 1)
        {
            LOGGER.warn("Conflicting values for attribute '{}' in dimension {}: {}; electing {} from source '{}'",
                    attributeConfig.getKey(), dimension.toUpperCase(Locale.ROOT), candidates, elected.baseValue,
                    elected.datasource);
        }

        return formatValue(elected.baseValue);
    }

    /**
     * One source's value already converted to the stored unit.
     *
     * @param datasource contributing datasource name, possibly {@code null}
     * @param baseValue  value expressed in the stored unit ({@code targetUnit}, else the dimension's base unit)
     */
    private record Candidate(String datasource, double baseValue)
    {
        @Override
        public String toString()
        {
            return datasource + "=" + baseValue;
        }
    }

    /**
     * Election order between conflicting sources: the trusted-source rank (the attribute's
     * {@code preferredSources}, else the legacy order) decides first, then the datasource name and finally the value itself, so that
     * the same set of contributions always elects the same value whatever order the
     * sources arrived in.
     */
    private static Comparator<Candidate> candidateOrder(List<String> priority)
    {
        return Comparator
                .comparingInt((Candidate candidate) -> SourcableAttribute.trustedSourcePriority(candidate.datasource, priority))
                .thenComparing(candidate -> candidate.datasource, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingDouble(candidate -> candidate.baseValue);
    }

    /**
     * Single-value entry point used by tests and direct callers.
     *
     * @param value          raw string value (e.g. "42 cm", "420mm", "0.42")
     * @param attributeConfig attribute configuration carrying dimension, unit hints and target unit
     * @param verticalConfig  vertical configuration, whose bounds select the unit of a unitless value
     * @return value in the stored unit as a String, or {@code null} when unparseable
     */
    @Override
    public String parse(String value, AttributeConfig attributeConfig, VerticalConfig verticalConfig)
            throws ParseException
    {
        if (value == null || value.isBlank())
        {
            return null;
        }
        String dimension = attributeConfig.getParser().getDimension();
        if (dimension == null || dimension.isBlank())
        {
            return null;
        }
        Double storedValue = parseToTarget(value, attributeConfig, verticalConfig);
        return storedValue == null ? null : formatValue(storedValue);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Reads one raw value in the stored unit of the attribute.
     *
     * @return the value in {@code targetUnit} (base unit when unset), or {@code null} when unparseable
     */
    private Double parseToTarget(String raw, AttributeConfig attributeConfig, VerticalConfig verticalConfig)
    {
        AttributeParserConfig parserConfig = attributeConfig.getParser();
        String dimension = parserConfig.getDimension().toUpperCase(Locale.ROOT);
        String normalized = normalize(raw);
        Matcher m = VALUE_UNIT_PATTERN.matcher(normalized);
        if (normalized.isBlank() || !m.matches())
        {
            return null;
        }
        String numStr = m.group(1).replace(',', '.');
        if (!IdHelper.isPureDouble(numStr))
        {
            return null;
        }
        double numericVal = Double.parseDouble(numStr);
        String unitToken = m.group(2);
        if (unitToken != null && !unitToken.isBlank())
        {
            return convert(numericVal, unitToken, dimension, parserConfig.getTargetUnit());
        }

        String defaultHint = parserConfig.getDefaultUnitHint();
        NumericBoundsConfig bounds = verticalConfig == null || verticalConfig.getParsers() == null ? null
                : verticalConfig.getParsers().boundsFor(attributeConfig.getKey());
        if (bounds != null)
        {
            for (String hint : parserConfig.getUnitlessHints())
            {
                Double candidate = convert(numericVal, hint, dimension, parserConfig.getTargetUnit());
                if (candidate != null && bounds.contains(candidate))
                {
                    return candidate;
                }
            }
        }
        if (defaultHint == null || defaultHint.isBlank())
        {
            LOGGER.debug("No unit found and no defaultUnitHint for dimension '{}', raw='{}'", dimension, raw);
            return null;
        }
        return convert(numericVal, defaultHint, dimension, parserConfig.getTargetUnit());
    }

    /**
     * Converts a value from {@code unit} to {@code targetUnit}, through the dimension's base unit.
     *
     * @return the converted value, or {@code null} when a unit is unknown in the dimension
     */
    private Double convert(double value, String unit, String dimension, String targetUnit)
    {
        UnitDefinition from = uudcRegistry.resolveUnit(unit, dimension);
        if (from == null)
        {
            return null;
        }
        double base = value * from.getMultiplier() + from.getOffset();
        if (targetUnit == null || targetUnit.isBlank())
        {
            return base;
        }
        UnitDefinition to = uudcRegistry.resolveUnit(targetUnit, dimension);
        if (to == null || to.getMultiplier() == 0)
        {
            return null;
        }
        return (base - to.getOffset()) / to.getMultiplier();
    }

    private static String normalize(String raw)
    {
        String s = raw.trim();
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        s = s.replace(' ', ' ');
        s = s.replace(' ', ' ');
        s = s.replaceAll("(?<=\\d)\\s*\\.\\s*(?=\\d)", ".");
        // thousands grouped by spaces: "1 200 W" is 1200 W
        s = s.replaceAll("(?<=\\d)\\s(?=\\d{3}(?!\\d))", "");
        return s.trim();
    }

    private static String formatValue(double value)
    {
        if (!Double.isFinite(value))
        {
            return null;
        }
        return BigDecimal.valueOf(value)
                .setScale(OUTPUT_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String getStringValue(SourcedAttribute src) {
        return src == null ? null : src.getValue();
    }
}
