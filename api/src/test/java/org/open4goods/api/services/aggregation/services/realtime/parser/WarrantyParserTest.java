package org.open4goods.api.services.aggregation.services.realtime.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.aggregation.services.realtime.parser.WarrantyParser;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalConfig.WarrantyParserConfig;
import org.open4goods.model.vertical.VerticalConfig.ParsersConfig;
import org.open4goods.model.exceptions.ParseException;

class WarrantyParserTest {

    private final WarrantyParser parser = new WarrantyParser();

    @Test
    void parseExplicitYears() throws ParseException {
        VerticalConfig vc = createConfig(0, 10);

        assertThat(parser.parse("2 ans", null, vc)).isEqualTo("2");
        assertThat(parser.parse("5 years", null, vc)).isEqualTo("5");
        assertThat(parser.parse("1 an", null, vc)).isEqualTo("1");
    }

    @Test
    void parseExplicitMonths() throws ParseException {
        VerticalConfig vc = createConfig(0, 10);

        assertThat(parser.parse("24 months", null, vc)).isEqualTo("2");
        assertThat(parser.parse("6 mois", null, vc)).isEqualTo("0.5");
        assertThat(parser.parse("12 mois", null, vc)).isEqualTo("1");
    }

    @Test
    void parseImplicit() throws ParseException {
        VerticalConfig vc = createConfig(0, 10);

        // "2" -> 2 years
        assertThat(parser.parse("2", null, vc)).isEqualTo("2");
        
        // "24" -> 24 years is out of range, so 24 months? -> 2 years
        assertThat(parser.parse("24", null, vc)).isEqualTo("2");
        
        // "50" -> 50 years (out), 50 months -> 4.17 years (in)
        assertThat(parser.parse("50", null, vc)).isEqualTo("4.17");
    }

    @Test
    void outOfBounds() throws ParseException {
        VerticalConfig vc = createConfig(0, 5);

        // 10 years -> out
        assertThat(parser.parse("10 ans", null, vc)).isNull();
        
        // 100 months -> 8.33 years -> out
        assertThat(parser.parse("100 mois", null, vc)).isNull();
    }
    
    private VerticalConfig createConfig(double min, double max) {
        VerticalConfig vc = new VerticalConfig();
        ParsersConfig pc = new ParsersConfig();
        WarrantyParserConfig wpc = new WarrantyParserConfig();
        wpc.setMinYears((double)min);
        wpc.setMaxYears((double)max);
        pc.setWarranty(wpc);
        vc.setParsers(pc);
        return vc;
    }
}
