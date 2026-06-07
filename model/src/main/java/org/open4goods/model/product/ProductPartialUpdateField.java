package org.open4goods.model.product;

/**
 * Supported product fields for partial Elasticsearch document updates.
 */
public enum ProductPartialUpdateField {

    LAST_CHANGE("lastChange"),
    PRICE("price"),
    OFFERS_COUNT("offersCount"),
    REVIEW_METADATA("reviewMetadata");

    private final String path;

    ProductPartialUpdateField(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public static String reviewEnoughData(String locale) {
        return reviewLocalePath(locale, "enoughData");
    }

    public static String reviewCreatedMs(String locale) {
        return reviewLocalePath(locale, "createdMs");
    }

    private static String reviewLocalePath(String locale, String leaf) {
        if (locale == null || locale.isBlank()) {
            throw new IllegalArgumentException("Review locale is required");
        }
        return "reviewMetadata.locales." + locale + "." + leaf;
    }
}
