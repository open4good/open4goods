package org.open4goods.services.reviewgeneration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductFetchDiagnostics;
import org.open4goods.model.product.ProductFact;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributeParserConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.dto.GoogleSearchResult;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.dto.ExtractedMetadataAttribute;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.open4goods.brand.service.BrandService;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests the review-generation retrieval preprocessing contract.
 */
@ExtendWith(MockitoExtension.class)
class ReviewGenerationPreprocessingServiceTest {

    private ReviewGenerationConfig properties;
    private ReviewGenerationPreprocessingService service;

    @Mock private GoogleSearchService googleSearchService;
    @Mock private UrlFetchingService urlFetchingService;
    @Mock private PromptService promptService;
    @Mock private SerialisationService serialisationService;
    @Mock private BrandService brandService;

    @BeforeEach
    void setUp() {
        properties = new ReviewGenerationConfig();
        properties.setSerpBudget(1);
        properties.setPreferredDomains(List.of("lesnumeriques.com", "fnac.com"));
        properties.setSearchResultsPerQuery(7);
        properties.setSearchGeoLocation("fr");
        properties.setSearchHostLanguage("fr");
        service = new ReviewGenerationPreprocessingService(properties, googleSearchService, urlFetchingService,
                promptService, serialisationService, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                brandService);
    }

