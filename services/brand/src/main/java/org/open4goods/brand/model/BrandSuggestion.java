package org.open4goods.brand.model;

/**
 * Candidate brand synonym or mapping that must be reviewed before becoming a
 * canonical alias.
 */
public class BrandSuggestion {

    private String rawName;
    private String normalizedName;
    private String suggestedCanonicalName;
    private String source;
    private long evidenceCount;
    private double confidence;
    private String reason;

    public String getRawName() {
        return rawName;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getSuggestedCanonicalName() {
        return suggestedCanonicalName;
    }

    public void setSuggestedCanonicalName(String suggestedCanonicalName) {
        this.suggestedCanonicalName = suggestedCanonicalName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getEvidenceCount() {
        return evidenceCount;
    }

    public void setEvidenceCount(long evidenceCount) {
        this.evidenceCount = evidenceCount;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
