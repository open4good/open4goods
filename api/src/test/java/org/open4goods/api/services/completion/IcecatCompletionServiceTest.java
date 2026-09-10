package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.icecat.config.yml.IcecatCompletionConfig;
import org.open4goods.icecat.model.IcecatLiveApiResponse.BulletPoints;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Category;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeatureLogos;
import org.open4goods.icecat.model.IcecatLiveApiResponse.GeneralInfo;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Name;
import org.open4goods.icecat.model.IcecatLiveApiResponse.ProductFamily;
import org.open4goods.icecat.model.IcecatLiveApiResponse.ProductSeries;
import org.open4goods.icecat.model.IcecatLiveApiResponse.ReasonsToBuy;
import org.open4goods.icecat.model.IcecatLiveApiResponse.SummaryDescription;
import org.open4goods.icecat.model.IcecatLiveApiResponse.VariantIdentifier;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Variants;
import org.open4goods.icecat.services.IcecatLiveClient;
import org.open4goods.icecat.services.IcecatLiveLookupResult;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;

class IcecatCompletionServiceTest {

        private IcecatCompletionService service;
        private VerticalConfig vertical;
        private Product product;
        private IcecatCompletionConfig config;

        @BeforeEach
        void setUp() throws Exception {
                service = Mockito.mock(IcecatCompletionService.class, Mockito.CALLS_REAL_METHODS);
                vertical = new VerticalConfig();
                product = new Product(1L);

                config = new IcecatCompletionConfig();
                config.setPolitenessDelayMs(0);
                inject(IcecatCompletionService.class, service, "icecatConfig", config);
                inject(org.open4goods.commons.services.AbstractCompletionService.class, service, "logger",
                        org.slf4j.LoggerFactory.getLogger(IcecatCompletionServiceTest.class));
        }

        private static void inject(Class<?> declaringClass, Object target, String fieldName, Object value) throws Exception {
                Field field = declaringClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
        }

        private static void inject(Object target, String fieldName, Object value) throws Exception {
                inject(IcecatCompletionService.class, target, fieldName, value);
        }

