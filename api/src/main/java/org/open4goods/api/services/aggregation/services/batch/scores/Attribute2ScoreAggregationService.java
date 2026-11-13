package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.scoring.ScoreDerivationMethod;
import org.slf4j.Logger;

/**
 * Aggregates product attributes into {@link Score} instances and prepares the
 * statistics required for relativisation.
 *
 * <p>A derived attribute ({@link AttributeConfig#getScoreSources()}) is never
 * scored from its own value: while it is weighted by the active ImpactScore
 * matrix, its score takes the raw value of the first source holding a
 * parseable value (no average), normalised with its own scoring, and records
 * that source in {@link #SCORE_SOURCE_METADATA}. Without any such source the
 * score stays absent (GOU-2041).
 *
 * <p>A {@link ScoreDerivationMethod#SCREEN_AREA_DENSITY} derived attribute
 * divides its first source by the screen area of its second source, a
 * diagonal in inches (GOU-1884): both must be real and positive, otherwise
 * the score stays absent.
 */
public class Attribute2ScoreAggregationService extends AbstractScoreAggregationService {

        /** Metadata of a derived score: the key of the source attribute its value was read from, or the keys of the
         * sources it combines, comma separated. */
        public static final String SCORE_SOURCE_METADATA = "source";

        private final Map<Long, Map<String, Double>> absoluteValuesOverrides = new HashMap<>();

        public Attribute2ScoreAggregationService(final Logger logger) {
                super(logger);
        }

        @Override
        public void init(Collection<Product> datas) {
                super.init(datas);
                absoluteValuesOverrides.clear();
        }

	@Override
	public void onProduct(Product data, VerticalConfig vConf) {

		Collection<IndexedAttribute> aggattrs = data.getAttributes().getIndexed().values();
		// Attributes of the product by configured key, read by the derived scores
		Map<String, IndexedAttribute> attributesByKey = new HashMap<>();
		for (IndexedAttribute aga : aggattrs) {
			// Scoring from attribute
			try {

				AttributesConfig attributesConfig = vConf.getAttributesConfig();
				// Resolve the attribute configuration directly using the provided name
				// to leverage synonym mapping in {@link AttributesConfig}
				String attributeKey = attributesConfig.getKeyForValue(aga.getName());
				if (attributeKey == null) {
					attributeKey = aga.getName();
				}

				AttributeConfig attrConfig = attributesConfig.getConfigFor(attributeKey);
				if (attrConfig == null) {
					dedicatedLogger.error("No attribute config for {}", aga);
					continue;
				}
				attributesByKey.put(attrConfig.getKey(), aga);
				if (attrConfig.isDerivedScore()) {
					// Scored from its sources only, see scoreDerivedAttributes()
					continue;
				}

				if (attrConfig.isAsScore()) {
					try {
						Double rawScore = generateScoresFromAttribute(attrConfig.getKey(), aga, vConf.getAttributesConfig());
						Double score = applyTransform(rawScore, attrConfig);
						storeAbsoluteOverride(data, attrConfig.getKey(), rawScore, score);

						// Processing cardinality
						incrementCardinality(attrConfig.getKey(), score, vConf);

						Score s = new Score(attrConfig.getKey(), score);
						s.setAbsolute(new Cardinality());
						s.getAbsolute().setValue(score);
						// Saving in product
						data.getScores().put(s.getName(), s);
					} catch (ValidationException e) {
						handleMissingScore(data, attrConfig, aga, vConf, e);
					}

				}
			} catch (Exception e) {
				dedicatedLogger.error("Error while processing attribute {}", aga);
			}
		}
		scoreDerivedAttributes(data, vConf, attributesByKey);
	}

	/**
	 * Scores the derived attributes weighted by the active ImpactScore matrix, normalised with the derived
	 * attribute's own scoring. A derived attribute without the source values its method needs gets no score, never
	 * a default one.
	 *
	 * @param data            the product
	 * @param vConf           the vertical configuration
	 * @param attributesByKey the product attributes, by configured key
	 */
	private void scoreDerivedAttributes(Product data, VerticalConfig vConf, Map<String, IndexedAttribute> attributesByKey) {
		if (vConf.getAttributesConfig() == null || vConf.getImpactScoreConfig() == null) {
			return;
		}
		Map<String, Double> weights = vConf.getImpactScoreConfig().activeCriteriasPonderation();
		for (AttributeConfig derived : vConf.getAttributesConfig().getConfigs()) {
			if (!derived.isAsScore() || !derived.isDerivedScore()) {
				continue;
			}
			Double weight = weights.get(derived.getKey());
			if (weight == null || weight <= 0.0) {
				continue;
			}
			if (derived.scoreDerivationMethod() == ScoreDerivationMethod.SCREEN_AREA_DENSITY) {
				scoreScreenAreaDensity(data, vConf, derived, attributesByKey);
			} else {
				scoreFirstAvailableSource(data, vConf, derived, attributesByKey);
			}
		}
	}

