package org.open4goods.commons.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.open4goods.model.exceptions.InvalidParameterException;

public class WarrantyParserTest {

    @Test
    public void testExplicitMonths() throws InvalidParameterException {
        assertEquals(6, WarrantyParser.parse("6 months"));
        assertEquals(6, WarrantyParser.parse("6 mois"));
        assertEquals(12, WarrantyParser.parse("12 m"));
        assertEquals(24, WarrantyParser.parse("24 months"));
        assertEquals(36, WarrantyParser.parse("36 mois"));
        assertEquals(19, WarrantyParser.parse("18.5 mois"));
        assertEquals(19, WarrantyParser.parse("18,5 m"));
    }

    @Test
    public void testExplicitYears() throws InvalidParameterException {
        assertEquals(12, WarrantyParser.parse("1 an"));
        assertEquals(24, WarrantyParser.parse("2 ans"));
        assertEquals(36, WarrantyParser.parse("3 years"));
        assertEquals(48, WarrantyParser.parse("4 y"));
        assertEquals(60, WarrantyParser.parse("5 a"));
        assertEquals(18, WarrantyParser.parse("1.5 ans"));
        assertEquals(30, WarrantyParser.parse("2,5 years"));
    }

    @Test
    public void testImplicitDefaultIsYears() throws InvalidParameterException {
        assertEquals(24, WarrantyParser.parse("2"));
        assertEquals(36, WarrantyParser.parse("3"));
        assertEquals(12, WarrantyParser.parse("1"));
        assertEquals(24, WarrantyParser.parse("gartie 2"));
    }

    @Test
    public void testInvalid() {
        assertThrows(InvalidParameterException.class, () -> {
            WarrantyParser.parse("");
        });
        assertThrows(InvalidParameterException.class, () -> {
            WarrantyParser.parse("unknown guarantee");
        });
    }
}
