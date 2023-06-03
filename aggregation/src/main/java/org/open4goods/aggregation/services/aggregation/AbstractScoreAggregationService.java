package org.open4goods.aggregation.services.aggregation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.model.attribute.Cardinality;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.Product;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractScoreAggregationService extends  AbstractAggregationService{

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractScoreAggregationService.class);

	private Map<String, Cardinality>  batchDatas = new HashMap<>();
	
	
	public AbstractScoreAggregationService(String logsFolder) {
		super(logsFolder);
	}



	@Override
	public void init(Set<Product> datas) {
		super.init(datas);
		batchDatas.clear();
	}

	
	@Override
	public void done(Set<Product> datas) {
		super.done(datas);


		////////////////////////
		// Scores relativisation 
		////////////////////////
		for (Product p : datas) {			
			for (String scoreName : batchDatas.keySet()) {
				Score s = p.getScores().get(scoreName);
				if (null != s) {
					s.setCardinality(relativize(s));									
				}
			}			
		}
		
		//////////////////////////
		// Virtual scores computing
		//////////////////////////
		for (Product p : datas) {			
			for (String scoreName : batchDatas.keySet()) {
				Score s = p.getScores().get(scoreName);
				if (null == s) {
					// Need a virtual score
					s = new Score();
					s.setName(scoreName);
					s.setVirtual(true);
										
					Cardinality ret = new Cardinality();
					Cardinality cardinality = batchDatas.get(scoreName);
					
					ret.setMax(cardinality.getMax());
					ret.setMin(cardinality.getMin());
					ret.setAvg(cardinality.getAvg());
					ret.setCount(cardinality.getCount());
					ret.setSum(cardinality.getSum());
					
					ret.setRelValue(cardinality.getAvg());					
					s.setCardinality(ret);					
				}
			}			
		}

	
	}
	
	/////////////////////////////////////////
	// Private methods
	/////////////////////////////////////////
	/**
	 * Computes relativ values
	 * @param score
	 */
	protected Cardinality relativize(Score score) {

		// Substracting unused min

		if (null == score.getValue()) {
			LOGGER.warn("Empty value for Score {} ! Consider normalizing in a futur export/import phase",score);
			return null;
		}
		
		Cardinality cardinality =  batchDatas.get(score.getName());

		if (null == cardinality) {
			LOGGER.warn("No cardinality found for score {}",score);
			return null;
		}
		
		
		Cardinality ret = new Cardinality();
		ret.setMax(cardinality.getMax());
		ret.setMin(cardinality.getMin());
		ret.setAvg(cardinality.getAvg());
		ret.setCount(cardinality.getCount());
		ret.setSum(cardinality.getSum());
		
		try {
			// Removing the min range
			Double minBorn = cardinality.getMin() - score.getMin();

			// Standardizing Score based on real max
			final Double max = cardinality.getMax();

			final Double value = score.getValue();
			ret.setRelValue((value -minBorn) * StandardiserService.DEFAULT_MAX_RATING / (max -minBorn));

		} catch (Exception e) {
			LOGGER.warn("Relativisation failed",e);
		}

		return ret;
	}


	/**
	 * Computes and maintains cardinality
	 * @param Scores
	 * @param batchDatas
	 */
	protected void processCardinality(Score score) {


		if (null == score || null == score.getValue()) {
			LOGGER.warn("Empty value for Score {} ! Consider normalizing in a futur export/import phase",score);
			return;
		}

		// Retrieving cardinality
		Cardinality c =  batchDatas.get(score.getName());
		if (null == c) {
			c = new Cardinality();
		}

		// Incrementing
		c.increment(score);

		batchDatas.put(score.getName(),c);
	}

}
