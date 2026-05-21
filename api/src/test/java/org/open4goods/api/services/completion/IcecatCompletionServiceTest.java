package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Category;
import org.open4goods.icecat.model.IcecatLiveApiResponse.GeneralInfo;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Name;
import org.mockito.Mockito;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;

class IcecatCompletionServiceTest {

        private IcecatCompletionService service;
        private VerticalConfig vertical;
        private Product product;

        @BeforeEach
        void setUp() {
                service = Mockito.mock(IcecatCompletionService.class, Mockito.CALLS_REAL_METHODS);
                vertical = new VerticalConfig();
                product = new Product(1L);
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

                Method convert = IcecatCompletionService.class.getDeclaredMethod("convert", IceDataItem.class, Product.class);
                convert.setAccessible(true);

                assertThatCode(() -> convert.invoke(service, item, product)).doesNotThrowAnyException();
        }
}
