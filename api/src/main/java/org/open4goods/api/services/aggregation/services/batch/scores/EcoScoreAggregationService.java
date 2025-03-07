package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.VerticalConfig;
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
		
		try {

			if (null != vConf.getImpactScoreConfig() && vConf.getImpactScoreConfig().getCriteriasPonderation().size() > 0 ) {
				// Compute the ecoscore from existing scores
				Double score = generateEcoScore(data.getScores(),vConf);
				
				// Processing cardinality
				incrementCardinality(ECOSCORE_SCORENAME,score);
				
				// Saving the actual score in the product, it will be relativized after this batch (see super().done())
				Score s = new Score(ECOSCORE_SCORENAME, score);
				data.getScores().put(s.getName(),s);
			} else {
				dedicatedLogger.info("No ImpactScore defined for vertical",vConf);
			}
		} catch (ValidationException e) {
			dedicatedLogger.error("Ecoscore aggregation failed for {} : {}",data,e.getMessage());
		}
	}



	private Double generateEcoScore(Map<String, Score> scores, VerticalConfig vConf) throws ValidationException {
		
		
		double ecoscoreVal = 0.0;
		for (String config :  vConf.getImpactScoreConfig().getCriteriasPonderation().keySet()) {
			Score score = scores.get(config);
			
			if (null == score) {
				throw new ValidationException ("EcoScore rating cannot proceed, missing subscore : " + config);
			} 			
			
		
			// Taking on the relativ
			ecoscoreVal += score. getRelativ().getValue() * Double.valueOf(vConf.getImpactScoreConfig().getCriteriasPonderation().get(config));
		}

		return ecoscoreVal;
	}


	@Override
	public void done(Collection<Product> datas, VerticalConfig vConf) {

		if (null != vConf.getImpactScoreConfig() && vConf.getImpactScoreConfig().getCriteriasPonderation().size() > 0) {

			super.done(datas, vConf);

			///////////////////////
			// EcoScore ranking and "best alternativ" reach
			///////////////////////
			List<Product> sorted = new ArrayList<>();
			sorted.addAll(datas);

				Collections.sort(sorted, (o1, o2) -> Double.compare(o1.ecoscore().getRelativ().getValue(), o2.ecoscore().getRelativ().getValue()));

				int count = sorted.size();
				for (int i = 0; i < count; i++) {
					Product p = sorted.get(i);
					p.getRanking().setGlobalCount(count);
					p.getRanking().setGlobalPosition(count - i);
					p.getRanking().setGlobalBest(sorted.getLast().getId());

					if (i < count - 1) {
						p.getRanking().setGlobalBetter(sorted.get(i + 1).getId());
					}

				}
	
		} else {
			dedicatedLogger.error("No ImpactScore defined for vertical",vConf);
		}
		
		
	}


}
