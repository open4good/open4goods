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
import org.slf4j.Logger;

/**
 * Aggregates product attributes into {@link Score} instances and prepares the
 * statistics required for relativisation.
 */
public class Attribute2ScoreAggregationService extends AbstractScoreAggregationService {

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
				if (null == attrConfig) {
					dedicatedLogger.error("No attribute config for {}", aga);
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

		if (null == ac) {
			throw new ValidationException("No attribute config for " + attributeKey);
		}

		if (ac.getFilteringType().equals(AttributeType.NUMERIC)) {
			try {
				return Double.valueOf(aga.getValue().replace(",", "."));
			} catch (Exception e) {
				throw new ValidationException("Cannot convert to numeric" + aga);
			}

		} else if (ac.getNumericMapping().size() > 0) {
			Double mapping = ac.getNumericMapping().get(aga.getValue());
			if (null == mapping || mapping.isInfinite() || mapping.isNaN()) {
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
			case EXCLUDE -> {
				dedicatedLogger.warn("Attribute to score excluded for {}", aga, e);
				return;
			}
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
			case NEUTRAL -> (min + max) / 2.0;
			case EXCLUDE -> null;
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
