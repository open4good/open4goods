package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Base class for batch score aggregation services. It manages lifecycle hooks,
 * cardinality accumulation, virtual score generation and relativisation for a
 * collection of products handled in a batch.
 */
public abstract class AbstractScoreAggregationService extends AbstractAggregationService {


        protected Map<String, Cardinality> batchDatas = new HashMap<>();

        protected Map<String, Cardinality> absoluteCardinalities = new HashMap<>();


        public AbstractScoreAggregationService(Logger logger) {
                super(logger);
        }


        @Override
        public void init(Collection<Product> datas) {
                super.init(datas);
                batchDatas.clear();
                absoluteCardinalities.clear();
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
				
				// Putting in the worse bag if match
				if (i < vConf.getWorseLimit()) {
					d.getWorsesScores().add(scoreName);
				}
				
				// Putting in the best bag if match
				if (i >  sorted.size() -  vConf.getBettersLimit()) {
					d.getBestsScores().add(scoreName);
				}
				
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
		ret.setMax(relativize(cardinality.getMax(),score.getAbsolute()));
		ret.setMin(relativize(cardinality.getMin(),score.getAbsolute()));
		ret.setAvg(relativize(cardinality.getAvg(),score.getAbsolute()));
		// Keep occurrence statistics absolute to avoid distorting
		// aggregation insights while still scaling comparable values.
		ret.setCount(cardinality.getCount());
		ret.setSum(cardinality.getSum());
		
		Double relValue = relativize(score.getValue(),score.getAbsolute());
		
		if (vConf != null) {
			org.open4goods.model.vertical.AttributeConfig attrConfig = vConf.getAttributesConfig().getAttributeConfigByKey(score.getName());
			if (attrConfig != null && org.open4goods.model.vertical.AttributeComparisonRule.LOWER.equals(attrConfig.getBetterIs())) {
				relValue = StandardiserService.DEFAULT_MAX_RATING - relValue;
			}
		}

		ret.setValue(relValue);

		score.setRelativ(ret);
		
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
        protected void incrementCardinality(String scoreName, Double value) throws ValidationException {


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

                absoluteCardinalities.put(scoreName, absolute);
                batchDatas.put(scoreName, relative);
        }






}
