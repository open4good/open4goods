package org.open4goods.model.vertical;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods to derive aggregated score definitions from vertical
 * configuration. The helper groups attributes that declare
 * {@code participateInScores} and normalises their ponderations to a 1.0 scale
 * for each aggregated score key.
 */
public final class ParticipatingScoreHelper {

    /**
     * Name of the ImpactScore, computed from {@code criteriasPonderation}. A
     * composite using this name would replace the weighted 0-20 score.
     */
    private static final String RESERVED_IMPACT_SCORE_NAME = "ECOSCORE";

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipatingScoreHelper.class);

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
     * <p>In V2 a derived criterion is available through its sources
     * ({@link AttributeConfig#getScoreSources()}), and a participating
     * attribute the V2 matrix does not weight is skipped (GOU-2041).
     *
     * @throws IllegalStateException in V1, if a participating score has no
     *                               ponderation defined in {@code impactScoreConfig}
     */
    public static Map<String, Map<String, Double>> buildNormalizedParticipatingScores(VerticalConfig verticalConfig) {
        Objects.requireNonNull(verticalConfig, "verticalConfig is required");
        Objects.requireNonNull(verticalConfig.getAttributesConfig(), "attributesConfig is required");
        Objects.requireNonNull(verticalConfig.getImpactScoreConfig(), "impactScoreConfig is required");

        Map<String, Double> criteriasPonderation = verticalConfig.getImpactScoreConfig().activeCriteriasPonderation();
        boolean v2 = verticalConfig.getImpactScoreConfig().isV2Active();
        Set<String> availableImpactScoreCriterias = resolveAvailableImpactScoreCriterias(verticalConfig);
        Map<String, Map<String, Double>> aggregates = new HashMap<>();

        for (AttributeConfig attribute : verticalConfig.getAttributesConfig().getConfigs()) {
            if (!attribute.isAsScore()) {
                continue;
            }

            if (!isAvailable(attribute, availableImpactScoreCriterias, v2)) {
                continue;
            }

            Set<String> participateInScores = attribute.getParticipateInScores();
            if (participateInScores == null || participateInScores.isEmpty()) {
                continue;
            }

            Double ponderation = criteriasPonderation.get(attribute.getKey());
            if (ponderation == null) {
                if (v2) {
                    // Not a criterion of the V2 matrix, which ImpactScoreMethodology validates: for instance a
                    // source replaced by a derived criterion
                    continue;
                }
                throw new IllegalStateException("Missing ponderation for participating score " + attribute.getKey());
            }

            for (String aggregateName : participateInScores) {
                if (RESERVED_IMPACT_SCORE_NAME.equals(aggregateName)) {
                    // Skipped rather than thrown: a configuration override must not stop a whole vertical
                    // from being scored. VerticalYamlValidationTest rejects it in the repository.
                    LOGGER.error("Attribute {} of vertical {} cannot participate in the reserved score {}: ignored",
                            attribute.getKey(), verticalConfig.getId(), RESERVED_IMPACT_SCORE_NAME);
                    continue;
                }
                aggregates.computeIfAbsent(aggregateName, key -> new HashMap<>())
                        .merge(attribute.getKey(), ponderation, Double::sum);
            }
        }

        aggregates.entrySet().removeIf(entry -> {
            double total = entry.getValue().values().stream().mapToDouble(Double::doubleValue).sum();
            return Double.compare(total, 0d) == 0;
        });

        aggregates.replaceAll((aggregate, weights) -> normalize(aggregate, weights));
        return aggregates;
    }

    /**
     * Whether an attribute is an available ImpactScore criterion: listed in {@code availableImpactScoreCriterias},
     * or, in V2 only, derived from a listed source.
     */
    private static boolean isAvailable(AttributeConfig attribute, Set<String> availableImpactScoreCriterias, boolean v2) {
        if (availableImpactScoreCriterias.contains(attribute.getKey())) {
            return true;
        }
        return v2 && attribute.isDerivedScore()
                && attribute.getScoreSources().stream().anyMatch(availableImpactScoreCriterias::contains);
    }

    private static Set<String> resolveAvailableImpactScoreCriterias(VerticalConfig verticalConfig) {
        Set<String> availableImpactScoreCriterias = verticalConfig.getAvailableImpactScoreCriterias() == null
                ? null
                : new HashSet<>(verticalConfig.getAvailableImpactScoreCriterias());
        if (availableImpactScoreCriterias == null) {
            return Set.of();
        }

        return availableImpactScoreCriterias.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static Map<String, Double> normalize(String aggregateName, Map<String, Double> weights) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Double> normalized = new HashMap<>();
        weights.forEach((score, ponderation) -> normalized.put(score, ponderation / total));
        return normalized;
    }
}
