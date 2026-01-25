package org.open4goods.api.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;

@ExtendWith(MockitoExtension.class)
class StandardReviewAggregatorHookTest {

    @Mock
    private AggregationFacadeService aggregationFacadeService;

    private StandardReviewAggregatorHook hook;

    @BeforeEach
    void setUp() {
        hook = new StandardReviewAggregatorHook(aggregationFacadeService);
    }

    @Test
    void onReviewGenerated_ShouldCallAggregate() throws AggregationSkipException {
        // GIVEN
        Product product = new Product();
        product.setId(123L);

        // WHEN
        hook.onReviewGenerated(product);

        // THEN
        verify(aggregationFacadeService, times(1)).aggregate(product);
    }

    @Test
    void onReviewGenerated_ShouldNotThrowException_WhenAggregationFails() throws AggregationSkipException {
        // GIVEN
        Product product = new Product();
        product.setId(123L);
        doThrow(new RuntimeException("Aggregation failed")).when(aggregationFacadeService).aggregate(product);

        // WHEN
        hook.onReviewGenerated(product);

        // THEN
        // verify no exception is thrown
        verify(aggregationFacadeService, times(1)).aggregate(product);
    }
}
