package org.open4goods.eprelservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.services.eprelservice.config.EprelServiceProperties;
import org.open4goods.services.eprelservice.service.EprelSearchService;
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
    private EprelServiceProperties properties;
    private SearchHits<EprelProduct> emptyHits;

    @BeforeEach
    void setUp()
    {
        properties = new EprelServiceProperties();
        service = new EprelSearchService(elasticsearchOperations, properties);
        emptyHits = org.mockito.Mockito.mock(SearchHits.class);
        org.mockito.Mockito.lenient().when(emptyHits.stream()).thenAnswer(invocation -> Stream.empty());
        org.mockito.Mockito.lenient().when(elasticsearchOperations.search(any(NativeQuery.class), any(Class.class))).thenReturn(emptyHits);
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
    @DisplayName("searchByGtin should filter on any provided EPREL categories")
    void searchByGtinShouldFilterByMultipleCategories()
    {
        service.searchByGtin("123456", java.util.List.of("TV", "MONITOR"));

        Query query = capturedQuery();
        assertThat(query.isBool()).isTrue();
        assertThat(query.bool().must()).hasSize(1);
        assertThat(query.bool().filter()).hasSize(1);

        Query categoryFilter = query.bool().filter().getFirst();
        assertThat(categoryFilter.isTerms()).isTrue();
        assertThat(categoryFilter.terms().field()).isEqualTo("productGroup");
        assertThat(categoryFilter.terms().terms().value())
            .extracting(FieldValue::stringValue)
            .containsExactly("TV", "MONITOR");
    }

    @Test
    @DisplayName("searchByExactModel should use a case insensitive term query")
    void searchByExactModelShouldUseTermQuery()
    {
        service.searchByExactModel("MODEL-123");
        Query query = capturedQuery();
        TermQuery termQuery = query.term();
        assertThat(termQuery.field()).isEqualTo("modelIdentifier");
        assertThat(termQuery.value().stringValue()).isEqualTo("model-123");
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

    @Test
    @DisplayName("searchByModel should fall back to keyword prefix bool query then to contains when needed")
    void searchByModelShouldUseBestMatchFallback()
    {
        service.searchByModel("MODEL12345XYZ");

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        // exact(0), prefix(1), bestMatch(2), contains(3)
        org.mockito.Mockito.verify(elasticsearchOperations, org.mockito.Mockito.times(4)).search(captor.capture(), any(Class.class));

        Query boolQuery = captor.getAllValues().get(2).getQuery();
        assertThat(boolQuery.isBool()).isTrue();
        assertThat(boolQuery.bool().should()).isNotEmpty();
        assertThat(boolQuery.bool().should().getFirst().term().field()).isEqualTo("modelIdentifier");
        assertThat(boolQuery.bool().should().getFirst().term().value().stringValue()).isEqualTo("model12345xyz");
        assertThat(boolQuery.bool().minimumShouldMatch()).isEqualTo("1");

        Query containsQuery = captor.getAllValues().get(3).getQuery();
        assertThat(containsQuery.isWildcard()).isTrue();
        WildcardQuery wildcardQuery = containsQuery.wildcard();
        assertThat(wildcardQuery.field()).isEqualTo("modelIdentifier");
        assertThat(wildcardQuery.value()).isEqualTo("*model12345xyz*");
    }

    @Test
    @DisplayName("search with model list should skip candidates exceeding space limit")
    void searchShouldSkipModelsWithTooManySpaces()
    {
        properties.setExcludeIfSpaces(0);
        service.search("INVALID", java.util.List.of("ACCEPTED", "TOO MANY SPACES"));

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        // GTIN "INVALID" is non-numeric so skips ES; for "ACCEPTED": exact(0), prefix(1), bestMatch(2), contains(3)
        org.mockito.Mockito.verify(elasticsearchOperations, org.mockito.Mockito.times(4)).search(captor.capture(), any(Class.class));

        java.util.List<Query> queries = captor.getAllValues().stream().map(NativeQuery::getQuery).toList();
        assertThat(queries).hasSize(4);
        TermQuery termQuery = queries.getFirst().term();
        assertThat(termQuery.value().stringValue()).isEqualTo("accepted");
    }

    @Test
    @DisplayName("search should filter out purely-numeric and degenerate model candidates")
    void searchShouldSkipDegenerateModels()
    {
        // "33703828" is all-numeric, "1" has only 1 alphanumeric char (< minAlnumLength=3)
        // "N3B3HTX" is the only valid candidate
        service.search("INVALID", java.util.List.of("33703828", "1", "N3B3HTX"));

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        // Only "N3B3HTX" survives filtering: exact(0), prefix(1), bestMatch(2), contains(3)
        org.mockito.Mockito.verify(elasticsearchOperations, org.mockito.Mockito.times(4)).search(captor.capture(), any(Class.class));

        java.util.List<Query> queries = captor.getAllValues().stream().map(NativeQuery::getQuery).toList();
        TermQuery firstQuery = queries.getFirst().term();
        assertThat(firstQuery.value().stringValue()).isEqualTo("n3b3htx");
    }

    @Test
    @DisplayName("filterByBrand should narrow results to matching supplier")
    void filterByBrandShouldMatchSupplier()
    {
        EprelProduct candy = new EprelProduct();
        candy.setSupplierOrTrademark("Candy");

        EprelProduct other = new EprelProduct();
        other.setSupplierOrTrademark("Bosch");

        java.util.List<EprelProduct> filtered = service.filterByBrand(java.util.List.of(candy, other), "CANDY");
        assertThat(filtered).containsExactly(candy);
    }

    @Test
    @DisplayName("filterByBrand should return original list when no supplier matches")
    void filterByBrandShouldReturnAllWhenNoMatch()
    {
        EprelProduct p1 = new EprelProduct();
        p1.setSupplierOrTrademark("Bosch");

        java.util.List<EprelProduct> all = java.util.List.of(p1);
        java.util.List<EprelProduct> filtered = service.filterByBrand(all, "UNKNOWN");
        assertThat(filtered).isEqualTo(all);
    }

    private Query capturedQuery()
    {
        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        org.mockito.Mockito.verify(elasticsearchOperations).search(captor.capture(), any(Class.class));
        return captor.getValue().getQuery();
    }
}
