package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.ParticipatingScoreHelper;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Aggregates scores based on {@code participateInScores} declarations in
 * attribute configurations. The service computes a weighted sum (scaled to 1.0)
 * of participating scores for each aggregate key defined in the vertical.
 */
public class ParticipatingScoresAggregationService extends AbstractScoreAggregationService {

        private final Map<String, Map<String, Map<String, Double>>> normalizedParticipations = new HashMap<>();

        public ParticipatingScoresAggregationService(Logger logger) {
                super(logger);
        }

        @Override
        public void init(Collection<Product> datas) {
                super.init(datas);
                normalizedParticipations.clear();
        }

        @Override
        public void onProduct(Product data, VerticalConfig vConf) {
                Map<String, Map<String, Double>> aggregates = normalizedParticipations.computeIfAbsent(vConf.getId(),
                                key -> ParticipatingScoreHelper.buildNormalizedParticipatingScores(vConf));

                if (aggregates.isEmpty()) {
                        return;
                }

                aggregates.forEach((aggregateName, participants) -> handleAggregate(data, aggregateName, participants));
        }

        private void handleAggregate(Product product, String aggregateName, Map<String, Double> participants) {
                Double aggregatedValue = computeAggregatedValue(product, participants);
                if (aggregatedValue == null) {
                        dedicatedLogger.warn("Skipping aggregate {} for {} due to missing sub-scores", aggregateName,
                                        product.getId());
                        return;
                }

                try {
                        incrementCardinality(aggregateName, aggregatedValue);
                } catch (ValidationException e) {
                        dedicatedLogger.warn("Cannot increment cardinality for {}", aggregateName, e);
                        return;
                }

                Score score = new Score(aggregateName, aggregatedValue);
                score.setAggregates(new HashMap<>(participants));
                product.getScores().put(aggregateName, score);
        }

        private Double computeAggregatedValue(Product product, Map<String, Double> participants) {
                double aggregateValue = 0d;

                for (Map.Entry<String, Double> entry : participants.entrySet()) {
                        String scoreName = entry.getKey();
                        Score score = product.getScores().get(scoreName);
                        if (score == null) {
                                dedicatedLogger.warn("Missing participating score {} for product {}", scoreName,
                                                product.getId());
                                return null;
                        }

                        Double relativeValue = resolveRelativeValue(scoreName, score);
                        if (relativeValue == null) {
                                return null;
                        }

                        aggregateValue += relativeValue * entry.getValue();
                }

                return aggregateValue;
        }

        private Double resolveRelativeValue(String config, Score score) {
                if (score.getRelativ() != null && score.getRelativ().getValue() != null) {
                        return score.getRelativ().getValue();
                }

                if (score.getAbsolute() != null && score.getAbsolute().getValue() != null) {
                        try {
                                return relativize(score.getAbsolute().getValue(), score.getAbsolute());
                        } catch (ValidationException e) {
                                dedicatedLogger.warn("Participating score relativization failed for {} : {}", config,
                                                e.getMessage());
                                return null;
                        }
                }

                if (score.getValue() != null) {
                        dedicatedLogger.warn("Using raw value for {} due to missing cardinalities", config);
                        return score.getValue();
                }

                dedicatedLogger.warn("Cannot compute aggregate, missing value for {}", config);
                return null;
        }
}
