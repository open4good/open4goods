package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.AggregationPipelineProperties;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.api.services.aggregation.services.realtime.IdentityAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.PriceAggregationService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.open4goods.brand.service.BrandScoreService;
import org.open4goods.brand.service.BrandService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

class AggregationFacadeServicePipelineTest
{

    @Test
    void standardAggregatorUsesConfiguredPipelineOrder() throws Exception
    {
        AggregationPipelineProperties pipelineProperties = new AggregationPipelineProperties();
        AggregationPipelineProperties.Pipelines pipelines = pipelineProperties.getPipelines();
        pipelines.setRealtime(List.of("price", "identity"));

        AggregationFacadeService facade = aggregationFacadeService(pipelineProperties);

        StandardAggregator aggregator = facade.getStandardAggregator("realtime");
        List<AbstractAggregationService> services = getServices(aggregator);

        assertThat(services).hasSize(2);
        assertThat(services.get(0)).isInstanceOf(PriceAggregationService.class);
        assertThat(services.get(1)).isInstanceOf(IdentityAggregationService.class);
    }

    private AggregationFacadeService aggregationFacadeService(AggregationPipelineProperties pipelineProperties)
    {
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
                new DjlEmbeddingProperties(),
                pipelineProperties);
    }

    @SuppressWarnings("unchecked")
    private List<AbstractAggregationService> getServices(StandardAggregator aggregator) throws Exception
    {
        Field field = org.open4goods.api.services.aggregation.aggregator.AbstractAggregator.class
                .getDeclaredField("services");
        field.setAccessible(true);
        return (List<AbstractAggregationService>) field.get(aggregator);
    }
}
