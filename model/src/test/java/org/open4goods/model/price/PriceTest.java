package org.open4goods.model.price;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class PriceTest {

    @Test
    void testSetPriceValue() throws ParseException {
        Price price = new Price();

        // Standard format
        price.setPriceValue("61.99", Locale.US);
        assertEquals(61.99, price.getPrice());

        // Localized format with comma (French locale)
        price.setPriceValue("61,99", Locale.FRANCE);
        assertEquals(61.99, price.getPrice());

        // Format with currency suffix (should not throw NumberFormatException)
        price.setPriceValue("61.99 EUR", Locale.US);
        assertEquals(61.99, price.getPrice());

        price.setPriceValue("61,99 EUR", Locale.FRANCE);
        assertEquals(61.99, price.getPrice());

        // Formatting with spaces
        price.setPriceValue("61.99 ", Locale.US);
        assertEquals(61.99, price.getPrice());

        // Invalid formats
        assertThrows(ParseException.class, () -> {
            Price p = new Price();
            p.setPriceValue("invalid-price", Locale.US);
        });
    }
}
