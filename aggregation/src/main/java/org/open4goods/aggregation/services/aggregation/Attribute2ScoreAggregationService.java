package org.open4goods.aggregation.services.aggregation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.attributes.AttributeParser;
import org.open4goods.config.yml.ui.AttributesConfig;
import org.open4goods.exceptions.ParseException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Rating;
import org.open4goods.model.data.RatingType;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.AggregatedAttribute;
import org.open4goods.model.product.AggregatedAttributes;
import org.open4goods.model.product.AggregatedFeature;
import org.open4goods.model.product.IAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.SourcedAttribute;
import org.open4goods.services.StandardiserService;

// TODO : Deduplicate code beween datafragment and aggregateddata
public class Attribute2ScoreAggregationService extends AbstractScoreAggregationService {

	private final AttributesConfig attributesConfig;

	public Attribute2ScoreAggregationService(final AttributesConfig attributesConfig,  final String logsFolder) {
		super(logsFolder);
		this.attributesConfig = attributesConfig;
	}



	@Override
	public Product onAggregatedData(Product data, Set<Product> datas) {

		/////////////////////////////////////////
		// Update referentiel attributes
		/////////////////////////////////////////

		// 2 - Classifying "matched/unmatched" attributes
	

		// 3 - Applying attribute transformations on matched ones
		//TODO : handle conflicts
		// TODO : attribute removing by name from conf
		
		
		Collection<AggregatedAttribute> aggattrs =    data.getAttributes().getAggregatedAttributes().values()  ;
		for (AggregatedAttribute aga : aggattrs) {
			aga.setScore(generateScoresFromAttribute(aga.getName() ,aga));
		}
		return data;
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
		//TODO : handle conflicts

		Collection<AggregatedAttribute> aggattrs = (output.getAttributes().getAggregatedAttributes()).values();
		for (AggregatedAttribute aga : aggattrs) {
			aga.setScore(generateScoresFromAttribute(aga.getName() ,aga));
			dedicatedLogger.info("attribute {} : scored {} ",aga.getName(), aga.getScore());

		}


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

				r.setMax(ac.maxRating());
				r.setMin(ac.minRating().intValue());

				r.setValue(ac.getNumericMapping().get(a.getValue()));

				if (null == r.getValue()) {
					dedicatedLogger.warn("No matching found in numericMappings for attribute {} and value  {}",ac,a.getValue());
					return null;
				}


				// tags
				r.getTags().addAll(ac.getRatingTags());
				r.getTags().add(RatingType.FROM_ATTRIBUTE.toString());

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
