package org.open4goods.services.reviewgeneration.service;

import org.open4goods.model.product.Product;

/**
 * Interface for hooks to be executed after a review has been generated.
 */
public interface ReviewGenerationHook {

    /**
     * Called after an AI review has been successfully generated and applied to the product object.
     * Note: The product might not yet be persisted to the database with the new review when this is called,
     * or it might be in the process of being saved.
     * Use this to trigger further enrichments or aggregations on the product.
     *
     * @param product The product with the newly generated review.
     */
    void onReviewGenerated(Product product);
}
