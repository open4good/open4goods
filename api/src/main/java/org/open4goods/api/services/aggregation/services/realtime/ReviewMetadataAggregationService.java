package org.open4goods.api.services.aggregation.services.realtime;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductReviewMetadata;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Re-derives the indexed {@link ProductReviewMetadata} from the product's unindexed review payload.
 *
 * <p>The full {@link Product#getReviews()} payload is large and presentation-oriented, so it is not
 * indexed. This step projects it into the compact, queryable metadata used for review counts and
 * batch selection. It is a pure, idempotent enrichment and therefore belongs to the aggregation
 * pipeline rather than the persistence layer.</p>
 *
 * <p>Reviews only change during a full aggregation pass or when a review is generated, so the
 * derivation is kept here (covering every aggregation run) and is re-applied explicitly by the
 * review generation service right after it mutates the reviews.</p>
 */
public class ReviewMetadataAggregationService extends AbstractAggregationService {

	/**
	 * @param logger dedicated aggregation logger
	 */
	public ReviewMetadataAggregationService(final Logger logger) {
		super(logger);
	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {
		data.rebuildReviewMetadata();
	}

}
