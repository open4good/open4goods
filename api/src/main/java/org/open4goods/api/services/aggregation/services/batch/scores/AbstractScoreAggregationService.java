package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributeComparisonRule;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationMethod;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;
import org.slf4j.Logger;

import org.open4goods.api.services.aggregation.services.batch.scores.normalization.NormalizationContext;
import org.open4goods.api.services.aggregation.services.batch.scores.normalization.NormalizationResult;
import org.open4goods.api.services.aggregation.services.batch.scores.normalization.NormalizationStrategy;
import org.open4goods.api.services.aggregation.services.batch.scores.normalization.NormalizationStrategyFactory;

/**
 * Base class for batch score aggregation services. It manages lifecycle hooks,
 * cardinality accumulation, virtual score generation and relativisation for a
 * collection of products handled in a batch.
 */
public abstract class AbstractScoreAggregationService extends AbstractAggregationService {


        protected Map<String, Cardinality> batchDatas = new HashMap<>();

        protected Map<String, Cardinality> absoluteCardinalities = new HashMap<>();

        protected Map<String, Map<Double, Integer>> valueFrequencies = new HashMap<>();

        private final Set<String> legacyScoringLogged = ConcurrentHashMap.newKeySet();

        public AbstractScoreAggregationService(Logger logger) {
                super(logger);
        }


        @Override
        public void init(Collection<Product> datas) {
                super.init(datas);
                batchDatas.clear();
                absoluteCardinalities.clear();
                valueFrequencies.clear();
                legacyScoringLogged.clear();
        }


        @Override
        public void done(Collection<Product> datas, VerticalConfig vConf) {
                super.done(datas, vConf);

                dedicatedLogger.info("{} -> Scores relativisation for {} products", this.getClass().getSimpleName(), datas.size());

                //////////////////////////
                // Virtual scores computing
                // Operated on absolute values
                //////////////////////////
                for (Product p : datas) {
                        for (String scoreName : absoluteCardinalities.keySet()) {
                                Score s = p.getScores().get(scoreName);
                                Cardinality source = absoluteCardinalities.get(scoreName);
                                Cardinality virtual = new Cardinality(source);
                                if (null == s) {


                                        // Need a virtual score
                                        s = new Score(scoreName, source.getAvg());
                                        s.setName(scoreName);
                                        s.setVirtual(true);

                                }
                                virtual.setValue(resolveAbsoluteValue(p, scoreName, s));
                                s.setAbsolute(virtual);
                                p.getScores().put(scoreName, s);
                        }
                }

		
		
                ////////////////////////
                // Scores relativisation
                // Create a relativized cardinality in each product
                ////////////////////////
                for (Product p : datas) {
                        for (String scoreName : batchDatas.keySet()) {
                                Score s = p.getScores().get(scoreName);
				if (null != s) {
					try {
						relativize(s, vConf);
					} catch (ValidationException e) {
						dedicatedLogger.warn("{} -> Relativization of {} failed", this.getClass().getSimpleName(),p.getScores().get(scoreName), e);
					}									
				}
			}			
		}
		

			
		////////////////////////
		// Setting the ranking and worse / best bags
		////////////////////////
		
		for (String scoreName : batchDatas.keySet()) {
			// Sort in the list
			List<Product> sorted = datas.stream(). sorted((e1, e2) -> Double.compare(
				    e1.getScores().get(scoreName).getRelativ().getValue(),
				    e2.getScores().get(scoreName).getRelativ().getValue()
				)).toList();
			
			Long worseGtin = sorted.getFirst().getId();
			Long bestGtin = sorted.getLast().getId();
			
			for (int i = 0 ; i < sorted.size(); i ++ ) {
				Product d = sorted.get(i);
				d.getScores().get(scoreName).setRanking(i);
				d.getScores().get(scoreName).setLowestScoreId(worseGtin);
				d.getScores().get(scoreName).setHighestScoreId(bestGtin);
			}
		}
		
		
	}
	
