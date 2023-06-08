package org.open4goods.aggregation.services.aggregation;

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
	public void init(Collection<Product> datas) {
		super.init(datas);
		
		// TODO : Should be cleaned in a specific service
		for (Product d : datas) {
			d.getScores().clear();
		}
	}
	
	
	@Override
	public void onProduct(Product data) {

		
		Collection<AggregatedAttribute> aggattrs =    data.getAttributes().getAggregatedAttributes().values()  ;
		for (AggregatedAttribute aga : aggattrs) {
			// Scoring from attribute
			if (attributesConfig.getAttributeConfigByKey(aga.getName()).isAsRating()) {
				Score score = generateScoresFromAttribute(aga.getName() ,aga);
				if (null == score || null == score.getRelativValue()) {
					dedicatedLogger.error("Null score generated for attribute {}", aga);
				} else {
					// Processing cardinality
					processCardinality(score);
					
					// Saving in product
					data.getScores().put(score.getName(), score);									
				}
			}
		}
	}


	
	
	/**
	 * Associate and match a set of nativ attributes in a product
	 *
	 * @param d
	 * @param p
	 * @param match2
	 */
	@Override
	public void onDataFragment(final DataFragment d, final Product output) {

		// 3 - Applying attribute transformations on matched ones
		//TODO : No scoring in real time, but could be thinked to have score on non verticalised products. But be aware of not erasing the batched scores

//		Collection<AggregatedAttribute> aggattrs = (output.getAttributes().getAggregatedAttributes()).values();
//		for (AggregatedAttribute aga : aggattrs) {
//			aga.setScore(generateScoresFromAttribute(aga.getName() ,aga));
//			dedicatedLogger.info("attribute {} : scored {} ",aga.getName(), aga.getScore());
//
//		}


	}


	/**
	 * Generate the score (min, max, value) from an aggregatedattribute
	 * @param attributeKey
	 * @param a
	 * @return
	 */
	public Score generateScoresFromAttribute(String attributeKey , AggregatedAttribute a) {

		AttributeConfig ac = attributesConfig.getAttributeConfigByKey(attributeKey);
		// transformation required

		
		if (ac.getType().equals(AttributeType.NUMERIC)) {
			try {
				return fromScorableAttribute(a, ac, Double.valueOf(a.getValue()));
			} catch (NoSuchFieldException | ValidationException e) {
				dedicatedLogger.warn("Attribute to numeric conversion failed : {}",e.getMessage());
			}
			
		} else if (ac.getNumericMapping().size() > 0) {
			try {
				return fromScorableAttribute(a, ac, ac.getNumericMapping().get(a.getValue()));

			} catch (NoSuchFieldException | ValidationException e) {
				dedicatedLogger.warn("Attribute to rating conversion failed : {}",e.getMessage());
			}

		} else {
			dedicatedLogger.error("Was asking to  translate {} into rating, but no numericMapping definition nor numeric attribute found !",a);
		}

		return null;


	}



	private Score fromScorableAttribute(AggregatedAttribute a, AttributeConfig ac, Double value)
			throws ValidationException, NoSuchFieldException {
		// This is a numeric mapping
		Score r = new Score();
		r.setName(a.getName());
//		r.setMax(ac.maxRating());
//		r.setMin(ac.minRating());
		
		r.setRelativValue(value);

		if (null == r.getRelativValue()) {
			dedicatedLogger.warn("No matching found in numericMappings for attribute {} and value  {}",ac,a.getValue());
			return null;
		}

		// Adding
		return r;
	}




}
