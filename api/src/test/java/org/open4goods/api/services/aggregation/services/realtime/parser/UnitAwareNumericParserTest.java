package org.open4goods.api.services.aggregation.services.realtime.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.services.uudc.UUDCRegistry;
import org.open4goods.model.exceptions.ParseException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributeParserConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;

/**
 * Unit tests for {@link UnitAwareNumericParser}.
 * <p>
 * Uses a fully initialized {@link UUDCRegistry} loaded from {@code dimensions.yml}.
 */
class UnitAwareNumericParserTest
{
    private UnitAwareNumericParser parser;
    private AttributeConfig lengthConfig;
    private VerticalConfig verticalConfig;

    @BeforeEach
    void setUp()
    {
        UUDCRegistry registry = new UUDCRegistry(new SerialisationService());
        registry.init();
        parser = new UnitAwareNumericParser(registry);

        AttributeParserConfig parserCfg = new AttributeParserConfig();
        parserCfg.setDimension("LENGTH");
        parserCfg.setDefaultUnitHint("cm");

        lengthConfig = new AttributeConfig();
        lengthConfig.setParser(parserCfg);

        verticalConfig = new VerticalConfig();
    }

    @Test
    void parseCentimeters()
            throws ParseException
    {
        String result = parser.parse("42 cm", lengthConfig, verticalConfig);
        assertThat(result).isEqualTo("0.42");
    }

    @Test
    void parseMillimeters()
            throws ParseException
    {
        String result = parser.parse("420 mm", lengthConfig, verticalConfig);
        assertThat(result).isEqualTo("0.42");
    }

    @Test
    void parseMeters()
            throws ParseException
    {
        String result = parser.parse("0.42 m", lengthConfig, verticalConfig);
        assertThat(result).isEqualTo("0.42");
    }

    @Test
    void parseNoUnitFallsBackToDefaultHint()
            throws ParseException
    {
        String result = parser.parse("42", lengthConfig, verticalConfig);
        // 42 cm = 0.42 m
        assertThat(result).isEqualTo("0.42");
    }

    @Test
    void parseCommaSeparatedDecimal()
            throws ParseException
    {
        String result = parser.parse("42,5 cm", lengthConfig, verticalConfig);
        assertThat(result).isEqualTo("0.425");
    }

    @Test
    void parseNullReturnsNull()
            throws ParseException
    {
        assertThat(parser.parse((String) null, lengthConfig, verticalConfig)).isNull();
    }

    @Test
    void parseBlankReturnsNull()
            throws ParseException
    {
        assertThat(parser.parse("   ", lengthConfig, verticalConfig)).isNull();
    }

    @Test
    void parseUnknownUnitReturnsNull()
            throws ParseException
    {
        String result = parser.parse("42 furlong", lengthConfig, verticalConfig);
        assertThat(result).isNull();
    }

    @Test
    void parseMassInKg()
            throws ParseException
    {
        AttributeParserConfig massCfg = new AttributeParserConfig();
        massCfg.setDimension("MASS");
        massCfg.setDefaultUnitHint("kg");
        AttributeConfig massAttrConfig = new AttributeConfig();
        massAttrConfig.setParser(massCfg);

        String result = parser.parse("1.5 kg", massAttrConfig, verticalConfig);
        assertThat(result).isEqualTo("1.5");
    }

    @Test
    void parseMassInGrams()
            throws ParseException
    {
        AttributeParserConfig massCfg = new AttributeParserConfig();
        massCfg.setDimension("MASS");
        massCfg.setDefaultUnitHint("g");
        AttributeConfig massAttrConfig = new AttributeConfig();
        massAttrConfig.setParser(massCfg);

        String result = parser.parse("500 g", massAttrConfig, verticalConfig);
        assertThat(result).isEqualTo("0.5");
    }
}
