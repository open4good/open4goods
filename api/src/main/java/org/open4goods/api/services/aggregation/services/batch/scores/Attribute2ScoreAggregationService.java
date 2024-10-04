package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Collection;
import java.util.Map;

import org.open4goods.commons.config.yml.attributes.AttributeConfig;
import org.open4goods.commons.config.yml.ui.AttributesConfig;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.model.attribute.AttributeType;
import org.open4goods.commons.model.data.Score;
import org.open4goods.commons.model.product.AggregatedAttribute;
import org.open4goods.commons.model.product.Product;
import org.slf4j.Logger;

public class Attribute2ScoreAggregationService extends AbstractScoreAggregationService {


	public Attribute2ScoreAggregationService(final Logger logger) {
		super(logger);
	}

	
	
	@Override
	public Map<String, Object> onProduct(Product data, VerticalConfig vConf) {

		
		Collection<AggregatedAttribute> aggattrs =    data.getAttributes().getAggregatedAttributes().values()  ;
		for (AggregatedAttribute aga : aggattrs) {
			// Scoring from attribute
			try {
				
				AttributesConfig attributesConfig = vConf.getAttributesConfig();
				AttributeConfig attrConfig = attributesConfig.getAttributeConfigByKey(attributesConfig.getKeyForValue(aga.getName()));
				if (null == attrConfig) {
                    dedicatedLogger.error("No attribute config for {}",aga);
                    continue;
                }
				
				if (attrConfig.isAsRating()) {
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
		return null;
	}



	/**
	 * Generate the score (min, max, value) from an aggregatedattribute
	 * @param attributeKey
	 * @param a
	 * @return
	 */
	public Double generateScoresFromAttribute(String attributeKey , AggregatedAttribute a, AttributesConfig attributesConfig) throws ValidationException{

		AttributeConfig ac = attributesConfig.getAttributeConfigByKey(attributeKey);
		// transformation required

		if (null == ac) {
			throw new ValidationException("No attribute config for " + attributeKey);
		}
		
		if (ac.getType().equals(AttributeType.NUMERIC)) {
			try {
				return Double.valueOf(a.getValue().replace(",", "."));
			} catch (Exception e) {
				throw new ValidationException("Cannot convert to numeric" +a);
			}
			
		} else if (ac.getNumericMapping().size() > 0) {
				Double mapping = ac.getNumericMapping().get(a.getValue());
				if (null == mapping || mapping.isInfinite() || mapping.isNaN()) {
					throw new ValidationException("Attribute to rating conversion failed "+a);
				}
				return mapping;
		} else {
			throw new ValidationException("Was asking to  translate {} into rating, but no numericMapping definition nor numeric attribute found : " + a);
		}

	}

}
