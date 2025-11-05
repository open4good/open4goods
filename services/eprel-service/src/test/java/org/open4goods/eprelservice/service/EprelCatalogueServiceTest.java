package org.open4goods.eprelservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.open4goods.services.eprelservice.client.EprelApiClient;
import org.open4goods.services.eprelservice.client.EprelProductGroup;
import org.open4goods.services.eprelservice.config.EprelServiceProperties;
import org.open4goods.services.eprelservice.model.EprelProduct;
import org.open4goods.services.eprelservice.service.EprelCatalogueParser;
import org.open4goods.services.eprelservice.service.EprelCatalogueService;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

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
    @DisplayName("refreshCatalogue should download, parse and index products in bulk")
    void refreshCatalogueShouldIndexBatches() throws IOException
    {
        when(apiClient.fetchProductGroups()).thenReturn(List.of(new EprelProductGroup("CODE", "code", "Name", "Reg")));
        when(apiClient.downloadCatalogueZip("code")).thenReturn(tempZip);
        doAnswer(invocation ->
        {
            Consumer<EprelProduct> consumer = invocation.getArgument(1);
            consumer.accept(product("1"));
            consumer.accept(product("2"));
            consumer.accept(product("3"));
            return null;
        }).when(parser).parse(any(Path.class), any());
        when(elasticsearchOperations.save(any(Iterable.class))).thenReturn(List.of());

        service.refreshCatalogue();

        ArgumentCaptor<Iterable<EprelProduct>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(elasticsearchOperations, times(2)).save(captor.capture());
        List<Iterable<EprelProduct>> batches = captor.getAllValues();
        assertThat(batches.get(0)).hasSize(2);
        assertThat(batches.get(1)).hasSize(1);
        assertThat(Files.exists(tempZip)).isFalse();
    }

    private EprelProduct product(String registration)
    {
        EprelProduct product = new EprelProduct();
        product.setEprelRegistrationNumber(registration);
        product.setModelIdentifier("MODEL-" + registration);
        return product;
    }
}
