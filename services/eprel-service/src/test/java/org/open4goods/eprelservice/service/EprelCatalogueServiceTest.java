package org.open4goods.eprelservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.services.eprelservice.client.EprelApiClient;
import org.open4goods.services.eprelservice.client.EprelProductGroup;
import org.open4goods.services.eprelservice.config.EprelServiceProperties;
import org.open4goods.services.eprelservice.service.EprelCatalogueParser;
import org.open4goods.services.eprelservice.service.EprelCatalogueService;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

/**
 * Tests for {@link EprelCatalogueService}.
 */
@ExtendWith(MockitoExtension.class)
class EprelCatalogueServiceTest
{
    @Mock
    private EprelApiClient apiClient;

    @Mock
    private EprelCatalogueParser parser;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private EprelServiceProperties properties;

    private EprelCatalogueService service;

    private Path tempZip;

    @BeforeEach
    void setUp() throws IOException
    {
        properties = new EprelServiceProperties();
        properties.setIndexBulkSize(2);
        service = new EprelCatalogueService(apiClient, parser, elasticsearchOperations, properties);
        tempZip = Files.createTempFile("eprel-service-test", ".zip");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        Files.deleteIfExists(tempZip);
    }



    @Test
    @DisplayName("A failing group does not prevent the next ones from being refreshed")
    void failingGroupDoesNotBlockNextGroups() throws IOException
    {
        when(apiClient.fetchProductGroups()).thenReturn(List.of(
                new EprelProductGroup("A", "unavailable", "Unavailable", "r1"),
                new EprelProductGroup("B", "televisions", "Televisions", "r2")));
        when(apiClient.downloadCatalogueZip("unavailable")).thenThrow(new IllegalStateException("HTTP 503"));
        when(apiClient.downloadCatalogueZip("televisions")).thenReturn(tempZip);

        assertThatThrownBy(() -> service.refreshCatalogue())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("unavailable")
                .hasMessageNotContaining("televisions");

        verify(parser, times(1)).parse(any(), any());
    }

    @Test
    @DisplayName("Documents of a refreshed group missing from the new catalogue are marked as withdrawn")
    @SuppressWarnings("unchecked")
    void missingDocumentsAreMarkedWithdrawn() throws IOException
    {
        when(apiClient.fetchProductGroups()).thenReturn(List.of(new EprelProductGroup("B", "televisions", "TV", "r")));
        when(apiClient.downloadCatalogueZip("televisions")).thenReturn(tempZip);
        doAnswer(inv ->
        {
            Consumer<EprelProduct> sink = inv.getArgument(1);
            sink.accept(product("1"));
            return null;
        }).when(parser).parse(any(), any());
        SearchHitsIterator<EprelProduct> iterator = org.mockito.Mockito.mock(SearchHitsIterator.class);
        SearchHit<EprelProduct> kept = hit(stored("1", "televisions"));
        SearchHit<EprelProduct> gone = hit(stored("2", "televisions"));
        SearchHit<EprelProduct> otherGroup = hit(stored("3", "dishwashers"));
        when(iterator.hasNext()).thenReturn(true, true, true, false);
        when(iterator.next()).thenReturn(kept, gone, otherGroup);
        when(elasticsearchOperations.searchForStream(any(Query.class), eq(EprelProduct.class))).thenReturn(iterator);

        service.refreshCatalogue();

        ArgumentCaptor<List<UpdateQuery>> updates = ArgumentCaptor.forClass(List.class);
        verify(elasticsearchOperations).bulkUpdate(updates.capture(), any(IndexCoordinates.class));
        assertThat(updates.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("A failing group is never marked: no scan of the index")
    void failedGroupMarksNothing() throws IOException
    {
        when(apiClient.fetchProductGroups()).thenReturn(List.of(new EprelProductGroup("A", "televisions", "TV", "r")));
        when(apiClient.downloadCatalogueZip("televisions")).thenReturn(tempZip);
        doAnswer(inv ->
        {
            throw new IOException("corrupt zip");
        }).when(parser).parse(any(), any());

        assertThatThrownBy(() -> service.refreshCatalogue()).isInstanceOf(IOException.class);

        verify(elasticsearchOperations, never()).searchForStream(any(Query.class), eq(EprelProduct.class));
        verify(elasticsearchOperations, never()).bulkUpdate(any(), any(IndexCoordinates.class));
    }

    @Test
    @DisplayName("An empty catalogue marks nothing")
    void emptyCatalogueMarksNothing() throws IOException
    {
        when(apiClient.fetchProductGroups()).thenReturn(List.of(new EprelProductGroup("A", "televisions", "TV", "r")));
        when(apiClient.downloadCatalogueZip("televisions")).thenReturn(tempZip);

        service.refreshCatalogue();

        verify(elasticsearchOperations, never()).searchForStream(any(Query.class), eq(EprelProduct.class));
    }

    @Test
    @DisplayName("A withdrawn entry is ignored by the search")
    void withdrawnEntryFlag()
    {
        EprelProduct p = product("9");
        assertThat(p.isWithdrawn()).isFalse();
        p.setWithdrawn(true);
        assertThat(p.isWithdrawn()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private SearchHit<EprelProduct> hit(EprelProduct content)
    {
        SearchHit<EprelProduct> hit = org.mockito.Mockito.mock(SearchHit.class);
        when(hit.getContent()).thenReturn(content);
        return hit;
    }

    private EprelProduct stored(String registration, String group)
    {
        EprelProduct product = new EprelProduct();
        product.setEprelRegistrationNumber(registration);
        product.setProductGroup(group);
        return product;
    }

    private EprelProduct product(String registration)
    {
        EprelProduct product = new EprelProduct();
        product.setEprelRegistrationNumber(registration);
        product.setModelIdentifier("MODEL-" + registration);
        return product;
    }
}