    @Test
    void preparePromptVariables_SearchesOfficialDiscoveryThenSupportAndPreferredDomains() throws Exception {
        properties.setSerpBudget(4);
        properties.setSearchLanguageRestrict("lang_fr");
        properties.setSearchCountryRestrict("countryFR");
        Product product = product("Sony", "XR55A80L");
        product.setAkaModels(Set.of("XR-55A80L"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, new VerticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService, org.mockito.Mockito.times(4)).search(requestCaptor.capture());
        List<GoogleSearchRequest> requests = requestCaptor.getAllValues();
        GoogleSearchRequest officialRequest = requests.getFirst();
        GoogleSearchRequest preferredRequest = requests.get(1);
        GoogleSearchRequest supportRequest = requests.get(3);

        assertThat(officialRequest.query()).isEqualTo("Sony \"XR55A80L\" (official OR officiel OR product OR produit)");
        assertThat(preferredRequest.query()).startsWith("(site:lesnumeriques.com OR site:fnac.com)");
        assertThat(supportRequest.query()).contains("support OR assistance OR manual OR notice");
        assertThat(preferredRequest.query()).contains("\"Sony XR55A80L\"");
        assertThat(preferredRequest.query()).contains("\"Sony XR-55A80L\"");
        assertThat(preferredRequest.numResults()).isEqualTo(7);
        assertThat(preferredRequest.lr()).isEqualTo("lang_fr");
        assertThat(preferredRequest.cr()).isEqualTo("countryFR");
        assertThat(preferredRequest.gl()).isEqualTo("fr");
        assertThat(preferredRequest.hl()).isEqualTo("fr");
        assertThat(preferredRequest.safe()).isEqualTo("off");
    }

    @Test
    void preparePromptVariables_KeepsLeadingScreenSizeDigitsInTvModelCodes() throws Exception {
        properties.setSerpBudget(1);
        Product product = product("Thomson", "Thomson TV 40FD2S13W");
        product.setId(9120106661804L);
        product.setOfferNames(Set.of("tv led thomson 40fd2s13w 101 cm full hd blanc"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService).search(requestCaptor.capture());
        assertThat(requestCaptor.getValue().query())
                .isEqualTo("Thomson \"40fd2s13w\" (official OR officiel OR product OR produit)")
                .doesNotContain("\"0fd2s13w\"");
    }

    @Test
    void preparePromptVariables_UsesVerticalPreferredDomainsWhenConfigured() throws Exception {
        properties.setSerpBudget(4);
        properties.setPreferredDomains(List.of("global.example"));
        properties.setPreferredDomainsByVertical(Map.of("refrigerator",
                List.of("electromenager-compare.com", "test-achats.be")));
        Product product = product("Liebherr", "CBNc5723-22");
        VerticalConfig verticalConfig = verticalConfig();
        verticalConfig.setId("refrigerator");
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig, new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService, org.mockito.Mockito.times(4)).search(requestCaptor.capture());
        String preferredQuery = requestCaptor.getAllValues().get(1).query();
        assertThat(preferredQuery).startsWith("(site:electromenager-compare.com OR site:test-achats.be)");
        assertThat(preferredQuery).doesNotContain("global.example");
    }

    @Test
    void preparePromptVariables_FailsBeforeSearchWhenRequiredSerpKeysAreMissing() {
        Product product = new Product();
        product.setId(42L);

        assertThatThrownBy(() -> service.preparePromptVariables(product, new VerticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing brand, model");
    }

    @Test
    void preparePromptVariables_InfersBrandAndModelFromOfferNamesWhenReferentialIdentityIsMissing() throws Exception {
        properties.setSerpBudget(1);
        Product product = new Product();
        product.setId(5902721194172L);
        product.setVertical("refrigerator");
        product.setOfferNames(Set.of("Glacière Yolco ET18 CARBON",
                "UNKNOWN Yolco ET18, Réfrigérateur portable à compresseur, Black (ET18 CARBON)"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService).search(requestCaptor.capture());
        assertThat(requestCaptor.getValue().query())
                .isEqualTo("Yolco \"ET18\" (official OR officiel OR product OR produit)");
        assertThat(product.brand()).isEqualTo("Yolco");
        assertThat(product.model()).isEqualTo("ET18");
    }

    @Test
    void preparePromptVariables_ReplacesShortFalseBrandWithLongBrandFromOfferEvidence() throws Exception {
        properties.setSerpBudget(1);
        Product product = product("GE", "7141223797872");
        product.setId(7141223797872L);
        product.setOfferNames(Set.of(
                "Réfrigérateur congélateur haut GEDTECH GE217DP 217L Blanc - 2 portes - Froid statique"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService).search(requestCaptor.capture());
        assertThat(requestCaptor.getValue().query())
                .isEqualTo("GEDTECH \"GE217DP\" (official OR officiel OR product OR produit)");
        assertThat(product.brand()).isEqualTo("GEDTECH");
        assertThat(product.model()).isEqualTo("GE217DP");
    }

    @Test
    void preparePromptVariables_PromotesHighOneIdentityForWeakSearchBrand() throws Exception {
        properties.setSerpBudget(3);
        properties.setWeakSearchBrands(List.of("SANTE & BEAUTE"));
        Product product = product("SANTE & BEAUTE", "3612408988217");
        product.setId(3612408988217L);
        product.setVertical("washing-machine");
        product.setOfferNames(Set.of("Lave linge hublot HIGH ONE WAD929C 9kg 1200 tours"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService, org.mockito.Mockito.times(3)).search(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).extracting(GoogleSearchRequest::query)
                .containsExactly(
                        "HIGH ONE \"WAD929C\" (support OR assistance OR manual OR notice OR datasheet OR \"fiche produit\")",
                        "\"WAD929C\" (manual OR notice OR support OR datasheet OR \"fiche produit\")",
                        "\"Lave linge hublot HIGH ONE WAD929C 9kg 1200 tours\"");
        assertThat(product.brand()).isEqualTo("HIGH ONE");
        assertThat(product.model()).isEqualTo("WAD929C");
    }

    @Test
    void preparePromptVariables_PromotesValbergIdentityForWeakSearchBrand() throws Exception {
        properties.setSerpBudget(3);
        properties.setWeakSearchBrands(List.of("SANTE & BEAUTE"));
        Product product = product("SANTE & BEAUTE", "3497674181878");
        product.setId(3497674181878L);
        product.setVertical("washing-machine");
        product.setOfferNames(Set.of("Lave linge VALBERG WF 712 D W180C blanc"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService, org.mockito.Mockito.times(3)).search(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).extracting(GoogleSearchRequest::query)
                .containsExactly(
                        "VALBERG \"WF 712 D W180C\" (support OR assistance OR manual OR notice OR datasheet OR \"fiche produit\")",
                        "\"WF 712 D W180C\" (manual OR notice OR support OR datasheet OR \"fiche produit\")",
                        "\"Lave linge VALBERG WF 712 D W180C blanc\"");
        assertThat(product.brand()).isEqualTo("VALBERG");
        assertThat(product.model()).isEqualTo("WF 712 D W180C");
    }

    @Test
    void preparePromptVariables_SearchesOfficialBrandDomainForNamedModelFromOfferTitle() throws Exception {
        properties.setSerpBudget(4);
        org.open4goods.brand.model.Brand brand = new org.open4goods.brand.model.Brand();
        brand.setOfficialDomains(List.of("klarstein"));
        when(brandService.resolve("Klarstein")).thenReturn(brand);
        Product product = product("Klarstein", "4060656565403");
        product.setId(4060656565403L);
        product.setOfferNames(Set.of("Klarstein Velaire Lave Vaisselle 45cm Pose Libre - 10 Couverts"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, new VerticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService, org.mockito.Mockito.times(4)).search(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).extracting(GoogleSearchRequest::query)
                .contains("site:klarstein Klarstein \"Velaire\"");
        assertThat(product.model()).isEqualTo("4060656565403");
    }

    @Test
    void preparePromptVariables_DoesNotPromoteGenericNamedModelFromOfferTitle() throws Exception {
        properties.setSerpBudget(1);
        Product product = product("ASKO", "0698142927998");
        product.setId(698142927998L);
        product.setOfferNames(Set.of("ASKO Lave vaisselle enchassable a usage intensif 2 niveaux"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService).search(requestCaptor.capture());
        assertThat(requestCaptor.getValue().query())
                .isEqualTo("ASKO \"0698142927998\" (official OR officiel OR product OR produit)");
        assertThat(product.model()).isEqualTo("0698142927998");
    }

    @Test
    void preparePromptVariables_IdentifiesAndPrioritizesManufacturerOfficialUrl() throws Exception {
        properties.setPreferredDomains(List.of("darty.com"));
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(3);
        Product product = product("Samsung", "SM-S921B/DS");
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Avis clients Samsung SM-S921B/DS", "https://www.darty.com/samsung.html"),
                new GoogleSearchResult("Samsung Galaxy S24 SM-S921B/DS | Produit officiel",
                        "https://www.samsung.com/fr/smartphones/galaxy-s24/galaxy-s24-sm-s921b-ds/"),
                new GoogleSearchResult("SM-S921B/DS | Assistance Samsung FR",
                        "https://www.samsung.com/fr/support/model/SM-S921BZVDEUC/"),
                new GoogleSearchResult("Generic shop", "https://example.com/samsung.html"))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String url = invocation.getArgument(0);
            String markdown = "Useful product content for " + url + " with display, camera, and battery details for Samsung model SM-S921B/DS.";
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown, FetchStrategy.HTTP));
        });

        Map<String, Object> variables = service.preparePromptVariables(product, verticalConfig(),
                new ReviewGenerationStatus());

        assertThat(product.getOfficialUrl())
                .isEqualTo("https://www.samsung.com/fr/smartphones/galaxy-s24/galaxy-s24-sm-s921b-ds/");
        assertThat(product.getOfficialSupportUrls().get("fr"))
                .contains("https://www.samsung.com/fr/support/model/SM-S921BZVDEUC/");
        assertThat(product.getReviewFacts()).extracting("url").contains(product.getOfficialUrl());
        @SuppressWarnings("unchecked")
        Map<String, String> sources = (Map<String, String>) variables.get("sources");
        assertThat(sources.keySet()).contains(product.getOfficialUrl(),
                "https://www.samsung.com/fr/support/model/SM-S921BZVDEUC/",
                "https://www.darty.com/samsung.html");
        assertThat((List<String>) variables.get("ACCEPTED_URLS")).contains(product.getOfficialUrl(),
                "https://www.samsung.com/fr/support/model/SM-S921BZVDEUC/",
                "https://www.darty.com/samsung.html");
        assertThat((List<String>) variables.get("SEARCHED_QUERIES")).isNotEmpty();
        assertThat((Map<String, String>) variables.get("REJECTED_URLS")).isEmpty();
    }

    @Test
    void preparePromptVariables_RetriesPartialOfficialFetchWithTargetedQueries() throws Exception {
        properties.setSerpBudget(2);
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(600);
        properties.setMinUrlCount(2);
        properties.setMaxUrlsPerProduct(3);
        Product product = product("Bosch", "SRS2IKW04E");
        String officialUrl = "https://www.bosch-home.fr/fr/product/SRS2IKW04E";
        String reviewUrl = "https://www.quechoisir.org/test-lave-vaisselle-bosch-srs2ikw04e-n123/";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(
                new GoogleSearchResponse(List.of(new GoogleSearchResult("Bosch SRS2IKW04E officiel", officialUrl))),
                new GoogleSearchResponse(List.of(new GoogleSearchResult("Test Bosch SRS2IKW04E", reviewUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String url = invocation.getArgument(0);
            String markdown = "Bosch SRS2IKW04E content with useful washing, drying, noise, water and energy details from " + url;
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown,
                    FetchStrategy.HTTP, List.of(), Set.of(), List.of(), false, null));
        });

        Map<String, Object> variables = service.preparePromptVariables(product, verticalConfig(),
                new ReviewGenerationStatus());

        @SuppressWarnings("unchecked")
        Map<String, String> sources = (Map<String, String>) variables.get("sources");
        assertThat(sources.keySet()).containsExactly(officialUrl, reviewUrl);
        @SuppressWarnings("unchecked")
        List<String> searchedQueries = (List<String>) variables.get("SEARCHED_QUERIES");
        assertThat(searchedQueries).hasSize(2);
        assertThat(searchedQueries.get(1)).contains("SRS2IKW04E");
    }

    @Test
    void preparePromptVariables_StoresSupportUrlAsOfficialUrlFallbackWhenNoProductPageFound() throws Exception {
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(1);
        Product product = product("Samsung", "SM-S921B/DS");
        String supportUrl = "https://www.samsung.com/fr/support/model/SM-S921BZVDEUC/";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("SM-S921B/DS | Assistance Samsung FR", supportUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String url = invocation.getArgument(0);
            String markdown = "Samsung SM-S921B/DS support page with useful display, camera, and battery details.";
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown, FetchStrategy.HTTP));
        });

        service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus());

        // When only support pages are found, the first one is stored as officialUrl fallback
        // so downstream services can still show a manufacturer link.
        assertThat(product.getOfficialUrl()).isEqualTo(supportUrl);
        assertThat(product.getOfficialSupportUrls().get("fr")).containsExactly(supportUrl);
    }

