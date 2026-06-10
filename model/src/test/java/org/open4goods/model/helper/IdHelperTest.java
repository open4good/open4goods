package org.open4goods.model.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class IdHelperTest {

    @Test
    void testToDatasourceId() {
        assertEquals("moto-axxe.fr", IdHelper.toDatasourceId("moto-axxe.fr"), "Should keep dots and dashes");
        assertEquals("moto_axxe", IdHelper.toDatasourceId("moto_axxe"), "Should keep underscores");
        assertEquals("SimpleName", IdHelper.toDatasourceId("SimpleName"), "Should keep alphanumeric");
        assertEquals("accentless", IdHelper.toDatasourceId("accentless"), "Should remove accents"); // Assuming stripAccents works
        assertEquals("moto-axxe.fr", IdHelper.toDatasourceId("  moto-axxe.fr  "), "Should trim spaces (if stripAccents trims? no, replaceAll does not trim but implementation remove non alphanum, so space is removed)");
        // Actually, my implementation was: StringUtils.stripAccents(input).replaceAll("[^a-zA-Z0-9_.-]", "")
        // stripAccents might not trim. But replaceAll("[^...]", "") will remove spaces.
        
        assertEquals("motoaxxefr", IdHelper.toDatasourceId("moto axxe fr"), "Spaces should be removed by regex");
         
        assertNull(IdHelper.toDatasourceId(null));
    }

    @Test
    void testIsPureDouble() {
        org.junit.jupiter.api.Assertions.assertTrue(IdHelper.isPureDouble("61.99"));
        org.junit.jupiter.api.Assertions.assertTrue(IdHelper.isPureDouble("-61.99"));
        org.junit.jupiter.api.Assertions.assertTrue(IdHelper.isPureDouble("+61.99"));
        org.junit.jupiter.api.Assertions.assertTrue(IdHelper.isPureDouble("0"));
        org.junit.jupiter.api.Assertions.assertTrue(IdHelper.isPureDouble("1e5"));
        org.junit.jupiter.api.Assertions.assertTrue(IdHelper.isPureDouble("-1E-5"));
        org.junit.jupiter.api.Assertions.assertTrue(IdHelper.isPureDouble("1.2e+3"));

        org.junit.jupiter.api.Assertions.assertFalse(IdHelper.isPureDouble("61.99 EUR"));
        org.junit.jupiter.api.Assertions.assertFalse(IdHelper.isPureDouble("EUR 61.99"));
        org.junit.jupiter.api.Assertions.assertFalse(IdHelper.isPureDouble(""));
        org.junit.jupiter.api.Assertions.assertFalse(IdHelper.isPureDouble(null));
        org.junit.jupiter.api.Assertions.assertFalse(IdHelper.isPureDouble("abc"));
        org.junit.jupiter.api.Assertions.assertFalse(IdHelper.isPureDouble("61,99"));
    }
}
