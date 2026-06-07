package org.open4goods.model.product;

/**
 * Indexed AI review state for one locale.
 */
public class ProductReviewLocaleMetadata {

    private boolean enoughData;

    private Long createdMs;

    public boolean isEnoughData() {
        return enoughData;
    }

    public void setEnoughData(boolean enoughData) {
        this.enoughData = enoughData;
    }

    public Long getCreatedMs() {
        return createdMs;
    }

    public void setCreatedMs(Long createdMs) {
        this.createdMs = createdMs;
    }
}
