package org.open4goods.aggregation.services.aggregation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.aggregation.AbstractRealTimeAggregationService;
import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.attributes.AttributeParser;
import org.open4goods.config.yml.ui.AttributesConfig;
import org.open4goods.exceptions.ParseException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.UnindexedKeyValTimestamp;
import org.open4goods.model.product.AggregatedAttribute;
import org.open4goods.model.product.AggregatedFeature;
import org.open4goods.model.product.IAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.services.BrandService;

public class AttributeRealtimeAggregationService extends AbstractRealTimeAggregationService {

	private final AttributesConfig attributesConfig;
	
	private final BrandService brandService;

	public AttributeRealtimeAggregationService(final AttributesConfig attributesConfig,  BrandService brandService, final String logsFolder,boolean toConsole) {
		super(logsFolder,toConsole);
		this.attributesConfig = attributesConfig;
		this.brandService = brandService;
	}


	/**
	 * Associate and match a set of nativ attributes from a datafragment into  a product
	 *
	 * @param dataFragment
	 * @param p
	 * @param match2
	 */
	@Override
	public void onDataFragment(final DataFragment dataFragment, final Product product) {

		Set<String> matchList = new HashSet<>();
		/////////////////////////////////////////
		// Converting to AggregatedAttributes for matches from config
		/////////////////////////////////////////

		for (Attribute attr :  dataFragment.getAttributes()) {
			IAttribute translated = attributesConfig.translateAttribute(attr, dataFragment.getDatasourceName());
			
			// We have a "raw" attribute that matches a aggragationconfig
			if (null != translated) {
				AggregatedAttribute agg = product.getAttributes().getAggregatedAttributes().get(attr.getName());
				matchList.add(translated.getName());
				
				if (null == agg) {
					// A first time match
					agg = new AggregatedAttribute();
					agg.setName(attr.getName());
				} 
				agg.addAttribute(attr.getName(), new UnindexedKeyValTimestamp(dataFragment.getDatasourceName(), translated.getValue().toString()));
				product.getAttributes().getAggregatedAttributes().put(agg.getName(), agg);				
			}
		}

		
		/////////////////////////////////////////
		// Update referentiel attributes
		/////////////////////////////////////////
		handleReferentielAttributes(dataFragment , product);
		// TODO : Add BRAND / MODEL from matches from attributes

		/////////////////////////////////////////
		// EXTRACTING FEATURES 
		/////////////////////////////////////////
		
		List<Attribute> matchedFeatures = dataFragment.getAttributes().stream()
				.filter(this::isFeatureAttribute)
				.collect(Collectors.toList());

		matchList.addAll(matchedFeatures.stream().map(e->e.getName()).collect(Collectors.toSet()));
		

		Collection<AggregatedFeature> af = aggregateFeatures(matchedFeatures);
		product.getAttributes().getFeatures().addAll(af);
		

		
		//////////////////////////
		// Aggregating unmatched attributes
		///////////////////////////
		
		for (Attribute attr : dataFragment.getAttributes()) {
			// Checking if to be removed
			if (matchList.contains(attr.getName())) {
				continue;
			}
			
			// TODO : remove from a config list
			
			AggregatedAttribute agg = product.getAttributes().getUnmapedAttributes().stream().filter(e->e.getName().equals(attr.getName())).findAny().orElse(null);
			
			if (null == agg) {
				// A first time match
				agg = new AggregatedAttribute();
				agg.setName(attr.getName());
			} 
			agg.addAttribute(attr.getName(), new UnindexedKeyValTimestamp(dataFragment.getDatasourceName(), attr.getValue().toString()));
			
			product.getAttributes().getUnmapedAttributes().add(agg);			
		}
	
	}



//	/**
//	 * Handle attributes (referentiels, aggregations, unmapped cleanings, ...)
//	 * @param existingReferentielAttributes
//	 * @param matchedAttrs
//	 * @param allAttrs
//	 * @param datasourceName
//	 * @param output
//	 */
//	private void processAttributes(Map<String, String> existingReferentielAttributes, List<SourcedAttribute> matchedAttrs, List<SourcedAttribute> allAttrs,   String datasourceName, final Product output ) {
//
//		
//		
//
//		
//
//
//
//		////////////////////////////////////
//		// Aggregating standard attributes
//		///////////////////////////////////
//
//		dedicatedLogger.info("{} featured attributes merged from {} matched sources and {} unmatched sources", af.size(), matchedFeatures.size(), unmatchedFeatures.size());
//
//		// 3 - Applying attribute transformations on matched ones
//		//TODO : handle conflicts
//
//		Set<AggregatedAttribute> aggattrs = aggregateAttributes(matchedAttrs);
//		for (AggregatedAttribute aga : aggattrs) {
//
//			output.getAttributes().getAggregatedAttributes().put(aga.getName(), aga);
//
//		}
//		dedicatedLogger.info("{} recognized attributes, {} are not ",matchedAttrs.size(),allAttrs.size());
//
//		///////////////////////////////////
//		// Adding unmatched attributes
//		///////////////////////////////////
//		for (SourcedAttribute attr : allAttrs) {
//			AggregatedAttribute aat = new AggregatedAttribute();
//			aat.setName(attr.getName());
//			aat.setValue(attr.getRawValue().toString());
//			output.getAttributes().getUnmapedAttributes().add(aat);
//		}
//		
//
//
//		
//	}


//
//	/**
//	 * Operates the "matched" attributes aggregation
//	 * @param matchedAttrs
//	 * @param aa
//	 * @return
//	 */
//	private Set<AggregatedAttribute> aggregateAttributes(List<SourcedAttribute> matchedAttrs) {
//		Set<AggregatedAttribute> ret = new HashSet<>();
//
//		// Split per attribute names
//
//		Map<String,Set<SourcedAttribute>> attrs = new HashMap<>();
//
//		matchedAttrs.forEach(a -> {
//			if (!attrs.containsKey(a.getName())) {
//				attrs.put(a.getName(),new HashSet<>());
//			}
//			attrs.get(a.getName()).add(a);
//		});
//
//		// Building aggregatedAttribute
//		for (Entry<String, Set<SourcedAttribute>> e : attrs.entrySet()) {
//			try {
//				ret.add(aggregateAttribute(e.getKey(),e.getValue()));
//			} catch (ValidationException e1) {
//				dedicatedLogger.warn(e1.getMessage());
//			}
//		}
//
//		return ret;
//
//	}

//	/**
//	 * Aggregates a set of attributes (assuming sharing the same name) to an AggregatedAttribute
//	 * @param key
//	 * @param value
//	 * @return
//	 * @throws ValidationException
//	 */
//	private AggregatedAttribute aggregateAttribute(String name, Set<SourcedAttribute> attributes) throws ValidationException {
//		AggregatedAttribute ret = new AggregatedAttribute();
//		ret.setName(name);
//
//
//		// Retrieving attrConfig
//		final AttributeConfig aConfig = attributesConfig.getAttributeConfigByKey(name);
//
//		///////////////////////////////
//		// Best value election, via counting map
//		///////////////////////////////
//		Map<Object, AtomicInteger> bestValue = new HashMap<>();
//
//		for (SourcedAttribute attr : attributes) {
//
//			SourcedAttribute parsed;
//
//			try {
//				// Parsing attribute
//				parsed = parseAttributeValue(attr, aConfig);
//			} catch (ValidationException e) {
//				dedicatedLogger.info("Attribute parsing failed for {} with message {} ", attr, e.getMessage());
//				continue;
//			}
//
//			// Detecting type and checking conformity
//			final Optional<AttributeType> type = parsed.detectType();
//			if (type.isEmpty() ||  type.get() != aConfig.getType()) {
//				dedicatedLogger.warn("Incompatible type for attribute {} : Expected {}, real type was {}", attr, aConfig);
//				continue;
//			}
//
//			// Adding attribute
//			ret.addAttribute(parsed);
//
//			if (!bestValue.containsKey(attr.getRawValue())) {
//				bestValue.put(attr.getRawValue(), new AtomicInteger(0));
//			}
//
//
//			// Standard datasources give 1 point
//			bestValue.get(attr.getRawValue()).incrementAndGet();
//
//
//
//		}
//
//		// Getting The elected one
//		Optional<Entry<Object, AtomicInteger>> elected = bestValue.entrySet().stream().max((entry1, entry2) -> entry1.getValue().intValue() > entry2.getValue().intValue() ? 1 : -1);
//
//
//		if (elected.isEmpty() || null ==  elected.get()) {
//			throw new ValidationException("Not enough data, cannot build "+name+" from "+attributes+", see previous errors");
//		}
//
//		ret.setType(Attribute.getType(elected.get().getKey()));
//		ret.setValue(elected.get().getKey().toString());
//
//		//TODO : Update the attributes conflicts / election
//
//		//		// Setting potential conflicts
//		//		ret.setHasConflicts( ret.getSources().size() > 1);
//
//
//		return ret;
//	}

