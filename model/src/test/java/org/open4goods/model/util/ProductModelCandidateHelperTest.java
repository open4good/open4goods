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
import org.open4goods.model.util.ProductModelCandidateHelper.ModelCandidateSource;
import org.open4goods.model.util.ProductModelCandidateHelper.TitleModelExtraction;

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

    @Test
    void namedModelsRequireStrongEvidence()
    {
        assertNull(ProductModelCandidateHelper.cleanForStorage("My Time", ModelCandidateSource.DATASOURCE_REFERENTIAL));
        assertEquals("My Time", ProductModelCandidateHelper.cleanForStorage(" My Time ",
                ModelCandidateSource.OFFICIAL_TEXT));
    }

    @Test
    void detectsSiblingDriftFamilies()
    {
        assertTrue(ProductModelCandidateHelper.isSiblingDrift("4D 511", "4D 515"));
        assertFalse(ProductModelCandidateHelper.isSiblingDrift("SM-S921B/DS", "SM-S921B/DS"));
        assertFalse(ProductModelCandidateHelper.isSiblingDrift("WRIC 3C34 PE", "W7X82O-W"));
    }

    // --- extractModelsFromTitles ---

    @Test
    void extractModelsFromTitlesReturnsEmptyForNullOrBlankInput()
    {
        assertTrue(ProductModelCandidateHelper.extractModelsFromTitles(null).isEmpty());
        assertTrue(ProductModelCandidateHelper.extractModelsFromTitles(List.of()).isEmpty());
        assertTrue(ProductModelCandidateHelper.extractModelsFromTitles(List.of("  ", "")).isEmpty());
    }

    @Test
    void extractModelsFromTitlesPicksBestTvModel()
    {
        TitleModelExtraction result = ProductModelCandidateHelper.extractModelsFromTitles(
                List.of("Samsung HG32EJ690WE television"));
        assertFalse(result.isEmpty());
        assertEquals("HG32EJ690WE", result.best());
    }

    @Test
    void extractModelsFromTitlesPrefixPreservationSmG991b()
    {
        // SM- prefix must survive; without it EPREL compact matching is weaker
        TitleModelExtraction result = ProductModelCandidateHelper.extractModelsFromTitles(
                List.of("Samsung Galaxy SM-G991B 128Go smartphone"));
        assertFalse(result.isEmpty());
        assertTrue(result.ranked().contains("SM-G991B"),
                "Expected SM-G991B in ranked candidates but got: " + result.ranked());
    }

    @Test
    void extractModelsFromTitlesElectsByFrequencyThenShortest()
    {
        // HG32EJ690WE appears twice, AB1234XY once — frequency wins
        TitleModelExtraction result = ProductModelCandidateHelper.extractModelsFromTitles(List.of(
                "Samsung HG32EJ690WE TV",
                "HG32EJ690WE 32 pouces",
                "AB1234XY display"));
        assertEquals("HG32EJ690WE", result.best());
    }

    @Test
    void extractModelsFromTitlesRejectsMeasureAndUnitTokens()
    {
        // All of these should be rejected as false positives
        assertTrue(ProductModelCandidateHelper.extractModelsFromTitles(
                List.of("TV 55POUCES", "Ecran 144HZ", "Ampli 1000W")).isEmpty());
    }

    @Test
    void extractModelsFromTitlesRejectsResolutions()
    {
        assertTrue(ProductModelCandidateHelper.extractModelsFromTitles(
                List.of("dalle 1920x1080", "panel 3840X2160")).isEmpty());
    }

    @Test
    void extractModelsFromTitlesRejectsStorageVariants()
    {
        assertTrue(ProductModelCandidateHelper.extractModelsFromTitles(
                List.of("Smartphone 256GO", "Disque 12GO/512GO")).isEmpty());
    }

    @Test
    void extractModelsFromTitlesRejectsCategoryWords()
    {
        assertTrue(ProductModelCandidateHelper.extractModelsFromTitles(
                List.of("SMARTPHONE", "TELEVISEUR", "REFRIGERATEUR")).isEmpty());
    }

    @Test
    void extractModelsFromTitlesRejectsDegenerateAndGtinCodes()
    {
        assertTrue(ProductModelCandidateHelper.extractModelsFromTitles(
                List.of("dimension 568X500X430MM", "ref 813276", "gtin 6941764449275")).isEmpty());
    }

    @Test
    void extractModelsFromTitlesApplianceTitle()
    {
        TitleModelExtraction result = ProductModelCandidateHelper.extractModelsFromTitles(
                List.of("Bosch SMV4HVX31E lave-vaisselle integrable"));
        assertFalse(result.isEmpty());
        assertEquals("SMV4HVX31E", result.best());
    }
}
