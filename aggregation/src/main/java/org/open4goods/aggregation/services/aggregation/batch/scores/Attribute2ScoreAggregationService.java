package org.open4goods.aggregation.services.aggregation.batch.scores;

import java.util.Collection;

import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.ui.AttributesConfig;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.AggregatedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.services.StandardiserService;

public class Attribute2ScoreAggregationService extends AbstractScoreAggregationService {

	private final AttributesConfig attributesConfig;

	public Attribute2ScoreAggregationService(final AttributesConfig attributesConfig,  final String logsFolder,boolean toConsole) {
		super(logsFolder, toConsole);
		this.attributesConfig = attributesConfig;
	}

	
	
	@Override
	public void onProduct(Product data) {

		
		Collection<AggregatedAttribute> aggattrs =    data.getAttributes().getAggregatedAttributes().values()  ;
		for (AggregatedAttribute aga : aggattrs) {
			// Scoring from attribute
			if (attributesConfig.getAttributeConfigByKey(aga.getName()).isAsRating()) {
					try {
						Double score = generateScoresFromAttribute(aga.getName() ,aga);

						// Processing cardinality
						processCardinality(aga.getName(),score);
						
						Score s = new Score(aga.getName(), score);
						// Saving in product
						data.getScores().put(s.getName(),s);
					} catch (ValidationException e) {
						dedicatedLogger.warn("Attribute to score fail for {}",aga,e);
					}									
				
			}
		}
	}



	/**
	 * Generate the score (min, max, value) from an aggregatedattribute
	 * @param attributeKey
	 * @param a
	 * @return
	 */
	public Double generateScoresFromAttribute(String attributeKey , AggregatedAttribute a) throws ValidationException{

		AttributeConfig ac = attributesConfig.getAttributeConfigByKey(attributeKey);
		// transformation required

		if (null == ac) {
			throw new ValidationException("No attribute config for " + attributeKey);
		}
		
		if (ac.getType().equals(AttributeType.NUMERIC)) {
			try {
				return Double.valueOf(a.getValue());
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