    @Test
    void preparePromptVariables_SupportUrlDoesNotReplaceExistingOfficialProductUrl() throws Exception {
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(2);
        Product product = product("Samsung", "SM-S921B/DS");
        String productUrl = "https://www.samsung.com/fr/smartphones/galaxy-s24/galaxy-s24-sm-s921b-ds/";
        String supportUrl = "https://www.samsung.com/fr/support/model/SM-S921BZVDEUC/";
        product.setOfficialUrl(productUrl);
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("SM-S921B/DS | Assistance Samsung FR", supportUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String url = invocation.getArgument(0);
            String markdown = "Samsung SM-S921B/DS support page with useful display, camera, and battery details.";
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown, FetchStrategy.HTTP));
        });

        service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus());

        // Pre-existing product URL must not be overwritten by a support URL
        assertThat(product.getOfficialUrl()).isEqualTo(productUrl);
        assertThat(product.getOfficialSupportUrls().get("fr")).containsExactly(supportUrl);
    }

    @Test
    void preparePromptVariables_PersistsPdfResourcesExtractedFromOfficialPages() throws Exception {
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(1);
        Product product = product("Samsung", "SM-S921B/DS");
        String officialUrl = "https://www.samsung.com/fr/smartphones/galaxy-s24/galaxy-s24-sm-s921b-ds/";
        String pdfUrl = "https://images.samsung.com/is/content/samsung/assets/fr/galaxy-s24/notice-sm-s921b-ds.pdf";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Samsung Galaxy S24 SM-S921B/DS | Produit officiel", officialUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String markdown = "Useful Samsung SM-S921B/DS official product content with display, camera, and battery details.";
            return CompletableFuture.completedFuture(new FetchResponse(officialUrl, 200, markdown, markdown,
                    FetchStrategy.HTTP, List.of(), Set.of(), List.of(new org.open4goods.services.urlfetching.dto.ExtractedResource(
                            pdfUrl, org.open4goods.services.urlfetching.dto.ResourceType.PDF, "link", "Notice")),
                    false, null));
        });

        service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus());

        assertThat(product.model()).isEqualTo("SM-S921B/DS");
        assertThat(product.getResources()).anySatisfy(resource ->
        {
            assertThat(resource.getUrl()).isEqualTo(pdfUrl);
            assertThat(resource.getResourceType()).isEqualTo(org.open4goods.model.resource.ResourceType.PDF);
            assertThat(resource.getDatasourceName()).isEqualTo("manufacturer");
            assertThat(resource.getTags()).contains("official", "official:fr");
        });
    }

    @Test
    void preparePromptVariables_PersistsOpaquePdfResourcesFromOfficialProductPagesWhenLabelIdentifiesProduct() throws Exception {
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(1);
        Product product = product("Hotpoint", "HIO3O41WFE");
        String officialUrl = "https://www.hotpoint.fr/h/lave-vaisselle/hio3o41wfe";
        String pdfUrl = "https://digitalassets-cdn.thron.com/api/v1/content-delivery/shares/xoxl70/contents/do-1041247f-504c-4069-879c-c52b2a84ad6b/pdf/doc.pdf";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Hotpoint HIO3O41WFE official product page", officialUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String markdown = "Hotpoint HIO3O41WFE official dishwasher content with programs, capacity and energy details.";
            return CompletableFuture.completedFuture(new FetchResponse(officialUrl, 200, markdown, markdown,
                    FetchStrategy.HTTP, List.of(), Set.of(), List.of(new org.open4goods.services.urlfetching.dto.ExtractedResource(
                            pdfUrl, org.open4goods.services.urlfetching.dto.ResourceType.PDF, "link",
                            "Hotpoint HIO3O41WFE user manual")),
                    false, null));
        });

        service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus());

        assertThat(product.getResources()).anySatisfy(resource ->
        {
            assertThat(resource.getUrl()).isEqualTo(pdfUrl);
            assertThat(resource.getResourceType()).isEqualTo(org.open4goods.model.resource.ResourceType.PDF);
            assertThat(resource.getDatasourceName()).isEqualTo("manufacturer");
            assertThat(resource.getTags()).contains("official");
        });
    }

    @Test
    void preparePromptVariables_FetchesOfficialEvidenceWithProxyFallback() throws Exception {
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(1);
        Product product = product("Samsung", "SM-S921B/DS");
        String officialUrl = "https://www.samsung.com/fr/smartphones/galaxy-s24/galaxy-s24-sm-s921b-ds/";
        String pdfUrl = "https://images.samsung.com/is/content/samsung/assets/fr/galaxy-s24/notice-sm-s921b-ds.pdf";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Samsung Galaxy S24 SM-S921B/DS | Produit officiel", officialUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String url = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, String> headers = invocation.getArgument(1);
            boolean proxied = Boolean.parseBoolean(headers.get("X-Open4goods-Playwright-Proxy"));
            if (!proxied) {
                return CompletableFuture.completedFuture(new FetchResponse(url, 403, "Forbidden", "",
                        FetchStrategy.HTTP));
            }
            String markdown = "Useful Samsung SM-S921B/DS official product content with display, camera, and battery details.";
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown,
                    FetchStrategy.PLAYWRIGHT, List.of(), Set.of(), List.of(
                            new org.open4goods.services.urlfetching.dto.ExtractedResource(pdfUrl,
                                    org.open4goods.services.urlfetching.dto.ResourceType.PDF, "link", "Notice")),
                    false, null));
        });

        service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus());

        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(urlFetchingService, org.mockito.Mockito.atLeastOnce())
                .fetchUrlAsync(org.mockito.ArgumentMatchers.eq(officialUrl), headersCaptor.capture());
        assertThat(headersCaptor.getAllValues()).anySatisfy(headers ->
                assertThat(headers.get("X-Open4goods-Playwright-Proxy")).isEqualTo("true"));
        assertThat(product.getResources()).anySatisfy(resource ->
                assertThat(resource.getUrl()).isEqualTo(pdfUrl));
    }

    @Test
    void preparePromptVariables_PromotesHighConfidenceOfficialModelBeforeResourceFiltering() throws Exception
    {
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(1);
        Product product = product("Tefcold", "Refrigerateur vitre professionnel FS1600H blanc");
        product.setAkaModels(Set.of("FS1600H", "12GO/512GO"));
        String officialUrl = "https://www.tefcold.com/fr/product/fs1600h";
        String pdfUrl = "https://assets.tefcold.com/manual-fs1600h.pdf";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Tefcold FS1600H product page", officialUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String markdown = """
                    # Tefcold FS1600H
                    Model: FS1600H
                    Refrigerated display cabinet official product content with dimensions, cooling, energy and warranty details.
                    """;
            return CompletableFuture.completedFuture(new FetchResponse(officialUrl, 200, markdown, markdown,
                    FetchStrategy.HTTP, List.of(new ExtractedMetadataAttribute("mpn", "FS1600H", "jsonld", "fr")),
                    Set.of(), List.of(new org.open4goods.services.urlfetching.dto.ExtractedResource(pdfUrl,
                            org.open4goods.services.urlfetching.dto.ResourceType.PDF, "link", "Manual")),
                    false, null));
        });

        Map<String, Object> variables = service.preparePromptVariables(product, verticalConfig(),
                new ReviewGenerationStatus());

        assertThat(product.model()).isEqualTo("FS1600H");
        assertThat(product.getAkaModels()).doesNotContain("12GO/512GO");
        assertThat(product.getResources()).anySatisfy(resource -> assertThat(resource.getUrl()).isEqualTo(pdfUrl));
        @SuppressWarnings("unchecked")
        Map<String, String> sources = (Map<String, String>) variables.get("sources");
        assertThat(sources).containsKey(officialUrl);
    }

    @Test
    void promoteModelFromOfficialEvidenceAllowsNamedModelFromOfficialMetadata()
    {
        Product product = product("Klarstein", "ABC1234");
        GoogleSearchResult result = new GoogleSearchResult("Klarstein ABC1234", "https://www.klarstein.fr/abc1234");
        FetchResponse response = new FetchResponse("https://www.klarstein.fr/abc1234", 200,
                "# Klarstein Velaire\nModel: Velaire", "# Klarstein Velaire\nModel: Velaire",
                FetchStrategy.HTTP, List.of(new ExtractedMetadataAttribute("model", "Velaire", "jsonld", "fr")),
                Set.of(), List.of(), false, null);

        Boolean promoted = ReflectionTestUtils.invokeMethod(service, "promoteModelFromOfficialEvidence",
                product, result, response);

        assertThat(promoted).isTrue();
        assertThat(product.model()).isEqualTo("Velaire");
    }

    @Test
    void promoteModelFromOfficialEvidenceDoesNotPromoteUrlOnlySibling()
    {
        Product product = product("Samsung", "4D 511");
        GoogleSearchResult result = new GoogleSearchResult("Samsung support", "https://www.samsung.com/fr/support/4d-515");
        FetchResponse response = new FetchResponse("https://www.samsung.com/fr/support/4d-515", 200,
                "# Samsung support\nUseful official support page.", "# Samsung support\nUseful official support page.",
                FetchStrategy.HTTP, List.of(), Set.of(), List.of(), false, null);

        Boolean promoted = ReflectionTestUtils.invokeMethod(service, "promoteModelFromOfficialEvidence",
                product, result, response);

        assertThat(promoted).isFalse();
        assertThat(product.model()).isEqualTo("4D 511");
    }

    @Test
    void preparePromptVariables_PersistsPdfResourcesButExcludesThemFromPromptSources() throws Exception
    {
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(2);
        Product product = product("Samsung", "SM-S921B/DS");
        String nonOfficialPdfUrl = "https://example.com/samsung-sm-s921b-ds-spec.pdf";
        String htmlUrl = "https://www.darty.com/samsung-s24.html";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Samsung Galaxy S24 Spec Sheet PDF", nonOfficialPdfUrl),
                new GoogleSearchResult("Samsung Galaxy S24 SM-S921B/DS review", htmlUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String url = invocation.getArgument(0);
            String markdown = "Samsung SM-S921B/DS review content with display, camera, and battery details.";
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown,
                    FetchStrategy.HTTP, List.of(), Set.of(), List.of(), false, null));
        });

        Map<String, Object> variables = service.preparePromptVariables(product, verticalConfig(),
                new ReviewGenerationStatus());

        assertThat(product.getResources()).anySatisfy(resource ->
        {
            assertThat(resource.getUrl()).isEqualTo(nonOfficialPdfUrl);
            assertThat(resource.getResourceType()).isEqualTo(org.open4goods.model.resource.ResourceType.PDF);
            assertThat(resource.getDatasourceName()).isEqualTo("manufacturer");
            assertThat(resource.getTags()).contains("official", "official:fr");
        });
        @SuppressWarnings("unchecked")
        Map<String, String> sources = (Map<String, String>) variables.get("sources");
        assertThat(sources).doesNotContainKey(nonOfficialPdfUrl);
        assertThat(sources).containsKey(htmlUrl);
        org.mockito.Mockito.verify(urlFetchingService, org.mockito.Mockito.never())
                .fetchUrlAsync(org.mockito.ArgumentMatchers.eq(nonOfficialPdfUrl),
                        org.mockito.ArgumentMatchers.any(Map.class));
    }

    @Test
    void preparePromptVariables_IgnoresUnrelatedPdfResources() throws Exception
    {
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(2);
        Product product = product("Samsung", "SM-S921B/DS");
        String unrelatedPdfUrl = "https://images.samsung.com/assets/fr/legal/digital-services-act.pdf";
        String htmlUrl = "https://www.darty.com/samsung-s24.html";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Digital Services Act", unrelatedPdfUrl),
                new GoogleSearchResult("Samsung Galaxy S24 SM-S921B/DS review", htmlUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String url = invocation.getArgument(0);
            String markdown = "Samsung SM-S921B/DS review content with display, camera, and battery details.";
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown,
                    FetchStrategy.HTTP, List.of(), Set.of(), List.of(), false, null));
        });

        Map<String, Object> variables = service.preparePromptVariables(product, verticalConfig(),
                new ReviewGenerationStatus());

        assertThat(product.getResources()).noneSatisfy(resource ->
                assertThat(resource.getUrl()).isEqualTo(unrelatedPdfUrl));
        @SuppressWarnings("unchecked")
        Map<String, String> sources = (Map<String, String>) variables.get("sources");
        assertThat(sources).doesNotContainKey(unrelatedPdfUrl);
        assertThat(sources).containsKey(htmlUrl);
        org.mockito.Mockito.verify(urlFetchingService, org.mockito.Mockito.never())
                .fetchUrlAsync(org.mockito.ArgumentMatchers.eq(unrelatedPdfUrl),
                        org.mockito.ArgumentMatchers.any(Map.class));
    }

    @Test
    void isProductRelevantResource_RequiresProductIdentifierInPdfUrl()
    {
        Product product = product("Samsung", "SM-S921B/DS");
        product.setId(8806094337471L);
        product.setAkaModels(Set.of("NV7B4550VAS/U1"));

        Boolean gtinMatch = ReflectionTestUtils.invokeMethod(service, "isProductRelevantResource", product,
                "https://example.com/manual-8806094337471.pdf", "Privacy policy");
        Boolean modelMatch = ReflectionTestUtils.invokeMethod(service, "isProductRelevantResource", product,
                "https://images.samsung.com/notice-sm-s921b-ds.pdf", "Notice");
        Boolean akaModelMatch = ReflectionTestUtils.invokeMethod(service, "isProductRelevantResource", product,
                "https://images.samsung.com/common-energylabel-nv7b4550vas-u1-productfiche.pdf", "A+");
        Boolean labelOnlyMatch = ReflectionTestUtils.invokeMethod(service, "isProductRelevantResource", product,
                "https://static-content.optimus.device.bolttech.eu/pdf/Samsung/SamsungPrivacyNotice/FR_PPOL.pdf",
                "Samsung SM-S921B/DS notice");
        Boolean genericNameOnlyMatch = ReflectionTestUtils.invokeMethod(service, "isProductRelevantResource", product,
                "https://images.samsung.com/common-energylabel-productfiche.pdf", "Classe energetique");
        Boolean terseOfficialModelPdfMatch = ReflectionTestUtils.invokeMethod(service, "isProductRelevantResource",
                product, "https://images.samsung.com/sm-s921b-ds.pdf", "SM-S921B/DS", true);

        assertThat(gtinMatch).isTrue();
        assertThat(modelMatch).isTrue();
        assertThat(akaModelMatch).isTrue();
        assertThat(labelOnlyMatch).isFalse();
        assertThat(genericNameOnlyMatch).isFalse();
        assertThat(terseOfficialModelPdfMatch).isTrue();
    }

    @Test
    void preparePromptVariables_SearchesBroadOfficialCandidatesBeforeGenericQueries() throws Exception {
        properties.setSerpBudget(4);
        properties.setPreferredDomains(List.of("darty.com"));
        Product product = product("Samsung", "SM-S921B/DS");
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService, org.mockito.Mockito.times(4)).search(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(GoogleSearchRequest::query)
                .containsExactly(
                        "Samsung \"SM-S921B/DS\" (official OR officiel OR product OR produit)",
                        "(site:darty.com) (\"Samsung SM-S921B/DS\")",
                        "Samsung \"SM-S921B/DS\" (avis OR review OR test OR guide)",
                        "Samsung \"SM-S921B/DS\" (support OR assistance OR manual OR notice OR datasheet OR \"fiche produit\")");
    }

    @Test
    void preparePromptVariables_PrefersPreciseAkaModelOverWrongPrimaryModel() throws Exception {
        properties.setSerpBudget(1);
        Product product = product("Samsung", "SM-F956BZAEEUB");
        product.setAkaModels(Set.of("SM-F966BZKBEUB", "Galaxy Z Fold7 256Go noir"));
        product.getExternalIds().setMpn(Set.of("SM-F966BZKBEUB"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService).search(requestCaptor.capture());
        assertThat(requestCaptor.getValue().query())
                .isEqualTo("Samsung \"SM-F966BZKBEUB\" (official OR officiel OR product OR produit)");
    }

    @Test
    void preparePromptVariables_ClassifiesApplianceEvidenceAsPartialUsable() throws Exception {
        properties.setSerpBudget(1);
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMaxUrlsPerProduct(1);
        Product product = product("ASKO", "OCS8478G");
        VerticalConfig verticalConfig = verticalConfig();
        verticalConfig.setId("oven");
        String officialUrl = "https://www.asko.hk/en/cooking/ovens/combi-steam-ovens/ocs8478g";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("ASKO OCS8478G official oven", officialUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(2000);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String markdown = "ASKO OCS8478G official oven content with steam, dimensions, cleaning and energy details.";
            return CompletableFuture.completedFuture(new FetchResponse(officialUrl, 200, markdown, markdown,
                    FetchStrategy.HTTP));
        });

        Map<String, Object> variables = service.preparePromptVariables(product, verticalConfig, new ReviewGenerationStatus());

        assertThat(variables.get("RESULT_QUALITY")).isEqualTo("PARTIAL_USABLE");
        assertThat(product.getOfficialUrl()).isEqualTo(officialUrl);
        assertThat(product.getReviewFacts()).extracting("url").containsExactly(officialUrl);
    }

    @Test
    void preparePromptVariables_FailsWhenOnlyMerchantAndSparePartPagesMatch() throws Exception {
        properties.setSerpBudget(1);
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(3);
        Product product = product("Electrolux", "EKG604000W");
        String merchantUrl = "https://buzzsxm.fr/fr/shop/cuisiniere";
        String sparePartUrl = "https://genuineapplianceparts.com.au/holder-glass-lower-left.html";
        String amazonSpareUrl = "https://www.amazon.co.uk/sparefixd-Front-Panel-Screw-Electrolux/dp/B08TCBYC1B";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Electrolux EKG604000W cuisiniere prix", merchantUrl),
                new GoogleSearchResult("Electrolux EKG604000W holder glass spare part", sparePartUrl),
                new GoogleSearchResult("sparefixd Front Panel Screw Electrolux EKG604000W", amazonSpareUrl))));
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String url = invocation.getArgument(0);
            String markdown = "Electrolux EKG604000W prix livraison shop stock achat cuisiniere gaz utile.";
            if (url.equals(sparePartUrl) || url.equals(amazonSpareUrl)) {
                markdown = "Electrolux EKG604000W replacement spare part holder glass front panel screw compatible.";
            }
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown,
                    FetchStrategy.HTTP));
        });

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        assertThat(product.getReviewFacts()).isEmpty();
    }

    @Test
    void preparePromptVariables_PersistsOpaqueOfficialSerpPdfWhenSerpTitleIdentifiesProduct() throws Exception {
        properties.setSerpBudget(1);
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        Product product = product("Electrolux", "EKG604000W");
        String pdfUrl = "https://www.electrolux-ui.com/DocumentDownLoad.aspx?DocURL=2021%5C867%5C365187umFR.pdf";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Electrolux EKG604000W notice mode d'emploi PDF", pdfUrl))));

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        assertThat(product.getResources()).anySatisfy(resource ->
        {
            assertThat(resource.getUrl()).isEqualTo(pdfUrl);
            assertThat(resource.getResourceType()).isEqualTo(org.open4goods.model.resource.ResourceType.PDF);
            assertThat(resource.getDatasourceName()).isEqualTo("manufacturer");
            assertThat(resource.getTags()).contains("official");
        });
    }

    @Test
    void preparePromptVariables_DoesNotPersistOpaquePdfFromOfficialPageWithoutProductIdentifier() throws Exception {
        properties.setSerpBudget(1);
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(1);
        Product product = product("Bosch", "HBG634BW1");
        String officialUrl = "https://www.bosch-home.fr/fr/mkt-product/HBG634BW1";
        String pdfUrl = "https://media3.bosch-home.com/Documents/20502352_CE_marking_conformity_letter_GR.pdf";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Bosch HBG634BW1 official product page", officialUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String markdown = "Bosch HBG634BW1 official oven content with cooking modes, energy and dimensions.";
            return CompletableFuture.completedFuture(new FetchResponse(officialUrl, 200, markdown, markdown,
                    FetchStrategy.HTTP, List.of(), Set.of(), List.of(new org.open4goods.services.urlfetching.dto.ExtractedResource(
                            pdfUrl, org.open4goods.services.urlfetching.dto.ResourceType.PDF, "link", "CE marking conformity letter")),
                    false, null));
        });

        service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus());

        assertThat(product.getResources()).noneSatisfy(resource ->
                assertThat(resource.getUrl()).isEqualTo(pdfUrl));
    }

    @Test
    void fetchWithFallbacks_UsesLongerRuntimeTimeoutForOfficialSources() throws Exception {
        properties.setMinMarkdownChars(20);
        String url = "https://www.electrolux.fr/kitchen/cooking/ovens/oven/eoc6p67wx/";
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String markdown = "Electrolux EOC6P67WX official oven content with steam, cooking and energy details.";
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown,
                    FetchStrategy.PLAYWRIGHT));
        });

        ReflectionTestUtils.invokeMethod(service, "fetchWithFallbacks", url, Map.of(), true);

        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(urlFetchingService).fetchUrlAsync(org.mockito.ArgumentMatchers.eq(url),
                headersCaptor.capture());
        assertThat(headersCaptor.getValue())
                .containsEntry(UrlFetchingService.FETCH_TIMEOUT_MS_HEADER, "15000");
    }

    @Test
    void classifySource_TreatsOfficialProductPathAsOfficialBeforeSparePartText() {
        Product product = product("Siemens", "HB774G1W1");
        org.open4goods.brand.model.Brand brand = new org.open4goods.brand.model.Brand();
        brand.setOfficialDomains(List.of("siemens-home.bsh-group.com"));
        when(brandService.resolve("Siemens")).thenReturn(brand);
        GoogleSearchResult result = new GoogleSearchResult("Siemens HB774G1W1 official product",
                "https://www.siemens-home.bsh-group.com/de/de/product/HB774G1W1");

        Object sourceClass = ReflectionTestUtils.invokeMethod(service, "classifySource", product, result,
                "Siemens HB774G1W1 replacement spare part wording appears in navigation.");

        assertThat(sourceClass).hasToString("OFFICIAL_PRODUCT");
    }

    @Test
    void classifySource_TreatsManufacturerHostAsOfficialBeforeCommerceAndForumText() {
        Product product = product("VEVOR", "BD-355JA");
        GoogleSearchResult result = new GoogleSearchResult("VEVOR chest freezer 345 L",
                "https://www.vevor.fr/machine-a-glacons-c_10725/vevor-congelateur-coffre-345-l-p_010826832541");

        Object sourceClass = ReflectionTestUtils.invokeMethod(service, "classifySource", product, result,
                "Question reponse panier acheter livraison stock. Item Model Number BD-355JA.");

        assertThat(sourceClass).hasToString("OFFICIAL_PRODUCT");
    }

    @Test
    void classifySource_RejectsBroadManualIndex() {
        Product product = product("Electrolux", "EKG604000W");
        GoogleSearchResult result = new GoogleSearchResult("Electrolux manuals and user guides",
                "https://www.example-manuals.test/electrolux/manuals");
        String markdown = """
                # Electrolux manuals
                Choose your model from all manuals. Page suivante. 430 results.
                ## EKG50100OW
                ## EKG51100OW
                ## EKG60100OW
                ## EKG61100OX
                ## EKK6450AOX
                ## EOB3400BOX
                ## EOC3430BOX
                ## EOC5654AOX
                ## ESL5205LO
                ## ESL5310LO
                ## ERN1300AOW
                ## ENN2801BOW
                """;

        Object sourceClass = ReflectionTestUtils.invokeMethod(service, "classifySource", product, result, markdown);

        assertThat(sourceClass).hasToString("MANUAL_INDEX");
    }

    @Test
    void classifySource_AcceptsExactModelManualPage() {
        Product product = product("Electrolux", "EKG604000W");
        GoogleSearchResult result = new GoogleSearchResult("Electrolux EKG604000W notice mode d'emploi",
                "https://www.example-manuals.test/electrolux/ekg604000w-manual");
        String markdown = """
                # Electrolux EKG604000W notice
                Manual and mode d emploi for the Electrolux EKG604000W cooker with installation and use details.
                """;

        Object sourceClass = ReflectionTestUtils.invokeMethod(service, "classifySource", product, result, markdown);

        assertThat(sourceClass).hasToString("GUIDE");
    }

    @Test
    void preparePromptVariables_AcceptsExactComparisonProductPageAsFallbackEvidence() throws Exception {
        properties.setSerpBudget(1);
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(1);
        Product product = product("TCL", "50C79K");
        String url = "https://www.lcd-compare.com/televiseur-TCL50C79K-TCL-50C79K.htm";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("TCL 50C79K - fiche technique, prix et avis", url))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String markdown = "TCL 50C79K fiche technique prix et avis televiseur QD Mini LED 50 pouces.";
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown,
                    FetchStrategy.HTTP));
        });

        Map<String, Object> variables = service.preparePromptVariables(product, verticalConfig(),
                new ReviewGenerationStatus());

        assertThat(variables.get("RESULT_QUALITY")).isEqualTo("COMPLETE");
        @SuppressWarnings("unchecked")
        Map<String, String> sourceClasses = (Map<String, String>) variables.get("SOURCE_CLASSES");
        assertThat(sourceClasses).containsEntry(url, "COMPARISON_PRODUCT_PAGE");
    }

    @Test
    void preparePromptVariables_AcceptsManufacturerPageWhenExactModelIsOnlyInFetchedContent() throws Exception {
        properties.setSerpBudget(1);
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(1);
        Product product = product("TCL", "50C79K");
        String url = "https://www.tcl.com/fr/fr/tvs/50c7k";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("TCL C7K Premium QD-Mini LED TV", url))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String markdown = "TCL C7K Premium QD-Mini LED TV. Modeles de la gamme: 50C79K, 55C79K, 65C79K.";
            return CompletableFuture.completedFuture(new FetchResponse(url, 200, markdown, markdown,
                    FetchStrategy.HTTP));
        });

        Map<String, Object> variables = service.preparePromptVariables(product, verticalConfig(),
                new ReviewGenerationStatus());

        assertThat(product.getOfficialUrl()).isEqualTo(url);
        assertThat(variables.get("RESULT_QUALITY")).isEqualTo("COMPLETE");
        @SuppressWarnings("unchecked")
        Map<String, String> sourceClasses = (Map<String, String>) variables.get("SOURCE_CLASSES");
        assertThat(sourceClasses).containsEntry(url, "OFFICIAL_PRODUCT");
    }

    @Test
    void preparePromptVariables_UsesConfiguredOfficialDomainForPrivateLabelBrand() throws Exception {
        properties.setSerpBudget(1);
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        org.open4goods.brand.model.Brand brand = new org.open4goods.brand.model.Brand();
        brand.setOfficialDomains(List.of("boulanger.com"));
        when(brandService.resolve("Essentiel B")).thenReturn(brand);
        Product product = product("Essentiel B", "ELS107-1B");
        String officialUrl = "https://www.boulanger.com/ref/8011605";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Lave linge sechant ESSENTIELB ELS107-1B", officialUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String markdown = "Essentiel B ELS107-1B fiche produit lave linge sechant avec capacite, programmes et consommation.";
            return CompletableFuture.completedFuture(new FetchResponse(officialUrl, 200, markdown, markdown,
                    FetchStrategy.HTTP));
        });

        Map<String, Object> variables = service.preparePromptVariables(product, verticalConfig(),
                new ReviewGenerationStatus());

        assertThat(product.getOfficialUrl()).isEqualTo(officialUrl);
        assertThat(variables.get("RESULT_QUALITY")).isEqualTo("COMPLETE");
        @SuppressWarnings("unchecked")
        Map<String, String> sourceClasses = (Map<String, String>) variables.get("SOURCE_CLASSES");
        assertThat(sourceClasses).containsEntry(officialUrl, "OFFICIAL_PRODUCT");
    }

    @Test
    void sanitizeMarkdown_RemovesConfiguredHeaderFooterNoiseLines() {
        String markdown = """
                Header
                Mesure fiable du contraste et de la luminosite.
                Abonnez-vous a notre newsletter
                Verdict avec des informations utiles.
                Copyright 2026
                """;

        String sanitized = ReflectionTestUtils.invokeMethod(service, "sanitizeMarkdown", markdown, "https://example.com/test");

        assertThat(sanitized).contains("Mesure fiable");
        assertThat(sanitized).contains("Verdict");
        assertThat(sanitized).doesNotContain("Header");
        assertThat(sanitized).doesNotContain("Abonnez-vous");
        assertThat(sanitized).doesNotContain("Copyright");
    }

    @Test
    void isValidFetch_RejectsCloudflareChallengeContent() {
        String challengeHtml = """
                <!DOCTYPE html><html lang="en-US"><head><title>Just a moment...</title></head>
                <body>
                <noscript>Enable JavaScript and cookies to continue</noscript>
                <script>window._cf_chl_opt = {}; var a = '/cdn-cgi/challenge-platform/h/b/orchestrate/chl_page/v1';</script>
                </body></html>
                """;
        FetchResponse response = new FetchResponse("https://manuall.fr/edson-ielv49-lave-vaisselle/", 200,
                challengeHtml, challengeHtml, FetchStrategy.HTTP);

        Boolean valid = ReflectionTestUtils.invokeMethod(service, "isValidFetch", response);

        assertThat(valid).isFalse();
    }

    @Test
    void isValidFetch_RejectsMarkdownBelowConfiguredMinimumLength() {
        properties.setMinMarkdownChars(100);
        FetchResponse response = new FetchResponse("https://example.com/review", 200,
                "<html><body>Short placeholder.</body></html>", "Short placeholder.", FetchStrategy.HTTP);

        Boolean valid = ReflectionTestUtils.invokeMethod(service, "isValidFetch", response);

        assertThat(valid).isFalse();
    }

    @Test
    void isValidFetch_AcceptsProductReviewContent() {
        String markdown = """
                # Test du lave-vaisselle Edson IELV49

                Ce contenu de test decrit les performances de lavage, le sechage, le bruit et la consommation.
                Le verdict compare les points forts et les limites du produit avec des observations exploitables.
                La partie lavage detaille les resultats sur les taches incrustees, les verres et les casseroles.
                La partie sechage indique les limites sur les plastiques, les couverts et la vaisselle dense.
                La partie ergonomie couvre le rangement interieur, le panier a couverts, les programmes et l'affichage.
                La partie consommation donne des reperes sur l'eau, l'electricite, la duree des cycles et le mode eco.
                La conclusion explique pour quels foyers ce modele est pertinent et dans quels cas il faut choisir un autre appareil.
                """;
        FetchResponse response = new FetchResponse("https://example.com/review", 200,
                "<html><body>" + markdown + "</body></html>", markdown, FetchStrategy.HTTP);

        Boolean valid = ReflectionTestUtils.invokeMethod(service, "isValidFetch", response);

        assertThat(valid).isTrue();
    }

    @Test
    void fetchWithFallbacks_ReturnsNoResponseWhenEveryStrategyIsInvalid() throws Exception {
        properties.setMinMarkdownChars(100);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenReturn(
                CompletableFuture.completedFuture(new FetchResponse("https://example.com/blocked", 403,
                        "Forbidden", "", FetchStrategy.HTTP)));

        Object outcome = ReflectionTestUtils.invokeMethod(service, "fetchWithFallbacks",
                "https://example.com/blocked", Map.of());

        assertThat(ReflectionTestUtils.getField(outcome, "response")).isNull();
        assertThat(ReflectionTestUtils.getField(outcome, "rejectionReason")).asString()
                .contains("HTTP 403");
    }

    @Test
    void isRelevantContent_ValidatesBrandAndModelPresence()
    {
        String content = "# Beko MWBM8147EB\nCe lave-linge Beko MWBM8147EB est d'une grande efficacite.";

        // Brand and model matches exactly
        Boolean match = ReflectionTestUtils.invokeMethod(service, "isRelevantContent",
                content, "Beko", "MWBM8147EB", Set.of("MWBM8147EB"));
        assertThat(match).isTrue();

        // Alternate model matches
        match = ReflectionTestUtils.invokeMethod(service, "isRelevantContent",
                content, "Beko", "Alternative", Set.of("MWBM8147EB"));
        assertThat(match).isTrue();

        // Tokenized fallback matches
        match = ReflectionTestUtils.invokeMethod(service, "isRelevantContent",
                content, "Beko", "Lave-linge MWBM8147EB 7004840077", Set.of());
        assertThat(match).isTrue();

        // Irrelevant content - no model match
        match = ReflectionTestUtils.invokeMethod(service, "isRelevantContent",
                "Avis sur le lave-vaisselle Beko qui fuit.", "Beko", "MWBM8147EB", Set.of());
        assertThat(match).isFalse();

        // Irrelevant content - no brand match
        match = ReflectionTestUtils.invokeMethod(service, "isRelevantContent",
                "Ce lave-linge MWBM8147EB est d'une grande efficacite.", "Miele", "MWBM8147EB", Set.of());
        assertThat(match).isFalse();

        // Compound brand with 2-char prefix (e.g. "LG Electronics") — "LG" must match via word fallback
        String lgContent = "# LG 32LM6380PLC\nLG smart TV with Full HD resolution and webOS.";
        match = ReflectionTestUtils.invokeMethod(service, "isRelevantContent",
                lgContent, "LG Electronics", "32LM6380PLC", Set.of());
        assertThat(match).isTrue();

        // Compound brand — none of the words present should still return false
        match = ReflectionTestUtils.invokeMethod(service, "isRelevantContent",
                "# 32LM6380PLC\nSmart TV with Full HD resolution.", "LG Electronics", "32LM6380PLC", Set.of());
        assertThat(match).isFalse();
    }

    @Test
    void buildPromptVariablesFromReviewFacts_OrdersOfficialSourcesBeforeOtherAcceptedFacts() throws Exception {
        Product product = product("Samsung", "S95D");
        String official = "https://www.samsung.com/fr/tvs/s95d/";
        String support = "https://www.samsung.com/fr/support/model/s95d/";
        String review = "https://www.lesnumeriques.com/tv/samsung-s95d-test.html";
        product.setOfficialUrl(official);
        product.addOfficialSupportUrl("fr", support);
        product.setReviewFacts(List.of(
                new ProductFact(review, "review markdown", "fr", 1L, "HTTP", 10, "a"),
                new ProductFact(support, "support markdown", "fr", 1L, "HTTP", 10, "b"),
                new ProductFact(official, "official markdown", "fr", 1L, "HTTP", 10, "c")));
        ProductFetchDiagnostics diagnostics = new ProductFetchDiagnostics();
        diagnostics.setAcceptedUrls(List.of(review, support, official));
        product.setReviewFetchDiagnostics(diagnostics);

        Map<String, Object> variables = service.buildPromptVariablesFromReviewFacts(product, verticalConfig(), true);

        assertThat((List<String>) variables.get("ACCEPTED_URLS"))
                .containsExactly(official, support, review);
        assertThat((String) variables.get("ATTRIBUTE_SOURCES_JSON"))
                .contains("\"number\":1")
                .contains(official);
    }

    @Test
    void buildPromptVariablesFromReviewFacts_IncludesAttributeUnitParserHints() throws Exception {
        Product product = product("Samsung", "S95D");
        String official = "https://www.samsung.com/fr/tvs/s95d/";
        product.setOfficialUrl(official);
        product.setReviewFacts(List.of(new ProductFact(official, "official markdown", "fr", 1L, "HTTP", 10, "a")));
        ProductFetchDiagnostics diagnostics = new ProductFetchDiagnostics();
        diagnostics.setAcceptedUrls(List.of(official));
        product.setReviewFetchDiagnostics(diagnostics);

        Map<String, Object> variables = service.buildPromptVariablesFromReviewFacts(product, verticalConfigWithNumericAttribute(), true);

        assertThat((String) variables.get("ATTRIBUTE_DEFINITIONS_JSON"))
                .contains("\"key\":\"HEIGHT\"")
                .contains("\"filteringType\":\"NUMERIC\"")
                .contains("\"dimension\":\"LENGTH\"")
                .contains("\"defaultUnitHint\":\"cm\"");
    }

    @Test
    void buildBasePromptVariables_IncludesOnlyTrustedStructuredFacts() {
        Product product = product("Samsung", "S95D");
        ProductAttribute energyClass = new ProductAttribute();
        energyClass.setName("ENERGY_CLASS");
        energyClass.addSourceAttribute(new SourcedAttribute(new Attribute("ENERGY_CLASS", "D", "fr"), "EPREL"));
        ProductAttribute merchantPrice = new ProductAttribute();
        merchantPrice.setName("PRICE");
        merchantPrice.addSourceAttribute(new SourcedAttribute(new Attribute("PRICE", "499", "fr"), "merchant-offer"));
        ProductAttribute icecatDisplayName = new ProductAttribute();
        icecatDisplayName.setName("CLASSE D'EFFICACITE ENERGETIQUE");
        icecatDisplayName.addSourceAttribute(new SourcedAttribute(
                new Attribute("CLASSE D'EFFICACITE ENERGETIQUE", "D", "fr"), "icecat.biz"));
        product.getAttributes().getAll().put("ENERGY_CLASS", energyClass);
        product.getAttributes().getAll().put("PRICE", merchantPrice);
        product.getAttributes().getAll().put("CLASSE D'EFFICACITE ENERGETIQUE", icecatDisplayName);

        Map<String, Object> variables = service.buildBasePromptVariables(product, verticalConfigWithEnergyClassAttribute());

        assertThat((String) variables.get("STRUCTURED_TRUSTED_FACTS_JSON"))
                .contains("\"key\":\"ENERGY_CLASS\"")
                .contains("\"datasource\":\"EPREL\"")
                .doesNotContain("\"key\":\"PRICE\"")
                .doesNotContain("CLASSE D'EFFICACITE ENERGETIQUE");
        assertThat((Map<?, ?>) variables.get("sources")).isEmpty();
        assertThat((Map<?, ?>) variables.get("tokens")).isEmpty();
    }

    @Test
    void preparePromptVariables_UsesStructuredFactsLimitedFallbackWhenFetchFails() throws Exception {
        Product product = product("Smeg", "SF64M3TVX");
        product.setId(8017709256524L);
        addTrustedAttribute(product, "ENERGY_CLASS", "A", "EPREL");
        addTrustedAttribute(product, "HEIGHT", "59.2 cm", "icecat.biz");
        addTrustedAttribute(product, "WIDTH", "59.7 cm", "EPREL");
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());
        when(promptService.estimateTokens(any(String.class))).thenReturn(220);

        Map<String, Object> variables = service.preparePromptVariables(product,
                verticalConfigWithStructuredFallbackAttributes(), new ReviewGenerationStatus());

        assertThat(variables.get("RESULT_QUALITY")).isEqualTo("LIMITED_STRUCTURED");
        assertThat(variables.get("FALLBACK_MODE")).isEqualTo(true);
        assertThat((Map<?, ?>) variables.get("sources")).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, String> sourceClasses = (Map<String, String>) variables.get("SOURCE_CLASSES");
        assertThat(sourceClasses)
                .containsEntry("https://www.open4goods.org/structured-facts/8017709256524", "STRUCTURED_FACTS");
        assertThat(product.getReviewFacts()).hasSize(1);
        assertThat(product.getReviewFacts().getFirst().getFetchStrategy()).isEqualTo("STRUCTURED_FACTS");
        assertThat(product.getReviewFacts().getFirst().getMarkdown())
                .contains("Donnees structurees verifiees")
                .contains("ENERGY_CLASS: A")
                .contains("HEIGHT: 59.2 cm")
                .contains("WIDTH: 59.7 cm")
                .contains("Limites");
    }

    private void addTrustedAttribute(Product product, String key, String value, String datasource) {
        ProductAttribute attribute = new ProductAttribute();
        attribute.setName(key);
        attribute.addSourceAttribute(new SourcedAttribute(new Attribute(key, value, "fr"), datasource));
        product.getAttributes().getAll().put(key, attribute);
    }

    private Product product(String brand, String model) {
        Product product = new Product();
        product.setId(1L);
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, brand);
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, model);
        return product;
    }

    private VerticalConfig verticalConfig() {
        VerticalConfig verticalConfig = new VerticalConfig();
        ProductI18nElements i18n = new ProductI18nElements();
        i18n.setPageTitle("lave-linge");
        Map<String, ProductI18nElements> texts = new HashMap<>();
        texts.put("fr", i18n);
        texts.put("default", i18n);
        verticalConfig.setI18n(texts);
        verticalConfig.setAttributesConfig(new AttributesConfig());
        return verticalConfig;
    }

    private VerticalConfig verticalConfigWithNumericAttribute() {
        VerticalConfig verticalConfig = verticalConfig();
        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey("HEIGHT");
        attributeConfig.setFilteringType(AttributeType.NUMERIC);
        Localisable<String, String> unit = new Localisable<>();
        unit.put("default", "cm");
        attributeConfig.setUnit(unit);
        AttributeParserConfig parser = new AttributeParserConfig();
        parser.setDimension("LENGTH");
        parser.setDefaultUnitHint("cm");
        attributeConfig.setParser(parser);
        AttributesConfig attributesConfig = new AttributesConfig();
        attributesConfig.setConfigs(List.of(attributeConfig));
        verticalConfig.setAttributesConfig(attributesConfig);
        return verticalConfig;
    }

    private VerticalConfig verticalConfigWithEnergyClassAttribute() {
        VerticalConfig verticalConfig = verticalConfig();
        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey("ENERGY_CLASS");
        AttributesConfig attributesConfig = new AttributesConfig();
        attributesConfig.setConfigs(List.of(attributeConfig));
        verticalConfig.setAttributesConfig(attributesConfig);
        return verticalConfig;
    }

    private VerticalConfig verticalConfigWithStructuredFallbackAttributes() {
        VerticalConfig verticalConfig = verticalConfig();
        AttributeConfig energyClass = new AttributeConfig();
        energyClass.setKey("ENERGY_CLASS");
        AttributeConfig height = new AttributeConfig();
        height.setKey("HEIGHT");
        AttributeConfig width = new AttributeConfig();
        width.setKey("WIDTH");
        AttributesConfig attributesConfig = new AttributesConfig();
        attributesConfig.setConfigs(List.of(energyClass, height, width));
        verticalConfig.setAttributesConfig(attributesConfig);
        return verticalConfig;
    }
}
