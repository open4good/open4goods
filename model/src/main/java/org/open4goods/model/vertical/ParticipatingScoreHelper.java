package org.open4goods.model.vertical;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility methods to derive aggregated score definitions from vertical
 * configuration. The helper groups attributes that declare
 * {@code participateInScores} and normalises their ponderations to a 1.0 scale
 * for each aggregated score key.
 */
public final class ParticipatingScoreHelper {

    private ParticipatingScoreHelper() {
    }

    /**
     * Builds the per-aggregate ponderations based on {@link AttributeConfig}
     * declarations and the vertical {@link ImpactScoreConfig} weights. For each
     * aggregate key, the method collects participating scores, verifies their
     * ponderation is defined, and returns weights scaled to sum to 1.
     *
     * @param verticalConfig the vertical configuration holding attributes and
     *                       impact score ponderations
     * @return a map keyed by aggregate name containing normalised ponderations of
     *         participating scores
     * @throws IllegalStateException if a participating score has no ponderation
     *                               defined in {@code impactScoreConfig}
     */
    public static Map<String, Map<String, Double>> buildNormalizedParticipatingScores(VerticalConfig verticalConfig) {
        Objects.requireNonNull(verticalConfig, "verticalConfig is required");
        Objects.requireNonNull(verticalConfig.getAttributesConfig(), "attributesConfig is required");
        Objects.requireNonNull(verticalConfig.getImpactScoreConfig(), "impactScoreConfig is required");

        Map<String, Double> criteriasPonderation = verticalConfig.getImpactScoreConfig().getCriteriasPonderation();
        Map<String, Map<String, Double>> aggregates = new HashMap<>();

        for (AttributeConfig attribute : verticalConfig.getAttributesConfig().getConfigs()) {
            if (!attribute.isAsScore()) {
                continue;
            }

            Set<String> participateInScores = attribute.getParticipateInScores();
            if (participateInScores == null || participateInScores.isEmpty()) {
                continue;
            }

            Double ponderation = criteriasPonderation.get(attribute.getKey());
            if (ponderation == null) {
                throw new IllegalStateException("Missing ponderation for participating score " + attribute.getKey());
            }

            for (String aggregateName : participateInScores) {
                aggregates.computeIfAbsent(aggregateName, key -> new HashMap<>())
                        .merge(attribute.getKey(), ponderation, Double::sum);
            }
        }

        aggregates.replaceAll((aggregate, weights) -> normalize(aggregate, weights));
        return aggregates;
    }

    private static Map<String, Double> normalize(String aggregateName, Map<String, Double> weights) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Double.compare(total, 0d) == 0) {
            throw new IllegalStateException("Ponderation sum is zero for aggregate " + aggregateName);
        }
        Map<String, Double> normalized = new HashMap<>();
        weights.forEach((score, ponderation) -> normalized.put(score, ponderation / total));
        return normalized;
    }
}
