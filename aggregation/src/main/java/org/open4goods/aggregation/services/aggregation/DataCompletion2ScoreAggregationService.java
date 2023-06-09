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
 * Create a score based on data quality (number of non virtual scores for this product)
 * @author goulven
 *
 */
public class DataCompletion2ScoreAggregationService extends AbstractScoreAggregationService {

	private static final String DATA_QUALITY_SCORENAME = "DATA-QUALITY";
	private final AttributesConfig attributesConfig;

	public DataCompletion2ScoreAggregationService(final AttributesConfig attributesConfig,  final String logsFolder,boolean toConsole) {
		super(logsFolder,toConsole);
		this.attributesConfig = attributesConfig;
	}



	@Override
	public void onProduct(Product data) {
		if (StringUtils.isEmpty(data.brand())) {
			return;
		}
		
		try {
			Double score = generateScoreFromDataquality(data.getScores());

			// Processing cardinality
			processCardinality(DATA_QUALITY_SCORENAME,score);			
			Score s = new Score(DATA_QUALITY_SCORENAME, score);
			// Saving in product
			data.getScores().put(s.getName(),s);
		} catch (ValidationException e) {
			dedicatedLogger.warn("DataQuality to score fail for {}",data,e);
		}								
		
		
	}


	/**
	 * The data score is the number of score that are not virtuals
	 * @param map
	 * @return
	 */
	private Double generateScoreFromDataquality(Map<String, Score> map) {
		
		return  Double.valueOf(map.values().stream().filter(e -> !e.getVirtual()).filter(e -> !e.getName().equals(DATA_QUALITY_SCORENAME)) .count());		
		
	}


}
