package org.open4goods.api.services.aggregation.services.realtime.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.exceptions.ParseException;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalConfig.WeightParserConfig;
import org.open4goods.model.vertical.VerticalConfig.ParsersConfig;


class WeightParserTest {

    private final WeightParser parser = new WeightParser();

    @Test
    void parseExplicitUnits() throws ParseException {
        VerticalConfig vc = new VerticalConfig(); // defaults from _default.yml simulated
        vc.setParsers(new ParsersConfig());
        vc.getParsers().getWeight().setMinKg(0.001);
        vc.getParsers().getWeight().setMaxKg(2000.0);

        assertThat(parser.parse("10 kg", null, vc)).isEqualTo("10");
        assertThat(parser.parse("500g", null, vc)).isEqualTo("0.5");
        assertThat(parser.parse("2.5 lbs", null, vc)).isEqualTo("1.133981"); // ~1.134 kg (no trailing zeros usually unless it rounds up to .000)
    }

    @Test
    void parseCompoundUnits() throws ParseException {
        VerticalConfig vc = new VerticalConfig();
        vc.setParsers(new ParsersConfig());
        vc.getParsers().getWeight().setMinKg(0.001);
        vc.getParsers().getWeight().setMaxKg(2000.0);

        assertThat(parser.parse("1 kg 500 g", null, vc)).isEqualTo("1.5");
    }

    @Test
    void parseImplicitUnitsWithVerticalDefaults() throws ParseException {
        VerticalConfig tvConfig = new VerticalConfig();
        ParsersConfig pc = new ParsersConfig();
        WeightParserConfig wpc = new WeightParserConfig();
        wpc.setMinKg(0.1);
        wpc.setMaxKg(200.0); // TV range
        pc.setWeight(wpc);
        tvConfig.setParsers(pc);

        // "55" -> strongly inferred as kg for TV (55kg is plausible, 55g is not)
        // actually 55g = 0.055kg which is < minKg (0.1), so 55g is penalized.
        // 55kg is in range.
        assertThat(parser.parse("55", null, tvConfig)).isEqualTo("55");

        // "20000" -> 20000kg is out of bounds. 20000g = 20kg is in range.
        assertThat(parser.parse("20000", null, tvConfig)).isEqualTo("20");
    }

    @Test
    void rejectionOutOfBounds() throws ParseException {
        VerticalConfig strictConfig = new VerticalConfig();
        ParsersConfig pc = new ParsersConfig();
        WeightParserConfig wpc = new WeightParserConfig();
        wpc.setMinKg(1.0);
        wpc.setMaxKg(10.0);
        pc.setWeight(wpc);
        strictConfig.setParsers(pc);

        // 100 kg is > 5x max (50kg), so it should be rejected by safety check
        assertThat(parser.parse("100 kg", null, strictConfig)).isNull();
        
        // 12 kg is slightly out of bounds but < 5x max. 
        assertThat(parser.parse("12 kg", null, strictConfig)).isNotNull();
    }
    
    @Test
    void datasourceHint() throws ParseException {
        VerticalConfig vc = new VerticalConfig();
        vc.setParsers(new ParsersConfig());
        vc.getParsers().getWeight().setMinKg(0.001);
        vc.getParsers().getWeight().setMaxKg(2000.0);
        
        // verify simple parse with datasource
        assertThat(parser.parse("100", null, vc, "test-source")).isEqualTo("100"); // assumes kg
    }

    @Test
    void parseWithSpaceAfterComma() throws ParseException {
        VerticalConfig vc = new VerticalConfig();
        vc.setParsers(new ParsersConfig());
        vc.getParsers().getWeight().setMinKg(0.001);
        vc.getParsers().getWeight().setMaxKg(2000.0);

        assertThat(parser.parse("43, 1 KG", null, vc)).isEqualTo("43.1");
        assertThat(parser.parse("43 ,1 KG", null, vc)).isEqualTo("43.1");
        assertThat(parser.parse("43 , 1 KG", null, vc)).isEqualTo("43.1");
    }

    @Test
    void parenthesisedEquivalentIsNotAdded() throws ParseException {
        VerticalConfig vc = defaultRangeConfig();

        // GOU-2275: "3.5 kg (3500 g)" used to give 7 kg (first kg + first g).
        assertThat(parser.parse("3.5 kg (3500 g)", null, vc)).isEqualTo("3.5");
        assertThat(parser.parse("3500 g (3.5 kg)", null, vc)).isEqualTo("3.5");
    }

