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
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.product.Product;
import org.open4goods.model.review.ReviewGenerationStatus;
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
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
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

    @BeforeEach
    void setUp() {
        properties = new ReviewGenerationConfig();
        properties.setMaxSearch(1);
        properties.setPreferredDomains(List.of("lesnumeriques.com", "fnac.com"));
        properties.setSearchResultsPerQuery(7);
        properties.setSearchGeoLocation("fr");
        properties.setSearchHostLanguage("fr");
        service = new ReviewGenerationPreprocessingService(properties, googleSearchService, urlFetchingService,
                promptService, serialisationService, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    @Test
    void preparePromptVariables_SearchesOfficialDiscoveryThenPreferredDomainsAndPreservesSearchKeys() throws Exception {
        properties.setMaxSearch(2);
        Product product = product("Sony", "XR55A80L");
        product.setAkaModels(Set.of("XR-55A80L"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, new VerticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService, org.mockito.Mockito.times(2)).search(requestCaptor.capture());
        List<GoogleSearchRequest> requests = requestCaptor.getAllValues();
        GoogleSearchRequest officialRequest = requests.getFirst();
        GoogleSearchRequest preferredRequest = requests.get(1);

        assertThat(officialRequest.query()).isEqualTo("Sony \"XR55A80L\" (official OR officiel OR product OR produit)");
        assertThat(preferredRequest.query()).startsWith("(site:lesnumeriques.com OR site:fnac.com)");
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
    void preparePromptVariables_FailsBeforeSearchWhenRequiredSerpKeysAreMissing() {
        Product product = new Product();
        product.setId(42L);

        assertThatThrownBy(() -> service.preparePromptVariables(product, new VerticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing brand, model");
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
    }

    @Test
    void preparePromptVariables_StoresOfficialSupportUrlWithoutReplacingOfficialProductUrl() throws Exception {
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

        assertThat(product.getOfficialUrl()).isNull();
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

        assertThat(product.getResources()).anySatisfy(resource ->
        {
            assertThat(resource.getUrl()).isEqualTo(pdfUrl);
            assertThat(resource.getResourceType()).isEqualTo(org.open4goods.model.resource.ResourceType.PDF);
            assertThat(resource.getDatasourceName()).isEqualTo("manufacturer");
            assertThat(resource.getTags()).contains("official", "official:fr");
        });
    }

    @Test
    void preparePromptVariables_PersistsPdfResourcesEvenFromNonOfficialPages() throws Exception
    {
        properties.setMinMarkdownChars(20);
        properties.setSourceMinTokens(1);
        properties.setMinGlobalTokens(1);
        properties.setMinUrlCount(1);
        properties.setMaxUrlsPerProduct(1);
        Product product = product("Samsung", "SM-S921B/DS");
        String nonOfficialPdfUrl = "https://example.com/samsung-s24-spec.pdf";
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse(List.of(
                new GoogleSearchResult("Samsung Galaxy S24 Spec Sheet PDF", nonOfficialPdfUrl))));
        when(promptService.estimateTokens(any(String.class))).thenReturn(300);
        when(urlFetchingService.fetchUrlAsync(any(String.class), any(Map.class))).thenAnswer(invocation ->
        {
            String markdown = "Samsung SM-S921B/DS spec details from PDF.";
            return CompletableFuture.completedFuture(new FetchResponse(nonOfficialPdfUrl, 200, markdown, markdown,
                    FetchStrategy.HTTP, List.of(), Set.of(), List.of(), false, null));
        });

        service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus());

        assertThat(product.getResources()).anySatisfy(resource ->
        {
            assertThat(resource.getUrl()).isEqualTo(nonOfficialPdfUrl);
            assertThat(resource.getResourceType()).isEqualTo(org.open4goods.model.resource.ResourceType.PDF);
            assertThat(resource.getDatasourceName()).isEqualTo("manufacturer");
            assertThat(resource.getTags()).contains("official", "official:fr");
        });
    }

    @Test
    void preparePromptVariables_SearchesBroadOfficialCandidatesBeforeGenericQueries() throws Exception {
        properties.setMaxSearch(3);
        properties.setPreferredDomains(List.of("darty.com"));
        Product product = product("Samsung", "SM-S921B/DS");
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, verticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService, org.mockito.Mockito.times(3)).search(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(GoogleSearchRequest::query)
                .containsExactly(
                        "Samsung \"SM-S921B/DS\" (official OR officiel OR product OR produit)",
                        "(site:darty.com) (\"Samsung SM-S921B/DS\")",
                        "test Samsung \"SM-S921B/DS\"");
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
        i18n.getH1Title().setPrefix("lave-linge");
        Map<String, ProductI18nElements> texts = new HashMap<>();
        texts.put("fr", i18n);
        texts.put("default", i18n);
        verticalConfig.setI18n(texts);
        return verticalConfig;
    }
}
