package org.open4goods.api.services.aggregation.services.realtime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.open4goods.api.services.completion.text.DjlTextEmbeddingService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.PrefixedAttrText;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NamesAggregationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(NamesAggregationServiceTest.class);

    @Test
    void testEmbeddingIntegration() throws Exception {
        // Mocks
        VerticalsConfigService verticalService = mock(VerticalsConfigService.class);
        EvaluationService evaluationService = mock(EvaluationService.class);
        BlablaService blablaService = mock(BlablaService.class);
        DjlTextEmbeddingService embeddingService = mock(DjlTextEmbeddingService.class);

        // Service under test
        NamesAggregationService service = new NamesAggregationService(
                logger, verticalService, evaluationService, blablaService, embeddingService);

        // Data Setup
        Product product = new Product(1L);
        product.setVertical("electronics");
        product.getOfferNames().add("Super TV 4K"); // bestName will pick this

        // Vertical Config Setup
        VerticalConfig vc = new VerticalConfig();
        vc.setId("electronics");
        ProductI18nElements i18n = new ProductI18nElements();
        PrefixedAttrText h1 = new PrefixedAttrText();
        h1.setPrefix("Televiseur");
        i18n.setH1Title(h1);
        vc.getI18n().put("fr", i18n);

        when(verticalService.getConfigByIdOrDefault("electronics")).thenReturn(vc);

        // Embedding Mock
        float[] mockEmbedding = new float[768];
        mockEmbedding[0] = 0.1f;
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);

        // Execution
        service.onProduct(product, vc);

        // Verification
        // Expected text: "Televiseur Super TV 4K" (Category + Name)
        verify(embeddingService, times(1)).embed("Televiseur Super TV 4K");
        
        Assertions.assertNotNull(product.getEmbedding());
        Assertions.assertEquals(0.1f, product.getEmbedding()[0]);
    }
}