    @Test
    void trueCompoundIsAdded() throws ParseException {
        VerticalConfig vc = defaultRangeConfig();

        assertThat(parser.parse("1 kg 250 g", null, vc)).isEqualTo("1.25");
        assertThat(parser.parse("1kg250g", null, vc)).isEqualTo("1.25");
    }

    @Test
    void secondValueOfSameMagnitudeIsNotAdded() throws ParseException {
        VerticalConfig vc = defaultRangeConfig();

        // Not a compound: the gram part is not below 1000, or both parts are in kg. The first explicit value wins.
        assertThat(parser.parse("1 kg 1500 g", null, vc)).isEqualTo("1");
        assertThat(parser.parse("3 kg 2 kg", null, vc)).isEqualTo("3");
        assertThat(parser.parse("12 kg brut, 10 kg net", null, vc)).isEqualTo("12");
    }

    @Test
    void thousandsGroupsAreJoined() throws ParseException {
        VerticalConfig vc = defaultRangeConfig();

        // GOU-2275: "1 200 g" used to give 0.2 kg (only "200 g" was read).
        assertThat(parser.parse("1 200 g", null, vc)).isEqualTo("1.2");
        assertThat(parser.parse("1\u00A0200 g", null, vc)).isEqualTo("1.2");
        assertThat(parser.parse("1\u202F200 g", null, vc)).isEqualTo("1.2");
        assertThat(parser.parse("1\u2009200 g", null, vc)).isEqualTo("1.2");
        assertThat(parser.parse("1 200,5 g", null, vc)).isEqualTo("1.2005");
        assertThat(parser.parse("5 000 kg", null, vc)).isEqualTo("5000");
    }

    @Test
    void rangeIsRejected() throws ParseException {
        VerticalConfig vc = defaultRangeConfig();

        // GOU-2275: "10-12 kg" used to give -12 kg. A range is not a single weight: it is rejected.
        assertThat(parser.parse("10-12 kg", null, vc)).isNull();
        assertThat(parser.parse("10 - 12 kg", null, vc)).isNull();
        assertThat(parser.parse("10\u201312 kg", null, vc)).isNull();
        assertThat(parser.parse("10-12", null, vc)).isNull();
    }

    @Test
    void negativeOrZeroWeightIsRejected() throws ParseException {
        VerticalConfig vc = defaultRangeConfig();

        assertThat(parser.parse("-5 kg", null, vc)).isNull();
        assertThat(parser.parse("-5", null, vc)).isNull();
        assertThat(parser.parse("\u22125 kg", null, vc)).isNull();
        assertThat(parser.parse("0 kg", null, vc)).isNull();
        assertThat(parser.parse("0 g", null, vc)).isNull();
        assertThat(parser.parse("0.1 mg", null, vc)).isNull();
    }

    @Test
    void productAttributePathRejectsRangesAndNegatives() throws ParseException {
        VerticalConfig vc = defaultRangeConfig();

        ProductAttribute onlyInvalid = attribute("WEIGHT", "a:10-12 kg", "b:-5 kg", "c:0 kg");
        assertThat(parser.parse(onlyInvalid, null, vc)).isNull();

        ProductAttribute mixed = attribute("WEIGHT", "a:10-12 kg", "b:-5 kg", "c:1 200 g");
        assertThat(parser.parse(mixed, null, vc)).isEqualTo("1.2");
        assertThat(parser.parse(attribute("WEIGHT", "a:3.5 kg (3500 g)"), null, vc)).isEqualTo("3.5");
        assertThat(parser.parse(attribute("WEIGHT", "a:1 200 g"), null, vc)).isEqualTo("1.2");
    }

    private VerticalConfig defaultRangeConfig() {
        VerticalConfig vc = new VerticalConfig();
        vc.setParsers(new ParsersConfig());
        vc.getParsers().getWeight().setMinKg(0.001);
        vc.getParsers().getWeight().setMaxKg(2000.0);
        return vc;
    }

    /**
     * Builds an attribute from {@code datasource:value} pairs.
     */
    private ProductAttribute attribute(String name, String... sourcedValues) {
        ProductAttribute attribute = new ProductAttribute();
        attribute.setName(name);
        for (String sourcedValue : sourcedValues) {
            String[] parts = sourcedValue.split(":", 2);
            SourcedAttribute source = new SourcedAttribute();
            source.setName(name);
            source.setDataSourcename(parts[0]);
            source.setValue(parts[1]);
            attribute.addSourceAttribute(source);
        }
        return attribute;
    }
}