	/**
	 *
	 * @param matchedFeatures
	 * @param unmatchedFeatures
	 * @return
	 */
	private Collection<AggregatedFeature> aggregateFeatures(List<Attribute> matchedFeatures) {

		Map<String,AggregatedFeature> ret = new HashMap<String, AggregatedFeature>();

		// Adding matched attributes features
		for (Attribute a : matchedFeatures) {
			if (! ret.containsKey(a.getName())) {
				ret.put(a.getName(), new AggregatedFeature(a.getName()));
			}
			//			ret.get(a.getName()).getDatasources().add(a.getDatasourceName());
		}

		

		return ret.values();
	}

	/**
	 * Returns if an attribute is a feature, by comparing "yes" values from config
	 * @param e
	 * @return
	 */
	private boolean isFeatureAttribute(Attribute e) {
		return attributesConfig.getFeaturedValues().contains(e.getRawValue().toString().trim().toUpperCase());
	}

	/**
	 * Aggregate ReferentielAttributes
	 * @param refAttrs
	 * @param aa
	 * @param output
	 */
	private void handleReferentielAttributes(DataFragment fragement, Product output) {

		
		for (Entry<String, String> attr : fragement.getReferentielAttributes().entrySet()) {

			ReferentielKey key = ReferentielKey.valueOf( attr.getKey());

			String value = attr.getValue();

			String existing = output.getAttributes().getReferentielAttributes().get(key);

			if (!StringUtils.isEmpty(existing) && !existing.equals(value)) {
				//TODO(0.5,p2,feature) : handle conflicts and "best value" election on referentiel attributes
				if (key.equals(ReferentielKey.MODEL)) {
					dedicatedLogger.info("Adding different {} name as alternate id. Existing is {}, would have erased with {}",key,existing, value);
					output.getAlternativeIds().add(new UnindexedKeyValTimestamp(fragement.getDatasourceName(), value));					
				} else if (key.equals(ReferentielKey.BRAND)) {
					
					value = brandService.normalizeBrand(value);
					if (null != value && !existing.equals(value)) {
						//TODO (gof) : elect best brand, exclude "non categorisée", ...
						dedicatedLogger.info("Adding different {} name as BRAND. Existing is {}, would have erased with {}",key,existing, value);						
						// Adding the old one in alternate brand
						output.getAlternativeBrands().add(new UnindexedKeyValTimestamp(fragement.getDatasourceName(), value));
						// Adding the new one
						output.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, value);
					}
				} 
				
				
				
				else {
					dedicatedLogger.warn("Skipping referentiel attribute erasure for {}. Existing is {}, would have erased with {}",key,existing, value);
				}
			} 
			
			
			else {
				// TODO : Bad design of this method
				if (key.equals(ReferentielKey.BRAND)) {
					value = brandService.normalizeBrand(value);
				}
				output.getAttributes().addReferentielAttribute( key, value);
			}
		}

	}



	/**
	 * Type attribute and apply parsing rules. Return null if the Attribute fail to exact parsing rules
	 *
	 * @param translated
	 * @param attributeConfigByKey
	 * @return
	 * @throws ValidationException
	 */
	public Attribute parseAttributeValue(final Attribute attr, final AttributeConfig conf) throws ValidationException {


		///////////////////
		// To upperCase / lowerCase
		///////////////////

		if (conf.getParser().getLowerCase()) {
			attr.lowerCase();
		}

		if (conf.getParser().getUpperCase()) {
			attr.upperCase();
		}

		//////////////////////////////
		// Deleting arbitrary tokens
		//////////////////////////////

		if (null != conf.getParser().getDeleteTokens()) {
			for (final String token : conf.getParser().getDeleteTokens()) {
				attr.replaceToken(token, "");
			}
		}

		///////////////////
		// removing parenthesis tokens
		///////////////////
		if (conf.getParser().isRemoveParenthesis()) {
			attr.replaceToken("\\(.*\\)", "");
		}

		///////////////////
		// Normalisation
		///////////////////
		if (conf.getParser().getNormalize()) {
			attr.normalize();
		}

		///////////////////
		// Trimming
		///////////////////
		if (conf.getParser().getTrim()) {
			attr.trim();
		}

		///////////////////
		// Exact match option
		///////////////////

		if (null != conf.getParser().getTokenMatch()) {
			boolean found = false;

			if (attr.multivalued()) {
				for (final String match : conf.getParser().getTokenMatch()) {
					if (attr.stringValues().contains(match)) {
						attr.setRawValue(conf.getParser().getTokenMatch());
					}
				}
			} else {
				final String val = attr.getRawValue().toString();
				for (final String match : conf.getParser().getTokenMatch()) {
					if (val.contains(match)) {
						attr.setRawValue(match);
						found = true;
						break;
					}
				}
			}

			if (!found) {
				throw new ValidationException("Token "+ attr.stringValue() + " does not match  attribute " + attr.getName() + " specifiction");
			}

		}

		/////////////////////////////////
		// Checking preliminary result
		/////////////////////////////////


		if (null == attr.getRawValue()) {
			throw new ValidationException ("Null rawValue in attribute " + attr);
		}

		////////////////////////////////////
		// Applying specific parser instance
		/////////////////////////////////////

		if (!StringUtils.isEmpty(conf.getParser().getClazz())) {
			try {
				final AttributeParser parser = conf.getParserInstance();
				final String parserRes = parser.parse(attr.getRawValue().toString(), attr, conf);
				attr.setRawValue(parserRes);
			} catch (final ResourceNotFoundException e) {
				dedicatedLogger.warn("Error while applying specific parser for {}", conf.getParser().getClazz(), e);
				throw new ValidationException (e.getMessage());
			} catch (final ParseException e) {
				dedicatedLogger.warn("Cannot parse {} with {} : {}", attr, conf.getParser().getClazz(), e.getMessage());
				throw new ValidationException (e.getMessage());
			} catch (final Exception e) {
				dedicatedLogger.error("Unexpected exception while parsing {} with {}", attr, conf.getParser().getClazz(), e);
				throw new ValidationException (e.getMessage());
			}
		}

		///////////////////
		// Typing attribute
		///////////////////


		attr.typeAttribute(conf.getType());

		return attr;

	}





	/**
	 * Removing some useless attributes
	 * @param unmapedAttributes
	 * @param data
	 * @return
	 */