	/////////////////////////////////////////
	// Private methods
	/////////////////////////////////////////
	/**
	 * Computes relativ values for the provided score while preserving
	 * absolute count/sum statistics. Relative scale is only applied to value,
	 * min, max and average to ensure comparisons remain meaningful without
	 * mutating occurrence information.
	 *
	 * @param score the score to relativize
	 * @param vConf the vertical configuration to check for inversion rules
	 * @throws ValidationException if required absolute values are missing
	 */
	protected void relativize(Score score, VerticalConfig vConf) throws ValidationException {

		// Substracting unused min

		if (null == score.getAbsolute()) {
			dedicatedLogger.warn("Empty value for Score {} ! Consider normalizing in a futur export/import phase",score);
			return ;
		}
		
		Cardinality cardinality =  batchDatas.get(score.getName());

		if (null == cardinality) {
			dedicatedLogger.warn("No source cardinality found for score {}",score);
			return ;
		}
		
		Cardinality ret = new Cardinality();
		ret.setMax(relativizeScoreValue(score.getName(), cardinality.getMax(), score.getAbsolute(), vConf));
		ret.setMin(relativizeScoreValue(score.getName(), cardinality.getMin(), score.getAbsolute(), vConf));
		ret.setAvg(relativizeScoreValue(score.getName(), cardinality.getAvg(), score.getAbsolute(), vConf));
		// Keep occurrence statistics absolute to avoid distorting
		// aggregation insights while still scaling comparable values.
		ret.setCount(cardinality.getCount());
		ret.setSum(cardinality.getSum());
		ret.setSumOfSquares(cardinality.getSumOfSquares());
		
		Double relValue = relativizeScoreValue(score.getName(), score.getValue(), score.getAbsolute(), vConf);
		
		if (vConf != null && vConf.getAttributesConfig() != null) {
			AttributeConfig attrConfig = vConf.getAttributesConfig().getAttributeConfigByKey(score.getName());
			if (attrConfig == null) {
				dedicatedLogger.warn("Scoring inversion check failed: No AttributeConfig found for score '{}'. Check vertical config.", score.getName());
			} else if (AttributeComparisonRule.LOWER.equals(attrConfig.getImpactBetterIs())) {
				relValue = resolveScaleMax(attrConfig) + resolveScaleMin(attrConfig) - relValue;
			}
		}

		ret.setValue(relValue);

		score.setRelativ(ret);
		
	}

	protected Double relativizeScoreValue(String scoreName, Double value, Cardinality abs, VerticalConfig vConf)
			throws ValidationException {
		AttributeConfig attributeConfig = resolveAttributeConfig(scoreName, vConf);
		ScoreNormalizationMethod method = resolveNormalizationMethod(attributeConfig);
		if (method == null) {
			if (shouldUsePercentileScoring(scoreName, vConf)) {
				logLegacyScoring(scoreName);
				return relativizeUsingPercentile(scoreName, value, abs);
			}
			logLegacyScoring(scoreName);
			return relativize(value, abs);
		}

		NormalizationStrategy strategy = NormalizationStrategyFactory.strategyFor(method);
		if (strategy == null) {
			throw new ValidationException("Unknown normalization method for score " + scoreName);
		}

		NormalizationContext context = new NormalizationContext(abs, valueFrequencies.get(scoreName));
		NormalizationResult result = strategy.normalize(value, context, attributeConfig);
		if (result.legacy()) {
			logLegacyScoring(scoreName);
		}
		return result.value();
	}

	private boolean shouldUsePercentileScoring(String scoreName, VerticalConfig vConf) {
		if (vConf == null || vConf.getImpactScoreConfig() == null) {
			return false;
		}

		Integer minDistinctValues = vConf.getImpactScoreConfig().getMinDistinctValuesForSigma();
		if (minDistinctValues == null || minDistinctValues <= 0) {
			return false;
		}

		Map<Double, Integer> frequencies = valueFrequencies.get(scoreName);
		if (frequencies == null) {
			return false;
		}

		return frequencies.size() < minDistinctValues;
	}

	private Double relativizeUsingPercentile(String scoreName, Double value, Cardinality abs) throws ValidationException {
		if (value == null) {
			throw new ValidationException("Empty value in relativization");
		}

		Map<Double, Integer> frequencies = valueFrequencies.get(scoreName);
		if (frequencies == null || frequencies.isEmpty()) {
			return StandardiserService.DEFAULT_MAX_RATING / 2.0;
		}

		Integer totalCount = abs.getCount();
		if (totalCount == null || totalCount == 0) {
			return StandardiserService.DEFAULT_MAX_RATING / 2.0;
		}

		int countBelow = 0;
		int countAt = 0;

		for (Map.Entry<Double, Integer> entry : frequencies.entrySet()) {
			int comparison = Double.compare(entry.getKey(), value);
			if (comparison < 0) {
				countBelow += entry.getValue();
			} else if (comparison == 0) {
				countAt += entry.getValue();
			}
		}

		double percentile = (countBelow + (0.5 * countAt)) / totalCount;
		double scaled = percentile * StandardiserService.DEFAULT_MAX_RATING;

		return Math.max(0.0, Math.min(StandardiserService.DEFAULT_MAX_RATING, scaled));
	}


        private Integer relativize(Integer count, Cardinality absolute) throws ValidationException{
		
                return relativize(Double.valueOf(count), absolute).intValue();
        }

        /**
         * Hook used to let subclasses override the absolute value stored for a score.
         */
        protected Double resolveAbsoluteValue(Product product, String scoreName, Score score) {
                return score.getValue();
        }


