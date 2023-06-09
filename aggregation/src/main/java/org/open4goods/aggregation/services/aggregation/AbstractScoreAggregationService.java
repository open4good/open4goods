package org.open4goods.aggregation.services.aggregation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
		////////////////////////
		// Scores relativisation 
		////////////////////////
		for (Product p : datas) {			
			for (String scoreName : batchDatas.keySet()) {
				Score s = p.getScores().get(scoreName);
				if (null != s) {
					relativize(s);									
				}
			}			
		}
		
		
		dedicatedLogger.info("{} -> Virtual score computing for {} products", this.getClass().getSimpleName(),datas.size());
		
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
										
					
					s.setRelativValue(batchDatas.get(scoreName).getAvg());		
					relativize(s);
//					s.getCardinality().setValue(batchDatas.get(scoreName).getAvg());
					
					p.getScores().put(scoreName, s);
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
	protected void relativize(Score score) {

		// Substracting unused min

		if (null == score.getRelativValue()) {
			LOGGER.warn("Empty value for Score {} ! Consider normalizing in a futur export/import phase",score);
			return ;
		}
		
		Cardinality cardinality =  batchDatas.get(score.getName());

		if (null == cardinality) {
			LOGGER.warn("No cardinality found for score {}",score);
			return ;
		}
		
		
		Cardinality ret = new Cardinality();
		ret.setMax(cardinality.getMax());
		ret.setMin(cardinality.getMin());
		ret.setAvg(cardinality.getAvg());
		ret.setCount(cardinality.getCount());
		ret.setSum(cardinality.getSum());
		
		score.setCardinality(ret);
		try {
			// Removing the min range
			Double minBorn = cardinality.getMin();

			// Standardizing Score based on real max
			final Double max = cardinality.getMax();

			final Double value = score.getRelativValue();
			score.setRelativValue((value -minBorn) * StandardiserService.DEFAULT_MAX_RATING / (max -minBorn));

		} catch (Exception e) {
			LOGGER.warn("Relativisation failed",e);
		}
	}


	/**
	 * Computes and maintains cardinality
	 * @param Scores
	 * @param batchDatas
	 */
	protected void processCardinality(Score score) {


		if (null == score || null == score.getRelativValue()) {
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
