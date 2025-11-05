package org.open4goods.eprelservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.eprelservice.model.EprelProduct;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

/**
 * Tests for {@link EprelSearchService}.
 */
@ExtendWith(MockitoExtension.class)
class EprelSearchServiceTest
{
    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private EprelSearchService service;
    private SearchHits<EprelProduct> emptyHits;

    @BeforeEach
    void setUp()
    {
        service = new EprelSearchService(elasticsearchOperations);
        emptyHits = org.mockito.Mockito.mock(SearchHits.class);
        when(emptyHits.stream()).thenReturn(Stream.empty());
        when(elasticsearchOperations.search(any(NativeQuery.class), any(Class.class))).thenReturn(emptyHits);
    }

    @Test
    @DisplayName("searchByGtin should build a term query on numericGtin")
    void searchByGtinShouldUseNumericField()
    {
        service.searchByGtin("123456");
        Query query = capturedQuery();
        TermQuery termQuery = query.term();
        assertThat(termQuery.field()).isEqualTo("numericGtin");
        assertThat(termQuery.value().longValue()).isEqualTo(123456L);
    }

    @Test
    @DisplayName("searchByExactModel should use a case insensitive match query")
    void searchByExactModelShouldUseMatchQuery()
    {
        service.searchByExactModel("MODEL-123");
        Query query = capturedQuery();
        MatchQuery matchQuery = query.match();
        assertThat(matchQuery.field()).isEqualTo("modelIdentifier");
        assertThat(matchQuery.query().stringValue()).isEqualTo("model-123");
    }

    @Test
    @DisplayName("searchByModelPrefix should use a case insensitive prefix query")
    void searchByModelPrefixShouldUsePrefixQuery()
    {
        service.searchByModelPrefix("MODEL");
        Query query = capturedQuery();
        PrefixQuery prefixQuery = query.prefix();
        assertThat(prefixQuery.field()).isEqualTo("modelIdentifier");
        assertThat(prefixQuery.value()).isEqualTo("model");
    }

    private Query capturedQuery()
    {
        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        org.mockito.Mockito.verify(elasticsearchOperations).search(captor.capture(), any(Class.class));
        return captor.getValue().getQuery();
    }
}
