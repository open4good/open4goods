package org.open4goods.aggregation.services.aggregation;

import java.util.Collection;

import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.ui.AttributesConfig;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.AggregatedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.services.StandardiserService;

public class Attribute2ScoreAggregationService extends AbstractScoreAggregationService {

	private final AttributesConfig attributesConfig;

	public Attribute2ScoreAggregationService(final AttributesConfig attributesConfig,  final String logsFolder) {
		super(logsFolder);
		this.attributesConfig = attributesConfig;
	}



	@Override
	public void onProduct(Product data) {

		/////////////////////////////////////////
		// Update referentiel attributes
		/////////////////////////////////////////

		// 2 - Classifying "matched/unmatched" attributes
	

		// 3 - Applying attribute transformations on matched ones
		//TODO : handle conflicts
		// TODO : attribute removing by name from conf
		
		
		Collection<AggregatedAttribute> aggattrs =    data.getAttributes().getAggregatedAttributes().values()  ;
		for (AggregatedAttribute aga : aggattrs) {
			// Scoring from attribute
			Score score = generateScoresFromAttribute(aga.getName() ,aga);
			
			// Processing cardinality
			processCardinality(score);
			
			// Saving in product
			data.getScores().put(score.getName(), score);
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
		if (ac.getNumericMapping().size() > 0) {
			try {
				// This is a numeric mapping
				Score r = new Score();
				r.setName(a.getName());
				r.setMax(ac.maxRating());
				r.setMin(ac.minRating());
				
				r.setValue(ac.getNumericMapping().get(a.getValue()));

				if (null == r.getValue()) {
					dedicatedLogger.warn("No matching found in numericMappings for attribute {} and value  {}",ac,a.getValue());
					return null;
				}

				// Standardization (re-scaling)
				StandardiserService.standarise(r);

				// Adding
				return r;

			} catch (NoSuchFieldException | ValidationException e) {
				dedicatedLogger.warn("Attribute to rating conversion failed : {}",e.getMessage());
			}

		} else {
			dedicatedLogger.error("Was asking to  translate {} into rating, but no numericMapping definition !",a);
		}




		return null;


	}




}
