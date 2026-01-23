package org.open4goods.model.vertical.scoring;

/**
 * Policy applied when the distribution is degenerate (e.g. sigma == 0).
 */
public enum ScoreDegeneratePolicy {
    NEUTRAL,
    ERROR,
    FALLBACK
}