	/**
	 * {@link ScoreDerivationMethod#FIRST_AVAILABLE}: the raw value of the first source holding a parseable value,
	 * never an average (GOU-2041).
	 */
	private void scoreFirstAvailableSource(Product data, VerticalConfig vConf, AttributeConfig derived,
			Map<String, IndexedAttribute> attributesByKey) {
		for (String source : derived.getScoreSources()) {
			IndexedAttribute aga = attributesByKey.get(source);
			if (aga == null) {
				continue;
			}
			Double rawScore;
			try {
				rawScore = generateScoresFromAttribute(source, aga, vConf.getAttributesConfig());
			} catch (ValidationException e) {
				dedicatedLogger.debug("Source {} of derived score {} not parseable for {}", source, derived.getKey(),
						data.getId());
				continue;
			}
			storeDerivedScore(data, vConf, derived, rawScore, source);
			break;
		}
	}

	/**
	 * {@link ScoreDerivationMethod#SCREEN_AREA_DENSITY}: the first source divided by the screen area, in square
	 * decimetres, of the second source, a diagonal in inches (GOU-1884). Both values must be real and positive: a zero
	 * power or diagonal is not a measure, and nothing is imputed.
	 */
	private void scoreScreenAreaDensity(Product data, VerticalConfig vConf, AttributeConfig derived,
			Map<String, IndexedAttribute> attributesByKey) {
		List<String> sources = derived.getScoreSources();
		if (sources.size() != 2) {
			dedicatedLogger.error("Derived score {} needs a value and a diagonal source, got {}", derived.getKey(), sources);
			return;
		}
		Double value = positiveSourceValue(data, vConf, derived, sources.get(0), attributesByKey);
		Double diagonal = positiveSourceValue(data, vConf, derived, sources.get(1), attributesByKey);
		if (value == null || diagonal == null) {
			return;
		}
		double density = value / derived.getScoreDerivation().screenAreaSquareDecimetres(diagonal);
		storeDerivedScore(data, vConf, derived, density, String.join(",", sources));
	}

	/**
	 * The parsed value of a source of a derived score, or {@code null} when it is missing, not parseable or not
	 * strictly positive.
	 */
	private Double positiveSourceValue(Product data, VerticalConfig vConf, AttributeConfig derived, String source,
			Map<String, IndexedAttribute> attributesByKey) {
		IndexedAttribute aga = attributesByKey.get(source);
		if (aga == null) {
			return null;
		}
		try {
			Double value = generateScoresFromAttribute(source, aga, vConf.getAttributesConfig());
			if (value == null || value.isNaN() || value.isInfinite() || value <= 0.0) {
				dedicatedLogger.debug("Source {} of derived score {} not positive for {}: {}", source,
						derived.getKey(), data.getId(), value);
				return null;
			}
			return value;
		} catch (ValidationException e) {
			dedicatedLogger.debug("Source {} of derived score {} not parseable for {}", source, derived.getKey(),
					data.getId());
			return null;
		}
	}

	/**
	 * Stores a derived score computed from a raw value, recording the source attributes it was read from.
	 */
	private void storeDerivedScore(Product data, VerticalConfig vConf, AttributeConfig derived, Double rawScore,
			String source) {
		try {
			Double score = applyTransform(rawScore, derived);
			storeAbsoluteOverride(data, derived.getKey(), rawScore, score);
			incrementCardinality(derived.getKey(), score, vConf);
			Score s = new Score(derived.getKey(), score);
			s.setAbsolute(new Cardinality());
			s.getAbsolute().setValue(score);
			s.getMetadatas().put(SCORE_SOURCE_METADATA, source);
			data.getScores().put(s.getName(), s);
		} catch (ValidationException e) {
			dedicatedLogger.warn("Derived score {} failed for {} from {}", derived.getKey(), data.getId(), source, e);
		}
	}

