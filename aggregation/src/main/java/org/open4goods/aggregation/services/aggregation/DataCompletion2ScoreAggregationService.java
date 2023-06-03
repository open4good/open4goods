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

	private final AttributesConfig attributesConfig;

	public DataCompletion2ScoreAggregationService(final AttributesConfig attributesConfig,  final String logsFolder) {
		super(logsFolder);
		this.attributesConfig = attributesConfig;
	}



	@Override
	public void onProduct(Product data) {

		// Scoring from attribute
		Score score = generateScoreFromDataquality(data.getScores());
		
		// Processing cardinality
		processCardinality(score);
		
		// Saving in product
		data.getScores().put(score.getName(), score);
		
	}


	// TODO : complete with real datas
	private Score generateScoreFromDataquality(Map<String, Score> map) {
		
		Score s = new Score();
		s.setName("DATA-QUALITY");
		s.setMax(20.0);
		s.setMin(0.0);
		s.setVirtual(false);
		s.setValue( Double.valueOf(map.values().stream().filter(e -> !e.getVirtual()).count()));		
		return s;
	}


}
