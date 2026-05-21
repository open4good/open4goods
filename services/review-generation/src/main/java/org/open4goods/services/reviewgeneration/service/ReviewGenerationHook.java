package org.open4goods.services.reviewgeneration.service;

import org.open4goods.model.product.Product;

/**
 * Interface for hooks executed at review pipeline stages.
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

    /**
     * Called after the remote-source fetching stage completes successfully and the product has been
     * re-indexed with the persisted {@link org.open4goods.model.product.ProductFact} review facts.
     * <p>
     * Implementations can use this to trigger additional enrichments (e.g. EPREL, Icecat) on products
     * for which scraping succeeded but upstream data sources were missing.
     * </p>
     * <p>
     * The default no-op implementation allows existing hooks to ignore this event.
     * </p>
     *
     * @param product The product with newly persisted review facts.
     */
    default void onSourcesFetched(Product product) {
    }

    /**
     * Called after the remote-source fetching stage completes successfully, with
     * explicit state captured before fetching mutated the product.
     *
     * @param product the product with newly persisted review facts
     * @param hadEprelBeforeFetch whether the product already had EPREL data before fetching
     */
    default void onSourcesFetched(Product product, boolean hadEprelBeforeFetch) {
        onSourcesFetched(product);
    }
}
