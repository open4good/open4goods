package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.eprel.EprelModelAlias;
import org.open4goods.model.eprel.EprelModelAliasTable;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.eprelservice.service.EprelSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

import ch.qos.logback.classic.Level;

class EprelCompletionServiceTest {

    private EprelCompletionService service;
    private EprelSearchService eprelSearchService;
    private VerticalConfig vertical;
    private Product product;

    @BeforeEach
    void setUp() {
        VerticalsConfigService verticalConfigService = Mockito.mock(VerticalsConfigService.class);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        ApiProperties apiProperties = Mockito.mock(ApiProperties.class);
        eprelSearchService = Mockito.mock(EprelSearchService.class);
        AggregationFacadeService aggregationFacadeService = Mockito.mock(AggregationFacadeService.class);
        StandardAggregator aggregator = Mockito.mock(StandardAggregator.class);

        when(apiProperties.logsFolder()).thenReturn("/tmp/logs");
        when(apiProperties.aggLogLevel()).thenReturn(Level.INFO);
        when(apiProperties.getEprelRefreshDays()).thenReturn(1);
        when(apiProperties.getEprelMissRefreshDays()).thenReturn(7);
        when(aggregationFacadeService.getStandardAggregator("eprel-aggregation")).thenReturn(aggregator);

        service = new EprelCompletionService(verticalConfigService, productRepository, apiProperties,
                eprelSearchService, aggregationFacadeService);
        vertical = new VerticalConfig();
        vertical.setEprelGroupNames(List.of("TELEVISION"));
        product = new Product(123L);
        product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "MODEL-A1");
    }

    @Test
    void processProductUsesLatestEprelVersionWhenAvailable() {
        EprelProduct current = new EprelProduct();
        current.setEprelRegistrationNumber("old");
        current.setModelIdentifier("MODEL-A1");
        current.setLastVersion(false);
        current.setProductModelCoreId(10L);
        current.setVersionId(1L);

        EprelProduct latest = new EprelProduct();
        latest.setEprelRegistrationNumber("new");
        latest.setModelIdentifier("MODEL-A1");
        latest.setLastVersion(true);
        latest.setProductModelCoreId(10L);
        latest.setVersionId(2L);

        when(eprelSearchService.search(anyString(), anyList(), anyCollection()))
                .thenReturn(List.of(current));
        when(eprelSearchService.searchByProductModelCoreId(Mockito.eq(10L), anyCollection()))
                .thenReturn(List.of(current, latest));

        service.processProduct(vertical, product);

        assertThat(product.getEprelDatas()).isEqualTo(latest);
        assertThat(product.getExternalIds().getEprel()).isEqualTo("new");
    }

    @Test
    void processProductPromotesEprelModelOnGtinMatch() {
        product = new Product(123L);
        product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "WEAK123");

        EprelProduct eprelMatch = new EprelProduct();
        eprelMatch.setEprelRegistrationNumber("gtin-match");
        eprelMatch.setModelIdentifier("EPREL-MODEL-123");
        eprelMatch.setNumericGtin(123L);
        eprelMatch.setLastVersion(true);

        when(eprelSearchService.search(anyString(), anyList(), anyCollection()))
                .thenReturn(List.of(eprelMatch));

        service.processProduct(vertical, product);

        assertThat(product.getExternalIds().getEprel()).isEqualTo("gtin-match");
        assertThat(product.model()).isEqualTo("EPREL-MODEL-123");
    }

    @Test
    void processProductUsesBrandFilterWhenSearchReturnsTooManyResults() {
        product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Candy");

        EprelProduct candyProduct = new EprelProduct();
        candyProduct.setEprelRegistrationNumber("candy-oven");
        candyProduct.setLastVersion(true);
        candyProduct.setSupplierOrTrademark("Candy");

        EprelProduct otherProduct = new EprelProduct();
        otherProduct.setEprelRegistrationNumber("other-oven");
        otherProduct.setSupplierOrTrademark("Bosch");

        List<EprelProduct> multipleResults = List.of(candyProduct, otherProduct);
        when(eprelSearchService.search(anyString(), anyList(), anyCollection()))
                .thenReturn(multipleResults);
        when(eprelSearchService.filterByBrand(multipleResults, "Candy"))
                .thenReturn(List.of(candyProduct));

        service.processProduct(vertical, product);

        verify(eprelSearchService).filterByBrand(multipleResults, "Candy");
        assertThat(product.getEprelDatas()).isEqualTo(candyProduct);
        assertThat(product.getExternalIds().getEprel()).isEqualTo("candy-oven");
    }

    @Test
    void processProductUsesModelLabelFilterWhenBrandStillMatchesMultipleResults() {
        product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Indesit");
        product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "IM 760");
        product.getAkaModels().add("IM 760 MY TIME IT machine a laver charge avant");

        EprelProduct italy = new EprelProduct();
        italy.setEprelRegistrationNumber("it");
        italy.setModelIdentifier("IM 760 MY TIME IT");
        italy.setLastVersion(true);
        italy.setSupplierOrTrademark("INDESIT");

        EprelProduct france = new EprelProduct();
        france.setEprelRegistrationNumber("fr");
        france.setModelIdentifier("IM 760 MY TIME FR");
        france.setLastVersion(true);
        france.setSupplierOrTrademark("INDESIT");

        List<EprelProduct> multipleResults = List.of(italy, france);
        when(eprelSearchService.search(anyString(), anyList(), anyCollection()))
                .thenReturn(multipleResults);
        when(eprelSearchService.filterByBrand(multipleResults, "Indesit"))
                .thenReturn(multipleResults);

        service.processProduct(vertical, product);

        assertThat(product.getEprelDatas()).isEqualTo(italy);
        assertThat(product.getExternalIds().getEprel()).isEqualTo("it");
    }

    @Test
    void processProductDeterministicallyResolvesEquivalentModelMatches() {
        product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Toshiba");
        product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "32WA3B63DG");
        vertical.setEprelGroupNames(List.of("televisions", "electronicdisplays"));

        EprelProduct staleDisplay = new EprelProduct();
        staleDisplay.setEprelRegistrationNumber("old");
        staleDisplay.setModelIdentifier("32WA3B63DG");
        staleDisplay.setSupplierOrTrademark("TOSHIBA");
        staleDisplay.setProductGroup("televisions");
        staleDisplay.setLastVersion(false);
        staleDisplay.setProductModelCoreId(309853L);
        staleDisplay.setVersionNumber(BigDecimal.ONE);
        staleDisplay.setPublishedOnDateTs(1_700_000_000_000L);

        EprelProduct newerDisplay = new EprelProduct();
        newerDisplay.setEprelRegistrationNumber("new");
        newerDisplay.setModelIdentifier("32WA3B63DG");
        newerDisplay.setSupplierOrTrademark("TOSHIBA");
        newerDisplay.setProductGroup("electronicdisplays");
        newerDisplay.setLastVersion(false);
        newerDisplay.setProductModelCoreId(366760L);
        newerDisplay.setVersionNumber(BigDecimal.TEN);
        newerDisplay.setPublishedOnDateTs(1_800_000_000_000L);

        List<EprelProduct> multipleResults = List.of(staleDisplay, newerDisplay);
        when(eprelSearchService.search(anyString(), anyList(), anyCollection()))
                .thenReturn(multipleResults);
        when(eprelSearchService.filterByBrand(multipleResults, "Toshiba"))
                .thenReturn(multipleResults);

        service.processProduct(vertical, product);

        assertThat(product.getEprelDatas()).isEqualTo(newerDisplay);
        assertThat(product.getExternalIds().getEprel()).isEqualTo("new");
    }

    @Test
    void shouldProcessReturnsTrueWhenNeverProcessed()
    {
        assertThat(service.shouldProcess(vertical, product)).isTrue();
    }

    @Test
    void processProductRecordsNegativeLookupAttemptWithinRefreshWindow()
    {
        when(eprelSearchService.search(anyString(), anyList(), anyCollection())).thenReturn(List.of());

        service.processProduct(vertical, product);

        assertThat(product.getDatasourceCodes()).containsKey(EprelCompletionService.EPREL_DS_NAME);
        assertThat(service.shouldProcess(vertical, product)).isFalse();
    }

    @Test
    void shouldProcessReturnsFalseWhenCompletedWithinRefreshWindow()
    {
        product.setEprelDatas(new EprelProduct());
        product.getDatasourceCodes().put(EprelCompletionService.EPREL_DS_NAME, System.currentTimeMillis());
        assertThat(service.shouldProcess(vertical, product)).isFalse();
    }

    @Test
    void shouldProcessReturnsTrueWhenCompletionIsStale()
    {
        product.setEprelDatas(new EprelProduct());
        product.getDatasourceCodes().put(EprelCompletionService.EPREL_DS_NAME, daysAgo(2));
        assertThat(service.shouldProcess(vertical, product)).isTrue();
    }

    /**
     * GOU-2099: a product left without EPREL association is not searched again on the next daily batch, its
     * search would run every model strategy for the same result.
     */
    @Test
    void shouldProcessSkipsUnmatchedProductWithinMissWindow()
    {
        product.getDatasourceCodes().put(EprelCompletionService.EPREL_DS_NAME, daysAgo(2));
        product.getDatasourceCodes().put(EprelCompletionService.EPREL_SEARCH_SIGNATURE_CODE,
                service.searchSignature(vertical, product));

        assertThat(service.shouldProcess(vertical, product)).isFalse();
    }

    @Test
    void shouldProcessSearchesUnmatchedProductAgainAfterMissWindow()
    {
        product.getDatasourceCodes().put(EprelCompletionService.EPREL_DS_NAME, daysAgo(8));
        product.getDatasourceCodes().put(EprelCompletionService.EPREL_SEARCH_SIGNATURE_CODE,
                service.searchSignature(vertical, product));

        assertThat(service.shouldProcess(vertical, product)).isTrue();
    }

    /** A product searched before the signature existed keeps the miss window: its inputs are deemed unchanged. */
    @Test
    void shouldProcessSkipsUnmatchedProductWithoutSignatureWithinMissWindow()
    {
        product.getDatasourceCodes().put(EprelCompletionService.EPREL_DS_NAME, daysAgo(2));

        assertThat(service.shouldProcess(vertical, product)).isFalse();
    }

    /** The exclusion does not change the rule: an excluded product is searched again like the others. */
    @Test
    void shouldProcessAppliesTheSameRuleToExcludedProducts()
    {
        product.setExcluded(true);
        product.getDatasourceCodes().put(EprelCompletionService.EPREL_DS_NAME, daysAgo(2));

        assertThat(service.shouldProcess(vertical, product)).isFalse();

        product.getDatasourceCodes().put(EprelCompletionService.EPREL_DS_NAME, daysAgo(8));

        assertThat(service.shouldProcess(vertical, product)).isTrue();
    }

    /**
     * GOU-2099: the miss window must not delay a match. A product whose model candidates changed since its last
     * unsuccessful search (new offer, new model name) is searched again at once.
     */
    @Test
    void unmatchedProductIsSearchedAgainAsSoonAsItsModelCandidatesChange()
    {
        when(eprelSearchService.search(anyString(), anyList(), anyCollection())).thenReturn(List.of());
        service.processProduct(vertical, product);

        assertThat(product.getDatasourceCodes()).containsKey(EprelCompletionService.EPREL_SEARCH_SIGNATURE_CODE);
        assertThat(service.shouldProcess(vertical, product)).isFalse();

        product.getAkaModels().add("MODEL-A1-NEW");

        assertThat(service.shouldProcess(vertical, product)).isTrue();
    }

    @Test
    void unmatchedProductIsSearchedAgainWhenTheVerticalEprelGroupsChange()
    {
        when(eprelSearchService.search(anyString(), anyList(), anyCollection())).thenReturn(List.of());
        service.processProduct(vertical, product);

        vertical.setEprelGroupNames(List.of("TELEVISION", "ELECTRONIC_DISPLAYS"));

        assertThat(service.shouldProcess(vertical, product)).isTrue();
    }

    /** The miss window does not apply to a matched product, refreshed after the shorter refresh window. */
    @Test
    void matchedProductKeepsTheRefreshWindow()
    {
        EprelProduct match = new EprelProduct();
        match.setEprelRegistrationNumber("match");
        match.setModelIdentifier("MODEL-A1");
        match.setLastVersion(true);
        product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Candy");
        match.setSupplierOrTrademark("Candy");
        when(eprelSearchService.search(anyString(), anyList(), anyCollection())).thenReturn(List.of(match));
        service.processProduct(vertical, product);
        assertThat(product.getEprelDatas()).isNotNull();

        product.getDatasourceCodes().put(EprelCompletionService.EPREL_DS_NAME, daysAgo(2));

        assertThat(service.shouldProcess(vertical, product)).isTrue();
    }

    private static long daysAgo(int days)
    {
        return System.currentTimeMillis() - days * 24L * 3600 * 1000;
    }

    /**
     * GOU-2042: an iPhone sold under its commercial name and part number reaches the EPREL record registered
     * under Apple's regulatory number, through the vertical's model alias table.
     */
    @Test
    void processProductMatchesAppleRegulatoryModelThroughAliasTable()
    {
        Product iphone = appleIphone15Plus();
        vertical.setEprelModelAliases(List.of(appleTable()));
        EprelProduct a3094 = appleEprelRecord();
        when(eprelSearchService.search(anyString(), argThat((List<String> models) -> models.contains("A3094")),
                anyCollection())).thenReturn(List.of(a3094));

        service.processProduct(vertical, iphone);

        assertThat(iphone.getEprelDatas()).isEqualTo(a3094);
        assertThat(iphone.getExternalIds().getEprel()).isEqualTo("2251145");
    }

    @Test
    void processProductLeavesAppleUnmatchedWithoutAliasTable()
    {
        Product iphone = appleIphone15Plus();
        when(eprelSearchService.search(anyString(), argThat((List<String> models) -> models.contains("A3094")),
                anyCollection())).thenReturn(List.of(appleEprelRecord()));

        service.processProduct(vertical, iphone);

        assertThat(iphone.getEprelDatas()).isNull();
    }

    private static Product appleIphone15Plus()
    {
        Product iphone = new Product(195949041273L);
        iphone.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "APPLE");
        iphone.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "MU163ZD/A");
        iphone.setOfferNames(new HashSet<>(Set.of("Apple iPhone 15 Plus 128 Go Bleu", "iPhone 15 Plus 128Go Bleu 5G")));
        return iphone;
    }

    private static EprelModelAliasTable appleTable()
    {
        EprelModelAliasTable table = new EprelModelAliasTable();
        table.setBrand("APPLE");
        table.setModels(List.of(new EprelModelAlias("A3090", null, List.of("iPhone 15"), null),
                new EprelModelAlias("A3094", null, List.of("iPhone 15 Plus"), List.of("MU163"))));
        return table;
    }

    private static EprelProduct appleEprelRecord()
    {
        EprelProduct a3094 = new EprelProduct();
        a3094.setEprelRegistrationNumber("2251145");
        a3094.setModelIdentifier("A3094");
        a3094.setSupplierOrTrademark("APPLE");
        a3094.setLastVersion(true);
        return a3094;
    }

    @Test
    void processProductRejectsSingleResultWithoutModelEvidence()
    {
        product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Indesit");

        EprelProduct unrelated = new EprelProduct();
        unrelated.setEprelRegistrationNumber("unrelated");
        unrelated.setModelIdentifier("IM 760 MY TIME IT");
        unrelated.setLastVersion(true);
        unrelated.setSupplierOrTrademark("INDESIT");

        when(eprelSearchService.search(anyString(), anyList(), anyCollection()))
                .thenReturn(List.of(unrelated));

        service.processProduct(vertical, product);

        assertThat(product.getEprelDatas()).isNull();
        assertThat(product.getExternalIds().getEprel()).isNull();
    }

    @Test
    void processProductMatchesSingleResultWithShorterCandidateModel()
    {
        product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Indesit");
        product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "IM 760");

        EprelProduct matching = new EprelProduct();
        matching.setEprelRegistrationNumber("matching-shorter");
        matching.setModelIdentifier("IM 760 MY TIME IT");
        matching.setLastVersion(true);
        matching.setSupplierOrTrademark("INDESIT");

        when(eprelSearchService.search(anyString(), anyList(), anyCollection()))
                .thenReturn(List.of(matching));

        service.processProduct(vertical, product);

        assertThat(product.getEprelDatas()).isEqualTo(matching);
        assertThat(product.getExternalIds().getEprel()).isEqualTo("matching-shorter");
    }

    @Test
    void processProductMatchesSingleResultWithCompactContainment()
    {
        product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Mitsubishi Electric");
        product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "PUZ-SM100YKA");

        EprelProduct matching = new EprelProduct();
        matching.setEprelRegistrationNumber("matching-compact");
        matching.setModelIdentifier("PEAD-SM100JA / PUZ-SM100YKA");
        matching.setLastVersion(true);
        matching.setSupplierOrTrademark("Mitsubishi Electric");

        when(eprelSearchService.search(anyString(), anyList(), anyCollection()))
                .thenReturn(List.of(matching));

        service.processProduct(vertical, product);

        assertThat(product.getEprelDatas()).isEqualTo(matching);
        assertThat(product.getExternalIds().getEprel()).isEqualTo("matching-compact");
    }

    @Test
    void eprelAttributesAreLanguageNeutralAndCarryNoIcecatIdentifier() throws Exception {
        EprelProduct eprelProduct = new EprelProduct();
        eprelProduct.setCategorySpecificAttributes(new java.util.LinkedHashMap<>(
                java.util.Map.of("screenDiagonalCm", "139", "energyClassImage", "A")));
        eprelProduct.setEnergyClass("A");
        Product data = new Product(1234567890123L);
        data.setEprelDatas(eprelProduct);

        java.lang.reflect.Method method = EprelCompletionService.class
                .getDeclaredMethod("getEprelAttributesFragments", Product.class, VerticalConfig.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<org.open4goods.model.datafragment.DataFragment> fragments =
                (java.util.Set<org.open4goods.model.datafragment.DataFragment>) method.invoke(service, data, vertical);

        assertThat(fragments).hasSize(1);
        org.open4goods.model.datafragment.DataFragment fragment = fragments.iterator().next();
        assertThat(fragment.getAttributes()).isNotEmpty();
        assertThat(fragment.getAttributes())
                .allSatisfy(attribute -> {
                    // EPREL parameter values are language-neutral; stamping them "fr" made
                    // them unusable for any other locale.
                    assertThat(attribute.getLanguage()).isEqualTo(SourcedAttribute.UNDETERMINED_LANGUAGE);
                    // An EPREL parameter identifier is not an Icecat feature identifier.
                    assertThat(attribute.getIcecatFeatureId()).isNull();
                });
    }

    /**
     * GOU-1891: real shape of a smartphone EPREL document (Samsung SM-S936B/DS, 2339793). Cycles are published
     * in hundreds and converted; the minutes key stays raw; a missing value is not turned into zero.
     */
    @Test
    void smartphoneBatteryEnduranceIsConvertedFromHundredsOfCycles() throws Exception {
        EprelProduct eprelProduct = new EprelProduct();
        eprelProduct.setCategorySpecificAttributes(new java.util.LinkedHashMap<>(java.util.Map.of(
                "repairabilityClass", "C", "repairabilityIndex", 3.23, "batteryEnduranceInCycles", 20,
                "batteryEndurancePerCycleInHours", 43.63, "minYearsSoftwareUpdates", 5)));
        Product data = new Product(1234567890123L);
        data.setEprelDatas(eprelProduct);

        java.lang.reflect.Method method = EprelCompletionService.class
                .getDeclaredMethod("getEprelAttributesFragments", Product.class, VerticalConfig.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<org.open4goods.model.datafragment.DataFragment> fragments =
                (java.util.Set<org.open4goods.model.datafragment.DataFragment>) method.invoke(service, data, vertical);
        org.open4goods.model.datafragment.DataFragment fragment = fragments.iterator().next();

        assertThat(valueOf(fragment, "batteryEnduranceCycles")).isEqualTo("2000");
        assertThat(valueOf(fragment, "repairabilityClass")).isEqualTo("C");
        assertThat(valueOf(fragment, "repairabilityIndex")).isEqualTo("3.23");

        java.util.Map<String, Object> missing = new java.util.HashMap<>();
        missing.put("batteryEnduranceInCycles", null);
        eprelProduct.setCategorySpecificAttributes(missing);
        fragments = (java.util.Set<org.open4goods.model.datafragment.DataFragment>) method.invoke(service, data, vertical);
        assertThat(valueOf(fragments.iterator().next(), "batteryEnduranceCycles")).isNull();
    }

    private static String valueOf(org.open4goods.model.datafragment.DataFragment fragment, String name) {
        return fragment.getAttributes().stream().filter(a -> name.equalsIgnoreCase(a.getName()))
                .map(org.open4goods.model.attribute.Attribute::getValue).findFirst().orElse(null);
    }
}
