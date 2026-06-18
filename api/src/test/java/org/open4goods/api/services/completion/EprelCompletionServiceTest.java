package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.eprel.EprelProduct;
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
    void shouldProcessReturnsFalseWhenCompletedWithinRefreshWindow()
    {
        product.getDatasourceCodes().put(EprelCompletionService.EPREL_DS_NAME, System.currentTimeMillis());
        assertThat(service.shouldProcess(vertical, product)).isFalse();
    }

    @Test
    void shouldProcessReturnsTrueWhenCompletionIsStale()
    {
        long twoDaysAgo = System.currentTimeMillis() - 2L * 24 * 3600 * 1000;
        product.getDatasourceCodes().put(EprelCompletionService.EPREL_DS_NAME, twoDaysAgo);
        assertThat(service.shouldProcess(vertical, product)).isTrue();
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
}
