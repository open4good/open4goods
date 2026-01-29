package org.open4goods.api.services.aggregation.services.realtime.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
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
}
