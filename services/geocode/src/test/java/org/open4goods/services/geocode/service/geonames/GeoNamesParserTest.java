package org.open4goods.services.geocode.service.geonames;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GeoNamesParser}.
 */
class GeoNamesParserTest
{
    private final GeoNamesParser parser = new GeoNamesParser();

    @Test
    void parsesValidLine()
    {
        String line = "2988507\tParis\tParis\tParis,City of Paris\t48.8566\t2.3522\tP\tPPLC\tFR\t\t11\t75\t\t\t2148327\t35\t35\tEurope/Paris\t2023-01-01";

        GeoNamesEntry entry = parser.parseLine(line);

        assertThat(entry).isNotNull();
        assertThat(entry.record().name()).isEqualTo("Paris");
        assertThat(entry.record().countryCode()).isEqualTo("FR");
        assertThat(entry.record().population()).isEqualTo(2148327L);
        assertThat(entry.alternateNames()).contains("City of Paris");
    }

    @Test
    void skipsLinesWithMissingColumns()
    {
        String line = "123\tIncomplete";
        GeoNamesEntry entry = parser.parseLine(line);
        assertThat(entry).isNull();
    }
}
