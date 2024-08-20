package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;

/**
 * Create an ecoscore based on existing scores, (see yaml config files)
 * @author goulven
 *
 */
public class EcoScoreAggregationService extends AbstractScoreAggregationService {

	private static final String ECOSCORE_SCORENAME = "ECOSCORE";

	public EcoScoreAggregationService(final Logger logger) {
		super(logger);
	}



	@Override
	public void onProduct(Product data, VerticalConfig vConf) {
		if (StringUtils.isEmpty(data.brand())) {
			return;
		}
		
		try {

			// Compute the ecoscore from existing scores
			Double score = generateEcoScore(data.getScores(),vConf);

			// Processing cardinality
			incrementCardinality(ECOSCORE_SCORENAME,score);
			
			// Saving the actual score in the product, it will be relativized after this batch (see super().done())
			Score s = new Score(ECOSCORE_SCORENAME, score);
			data.getScores().put(s.getName(),s);
		} catch (ValidationException e) {
			dedicatedLogger.warn("Brand to score fail for {} : {}",data,e.getMessage());
		}								
		
	}



	private Double generateEcoScore(Map<String, Score> scores, VerticalConfig vConf) throws ValidationException {
		
		
		double va = 0.0;
		for (String config :  vConf.getEcoscoreConfig().keySet()) {
			Score score = scores.get(config);
			
			if (null == score) {
				throw new ValidationException ("EcoScore rating cannot proceed, missing subscore : " + config);
			} 			
			
		
			// Taking on the relativ
			va += score. getRelativ().getValue() * Double.valueOf(vConf.getEcoscoreConfig().get(config));
		}

		return va;
	}


	@Override
	public void done(Collection<Product> datas) {
		super.done(datas);
		
		///////////////////////
		// EcoScore ranking and "best alternativ" reach
		///////////////////////
		List<Product> sorted = new ArrayList<>();
		sorted.addAll(datas);

		Collections.sort(sorted, (o1, o2) -> Double.compare(o1.ecoscore().getRelativ().getValue() , o2.ecoscore().getRelativ().getValue()));
		
		int count = sorted.size();
		for (int i = 0; i < count; i++) {
			Product p = sorted.get(i);
			p.getRanking().setGlobalCount(count);
			p.getRanking().setGlobalPosition(count - i);
			p.getRanking().setGlobalBest(sorted.getLast().getId());
			
			if (i < count - 1) {
				p.getRanking().setGlobalBetter(sorted.get(i+1).getId());
			}

		}
		
		
		
	}


}
