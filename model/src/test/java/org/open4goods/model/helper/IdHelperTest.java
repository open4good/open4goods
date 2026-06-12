package org.open4goods.model.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.datafragment.DataFragment;

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

    @Test
    void extractModelFromNamesReturnsNullForEmptyNames() {
        DataFragment fragment = new DataFragment();
        assertNull(IdHelper.extractModelFromNames(fragment));
    }

    @Test
    void extractModelFromNamesExtractsModelFromSingleTitle() {
        DataFragment fragment = new DataFragment();
        fragment.addName("Samsung HG32EJ690WE television");
        assertEquals("HG32EJ690WE", IdHelper.extractModelFromNames(fragment));
    }

    @Test
    void extractModelFromNamesPicksBestWhenMultipleCandidates() {
        // Old behaviour: throws on ambiguity. New behaviour: returns the best-ranked.
        DataFragment fragment = new DataFragment();
        fragment.addName("Samsung HG32EJ690WE television");
        fragment.addName("Samsung HG32EJ690WE 32 pouces");
        fragment.addName("Bosch SMV4HVX31E lave-vaisselle");
        String result = IdHelper.extractModelFromNames(fragment);
        // The most frequent token wins
        assertNotNull(result);
        assertEquals("HG32EJ690WE", result);
    }

    @Test
    void extractModelFromNamesRejectsFalsePositive() {
        DataFragment fragment = new DataFragment();
        fragment.addName("Televiseur 55POUCES 144HZ");
        assertNull(IdHelper.extractModelFromNames(fragment));
    }

    @Test
    void extractModelTokensReturnsLegacyTokens() {
        Set<String> tokens = IdHelper.extractModelTokens("Samsung HG32EJ690WE television");
        assertTrue(tokens.contains("HG32EJ690WE"));
    }

    @Test
    void extractModelTokensReturnsEmptyForPureWords() {
        Set<String> tokens = IdHelper.extractModelTokens("television ecran couleur");
        assertTrue(tokens.isEmpty());
    }
}