	/**
	 * Relatives a number on a 0 - StandardiserService.DEFAULT_MAX_RATING scale, given the absolute min and max in the provided cardinality
	 * Uses a Sigma (Standard Deviation) approach to handle outliers:
     * Range is [Mean - 2*Sigma, Mean + 2*Sigma]
	 * @param value
	 * @param abs
	 * @return
	 * @throws ValidationException 
	 */
	public Double relativize(Double value, Cardinality abs) throws ValidationException {
		if (null == value) {
			throw new ValidationException("Empty value in relativization");
		}
        
        Double mean = abs.getAvg();
        Double sigma = abs.getStdDev();
        
        // Handle zero variance case (all values are identical)
        if (sigma == 0.0) {
            return StandardiserService.DEFAULT_MAX_RATING / 2.0; // Return middle score (2.5)
        }
        
        // Sigma Factor (Sensitivity). k=2 covers ~95% of distribution.
        double k = 2.0;
        
        double lowerBound = mean - (k * sigma);
        double upperBound = mean + (k * sigma);
        
        // Avoid division by zero if bounds are effectively equal (floating point safety)
        if (Math.abs(upperBound - lowerBound) < 0.000001) {
             return StandardiserService.DEFAULT_MAX_RATING / 2.0;
        }

        // Normalize to [0, 1] relative to the bounds
        double normalized = (value - lowerBound) / (upperBound - lowerBound);
        
        // Scale to [0, MaxRating]
        double scaled = normalized * StandardiserService.DEFAULT_MAX_RATING;
        
        // Clamp result to [0, MaxRating] to handle outliers
        return Math.max(0.0, Math.min(StandardiserService.DEFAULT_MAX_RATING, scaled));
    }
	
        /**
         * Computes and maintains cardinality for both absolute and relative views.
         * @param scoreName the score identifier
         * @param value the value to register
         */
        protected void incrementCardinality(String scoreName, Double value, VerticalConfig vConf) throws ValidationException {


                if (null == value) {
                        throw new ValidationException("Empty value for Score " + scoreName + " ! Consider normalizing in a futur export/import phase");
                }

                Cardinality absolute = absoluteCardinalities.get(scoreName);
                if (null == absolute) {
                        absolute = new Cardinality();
                }

                Cardinality relative = batchDatas.get(scoreName);
                if (null == relative) {
                        relative = new Cardinality();
                }

                absolute.increment(value);
                relative.increment(value);
                if (shouldTrackFrequencies(scoreName, vConf)) {
                        incrementValueFrequency(scoreName, value);
                }

                absoluteCardinalities.put(scoreName, absolute);
                batchDatas.put(scoreName, relative);
        }

        private void incrementValueFrequency(String scoreName, Double value) {
                Map<Double, Integer> frequencies = valueFrequencies.computeIfAbsent(scoreName, key -> new HashMap<>());
                Integer current = frequencies.get(value);
                if (current == null) {
                        frequencies.put(value, 1);
                        return;
                }
                frequencies.put(value, current + 1);
        }

        private boolean shouldTrackFrequencies(String scoreName, VerticalConfig vConf) {
                AttributeConfig attributeConfig = resolveAttributeConfig(scoreName, vConf);
                ScoreNormalizationMethod method = resolveNormalizationMethod(attributeConfig);
                if (method == null) {
                        if (vConf == null || vConf.getImpactScoreConfig() == null) {
                                return false;
                        }
                        Integer minDistinctValues = vConf.getImpactScoreConfig().getMinDistinctValuesForSigma();
                        return minDistinctValues != null && minDistinctValues > 0;
                }
                return ScoreNormalizationMethod.PERCENTILE.equals(method)
                        || ScoreNormalizationMethod.MINMAX_QUANTILE.equals(method);
        }

        private AttributeConfig resolveAttributeConfig(String scoreName, VerticalConfig vConf) {
                if (vConf == null || vConf.getAttributesConfig() == null) {
                        return null;
                }
                return vConf.getAttributesConfig().getAttributeConfigByKey(scoreName);
        }

        private ScoreNormalizationMethod resolveNormalizationMethod(AttributeConfig attributeConfig) {
                if (attributeConfig == null) {
                        return null;
                }
                ScoreScoringConfig scoring = attributeConfig.getScoring();
                if (scoring == null || scoring.getNormalization() == null) {
                        return null;
                }
                return scoring.getNormalization().getMethod();
        }

        private void logLegacyScoring(String scoreName) {
                if (legacyScoringLogged.add(scoreName)) {
                        dedicatedLogger.warn("Legacy scoring applied for score '{}'. Please migrate to scoring.normalization.method.", scoreName);
                }
        }

        private double resolveScaleMax(AttributeConfig attributeConfig) {
                if (attributeConfig == null || attributeConfig.getScoring() == null
                        || attributeConfig.getScoring().getScale() == null
                        || attributeConfig.getScoring().getScale().getMax() == null) {
                        return StandardiserService.DEFAULT_MAX_RATING;
                }
                return attributeConfig.getScoring().getScale().getMax();
        }

        private double resolveScaleMin(AttributeConfig attributeConfig) {
                if (attributeConfig == null || attributeConfig.getScoring() == null
                        || attributeConfig.getScoring().getScale() == null
                        || attributeConfig.getScoring().getScale().getMin() == null) {
                        return 0.0;
                }
                return attributeConfig.getScoring().getScale().getMin();
        }





}
