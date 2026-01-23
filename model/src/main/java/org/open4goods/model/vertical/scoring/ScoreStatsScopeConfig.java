package org.open4goods.model.vertical.scoring;

/**
 * Configures the population scope used to compute statistics.
 */
public class ScoreStatsScopeConfig {

    /**
     * Population scope used to compute statistics.
     */
    private ScoreStatsScope population = ScoreStatsScope.VERTICAL;

    public ScoreStatsScope getPopulation() {
        return population;
    }

    public void setPopulation(ScoreStatsScope population) {
        this.population = population == null ? ScoreStatsScope.VERTICAL : population;
    }
}
