package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ScoringAggregationConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.open4goods.brand.service.BrandScoreService;
import org.open4goods.brand.service.BrandService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

class AggregationFacadeServiceParticipatingScoresIT {

    @Test
    void scoringAggregatorShouldProduceAggregatedScoreFromParticipations() throws Exception {
        VerticalConfig verticalConfig = verticalConfig();
        Product first = productWithAttributes(1L, "5", "1");
        Product second = productWithAttributes(2L, "1", "5");
        List<Product> dataset = List.of(first, second);

        AggregationFacadeService facade = aggregationFacadeService();
        ScoringBatchedAggregator aggregator = facade.getScoringAggregator();

        aggregator.score(dataset, verticalConfig);

        Score aggregated = first.getScores().get("AGG");
        assertThat(aggregated).isNotNull();
        assertThat(aggregated.getAggregates()).containsEntry("SCORE_A", 0.6).containsEntry("SCORE_B", 0.4);
        assertThat(aggregated.getAbsolute().getValue()).isCloseTo(3.4, within(1e-6));
    }

    private AggregationFacadeService aggregationFacadeService() {
        EvaluationService evaluationService = mock(EvaluationService.class);
        ProductRepository repository = mock(ProductRepository.class);
        ApiProperties apiProperties = new ApiProperties();
        apiProperties.setRootFolder("/tmp/open4goods/");

        AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
        doAnswer(invocation -> null).when(beanFactory).autowireBean(any());

        return new AggregationFacadeService(
                evaluationService,
                mock(org.open4goods.model.StandardiserService.class),
                beanFactory,
                repository,
                apiProperties,
                mock(Gs1PrefixService.class),
                mock(DataSourceConfigService.class),
                mock(VerticalsConfigService.class),
                mock(BarcodeValidationService.class),
                mock(BrandService.class),
                mock(GoogleTaxonomyService.class),
                mock(BlablaService.class),
                mock(IcecatService.class),
                mock(SerialisationService.class),
                mock(BrandScoreService.class),
                mock(DjlTextEmbeddingService.class),
                new DjlEmbeddingProperties());
    }

    private VerticalConfig verticalConfig() {
        AttributeConfig first = new AttributeConfig();
        first.setKey("SCORE_A");
        first.setAsScore(true);
        first.setFilteringType(AttributeType.NUMERIC);
        first.setParticipateInScores(Set.of("AGG"));

        AttributeConfig second = new AttributeConfig();
        second.setKey("SCORE_B");
        second.setAsScore(true);
        second.setFilteringType(AttributeType.NUMERIC);
        second.setParticipateInScores(Set.of("AGG"));

        AttributesConfig attributesConfig = new AttributesConfig(List.of(first, second));
        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("SCORE_A", 0.6, "SCORE_B", 0.4));

        VerticalConfig config = new VerticalConfig();
        config.setId("vertical-test");
        config.setAttributesConfig(attributesConfig);
        config.setImpactScoreConfig(impactScoreConfig);
        config.setScoringAggregationConfig(new ScoringAggregationConfig());
        return config;
    }

    private Product productWithAttributes(Long id, String scoreAValue, String scoreBValue) {
        Product product = new Product(id);
        product.getAttributes().getIndexed().put("SCORE_A", new IndexedAttribute("SCORE_A", scoreAValue));
        product.getAttributes().getIndexed().put("SCORE_B", new IndexedAttribute("SCORE_B", scoreBValue));
        return product;
    }
}
