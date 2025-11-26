package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

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

	public Attribute2ScoreAggregationService(final Logger logger) {
		super(logger);
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
						Double score = generateScoresFromAttribute(attrConfig.getKey(), aga, vConf.getAttributesConfig());

						// Processing cardinality
						incrementCardinality(attrConfig.getKey(), score);

						Score s = new Score(attrConfig.getKey(), score);
						// Saving in product
						data.getScores().put(s.getName(), s);
					} catch (ValidationException e) {
						dedicatedLogger.warn("Attribute to score fail for {}", aga, e);
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

	/**
	 * Reverses the configured scores (so that "lower is better" criteria can be
	 * compared) and recalculates the batch cardinalities before delegating to the
	 * standard relativisation process.
	 */
	@Override
	public void done(Collection<Product> datas, VerticalConfig vConf) {

		/////////////////////////////////////////////////
		// Reversing the scores that need to be (ie. weight, electric consumption :
		///////////////////////////////////////////////// lower values are the best
		///////////////////////////////////////////////// scored)
		// To reverse a score, we substract max absolute score from item absolute score
		/////////////////////////////////////////////////

		// Selecting the scores to reverse
                List<String> scoresToReverse = vConf.getAttributesConfig().getConfigs().stream().filter(e -> e.isReverseScore()).map(e -> e.getKey()).toList();

                // For each score to reverse
                for (String key : scoresToReverse) {

                        Cardinality sourceCardinality = ensureCardinality(key, datas);

                        Double max = sourceCardinality.getMax();
                        if (null == max) {
                                throw new IllegalStateException("Cannot reverse score %s – no max value available".formatted(key));
                        }

			// Recompute the global cardinality with the reversed values so that
			// subsequent relativisation and frontend explanations rely on the same extrema.
			Cardinality reversedCardinality = new Cardinality();

			for (Product p : datas) {
                                Score score = p.getScores().get(key);
                                if (score == null) {
                                        dedicatedLogger.info("No score {} for {}", key, p);
                                        continue;
                                }
                                Double originalValue = score.getValue();
                                if (originalValue == null) {
                                        throw new IllegalStateException("Cannot reverse score %s – product %s has a null value".formatted(key, p.getId()));
                                }
                                Double reversed = max - originalValue;
                                score.setValue(reversed);
                                reversedCardinality.increment(reversed);
                                dedicatedLogger.info("Score {} reversed for {}. Original was {}, max is {}. New value is {}", key, p, originalValue, max, reversed);
                        }

			batchDatas.put(key, reversedCardinality);
		}

		// SUPER important : Score relativisation is operated in the
		// AbstactScoreAggService
                super.done(datas, vConf);
        }

        private Cardinality ensureCardinality(String key, Collection<Product> datas) {

                Cardinality existing = batchDatas.get(key);
                if (null != existing && Objects.nonNull(existing.getMax())) {
                        return existing;
                }

                Cardinality rebuilt = rebuildCardinality(key, datas);
                batchDatas.put(key, rebuilt);
                return rebuilt;
        }

        private Cardinality rebuildCardinality(String key, Collection<Product> datas) {
                Cardinality rebuilt = new Cardinality();

                for (Product product : datas) {
                        Score score = product.getScores().get(key);
                        if (score == null || score.getValue() == null) {
                                continue;
                        }
                        rebuilt.increment(score.getValue());
                }

                if (rebuilt.getMax() == null) {
                        throw new IllegalStateException("Cannot reverse score %s – no values available to rebuild cardinality".formatted(key));
                }

                return rebuilt;
        }

}
