package org.open4goods.services.reviewgeneration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class ReviewGenerationReproductionTest {

    private ReviewGenerationService reviewGenerationService;
    private ReviewGenerationPreprocessingService preprocessingService;

    private ReviewGenerationConfig properties;
    @Mock private GoogleSearchService googleSearchService;
    @Mock private org.open4goods.services.urlfetching.service.UrlFetchingService urlFetchingService;
    @Mock private PromptService genAiService;
    @Mock private BatchPromptService batchAiService;
    @Mock private ProductRepository productRepository;
    @Mock private VerticalsConfigService verticalsConfigService;
    private MeterRegistry meterRegistry;

    // We can use a real TemplateEngine to verify the template expression evaluation
    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        properties = new ReviewGenerationConfig();
        properties.setPromptKey("review-generation");
        properties.setThreadPoolSize(1);
        properties.setMaxQueueSize(10);
        properties.setBatchFolder(System.getProperty("java.io.tmpdir") + "/batch-test");

        meterRegistry = new SimpleMeterRegistry();

        preprocessingService = new ReviewGenerationPreprocessingService(
                properties,
                googleSearchService,
                (org.open4goods.services.urlfetching.service.UrlFetchingService) urlFetchingService,
                genAiService,
                new SerialisationService()
        );

        reviewGenerationService = new ReviewGenerationService(
                properties,
                googleSearchService,
                urlFetchingService,
                genAiService,
                batchAiService,
                meterRegistry,
                productRepository,
                preprocessingService,
                verticalsConfigService
        );

        // Setup template engine similar to production
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.TEXT);
        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    @Test
    void reproduceNPE_WithModelWebSearch() throws Exception {
        // GIVEN
        Product product = new Product(123L);
        product.setReviews(new Localisable<>());

        // Product doesn't have simple setters, use attributes directly
        product.getAttributes().addReferentielAttribute(org.open4goods.model.attribute.ReferentielKey.BRAND, "TestBrand");
        product.getAttributes().addReferentielAttribute(org.open4goods.model.attribute.ReferentielKey.MODEL, "TestModel");

        // Mock Vertical Config hierarchy
        VerticalConfig verticalConfig = mock(VerticalConfig.class);
        org.open4goods.model.vertical.ProductI18nElements i18nElements = mock(org.open4goods.model.vertical.ProductI18nElements.class);
        org.open4goods.model.vertical.PrefixedAttrText h1Title = mock(org.open4goods.model.vertical.PrefixedAttrText.class);
        when(h1Title.getPrefix()).thenReturn("VerticalName");
        when(i18nElements.getH1Title()).thenReturn(h1Title);
        when(verticalConfig.i18n("fr")).thenReturn(i18nElements);
        org.open4goods.model.vertical.AttributesConfig attributesConfig = mock(org.open4goods.model.vertical.AttributesConfig.class);
        when(attributesConfig.getConfigs()).thenReturn(List.of());
        when(verticalConfig.getAttributesConfig()).thenReturn(attributesConfig);
        
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "BrandX");
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "ModelY");
        
        // Add ecoscore
        org.open4goods.model.product.Score ecoscore = new org.open4goods.model.product.Score();
        ecoscore.setName("ecoscore");
        ecoscore.setValue(15.5);
        product.getScores().put("ECOSCORE", ecoscore);
        
        // Add detailed scores
        org.open4goods.model.product.Score detail1 = new org.open4goods.model.product.Score();
        detail1.setName("repairability");
        detail1.setValue(8.2);
        
        org.open4goods.model.product.Score detail2 = new org.open4goods.model.product.Score();
        detail2.setName("durability");
        detail2.setValue(7.5);
        
        Map<String, org.open4goods.model.product.Score> scores = new HashMap<>();
        scores.put("repairability", detail1);
        scores.put("durability", detail2);
        scores.put("ECOSCORE", ecoscore); // Add ecoscore to the map
        product.setScores(scores);

        when(verticalConfig.getAvailableImpactScoreCriterias()).thenReturn(List.of("repairability")); // Only repairability should be in PRODUCT_SCORES_JSON

        // When generating review, the preprocessing service calls buildBasePromptVariables
        Map<String, Object> variables = preprocessingService.buildBasePromptVariables(product, verticalConfig);

        // ATTEMPT TO REPRODUCE: Check if variables are missing
        // In the buggy version, "sources" is missing.
        assertThat(variables).containsKey("sources");
        assertThat(variables).containsKey("PRODUCT_ECOSCORE_JSON");
        assertThat(variables).containsKey("PRODUCT_SCORES_JSON");

        // Let's simulate the template execution that fails
        String template = "[# th:if=\"${!sources.isEmpty()}\"]Has Sources[/][# th:if=\"${sources.isEmpty()}\"]No Sources[/]";
        template += " | Ecoscore: [(${PRODUCT_ECOSCORE_JSON})]";
        template += " | Scores: [(${PRODUCT_SCORES_JSON})]";
        template += " | Caracs: [[${PRODUCT.caracteristics()}]]";
        
        Context context = new Context();
        context.setVariables(variables);

        // This fails if sources is null
        // With the fix, this should now succeed
        String result = templateEngine.process(template, context);
        assertThat(result).contains("No Sources");
        assertThat(result).contains("\"name\":\"ecoscore\"");
        assertThat(result).contains("\"value\":15.5");
        assertThat(result).contains("\"repairability\"");
        assertThat(result).doesNotContain("\"durability\""); // Filtered out
        assertThat(result).contains("Caracs: ");
    }
}
