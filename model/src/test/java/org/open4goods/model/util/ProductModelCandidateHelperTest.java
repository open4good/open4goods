package org.open4goods.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;

class ProductModelCandidateHelperTest
{
    @Test
    void expandedCandidatesIncludesSeparatorVariants()
    {
        Product product = new Product(1L);
        product.addModel("WRIC 3C34 PE");
        product.setAkaModels(Set.of("W7X82O-W"));

        List<String> candidates = ProductModelCandidateHelper.expandedCandidates(product);

        assertTrue(candidates.contains("WRIC 3C34 PE"));
        assertTrue(candidates.contains("WRIC-3C34-PE"));
        assertTrue(candidates.contains("WRIC3C34PE"));
        assertTrue(candidates.contains("W7X82O-W"));
        assertTrue(candidates.contains("W7X82O W"));
        assertTrue(candidates.contains("W7X82OW"));
    }

    @Test
    void sanitiseRejectsInternalReferencesAndDimensionCodes()
    {
        List<String> candidates = ProductModelCandidateHelper.sanitise(List.of(
                "813276",
                "874538",
                "568X500X430MM",
                "W7X82OW",
                "WRIC 3C34 PE",
                "AB"), 3, 3);

        assertIterableEquals(List.of("WRIC 3C34 PE", "W7X82OW"), candidates);
    }

    @Test
    void normaliseAndCompactAreAsciiAndAlphanumeric()
    {
        assertEquals("cafe wric 3c34 pe", ProductModelCandidateHelper.normalizePhrase("Café WRIC-3C34 PE"));
        assertEquals("cafewric3c34pe", ProductModelCandidateHelper.compactModel("Café WRIC-3C34 PE"));
        assertNull(ProductModelCandidateHelper.normalizePhrase(" - "));
    }

    @Test
    void humanSearchCandidateRejectsBareNumericReferences()
    {
        assertFalse(ProductModelCandidateHelper.isHumanSearchCandidate("813276"));
        assertFalse(ProductModelCandidateHelper.isHumanSearchCandidate("568X500X430MM"));
        assertTrue(ProductModelCandidateHelper.isHumanSearchCandidate("IPHONE15PRO-256-BLUE"));
    }

    @Test
    void cleanForStorageKeepsConciseManufacturerModelCodes()
    {
        assertEquals("SM-S921B/DS", ProductModelCandidateHelper.cleanForStorage(" sm-s921b/ds "));
        assertEquals("NV7B4550VAS/U1", ProductModelCandidateHelper.cleanForStorage("NV7B4550VAS/U1"));
        assertEquals("FS1600H", ProductModelCandidateHelper.cleanForStorage("FS1600H"));
    }

    @Test
    void cleanForStorageRejectsNoisyMerchantTitlesAndWeakVariants()
    {
        assertNull(ProductModelCandidateHelper.cleanForStorage(
                "SMARTPHONE GT7 PRO 6,78 5G DOUBLE NANO SIM 512 GO GRIS"));
        assertNull(ProductModelCandidateHelper.cleanForStorage("12GO/512GO"));
        assertNull(ProductModelCandidateHelper.cleanForStorage("56X55X59"));
        assertNull(ProductModelCandidateHelper.cleanForStorage("6941764449275"));
        assertNull(ProductModelCandidateHelper.cleanForStorage(
                "Refrigerateur vitre professionnel grande capacite blanc"));
    }
}
