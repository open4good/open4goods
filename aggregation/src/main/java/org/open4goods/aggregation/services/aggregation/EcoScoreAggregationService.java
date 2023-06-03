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

	private final Map<String, String> ecoScoreconfig;

	public EcoScoreAggregationService(final Map<String, String> ecoScoreconfig,  final String logsFolder) {
		super(logsFolder);
		this.ecoScoreconfig = ecoScoreconfig;
	}



	@Override
	public void onProduct(Product data) {

		// Scoring from attribute
		Score score = generateEcoScore(data.getScores());
		
		if (null != score) {
			// Processing cardinality
			processCardinality(score);
			
			// Saving in product
			data.getScores().put(score.getName(), score);
		}
	}



	private Score generateEcoScore(Map<String, Score> scores) {
		
		
		Double va = 0.0;
		
		
		for (Entry<String, String> config :  ecoScoreconfig.entrySet()) {
			Score score = scores.get(config.getKey());
			
			if (null == score) {
				// If one composed score is null, then do not proceed
				// There will be a virtual score instead
				return null;
			}
			
			va += score.getCardinality().getRelValue() * Double.valueOf(config.getValue());
			// TODO : compute virtual score
		}
		
		
		
		
		Score s = new Score();
		s.setName("ECOSCORE");
		// TODO : from conf
		s.setMax(5.0);
		s.setMin(0.0);
		s.setVirtual(false);
		
		s.setValue(va);
		
		
		return s;
	}




}
