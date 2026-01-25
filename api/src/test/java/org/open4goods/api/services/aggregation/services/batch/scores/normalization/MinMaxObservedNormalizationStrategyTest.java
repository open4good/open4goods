package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreDegeneratePolicy;
import org.open4goods.model.vertical.scoring.ScoreNormalizationConfig;
import org.open4goods.model.vertical.scoring.ScoreScaleConfig;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

class MinMaxObservedNormalizationStrategyTest {

    private final MinMaxObservedNormalizationStrategy strategy = new MinMaxObservedNormalizationStrategy();

    @Test
    void normalize_NormalDistribution_ShouldNormalize() throws ValidationException {
        // Arrange
        AttributeConfig config = createConfig(0.0, 10.0, ScoreDegeneratePolicy.NEUTRAL);
        NormalizationContext context = createContext(0.0, 100.0);

        // Act
        NormalizationResult result = strategy.normalize(50.0, context, config);

        // Assert
        assertThat(result.value()).isEqualTo(5.0);
        assertThat(result.legacy()).isFalse();
    }

    @Test
    void normalize_DegenerateDistribution_PolicyNeutral_ShouldReturnNeutral() throws ValidationException {
        // Arrange
        AttributeConfig config = createConfig(0.0, 10.0, ScoreDegeneratePolicy.NEUTRAL);
        NormalizationContext context = createContext(50.0, 50.0); // min == max

        // Act
        NormalizationResult result = strategy.normalize(50.0, context, config);

        // Assert
        assertThat(result.value()).isEqualTo(5.0); // (0+10)/2
    }

    @Test
    void normalize_DegenerateDistribution_PolicyFallback_ShouldReturnNeutral() throws ValidationException {
        // Arrange
        AttributeConfig config = createConfig(0.0, 10.0, ScoreDegeneratePolicy.FALLBACK);
        NormalizationContext context = createContext(50.0, 50.0); // min == max

        // Act
        NormalizationResult result = strategy.normalize(50.0, context, config);

        // Assert
        assertThat(result.value()).isEqualTo(5.0); // Fallback to neutral for now as per plan
    }

    @Test
    void normalize_DegenerateDistribution_PolicyError_ShouldThrow() {
        // Arrange
        AttributeConfig config = createConfig(0.0, 10.0, ScoreDegeneratePolicy.ERROR);
        NormalizationContext context = createContext(50.0, 50.0); // min == max

        // Act & Assert
        assertThatThrownBy(() -> strategy.normalize(50.0, context, config))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid observed bounds");
    }
    
    @Test
    void normalize_InvalidDistribution_MinGreaterThanMax_ShouldFollowPolicy() throws ValidationException {
         // Arrange
        AttributeConfig config = createConfig(0.0, 10.0, ScoreDegeneratePolicy.NEUTRAL);
        NormalizationContext context = createContext(60.0, 50.0); // min > max

        // Act
        NormalizationResult result = strategy.normalize(50.0, context, config);

        // Assert
        assertThat(result.value()).isEqualTo(5.0);
    }

    private AttributeConfig createConfig(Double minScale, Double maxScale, ScoreDegeneratePolicy policy) {
        AttributeConfig config = mock(AttributeConfig.class);
        ScoreScoringConfig scoring = mock(ScoreScoringConfig.class);
        ScoreScaleConfig scale = mock(ScoreScaleConfig.class);
        ScoreNormalizationConfig norm = mock(ScoreNormalizationConfig.class);

        when(config.getScoring()).thenReturn(scoring);
        when(scoring.getScale()).thenReturn(scale);
        when(scoring.getDegenerateDistributionPolicy()).thenReturn(policy);
        when(scale.getMin()).thenReturn(minScale);
        when(scale.getMax()).thenReturn(maxScale);
        
        return config;
    }

    private NormalizationContext createContext(Double minObserved, Double maxObserved) {
        Cardinality cardinality = mock(Cardinality.class);
        when(cardinality.getMin()).thenReturn(minObserved);
        when(cardinality.getMax()).thenReturn(maxObserved);
        
        return new NormalizationContext(cardinality, null);
    }
}
