package org.open4goods.brand.model;

/**
 * External evidence that a source name points to a canonical brand.
 */
public class BrandSourceEvidence {

    private String source;
    private String sourceId;
    private String rawName;
    private long count;

    public BrandSourceEvidence() {
    }

    public BrandSourceEvidence(String source, String sourceId, String rawName, long count) {
        this.source = source;
        this.sourceId = sourceId;
        this.rawName = rawName;
        this.count = count;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getRawName() {
        return rawName;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
