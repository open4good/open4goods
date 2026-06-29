package org.open4goods.model.product;

/**
 * Supported product fields for partial Elasticsearch document updates.
 */
public enum ProductPartialUpdateField {

    LAST_CHANGE("lastChange"),
    PRICE("price"),
    OFFERS_COUNT("offersCount");

    private final String path;

    ProductPartialUpdateField(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

}
