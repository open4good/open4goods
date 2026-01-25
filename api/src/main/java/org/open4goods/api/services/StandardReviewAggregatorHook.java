package org.open4goods.api.services;

import org.open4goods.model.product.Product;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationHook;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hook implementation that triggers standard standard aggregations (sanitization, enrichment)
 * after a review has been generated.
 */
@Component
public class StandardReviewAggregatorHook implements ReviewGenerationHook {

    private static final Logger logger = LoggerFactory.getLogger(StandardReviewAggregatorHook.class);

    private final AggregationFacadeService aggregationFacadeService;

    public StandardReviewAggregatorHook(AggregationFacadeService aggregationFacadeService) {
        this.aggregationFacadeService = aggregationFacadeService;
    }

    @Override
    public void onReviewGenerated(Product product) {
        try {
            logger.info("Triggering standard aggregation for product {} after review generation", product.getId());
            // trigger the standard aggregation (sanitization/enrichment)
            aggregationFacadeService.aggregate(product);
        } catch (Exception e) {
            logger.error("Failed to run standard aggregation hook for product {}: {}", product.getId(), e.getMessage(), e);
            // We consciously do not rethrow to avoid failing the review generation just because aggregation failed.
            // However, the caller log this too.
        }
    }
}
