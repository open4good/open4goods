package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceRecordTransition;
import org.open4goods.datareference.model.SourceRecordTransitionOutcome;
import org.open4goods.datareference.port.IngestionCheckpointStore;
import org.open4goods.datareference.port.SourceRecordHeadStore;
import org.open4goods.icecat.config.yml.IcecatCompletionConfig;
import org.open4goods.icecat.model.IcecatLiveApiResponse.GeneralInfo;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.icecat.services.IcecatLiveClient;
import org.open4goods.icecat.services.IcecatLiveLookupResult;
import org.open4goods.icecat.services.IcecatSourceRecordAdapter;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;

/** Tests for {@link IcecatCompletionService}: this service only ever writes source-record heads. */
class IcecatCompletionServiceTest {

        private static final Instant RETRIEVED = Instant.parse("2026-09-30T12:00:00Z");

        private IcecatCompletionService service;
        private VerticalConfig vertical;
        private Product product;
        private IcecatCompletionConfig config;
        private IcecatLiveClient liveClient;
        private IcecatSourceRecordAdapter adapter;
        private SourceRecordHeadStore sourceRecordStore;
        private IngestionCheckpointStore checkpointStore;

        @BeforeEach
        void setUp() throws Exception {
                service = Mockito.mock(IcecatCompletionService.class, Mockito.CALLS_REAL_METHODS);
                vertical = new VerticalConfig();
                product = new Product(1L);

                config = new IcecatCompletionConfig();
                config.setPolitenessDelayMs(0);
                liveClient = Mockito.mock(IcecatLiveClient.class);
                adapter = Mockito.mock(IcecatSourceRecordAdapter.class);
                sourceRecordStore = Mockito.mock(SourceRecordHeadStore.class);
                checkpointStore = Mockito.mock(IngestionCheckpointStore.class);

                inject(IcecatCompletionService.class, service, "icecatConfig", config);
                inject(IcecatCompletionService.class, service, "liveClient", liveClient);
                inject(IcecatCompletionService.class, service, "adapter", adapter);
                inject(IcecatCompletionService.class, service, "sourceRecordStore", sourceRecordStore);
                inject(IcecatCompletionService.class, service, "checkpointStore", checkpointStore);
                inject(org.open4goods.commons.services.AbstractCompletionService.class, service, "logger",
                        org.slf4j.LoggerFactory.getLogger(IcecatCompletionServiceTest.class));

                Mockito.when(checkpointStore.find(anyString(), any())).thenReturn(Optional.empty());
        }

        private static void inject(Class<?> declaringClass, Object target, String fieldName, Object value) throws Exception {
                Field field = declaringClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
        }

        private static String missKey() {
                return "icecat.biz.miss";
        }

        private static SourceRecordKey key(int icecatId) {
                return IcecatSourceRecordAdapter.keyFor(icecatId);
        }

        private static SourceRecordHead head(SourceRecordKey key) {
                return new SourceRecordHead(key, "icecat-live-v1", null, RETRIEVED, RETRIEVED, null,
                        SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE,
                        new PayloadHash("SHA-256", "0".repeat(64)),
                        java.net.URI.create("urn:o4g:icecat:record:test"), IcecatSourceRecordAdapter.USAGE_POLICY,
                        List.of(), List.of());
        }

        private static SourceRecordTransition transition(SourceRecordKey key, SourceRecordTransitionOutcome outcome) {
                PayloadHash hash = new PayloadHash("SHA-256", "0".repeat(64));
                return new SourceRecordTransition(SourceRecordTransition.idFor(key, 1), key, 1, "icecat-live-v1", null, null,
                        hash, RETRIEVED, RETRIEVED, SourceRecordState.ACTIVE, outcome, null, List.of());
        }

        @Test
        void shouldSkipProcessingWhenLastProcessedIsFresh() {
                long freshTimestamp = System.currentTimeMillis() - Duration.ofDays(30).toMillis() + 1_000L;
                product.getDatasourceCodes().put(service.getDatasourceName(), freshTimestamp);

                assertThat(service.shouldProcess(vertical, product)).isFalse();
        }

