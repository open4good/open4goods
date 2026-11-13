package org.open4goods.icecat.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchRepositoryFactory;

import java.util.List;

/**
 * GOU-2138: attribute names with control characters, quotes or backslashes must reach
 * Elasticsearch as a structured query value, never spliced into a JSON string template.
 */
class IcecatFeatureRepositoryQueryTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "DUREE DE RECHARGEMENT DE LA BATTERIE \t(90 %)",
            "ECRAN 6\" \\ \"DIAGONALE\""
    })
    void normalizedNameIsPassedAsStructuredCriteria(String name) {
        SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();
        MappingElasticsearchConverter converter = new MappingElasticsearchConverter(context);
        converter.afterPropertiesSet();

        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        when(operations.getElasticsearchConverter()).thenReturn(converter);
        when(operations.getIndexCoordinatesFor(IcecatFeatureDocument.class))
                .thenReturn(org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.of("icecat-features"));
        SearchHits<IcecatFeatureDocument> empty = mock(SearchHits.class);
        when(empty.getSearchHits()).thenReturn(List.of());
        when(operations.search(any(Query.class), any(Class.class), any(org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.class)))
                .thenReturn(empty);

        IcecatFeatureRepository repository = new ElasticsearchRepositoryFactory(operations)
                .getRepository(IcecatFeatureRepository.class);

        assertThat(repository.findByNormalizedNames(name)).isEmpty();

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(operations).search(captor.capture(), any(Class.class),
                any(org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.class));
        assertThat(captor.getValue()).isInstanceOf(CriteriaQuery.class);
        Criteria criteria = ((CriteriaQuery) captor.getValue()).getCriteria();
        assertThat(criteria.getField().getName()).isEqualTo("normalizedNames");
        assertThat(criteria.getQueryCriteriaEntries()).hasSize(1);
        assertThat(criteria.getQueryCriteriaEntries().iterator().next().getValue()).isEqualTo(name);
    }
}