//	private Set<AggregatedAttribute> cleanUnmapped(Set<AggregatedAttribute> unmapedAttributes, Product data) {
//
//		
//		// TODO : put back attribute unmapped attributes cleaning
//		
//
//		return unmapedAttributes;
//		
//		
////		
////		
////		
////		Set<AggregatedAttribute> ret = new HashSet<>();
////		
////		//////////////
////		// A dictionary of "to exclude" attributes names
////		//////////////
////		
////		// Adding aggregated attribute names
////		Set<String> excludedAttrNames = new HashSet<>();
////		
////		excludedAttrNames.addAll(data.getAttributes().getAggregatedAttributes().keySet());
////				
////				
////		// Adding referentiel attribute names
////		excludedAttrNames.addAll(data.getAttributes().getReferentielAttributes().keySet().stream().map(e -> e.toString()).toList());
////		// Adding "matching attribute" définitions
////		
////		attributesConfig.synonyms().values().stream().map(e -> e.keySet()).forEach(e -> {
////			excludedAttrNames.addAll(e);
////		}
////		); 
////		
////		// Adding configured exclusions		
////		 excludedAttrNames.addAll(attributesConfig.getExclusions());
////		
////		
////		////
////		// Process exclusions
////		////
////		
////		for (AggregatedAttribute aga : unmapedAttributes) {
////			if (!excludedAttrNames.contains(aga.getName())) {
////				ret.add(aga);
////			}		
////		}
////		
////		return ret;
//	}




}