        @Test
        void shouldProcessWhenLastProcessedIsStale() {
                long staleTimestamp = System.currentTimeMillis() - Duration.ofDays(30).toMillis() - 1_000L;
                product.getDatasourceCodes().put(service.getDatasourceName(), staleTimestamp);

                assertThat(service.shouldProcess(vertical, product)).isTrue();
        }

        @Test
        void shouldSkipProcessingWhenRecentMiss() {
                long freshMiss = System.currentTimeMillis() - Duration.ofDays(30).toMillis() + 1_000L;
                product.getDatasourceCodes().put(missKey(), freshMiss);

                assertThat(service.shouldProcess(vertical, product)).isFalse();
        }

        @Test
        void shouldProcessWhenMissIsStale() {
                long staleMiss = System.currentTimeMillis() - Duration.ofDays(30).toMillis() - 1_000L;
                product.getDatasourceCodes().put(missKey(), staleMiss);

                assertThat(service.shouldProcess(vertical, product)).isTrue();
        }

        @Test
        void processProductSkipsWhenProductIdIsNull() {
                Product noId = new Product();

                assertThatCode(() -> service.processProduct(vertical, noId)).doesNotThrowAnyException();
        }

        @Test
        void processProductSearchesByGtinWhenNoIcecatIdKnown() {
                Mockito.when(liveClient.fetchProduct(anyLong(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.notFound());

                service.processProduct(vertical, product);

                Mockito.verify(liveClient).fetchProduct(1L, config.getDomainLanguage());
                Mockito.verify(liveClient, Mockito.never()).fetchProductByIcecatId(anyString(), any());
                Mockito.verifyNoInteractions(sourceRecordStore);
        }

        @Test
        void processProductRefreshesByIcecatIdWhenAlreadyMatched() {
                product.getExternalIds().setIcecat("42");
                SourceRecordKey key = key(42);
                SourceRecordMutation tombstone = SourceRecordMutation.full(head(key));
                Mockito.when(liveClient.fetchProductByIcecatId(anyString(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.notFound());
                Mockito.when(adapter.tombstone(eq(key), anyString(), any())).thenReturn(tombstone);
                Mockito.when(sourceRecordStore.apply(tombstone)).thenReturn(transition(key, SourceRecordTransitionOutcome.TOMBSTONED));

                service.processProduct(vertical, product);

                Mockito.verify(liveClient).fetchProductByIcecatId("42", config.getDomainLanguage());
                Mockito.verify(liveClient, Mockito.never()).fetchProduct(anyLong(), any());
                Mockito.verify(adapter).tombstone(eq(key), anyString(), any());
                Mockito.verify(sourceRecordStore).apply(tombstone);
        }

        @Test
        void processProductNeverFabricatesAKeyForAnUnmatchedGtinSearchOnNotFound() {
                Mockito.when(liveClient.fetchProduct(anyLong(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.notFound());

                service.processProduct(vertical, product);

                assertThat(product.getDatasourceCodes()).containsKey(missKey());
                assertThat(product.getDatasourceCodes()).doesNotContainKey(service.getDatasourceName());
                Mockito.verifyNoInteractions(adapter, sourceRecordStore);
        }

        @Test
        void processProductRejectsKnownRecordOnRestrictedWithoutDeletingIt() {
                product.getExternalIds().setIcecat("7");
                SourceRecordKey key = key(7);
                SourceRecordMutation rejected = new SourceRecordMutation(rejectedHead(key), List.of(), List.of(), "RESTRICTED");
                Mockito.when(liveClient.fetchProductByIcecatId(anyString(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.restricted());
                Mockito.when(adapter.restricted(eq(key), anyString(), any(), eq("RESTRICTED"))).thenReturn(rejected);
                Mockito.when(sourceRecordStore.apply(rejected)).thenReturn(transition(key, SourceRecordTransitionOutcome.REJECTED));

                service.processProduct(vertical, product);

                Mockito.verify(adapter).restricted(eq(key), anyString(), any(), eq("RESTRICTED"));
                Mockito.verify(sourceRecordStore).apply(rejected);
                assertThat(product.getDatasourceCodes()).containsKey(missKey());
                assertThat(product.getDatasourceCodes()).doesNotContainKey(service.getDatasourceName());
        }

        @Test
        void processProductMarksUnavailableOnTransientError() {
                product.getExternalIds().setIcecat("7");
                SourceRecordKey key = key(7);
                SourceRecordMutation unavailable = new SourceRecordMutation(unavailableHead(key), List.of(), List.of(), "ERROR");
                Mockito.when(liveClient.fetchProductByIcecatId(anyString(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.error("boom"));
                Mockito.when(adapter.unavailable(eq(key), anyString(), any(), eq("ERROR"))).thenReturn(unavailable);
                Mockito.when(sourceRecordStore.apply(unavailable)).thenReturn(transition(key, SourceRecordTransitionOutcome.REJECTED));

                service.processProduct(vertical, product);

                Mockito.verify(adapter).unavailable(eq(key), anyString(), any(), eq("ERROR"));
                Mockito.verify(sourceRecordStore).apply(unavailable);
        }

        @Test
        void processProductDoesNotSetSuccessTimestampWhenMutationIsRejected() {
                IceDataItem item = itemWithId(7);
                SourceRecordMutation mutation = SourceRecordMutation.full(head(key(7)));
                Mockito.when(liveClient.fetchProduct(anyLong(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.found(item));
                Mockito.when(adapter.adapt(eq(item), any(), any(), anyString(), any())).thenReturn(Optional.of(mutation));
                Mockito.when(sourceRecordStore.apply(mutation)).thenReturn(transition(key(7), SourceRecordTransitionOutcome.REJECTED));

                service.processProduct(vertical, product);

                assertThat(product.getDatasourceCodes()).doesNotContainKey(service.getDatasourceName());
                assertThat(product.getDatasourceCodes()).containsKey(missKey());
        }

        @Test
        void processProductSetsSuccessTimestampAndChecksInProgressWhenHeadIsAccepted() {
                IceDataItem item = itemWithId(7);
                SourceRecordMutation mutation = SourceRecordMutation.full(head(key(7)));
                Mockito.when(liveClient.fetchProduct(anyLong(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.found(item));
                Mockito.when(adapter.adapt(eq(item), any(), any(), anyString(), any())).thenReturn(Optional.of(mutation));
                Mockito.when(sourceRecordStore.apply(mutation)).thenReturn(transition(key(7), SourceRecordTransitionOutcome.ACCEPTED));

                service.processProduct(vertical, product);

                assertThat(product.getDatasourceCodes()).containsKey(service.getDatasourceName());
                assertThat(product.getDatasourceCodes()).doesNotContainKey(missKey());
                assertThat(product.getExternalIds().getIcecat()).isEqualTo("7");
                Mockito.verify(checkpointStore).compareAndSet(any(), Mockito.eq(0L));
        }

        private static IceDataItem itemWithId(int icecatId) {
                IceDataItem item = new IceDataItem();
                item.generalInfo = new GeneralInfo();
                item.generalInfo.icecatId = icecatId;
                return item;
        }

        private static SourceRecordHead rejectedHead(SourceRecordKey key) {
                return new SourceRecordHead(key, "icecat-live-v1", null, RETRIEVED, RETRIEVED, null,
                        SourceRecordCompleteness.FULL, SourceRecordState.REJECTED,
                        new PayloadHash("SHA-256", "1".repeat(64)),
                        java.net.URI.create("urn:o4g:icecat:record:test"), IcecatSourceRecordAdapter.USAGE_POLICY,
                        List.of(), List.of());
        }

        private static SourceRecordHead unavailableHead(SourceRecordKey key) {
                return new SourceRecordHead(key, "icecat-live-v1", null, RETRIEVED, RETRIEVED, null,
                        SourceRecordCompleteness.FULL, SourceRecordState.UNAVAILABLE,
                        new PayloadHash("SHA-256", "2".repeat(64)),
                        java.net.URI.create("urn:o4g:icecat:record:test"), IcecatSourceRecordAdapter.USAGE_POLICY,
                        List.of(), List.of());
        }
}
