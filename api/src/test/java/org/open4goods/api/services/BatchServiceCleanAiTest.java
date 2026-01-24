package org.open4goods.api.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.VerticalsConfigService;

public class BatchServiceCleanAiTest {

    @Test
    public void testCleanAiData() {
        // Mocks
        AggregationFacadeService aggregationFacadeService = mock(AggregationFacadeService.class);
        CompletionFacadeService completionFacadeService = mock(CompletionFacadeService.class);
        VerticalsConfigService verticalsConfigService = mock(VerticalsConfigService.class);
        ProductRepository dataRepository = mock(ProductRepository.class);
        CsvDatasourceFetchingService csvDatasourceFetchingService = mock(CsvDatasourceFetchingService.class);
        FeedService feedService = mock(FeedService.class);
        SerialisationService serialisationService = mock(SerialisationService.class);

        BatchService batchService = new BatchService(aggregationFacadeService, completionFacadeService, verticalsConfigService, dataRepository, csvDatasourceFetchingService, feedService, serialisationService);

        // Setup Data
        VerticalConfig vertical = new VerticalConfig();
        vertical.setId("test-vertical");
        when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(List.of(vertical));

        Product product = new Product();
        product.setId(123L);
        
        // 1. Setup AI Review
        Localisable<String, AiReviewHolder> reviews = new Localisable<>();
        reviews.put("en", new AiReviewHolder());
        product.setReviews(reviews);

        // 2. Setup Attributes
        // Attribute with mixed sources (AI + others)
        IndexedAttribute mixedAttr = new IndexedAttribute();
        mixedAttr.setName("mixed");
        SourcedAttribute aiSource = new SourcedAttribute();
        aiSource.setDataSourcename("openAI");
        aiSource.setValue("ai-val");
        SourcedAttribute humanSource = new SourcedAttribute();
        humanSource.setDataSourcename("manual");
        humanSource.setValue("human-val");
        // Use mutable set
        mixedAttr.setSource(new HashSet<>(Set.of(aiSource, humanSource)));
        mixedAttr.setValue("ai-val"); // Assume AI was winner
        product.getAttributes().getIndexed().put("mixed", mixedAttr);

        // Attribute with only AI source
        ProductAttribute aiOnlyAttr = new ProductAttribute();
        aiOnlyAttr.setName("aiOnly");
        SourcedAttribute aiSource2 = new SourcedAttribute();
        aiSource2.setDataSourcename("some-AI-gen");
        aiSource2.setValue("ai-val-2");
        // Use mutable set
        aiOnlyAttr.setSource(new HashSet<>(Set.of(aiSource2)));
        product.getAttributes().getAll().put("aiOnly", aiOnlyAttr);

        // Attribute with no AI source
        IndexedAttribute cleanAttr = new IndexedAttribute();
        cleanAttr.setName("clean");
        SourcedAttribute cleanSource = new SourcedAttribute();
        cleanSource.setDataSourcename("clean-source");
        cleanSource.setValue("clean-val");
        // Use mutable set
        cleanAttr.setSource(new HashSet<>(Set.of(cleanSource)));
        product.getAttributes().getIndexed().put("clean", cleanAttr);


        when(dataRepository.getProductsMatchingVerticalId(vertical)).thenReturn(Stream.of(product));

        // Execute
        batchService.cleanAiData(vertical);

        // Verify
        verify(dataRepository, times(1)).index(product);

        // Check Review cleared
        assertTrue(product.getReviews().isEmpty());

        // Check Mixed Attribute
        IndexedAttribute updatedMixed = product.getAttributes().getIndexed().get("mixed");
        assertEquals(1, updatedMixed.getSource().size());
        assertFalse(updatedMixed.getSource().stream().anyMatch(s -> s.getDataSourcename().contains("AI")));
        assertEquals("human-val", updatedMixed.getValue());

        // Check AI Only Attribute - should be removed
        assertFalse(product.getAttributes().getAll().containsKey("aiOnly"));

        // Check Clean Attribute - should be untouched
        assertTrue(product.getAttributes().getIndexed().containsKey("clean"));
        assertEquals(1, product.getAttributes().getIndexed().get("clean").getSource().size());
    }
}
