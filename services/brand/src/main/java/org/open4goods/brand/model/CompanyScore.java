package org.open4goods.brand.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A company-level rating from a single curated provider (CDP, B Corp, etc.).
 * The raw {@link #value} is interpreted through its {@link #scale}; {@link #rating}
 * holds the human-facing label (e.g. {@code "A-"}, {@code "Certified"}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyScore {

    private Double value;
    private String rating;
    private ScoreScale scale;
    private String url;
    private String retrievedAt;

    /**
     * @return the score normalised to a 0-100 scale (higher is always better),
     *         or {@code null} when the value/scale cannot be normalised
     */
    public Double normalized() {
        if (value == null || scale == null || scale.getMin() == null || scale.getMax() == null) {
            return null;
        }
        double range = scale.getMax() - scale.getMin();
        if (range == 0) {
            return null;
        }
        double pct = (value - scale.getMin()) / range * 100.0;
        if (!scale.isHigherIsBetter()) {
            pct = 100.0 - pct;
        }
        return Math.max(0.0, Math.min(100.0, pct));
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public ScoreScale getScale() {
        return scale;
    }

    public void setScale(ScoreScale scale) {
        this.scale = scale;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(String retrievedAt) {
        this.retrievedAt = retrievedAt;
    }
}
