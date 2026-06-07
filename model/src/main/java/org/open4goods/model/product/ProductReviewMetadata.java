package org.open4goods.model.product;

import java.util.HashMap;
import java.util.Map;

import org.open4goods.model.Localisable;

/**
 * Indexed, locale-aware metadata for AI reviews.
 *
 * <p>The full {@link Product#getReviews()} payload remains unindexed because it is large and
 * presentation-oriented. This compact metadata object provides queryable review state for counts
 * and batch selection.</p>
 */
public class ProductReviewMetadata {

    private Map<String, ProductReviewLocaleMetadata> locales = new HashMap<>();

    /**
     * Derives the indexed review metadata from the unindexed review payload.
     *
     * <p>Pure projection of {@link Product#getReviews()}: it carries no side effect and is the
     * single source of truth used both by the aggregation pipeline and by the review generation
     * service so that the indexed metadata stays consistent with the reviews it summarises.</p>
     *
     * @param reviews the locale-keyed review holders, may be {@code null}
     * @return a freshly built metadata object, never {@code null}
     */
    public static ProductReviewMetadata from(Localisable<String, AiReviewHolder> reviews) {
        ProductReviewMetadata metadata = new ProductReviewMetadata();
        if (reviews != null) {
            reviews.forEach((locale, holder) -> {
                if (locale != null && holder != null) {
                    ProductReviewLocaleMetadata localeMetadata = new ProductReviewLocaleMetadata();
                    localeMetadata.setEnoughData(holder.isEnoughData());
                    localeMetadata.setCreatedMs(holder.getCreatedMs());
                    metadata.getLocales().put(locale, localeMetadata);
                }
            });
        }
        return metadata;
    }

    public Map<String, ProductReviewLocaleMetadata> getLocales() {
        return locales;
    }

    public void setLocales(Map<String, ProductReviewLocaleMetadata> locales) {
        this.locales = locales == null ? new HashMap<>() : locales;
    }
}
