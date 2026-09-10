package org.open4goods.api.services.aggregation.services.realtime.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import org.open4goods.api.services.uudc.UUDCRegistry;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
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

    /**
     * Builds a raw attribute whose contributions arrive in the given order.
     *
     * @param datasourceAndValues alternating datasource name and raw value
     */
    private static ProductAttribute attributeWith(String... datasourceAndValues)
    {
        ProductAttribute attribute = new ProductAttribute();
        attribute.setName("LARGEUR");
        for (int index = 0; index < datasourceAndValues.length; index += 2)
        {
            SourcedAttribute source = new SourcedAttribute();
            source.setDataSourcename(datasourceAndValues[index]);
            source.setValue(datasourceAndValues[index + 1]);
            source.setName("LARGEUR");
            attribute.addSourceAttribute(source);
        }
        return attribute;
    }

    @Test
    void parseElectsTheTrustedSourceValue_ratherThanBlendingConflictingSources()
            throws ParseException
    {
        // eprel says 55 cm, a merchant says 60 cm: the average 57.5 cm was a value no
        // source ever asserted.
        String result = parser.parse(attributeWith("eprel", "55 cm", "some-merchant.com", "60 cm"), lengthConfig,
                verticalConfig);

        assertThat(result).isEqualTo("0.55");
    }

    @Test
    void parseIsStableWhateverOrderTheSourcesArriveIn()
            throws ParseException
    {
        String[] first = {"some-merchant.com", "60 cm", "icecat.biz", "58 cm", "eprel", "55 cm"};
        String[] second = {"eprel", "55 cm", "some-merchant.com", "60 cm", "icecat.biz", "58 cm"};
        String[] third = {"icecat.biz", "58 cm", "eprel", "55 cm", "some-merchant.com", "60 cm"};

        List<String> results = List.of(parser.parse(attributeWith(first), lengthConfig, verticalConfig),
                parser.parse(attributeWith(second), lengthConfig, verticalConfig),
                parser.parse(attributeWith(third), lengthConfig, verticalConfig));

        assertThat(results).containsExactly("0.55", "0.55", "0.55");
    }

    @Test
    void parseKeepsTheAgreedValue_whenSourcesExpressItInDifferentUnits()
            throws ParseException
    {
        String result = parser.parse(attributeWith("some-merchant.com", "55 cm", "another-merchant.com", "0.55 m"),
                lengthConfig, verticalConfig);

        assertThat(result).isEqualTo("0.55");
    }

    @Test
    void parseElectsDeterministically_whenNoSourceIsTrusted()
            throws ParseException
    {
        String direct = parser.parse(attributeWith("zzz-merchant.com", "60 cm", "aaa-merchant.com", "58 cm"),
                lengthConfig, verticalConfig);
        String reversed = parser.parse(attributeWith("aaa-merchant.com", "58 cm", "zzz-merchant.com", "60 cm"),
                lengthConfig, verticalConfig);

        assertThat(direct).isEqualTo("0.58");
        assertThat(reversed).isEqualTo("0.58");
    }

    @Test
    void parseIgnoresUnparseableSources()
            throws ParseException
    {
        String result = parser.parse(attributeWith("eprel", "not a length", "some-merchant.com", "60 cm"),
                lengthConfig, verticalConfig);

        assertThat(result).isEqualTo("0.6");
    }
}
