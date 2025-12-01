package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}

