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
public abstract class AbstractScoreAggregationService extends  AbstractAggregationService{


	protected Map<String, Cardinality>  batchDatas = new HashMap<>();
	
	
	public AbstractScoreAggregationService(Logger logger) {
		super(logger);
	}


	@Override
	public void init(Collection<Product> datas) {
		super.init(datas);
		batchDatas.clear();
	}

	
	@Override
	public void done(Collection<Product> datas, VerticalConfig vConf) {
		super.done(datas,vConf);

		dedicatedLogger.info("{} -> Scores relativisation for {} products", this.getClass().getSimpleName(), datas.size());

		//////////////////////////
		// Virtual scores computing
		// Operated on absolute values
		//////////////////////////
                for (Product p : datas) {
                        for (String scoreName : batchDatas.keySet()) {
                                Score s = p.getScores().get(scoreName);
                                Cardinality source = batchDatas.get(scoreName);
                                Cardinality virtual = new Cardinality(source);
                                if (null == s) {


                                        // Need a virtual score
                                        s = new Score(scoreName, source.getAvg());
                                        s.setName(scoreName);
                                        s.setVirtual(true);

                                }
                                virtual.setValue(s.getValue());
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
						relativize(s);
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
         * @throws ValidationException if required absolute values are missing
         */
        protected void relativize(Score score) throws ValidationException {

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
                ret.setValue(relativize(score.getValue(),score.getAbsolute()));

		score.setRelativ(ret);
		
	}


	private Integer relativize(Integer count, Cardinality absolute) throws ValidationException{
		
		return relativize(Double.valueOf(count), absolute).intValue();
	}


	/**
	 * Relatives a number on a 0 - StandardiserService.DEFAULT_MAX_RATING scale, given the absolute min and max in the provided cardinality
	 * @param value
	 * @param abs
	 * @return
	 * @throws ValidationException 
	 */
	public Double relativize(Double value, Cardinality abs) throws ValidationException {
		if (null == value) {
			throw new ValidationException("Empty value in relativization");
		}
		// Removing the min range
		Double minBorn = abs.getMin();
		// Standardizing Score based on real max
		final Double max = abs.getMax();
		
		if (minBorn == max) {
			return value.doubleValue() == max.doubleValue()  ? StandardiserService.DEFAULT_MAX_RATING : (value -minBorn) * StandardiserService.DEFAULT_MAX_RATING ;
		} else {
			
			return (value -minBorn) * StandardiserService.DEFAULT_MAX_RATING / (max -minBorn);
		}
	}
	
	/**
	 * Computes and maintains cardinality
	 * @param Scores
	 * @param batchDatas
	 */
	protected void incrementCardinality(String scoreName, Double value) throws ValidationException{


		if (null == value) {
			throw new ValidationException("Empty value for Score "+scoreName+" ! Consider normalizing in a futur export/import phase");
		}

		// Retrieving cardinality
		Cardinality c =  batchDatas.get(scoreName);
		if (null == c) {
			c = new Cardinality();
		}

		// Incrementing
		c.increment(value);

		batchDatas.put(scoreName,c);
	}






}
