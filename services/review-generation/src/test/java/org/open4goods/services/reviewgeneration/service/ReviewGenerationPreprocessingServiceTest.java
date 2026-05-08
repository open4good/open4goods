package org.open4goods.services.reviewgeneration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.product.Product;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
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
                promptService, serialisationService);
    }

    @Test
    void preparePromptVariables_SearchesPreferredDomainsFirstAndPreservesSearchKeys() throws Exception {
        Product product = product("Sony", "XR55A80L");
        product.setAkaModels(Set.of("XR-55A80L"));
        when(googleSearchService.search(any(GoogleSearchRequest.class))).thenReturn(new GoogleSearchResponse());

        assertThatThrownBy(() -> service.preparePromptVariables(product, new VerticalConfig(), new ReviewGenerationStatus()))
                .isInstanceOf(NotEnoughDataException.class);

        ArgumentCaptor<GoogleSearchRequest> requestCaptor = ArgumentCaptor.forClass(GoogleSearchRequest.class);
        org.mockito.Mockito.verify(googleSearchService).search(requestCaptor.capture());
        GoogleSearchRequest request = requestCaptor.getValue();

        assertThat(request.query()).startsWith("(site:lesnumeriques.com OR site:fnac.com)");
        assertThat(request.query()).contains("\"Sony XR55A80L\"");
        assertThat(request.query()).contains("\"Sony XR-55A80L\"");
        assertThat(request.numResults()).isEqualTo(7);
        assertThat(request.lr()).isEqualTo("lang_fr");
        assertThat(request.cr()).isEqualTo("countryFR");
        assertThat(request.gl()).isEqualTo("fr");
        assertThat(request.hl()).isEqualTo("fr");
        assertThat(request.safe()).isEqualTo("off");
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

    private Product product(String brand, String model) {
        Product product = new Product();
        product.setId(1L);
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, brand);
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, model);
        return product;
    }
}