        private static String missKey() {
                return "icecat.biz.miss";
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
        void convertToleratesMissingOptionalIcecatSections() throws Exception {
                IceDataItem item = new IceDataItem();
                item.generalInfo = new GeneralInfo();
                item.generalInfo.icecatId = 123;
                item.generalInfo.title = "Test title";
                item.generalInfo.productName = "Test product";
                item.generalInfo.brand = "Test brand";
                item.generalInfo.brandPartCode = "ABC-123";
                item.generalInfo.category = new Category();
                item.generalInfo.category.name = new Name();
                item.generalInfo.category.name.value = "Test category";

                Method convert = IcecatCompletionService.class.getDeclaredMethod("convert", IceDataItem.class, Product.class,
                        DomainLanguage.class);
                convert.setAccessible(true);

                assertThatCode(() -> convert.invoke(service, item, product, DomainLanguage.fr)).doesNotThrowAnyException();
        }

        @Test
        void convertMapsFamilySeriesLifecycleSummaryBulletsReasonsLogosAndVariants() throws Exception {
                IceDataItem item = new IceDataItem();
                item.generalInfo = new GeneralInfo();
                item.generalInfo.icecatId = 999;
                item.generalInfo.title = "Rich product";
                item.generalInfo.releaseDate = "2020-06-15";
                item.generalInfo.endOfLifeDate = "2024-06-15";

                item.generalInfo.productFamily = new ProductFamily();
                item.generalInfo.productFamily.value = "City";
                item.generalInfo.productFamily.language = "en";

                item.generalInfo.productSeries = new ProductSeries();
                item.generalInfo.productSeries.value = "City Great Vehicles";
                item.generalInfo.productSeries.language = "en";

                item.generalInfo.summaryDescription = new SummaryDescription();
                item.generalInfo.summaryDescription.longSummaryDescription = "A long summary.";

                item.generalInfo.bulletPoints = new BulletPoints();
                item.generalInfo.bulletPoints.values = java.util.List.of("Fast", "Reliable");

                FeatureLogos logo = new FeatureLogos();
                logo.logoPic = "https://icecat.example/logo.png";
                item.featureLogos = java.util.List.of(logo);

                ReasonsToBuy reason = new ReasonsToBuy();
                reason.value = "Great battery life";
                reason.language = "en";
                item.reasonsToBuy = java.util.List.of(reason);

                Variants variant = new Variants();
                VariantIdentifier identifier = new VariantIdentifier();
                identifier.identifierType = "GTIN13";
                identifier.value = "1234567890123";
                variant.variantIdentifiers = java.util.List.of(identifier);
                item.variants = java.util.List.of(variant);

                Method convert = IcecatCompletionService.class.getDeclaredMethod("convert", IceDataItem.class, Product.class,
                        DomainLanguage.class);
                convert.setAccessible(true);

                Object df = convert.invoke(service, item, product, DomainLanguage.en);

                org.open4goods.model.datafragment.DataFragment fragment = (org.open4goods.model.datafragment.DataFragment) df;

                assertThat(fragment.getAttribute("YEAR").getRawValue()).isEqualTo("2020");
                assertThat(fragment.getAttribute("END_OF_LIFE_DATE").getRawValue()).isEqualTo("2024-06-15");
                assertThat(fragment.getAttribute("PRODUCT_FAMILY").getRawValue()).isEqualTo("City");
                assertThat(fragment.getAttribute("PRODUCT_SERIES").getRawValue()).isEqualTo("City Great Vehicles");
                assertThat(fragment.getAttribute("REASONS_TO_BUY").getRawValue()).isEqualTo("Great battery life");
                assertThat(fragment.getAttribute("ICECAT_VARIANTS").getRawValue()).isEqualTo("GTIN13:1234567890123");
                assertThat(fragment.getDescriptionsByDatasource().get("icecat.biz"))
                        .contains("A long summary.")
                        .contains("- Fast")
                        .contains("- Reliable");
                assertThat(fragment.getResources()).anyMatch(r -> r.getUrl().equals("https://icecat.example/logo.png"));
        }

        @Test
        void processProductSkipsWhenProductIdIsNull() {
                Product noId = new Product();

                assertThatCode(() -> service.processProduct(vertical, noId)).doesNotThrowAnyException();
        }

        @Test
        void processProductSearchesByGtinWhenNoIcecatIdKnown() throws Exception {
                IcecatLiveClient liveClient = Mockito.mock(IcecatLiveClient.class);
                StandardAggregator aggregator = Mockito.mock(StandardAggregator.class);
                inject(service, "liveClient", liveClient);
                inject(service, "aggregator", aggregator);

                Mockito.when(liveClient.fetchProduct(anyLong(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.notFound());

                service.processProduct(vertical, product);

                Mockito.verify(liveClient).fetchProduct(1L, config.getDomainLanguage());
                Mockito.verify(liveClient, Mockito.never()).fetchProductByIcecatId(anyString(), any());
        }

        @Test
        void processProductRefreshesByIcecatIdWhenAlreadyMatched() throws Exception {
                product.getExternalIds().setIcecat("42");

                IcecatLiveClient liveClient = Mockito.mock(IcecatLiveClient.class);
                StandardAggregator aggregator = Mockito.mock(StandardAggregator.class);
                inject(service, "liveClient", liveClient);
                inject(service, "aggregator", aggregator);

                Mockito.when(liveClient.fetchProductByIcecatId(anyString(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.notFound());

                service.processProduct(vertical, product);

                Mockito.verify(liveClient).fetchProductByIcecatId("42", config.getDomainLanguage());
                Mockito.verify(liveClient, Mockito.never()).fetchProduct(anyLong(), any());
        }

        @Test
        void processProductSetsMissTimestampNotSuccessOnNotFound() throws Exception {
                IcecatLiveClient liveClient = Mockito.mock(IcecatLiveClient.class);
                StandardAggregator aggregator = Mockito.mock(StandardAggregator.class);
                inject(service, "liveClient", liveClient);
                inject(service, "aggregator", aggregator);

                Mockito.when(liveClient.fetchProduct(anyLong(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.notFound());

                service.processProduct(vertical, product);

                assertThat(product.getDatasourceCodes()).containsKey(missKey());
                assertThat(product.getDatasourceCodes()).doesNotContainKey(service.getDatasourceName());
                Mockito.verifyNoInteractions(aggregator);
        }

        @Test
        void processProductDoesNotSetSuccessTimestampWhenAggregationSkipped() throws Exception {
                IcecatLiveClient liveClient = Mockito.mock(IcecatLiveClient.class);
                StandardAggregator aggregator = Mockito.mock(StandardAggregator.class);
                inject(service, "liveClient", liveClient);
                inject(service, "aggregator", aggregator);

                IceDataItem item = new IceDataItem();
                item.generalInfo = new GeneralInfo();
                item.generalInfo.icecatId = 7;

                Mockito.when(liveClient.fetchProduct(anyLong(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.found(item));
                Mockito.when(aggregator.onDatafragment(any(), any())).thenThrow(new AggregationSkipException("skip"));

                service.processProduct(vertical, product);

                assertThat(product.getDatasourceCodes()).doesNotContainKey(service.getDatasourceName());
        }

        @Test
        void processProductSetsSuccessTimestampWhenAggregationSucceeds() throws Exception {
                IcecatLiveClient liveClient = Mockito.mock(IcecatLiveClient.class);
                StandardAggregator aggregator = Mockito.mock(StandardAggregator.class);
                inject(service, "liveClient", liveClient);
                inject(service, "aggregator", aggregator);

                IceDataItem item = new IceDataItem();
                item.generalInfo = new GeneralInfo();
                item.generalInfo.icecatId = 7;

                Mockito.when(liveClient.fetchProduct(anyLong(), any(DomainLanguage.class)))
                        .thenReturn(IcecatLiveLookupResult.found(item));
                Mockito.when(aggregator.onDatafragment(any(), any())).thenReturn(product);

                service.processProduct(vertical, product);

                assertThat(product.getDatasourceCodes()).containsKey(service.getDatasourceName());
                assertThat(product.getDatasourceCodes()).doesNotContainKey(missKey());
                assertThat(product.getExternalIds().getIcecat()).isEqualTo("7");
        }
}
