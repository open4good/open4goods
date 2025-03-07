package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;
import java.util.List;

import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

public class Attribute2ScoreAggregationService extends AbstractScoreAggregationService {


	public Attribute2ScoreAggregationService(final Logger logger) {
		super(logger);
	}

	
	
	@Override
	public void onProduct(Product data, VerticalConfig vConf) {

		
		Collection<IndexedAttribute> aggattrs = data.getAttributes().getIndexed().values()  ;
		for (IndexedAttribute aga : aggattrs) {
			// Scoring from attribute
			try {
				
				AttributesConfig attributesConfig = vConf.getAttributesConfig();
				// TODO(p1, design) : check
				AttributeConfig attrConfig = attributesConfig.getAttributeConfigByKey(attributesConfig.getKeyForValue(aga.getName()));
				if (null == attrConfig) {
                    dedicatedLogger.error("No attribute config for {}",aga);
                    continue;
                }
				
				if (attrConfig.isAsScore()) {
						try {
							Double score = generateScoresFromAttribute(aga.getName() ,aga, vConf.getAttributesConfig());

							// Processing cardinality
							incrementCardinality(aga.getName(),score);
							
							Score s = new Score(aga.getName(), score);
							// Saving in product
							data.getScores().put(s.getName(),s);
						} catch (ValidationException e) {
							dedicatedLogger.warn("Attribute to score fail for {}",aga,e);
						}									
					
				}
			} catch (Exception e) {
				dedicatedLogger.error("Error while processing attribute {}",aga);
			}
		}
	}



	/**
	 * Generate the score (min, max, value) from an aggregatedattribute
	 * @param attributeKey
	 * @param aga
	 * @return
	 */
	public Double generateScoresFromAttribute(String attributeKey , IndexedAttribute aga, AttributesConfig attributesConfig) throws ValidationException{

		AttributeConfig ac = attributesConfig.getAttributeConfigByKey(attributeKey);
		// transformation required

		if (null == ac) {
			throw new ValidationException("No attribute config for " + attributeKey);
		}
		
		if (ac.getFilteringType().equals(AttributeType.NUMERIC)) {
			try {
				return Double.valueOf(aga.getValue().replace(",", "."));
			} catch (Exception e) {
				throw new ValidationException("Cannot convert to numeric" +aga);
			}
			
		} else if (ac.getNumericMapping().size() > 0) {
				Double mapping = ac.getNumericMapping().get(aga.getValue());
				if (null == mapping || mapping.isInfinite() || mapping.isNaN()) {
					throw new ValidationException("Attribute to rating conversion failed "+aga);
				}
				return mapping;
		} else {
			throw new ValidationException("Was asking to  translate {} into rating, but no numericMapping definition nor numeric attribute found : " + aga);
		}

	}
	
	@Override
	public void done(Collection<Product> datas, VerticalConfig vConf) {

		/////////////////////////////////////////////////
		//  Reversing the scores that need to be (ie. weight, electric consumption : lower values are the best scored)
		// To reverse a score, we substract max absolute score from item absolute score 
		/////////////////////////////////////////////////
		
		// Selecting the scores to reverse
		List<String> scoresToReverse = vConf.getAttributesConfig().getConfigs().stream().filter(e->e.isReverseScore()).map(e->e.getKey()).toList();
		
		// For each score to reverse
		for (String key : scoresToReverse) {
			
			// TODO : Check all cardinality are equivalent
			// We iterate on every score, and we update score abs value and batchdatas abs cardinality
			for (Product p : datas) {
				if (!p.getScores().containsKey(key)) {
					dedicatedLogger.info("No score {} for {}",key,p);
				} else {
					Double value = p.getScores().get(key).getValue();
					Double max = batchDatas.get(key).getMax();
					Double reversed = max - value;
					
					p.getScores().get(key).setValue(reversed);				
					batchDatas.get(key).setValue(reversed);
					
					dedicatedLogger.info("Score {} reversed for {}. Original was {}, max is {}. New value is {}",key, p, value,max, reversed );
				}
				
			}
		}
		
		// SUPER important : Score relativisation is operated in the AbstactScoreAggService
		super.done(datas, vConf);
	}

}
