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
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
 *   normalize: true
 * </pre>
 * <p>
 * Parsing strategy per sourced value:
 * <ol>
 *   <li>Normalize whitespace and Unicode.</li>
 *   <li>Extract numeric part and optional unit token using a regex.</li>
 *   <li>Resolve the unit token via {@link UUDCRegistry}; fall back to
 *       {@code defaultUnitHint} when absent.</li>
 *   <li>Convert to the base unit.</li>
 *   <li>Elect one valid conversion by legacy trusted-source rank, then datasource
 *       name, then value, and return it as a {@link String}. Conflicting sources are
 *       logged rather than blended: averaging two sources produced a value neither
 *       of them ever asserted.</li>
 * </ol>
 */
@Component
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

    @Autowired
    public UnitAwareNumericParser(UUDCRegistry uudcRegistry)
    {
        this.uudcRegistry = uudcRegistry;
    }

    /**
     * No-arg constructor for reflective instantiation via
     * {@link AttributeConfig#getParserInstance()}.
     * The registry is injected via Spring after construction when this class is used as
     * a Spring bean; for reflective use, the static singleton is used via the context.
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
        dimension = dimension.toUpperCase(Locale.ROOT);
        String defaultHint = attributeConfig.getParser().getDefaultUnitHint();

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
                Double baseValue = parseToBase(raw, dimension, defaultHint);
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

        Candidate elected = candidates.stream().min(CANDIDATE_ORDER).orElseThrow();

        long distinctValues = candidates.stream().map(candidate -> candidate.baseValue).distinct().count();
        if (distinctValues > 1)
        {
            LOGGER.warn("Conflicting values for attribute '{}' in dimension {}: {}; electing {} from source '{}'",
                    attributeConfig.getKey(), dimension, candidates, elected.baseValue, elected.datasource);
        }

        return formatValue(elected.baseValue);
    }

    /**
     * One source's value already converted to the dimension's base unit.
     *
     * @param datasource contributing datasource name, possibly {@code null}
     * @param baseValue  value expressed in the dimension's base unit
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
     * Election order between conflicting sources: the legacy trusted-source rank
     * decides first, then the datasource name and finally the value itself, so that
     * the same set of contributions always elects the same value whatever order the
     * sources arrived in.
     */
    private static final Comparator<Candidate> CANDIDATE_ORDER = Comparator
            .comparingInt((Candidate candidate) -> SourcableAttribute.trustedSourcePriority(candidate.datasource))
            .thenComparing(candidate -> candidate.datasource, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingDouble(candidate -> candidate.baseValue);

    /**
     * Single-value entry point used by tests and direct callers.
     *
     * @param value          raw string value (e.g. "42 cm", "420mm", "0.42")
     * @param attributeConfig attribute configuration carrying dimension and defaultUnitHint
     * @param verticalConfig  vertical configuration (currently unused; reserved for bounds)
     * @return value in base unit as a String, or {@code null} when unparseable
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
        String defaultHint = attributeConfig.getParser().getDefaultUnitHint();
        Double baseValue = parseToBase(value, dimension.toUpperCase(Locale.ROOT), defaultHint);
        return baseValue == null ? null : formatValue(baseValue);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Double parseToBase(String raw, String dimension, String defaultUnitHint)
    {
        String normalized = normalize(raw);
        if (normalized.isBlank())
        {
            return null;
        }

        Matcher m = VALUE_UNIT_PATTERN.matcher(normalized);
        if (!m.matches())
        {
            return null;
        }

        String numStr = m.group(1).replace(',', '.');
        String unitToken = m.group(2);

        double numericVal;
        if (!IdHelper.isPureDouble(numStr))
        {
            return null;
        }
        numericVal = Double.parseDouble(numStr);

        String effectiveUnit = (unitToken != null && !unitToken.isBlank()) ? unitToken : defaultUnitHint;
        if (effectiveUnit == null || effectiveUnit.isBlank())
        {
            LOGGER.debug("No unit found and no defaultUnitHint for dimension '{}', raw='{}'", dimension, raw);
            return null;
        }

        UnitDefinition def = uudcRegistry.resolveUnit(effectiveUnit, dimension);
        if (def == null)
        {
            return null;
        }

        return numericVal * def.getMultiplier() + def.getOffset();
    }

    private static String normalize(String raw)
    {
        String s = raw.trim();
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        s = s.replace(' ', ' ');
        s = s.replace(' ', ' ');
        s = s.replaceAll("(?<=\\d)\\s*\\.\\s*(?=\\d)", ".");
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
