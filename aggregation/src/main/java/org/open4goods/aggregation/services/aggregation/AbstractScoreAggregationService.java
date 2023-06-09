package org.open4goods.aggregation.services.aggregation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.attribute.Cardinality;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.Product;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractScoreAggregationService extends  AbstractAggregationService{

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractScoreAggregationService.class);

	private Map<String, Cardinality>  batchDatas = new HashMap<>();
	
	
	public AbstractScoreAggregationService(String logsFolder, boolean toConsole) {
		super(logsFolder, toConsole);
	}


	@Override
	public void init(Collection<Product> datas) {
		super.init(datas);
		batchDatas.clear();
	}

	
	@Override
	public void done(Collection<Product> datas) {
		super.done(datas);


		
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

					s.setAbsolute(virtual);
					
				} else {
					virtual.setValue(s.getValue());
					s.setAbsolute(virtual);				
				}
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
		
		
		dedicatedLogger.info("{} -> Virtual score computing for {} products", this.getClass().getSimpleName(),datas.size());
		

	}
	
	/////////////////////////////////////////
	// Private methods
	/////////////////////////////////////////
	/**
	 * Computes relativ values
	 * @param score
	 * @throws ValidationException 
	 */
	protected void relativize(Score score) throws ValidationException {

		// Substracting unused min

		if (null == score.getAbsolute()) {
			LOGGER.warn("Empty value for Score {} ! Consider normalizing in a futur export/import phase",score);
			return ;
		}
		
		Cardinality cardinality =  batchDatas.get(score.getName());

		if (null == cardinality) {
			LOGGER.warn("No source cardinality found for score {}",score);
			return ;
		}
		
		
		Cardinality ret = new Cardinality();
		ret.setMax(relativize(cardinality.getMax(),score.getAbsolute()));
		ret.setMin(relativize(cardinality.getMin(),score.getAbsolute()));
		ret.setAvg(relativize(cardinality.getAvg(),score.getAbsolute()));
		ret.setCount(relativize(cardinality.getCount(),score.getAbsolute()));
		ret.setSum(relativize(cardinality.getSum(),score.getAbsolute()));
		ret.setValue(relativize(score.getValue(),score.getAbsolute()));

		score.setRelativ(ret);
		
		try {


		} catch (Exception e) {
			LOGGER.warn("Relativisation failed",e);
		}
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
	protected void processCardinality(String scoreName, Double value) throws ValidationException{


		if (null == value) {
			throw new ValidationException("Empty value for Score {} ! Consider normalizing in a futur export/import phase");
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
