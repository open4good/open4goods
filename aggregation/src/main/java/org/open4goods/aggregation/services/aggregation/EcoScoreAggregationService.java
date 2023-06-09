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

/**
 * Create an ecoscore based on existing scores aggregations (based on config)
 * @author goulven
 *
 */
public class EcoScoreAggregationService extends AbstractScoreAggregationService {

	private static final String ECOSCORE_SCORENAME = "ECOSCORE";
	private final Map<String, String> ecoScoreconfig;

	public EcoScoreAggregationService(final Map<String, String> ecoScoreconfig,  final String logsFolder,boolean toConsole) {
		super(logsFolder, toConsole);
		this.ecoScoreconfig = ecoScoreconfig;
	}



	@Override
	public void onProduct(Product data) {

		
		
		if (StringUtils.isEmpty(data.brand())) {
			return;
		}
		
		try {
			Double score = generateEcoScore(data.getScores());

			// Processing cardinality
			processCardinality(ECOSCORE_SCORENAME,score);			
			Score s = new Score(ECOSCORE_SCORENAME, score);
			// Saving in product
			data.getScores().put(s.getName(),s);
		} catch (ValidationException e) {
			dedicatedLogger.warn("Brand to score fail for {}",data,e);
		}								
		
	}



	private Double generateEcoScore(Map<String, Score> scores) throws ValidationException {
		
		
		Double va = 0.0;
		for (String config :  ecoScoreconfig.keySet()) {
			Score score = scores.get(config);
			
			if (null == score) {
				throw new ValidationException ("EcoScore rating cannot proceed, missing subscore : " + config);
			} 			
			va += score.getValue() * Double.valueOf(ecoScoreconfig.get(config));
		}
		
		
	
		
		return va;
	}




}