	/**
	 * Generate the score (min, max, value) from an aggregatedattribute
	 *
	 * @param attributeKey
	 * @param aga
	 * @return
	 */
	public Double generateScoresFromAttribute(String attributeKey, IndexedAttribute aga, AttributesConfig attributesConfig) throws ValidationException {

		AttributeConfig ac = attributesConfig.getAttributeConfigByKey(attributeKey);
		// transformation required

		if (ac == null) {
			throw new ValidationException("No attribute config for " + attributeKey);
		}

		if (ac.getFilteringType().equals(AttributeType.NUMERIC)) {
			try {
				return Double.valueOf(aga.getValue().replace(",", "."));
			} catch (Exception e) {
				throw new ValidationException("Cannot convert to numeric" + aga);
			}

		} else if (ac.getNumericMapping() != null && !ac.getNumericMapping().isEmpty()) {
			Double mapping = ac.getNumericMapping().get(aga.getValue());
			if (mapping == null || mapping.isInfinite() || mapping.isNaN()) {
				throw new ValidationException("Attribute to rating conversion failed " + aga);
			}
			return mapping;
		} else {
			throw new ValidationException("Was asking to  translate {} into rating, but no numericMapping definition nor numeric attribute found : " + aga);
		}

	}

	private Double applyTransform(Double value, AttributeConfig attributeConfig) throws ValidationException {
		if (value == null) {
			return null;
		}
		if (attributeConfig == null || attributeConfig.getScoring() == null) {
			return value;
		}
		return switch (attributeConfig.getScoring().getTransform()) {
			case LOG -> {
				if (value < 0) {
					throw new ValidationException("Cannot apply LOG transform to negative value " + value);
				}
				yield Math.log1p(value);
			}
			case SQRT -> {
				if (value < 0) {
					throw new ValidationException("Cannot apply SQRT transform to negative value " + value);
				}
				yield Math.sqrt(value);
			}
			case NONE -> value;
		};
	}

	private void storeAbsoluteOverride(Product data, String scoreName, Double rawScore, Double transformedScore) {
		if (rawScore == null || transformedScore == null) {
			return;
		}
		if (!rawScore.equals(transformedScore)) {
			absoluteValuesOverrides.computeIfAbsent(data.getId(), id -> new HashMap<>()).put(scoreName, rawScore);
		}
	}

	private void handleMissingScore(Product data, AttributeConfig attrConfig, IndexedAttribute aga, VerticalConfig vConf,
			ValidationException e) {
		if (attrConfig == null || attrConfig.getScoring() == null) {
			dedicatedLogger.warn("Attribute to score fail for {}", aga, e);
			return;
		}

		switch (attrConfig.getScoring().getMissingValuePolicy()) {
			case WORST, NEUTRAL -> {
				Double fallback = resolveMissingValueFallback(attrConfig);
				if (fallback == null) {
					dedicatedLogger.warn("Attribute to score fail for {}", aga, e);
					return;
				}
				try {
					incrementCardinality(attrConfig.getKey(), fallback, vConf);
					Score s = new Score(attrConfig.getKey(), fallback);
					s.setVirtual(true);
					s.setAbsolute(new Cardinality());
					s.getAbsolute().setValue(fallback);
					data.getScores().put(s.getName(), s);
				} catch (ValidationException validationException) {
					dedicatedLogger.warn("Attribute missing value fallback failed for {}", aga, validationException);
				}
			}
		}
	}

	private Double resolveMissingValueFallback(AttributeConfig attrConfig) {
		if (attrConfig == null || attrConfig.getScoring() == null || attrConfig.getScoring().getScale() == null) {
			return null;
		}
		Double min = attrConfig.getScoring().getScale().getMin();
		Double max = attrConfig.getScoring().getScale().getMax();
		if (min == null || max == null) {
			return null;
		}
		return switch (attrConfig.getScoring().getMissingValuePolicy()) {
			case WORST -> min;
			case NEUTRAL -> null;
		};
	}
	/**
	 * Reverses the configured scores (so that "lower is better" criteria can be
	 * compared) and recalculates the batch cardinalities before delegating to the
	 * standard relativisation process.
	 */
	@Override
	public void done(Collection<Product> datas, VerticalConfig vConf) {
                // Score relativisation is operated in the AbstactScoreAggService
                super.done(datas, vConf);
                absoluteValuesOverrides.clear();
	}

        @Override
        protected Double resolveAbsoluteValue(Product product, String scoreName, Score score) {
                Map<String, Double> overrides = absoluteValuesOverrides.get(product.getId());
                if (overrides != null && overrides.containsKey(scoreName)) {
                        return overrides.get(scoreName);
                }

                return super.resolveAbsoluteValue(product, scoreName, score);
        }

}
