package org.open4goods.model.vertical.scoring;

/**
 * Configures how raw attribute values are normalized to a score scale.
 */
public class ScoreNormalizationConfig {

    /**
     * Normalization method to apply.
     */
    private ScoreNormalizationMethod method;

    /**
     * Method-specific parameters.
     */
    private ScoreNormalizationParams params = new ScoreNormalizationParams();

    public ScoreNormalizationMethod getMethod() {
        return method;
    }

    public void setMethod(ScoreNormalizationMethod method) {
        this.method = method;
    }

    public ScoreNormalizationParams getParams() {
        return params;
    }

    public void setParams(ScoreNormalizationParams params) {
        this.params = params == null ? new ScoreNormalizationParams() : params;
    }
}
