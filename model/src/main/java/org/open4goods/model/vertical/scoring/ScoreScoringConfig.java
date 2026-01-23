package org.open4goods.model.vertical.scoring;

/**
 * Configuration block for score normalization and policies.
 */
public class ScoreScoringConfig {

    private ScoreScaleConfig scale = new ScoreScaleConfig();

    private ScoreNormalizationConfig normalization = new ScoreNormalizationConfig();

    private ScoreTransform transform = ScoreTransform.NONE;

    private ScoreMissingValuePolicy missingValuePolicy = ScoreMissingValuePolicy.NEUTRAL;

    private ScoreDegeneratePolicy degenerateDistributionPolicy = ScoreDegeneratePolicy.NEUTRAL;

    private ScoreStatsScopeConfig statsScope = new ScoreStatsScopeConfig();

    public ScoreScaleConfig getScale() {
        return scale;
    }

    public void setScale(ScoreScaleConfig scale) {
        this.scale = scale == null ? new ScoreScaleConfig() : scale;
    }

    public ScoreNormalizationConfig getNormalization() {
        return normalization;
    }

    public void setNormalization(ScoreNormalizationConfig normalization) {
        this.normalization = normalization == null ? new ScoreNormalizationConfig() : normalization;
    }

    public ScoreTransform getTransform() {
        return transform;
    }

    public void setTransform(ScoreTransform transform) {
        this.transform = transform == null ? ScoreTransform.NONE : transform;
    }

    public ScoreMissingValuePolicy getMissingValuePolicy() {
        return missingValuePolicy;
    }

    public void setMissingValuePolicy(ScoreMissingValuePolicy missingValuePolicy) {
        this.missingValuePolicy = missingValuePolicy == null ? ScoreMissingValuePolicy.NEUTRAL : missingValuePolicy;
    }

    public ScoreDegeneratePolicy getDegenerateDistributionPolicy() {
        return degenerateDistributionPolicy;
    }

    public void setDegenerateDistributionPolicy(ScoreDegeneratePolicy degenerateDistributionPolicy) {
        this.degenerateDistributionPolicy = degenerateDistributionPolicy == null
                ? ScoreDegeneratePolicy.NEUTRAL
                : degenerateDistributionPolicy;
    }

    public ScoreStatsScopeConfig getStatsScope() {
        return statsScope;
    }

    public void setStatsScope(ScoreStatsScopeConfig statsScope) {
        this.statsScope = statsScope == null ? new ScoreStatsScopeConfig() : statsScope;
    }
}
