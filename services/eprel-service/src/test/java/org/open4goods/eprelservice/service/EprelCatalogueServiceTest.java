package org.open4goods.eprelservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.datareference.model.SourceRecordTransition;
import org.open4goods.datareference.model.SourceRecordTransitionOutcome;
import org.open4goods.datareference.port.SourceRecordHeadStore;
import org.open4goods.datareference.port.IngestionCheckpointStore;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.services.eprelservice.client.EprelApiClient;
import org.open4goods.services.eprelservice.client.EprelProductGroup;
import org.open4goods.services.eprelservice.config.EprelServiceProperties;
import org.open4goods.services.eprelservice.service.EprelCatalogueParser;
import org.open4goods.services.eprelservice.service.EprelCatalogueService;
import org.open4goods.services.eprelservice.service.EprelSourceRecordAdapter;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/** Tests for {@link EprelCatalogueService}. */
@ExtendWith(MockitoExtension.class)
class EprelCatalogueServiceTest {

    @Mock
    private EprelApiClient apiClient;
    @Mock
    private EprelCatalogueParser parser;
    @Mock
    private SourceRecordHeadStore sourceRecordStore;
    @Mock
    private IngestionCheckpointStore checkpointStore;
    @Mock
    private SourceRecordTransition transition;

    private EprelCatalogueService service;
    private Path tempZip;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() throws IOException {
        EprelServiceProperties properties = new EprelServiceProperties();
        properties.setIndexBulkSize(2);
        meterRegistry = new SimpleMeterRegistry();
        service = new EprelCatalogueService(apiClient, parser, sourceRecordStore, checkpointStore,
                new EprelSourceRecordAdapter(), properties, meterRegistry);
        tempZip = Files.createTempFile("eprel-service-test", ".zip");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempZip);
    }

    @Test
    void writesCatalogueRowsAsSourceRecordMutations() throws IOException {
        EprelProductGroup group = new EprelProductGroup("tv", "televisions", "Televisions", "REG");
        when(apiClient.fetchProductGroups()).thenReturn(java.util.List.of(group));
        when(apiClient.downloadCatalogueZip(group.urlCode())).thenReturn(tempZip);
        when(sourceRecordStore.apply(any())).thenReturn(transition);
        when(checkpointStore.find(any(), any())).thenReturn(java.util.Optional.empty());
        when(checkpointStore.compareAndSet(any(), any(Long.class))).thenReturn(true);
        when(transition.outcome()).thenReturn(SourceRecordTransitionOutcome.ACCEPTED);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<EprelProduct> consumer = invocation.getArgument(1, Consumer.class);
            consumer.accept(product("42"));
            EprelProduct mapped = product("43");
            mapped.setGtinIdentifier("4006381333931");
            consumer.accept(mapped);
            EprelProduct withdrawn = product("44");
            withdrawn.setStatus("withdrawn");
            consumer.accept(withdrawn);
            consumer.accept(new EprelProduct());
            return null;
        }).when(parser).parse(any(), any());

        service.refreshCatalogue();

        verify(sourceRecordStore, times(3)).apply(any());
        verify(checkpointStore).compareAndSet(any(), anyLong());
        assertThat(meterRegistry.get("o4g.eprel.catalogue.records").tag("outcome", "mapped").counter().count())
                .isEqualTo(1.0d);
        assertThat(meterRegistry.get("o4g.eprel.catalogue.records").tag("outcome", "unmapped").counter().count())
                .isEqualTo(2.0d);
        assertThat(meterRegistry.get("o4g.eprel.catalogue.records").tag("outcome", "rejected").counter().count())
                .isEqualTo(1.0d);
        assertThat(meterRegistry.get("o4g.eprel.catalogue.records").tag("outcome", "withdrawn").counter().count())
                .isEqualTo(1.0d);
        assertThat(meterRegistry.get("o4g.eprel.catalogue.records").tag("outcome", "changed").counter().count())
                .isEqualTo(3.0d);
    }

    @Test
    void doesNotCheckpointOrPublishAnIncompleteCatalogueBatch() throws IOException {
        EprelProductGroup group = new EprelProductGroup("tv", "televisions", "Televisions", "REG");
        when(apiClient.fetchProductGroups()).thenReturn(java.util.List.of(group));
        when(apiClient.downloadCatalogueZip(group.urlCode())).thenReturn(tempZip);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<EprelProduct> consumer = invocation.getArgument(1, Consumer.class);
            consumer.accept(product("42"));
            consumer.accept(product("43"));
            throw new IOException("incomplete catalogue");
        }).when(parser).parse(any(), any());

        assertThatIOException().isThrownBy(service::refreshCatalogue)
                .withMessageContaining("incomplete catalogue");

        verify(sourceRecordStore, never()).apply(any());
        verify(checkpointStore, never()).compareAndSet(any(), anyLong());
    }

    private EprelProduct product(String registration) {
        EprelProduct product = new EprelProduct();
        product.setEprelRegistrationNumber(registration);
        product.setModelIdentifier("MODEL-" + registration);
        return product;
    }
}
