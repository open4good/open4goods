package org.open4goods.api.services.aggregation.services.realtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.config.yml.attributes.AttributeConfig;
import org.open4goods.commons.config.yml.attributes.AttributeParser;
import org.open4goods.commons.config.yml.ui.AttributesConfig;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.model.attribute.Attribute;
import org.open4goods.commons.model.constants.ReferentielKey;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.product.AggregatedFeature;
import org.open4goods.commons.model.product.IndexedAttribute;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.model.product.ProductAttribute;
import org.open4goods.commons.model.product.SourcedAttribute;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.IcecatService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.slf4j.Logger;

public class AttributeRealtimeAggregationService extends AbstractAggregationService {

	private final BrandService brandService;

	private VerticalsConfigService verticalConfigService;
	private IcecatService featureService;

	public AttributeRealtimeAggregationService(final VerticalsConfigService verticalConfigService, BrandService brandService, final Logger logger, IcecatService featureService) {
		super(logger);
		this.verticalConfigService = verticalConfigService;
		this.brandService = brandService;
		this.featureService = featureService;
	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {

		//////////////////////////////////////////
		// Cleaning attributes that must be discarded
		//////////////////////////////////////////

		// Remove excluded attributes
		// TODO / Usefull for batch mode, could remove once initial sanitization
		vConf.getAttributesConfig().getExclusions().forEach(e -> {
			data.getAttributes().getAll().remove(e);
		});

		// TODO(p3,design) : Remove, this is some kind of legacy bug, some attributes have uncoherent and too many sources. Hope this is a done bug..
		data.getAttributes().getAll().keySet().removeIf(e-> data.getAttributes().getAll().get(e).sourcesCount() > 10);
		
		
		//////////////////////////////////////////////////////////////////////////
		// Checking if all mandatory attributes are present for this product
		//////////////////////////////////////////////////////////////////////////
//		if (!data.getAttributes().getAggregatedAttributes().keySet().containsAll(vConf.getAttributesConfig().getMandatory())) {
//			// Missing attributes.
//
//			Set<String> missing = vConf.getAttributesConfig().getMandatory();
//			missing.removeAll(data.getAttributes().getAggregatedAttributes().keySet());
//
//			dedicatedLogger.warn("{} excluded from {}. Missing mandatory attributes : {}", data.getId(), vConf.getId(), missing);
//			data.setExcluded(true);
//		} else {
//			data.setExcluded(false);
//		}

		// Attributing taxomy to attributes
		data.getAttributes().getAll().values().forEach(a -> {
			Set<Integer> icecatTaxonomyIds = featureService.resolveFeatureName(a.getName());
			if (null != icecatTaxonomyIds) {
				dedicatedLogger.info("Found icecat taxonomy for {} : {}", a.getName(), icecatTaxonomyIds);
				a.setIcecatTaxonomyIds(icecatTaxonomyIds);
			}
		});

		///////////////////////////////////////////////////
		// Extracting indexed attributes
		//////////////////////////////////////////////////
		AttributesConfig attributesConfig = vConf.getAttributesConfig();
		
		
		Map<String,IndexedAttribute> indexed = new HashMap<String, IndexedAttribute>();
		
		
		for (ProductAttribute attr : data.getAttributes().getAll().values()) {

			// Checking if a potential AggregatedAttribute
			// TODO(P1) : Detect and parse from the icecat taxonomy
			String indexedName = attributesConfig.isToBeIndexedAttribute(attr, null);

			// We have a "raw" attribute that matches an aggregationconfig
			if (null != indexedName) {

				try {
					AttributeConfig attrConfig = attributesConfig.getConfigFor(indexedName);

					// Applying parsing rule
					String cleanedValue =  parseAttributeValue(attr.getValue(), attrConfig);

					if (StringUtils.isEmpty(cleanedValue)) {
						dedicatedLogger.error("Empty indexed attribute value {}:{}",indexedName,attr.getValue());
						continue;
					}
					
					IndexedAttribute indexedAttr = indexed.get(indexedName);
					if (null != indexedAttr) {
						dedicatedLogger.info("Duplicate attribute candidate for indexation, for GTIN : {} and attrs {}",data.getId(), indexedName);
						if (!cleanedValue.equals(indexedAttr.getValue() )) {
							// TODO(p3,design) : Means we have multiple attributes matching for indexedbuilding. Have a merge strategy
							dedicatedLogger.error("Value mismatch for attribute {} : {}<>{}",attr.getName(),cleanedValue, indexedAttr.getValue());
						} 						
					} else {
						 indexedAttr = new IndexedAttribute(indexedName, cleanedValue);
					}
					
					indexedAttr.getSource().addAll(attr.getSource());					
					indexed.put(indexedName, indexedAttr);
					
				} catch (Exception e) {
					dedicatedLogger.error("Attribute parsing fail for matched attribute {}", indexedName);
				}
			}
		}
		
		
		// Replacing all previously indexed
		data.getAttributes().setIndexed(indexed);
		
	}

	/**
	 * On data fragment agg leveln we increment the "all" field, with sourced values
	 * for new or existing attributes. product
	 *
	 * @param dataFragment
	 * @param p
	 * @param match2
	 */
	@Override
	public Map<String, Object> onDataFragment(final DataFragment dataFragment, final Product product, VerticalConfig vConf) throws AggregationSkipException {

		if (dataFragment.getAttributes().size() == 0) {
			return null;
		}

		try {

//			AttributesConfig attributesConfig = vConf.getAttributesConfig();

//			// Remove excluded attributes
//			if (dataFragment.getAttributes().removeIf(e -> attributesConfig.getExclusions().contains(e.getName()))) {
//				dedicatedLogger.info("Attributes have been removed for {}", product.gtin());
//			}

			/////////////////////////////////////////
			// Incrementing "all" attributes
			/////////////////////////////////////////
			for (Attribute attr : dataFragment.getAttributes()) {

				ProductAttribute agg = product.getAttributes().getAll().get(attr.getName());

				if (null == agg) {
					// A first time match
					agg = new ProductAttribute();
					agg.setName(attr.getName());
				}

				// TODO(p1, gof) : update the add
				agg.addSourceAttribute(new SourcedAttribute(attr, dataFragment.getDatasourceName()));

				// Replacing new AggAttribute in product
				product.getAttributes().getAll().put(agg.getName(), agg);

			}


			// Checking model name from product words
//			completeModelNames(product, dataFragment.getReferentielAttributes().get(ReferentielKey.MODEL));

			/////////////////////////////////////////
			// Update referentiel attributes
			/////////////////////////////////////////
			handleReferentielAttributes(dataFragment, product);
			// TODO : Add BRAND / MODEL from matches from attributes

			//////////////////////////
			// Aggregating unmatched attributes
			///////////////////////////

//			for (Attribute attr : dataFragment.getAttributes()) {
//				// Checking if to be removed
////				if (toRemoveFromUnmatched.contains(attr.getName())) {
////					continue;
////				}
//
//				// TODO : remove from a config list
//
//				ProductAttribute agg = product.getAttributes().getUnmapedAttributes().stream().filter(e -> e.getName().equals(attr.getName())).findAny().orElse(null);
//
//				if (null == agg) {
//					// A first time match
//					agg = new ProductAttribute();
//					agg.setName(attr.getName());
//				}
//				agg.addAttribute(attr, new UnindexedKeyValTimestamp(dataFragment.getDatasourceName(), attr.getValue()));
//
//				product.getAttributes().getUnmapedAttributes().add(agg);
//			}

			// Removing
//			product.getAttributes().setUnmapedAttributes(product.getAttributes().getUnmapedAttributes().stream()
//					// TODO : Should be from path
//					// TODO : apply from sanitisation
//					.filter(e -> !e.getName().contains("CATEGORY")) 
////					.filter(e -> !toRemoveFromUnmatched.contains(e.getName())) 
//					.collect(Collectors.toSet()));

			// TODO : Removing matchlist again to handle remove of old attributes in case of
			// configuration change
//		product.getAttributes().getUnmapedAttributes().
		} catch (Exception e) {
			// TODO
			dedicatedLogger.error("Unexpected error", e);
			e.printStackTrace();
		}

		onProduct(product, vConf);
		return null;
	}

//	/**
//	 * Complete the model names by looking in product words for sequence starting with the shortest model name.
//	 * @param product
//	 * @param string
//	 */
//	private void completeModelNames(Product product, String string) {
//		// Get the known model names
//		Set<String> models = new HashSet<>();
//		if (!StringUtils.isEmpty(string)) {
//			models.add(string);
//		}
//		product.getAlternativeBrands().forEach(e -> models.add(e.getValue()));
//		
//		
//		// Compute the bag of known words
//		Set<String> words = new HashSet<>();
//		product.getDescriptions().forEach(e -> {
//			words.addAll(Arrays.asList(e.getContent().getText().split(" ")));
//		});
//		
//		product.getNames().getOfferNames().forEach(e -> {
//			words.addAll(Arrays.asList(e.split(" ")));
//		});
//		
//		
//		String shortest = product.shortestModel();
//		// Iterating on words to find the one who have matching starts with known model names
//		for (String w : words) {
//			w = w.toUpperCase();
//			if ((w.startsWith(shortest) || shortest.startsWith(w))  && !w.equals(shortest)) {
//				
//				if (StringUtils.isAlpha(w.substring(w.length()-1))) {
//					dedicatedLogger.info("Found a alternate model for {} in texts : {}", shortest, w);
//					product.addModel(w);
//					
//				}
//				
//			}
//		}
//	}
//

	/**
	 *
	 * @param matchedFeatures
	 * @param unmatchedFeatures
	 * @return
	 */
	private Collection<AggregatedFeature> aggregateFeatures(List<Attribute> matchedFeatures) {

		Map<String, AggregatedFeature> ret = new HashMap<String, AggregatedFeature>();

		// Adding matched attributes features
		for (Attribute a : matchedFeatures) {
			if (!ret.containsKey(a.getName())) {
				ret.put(a.getName(), new AggregatedFeature(a.getName()));
			}
			// ret.get(a.getName()).getDatasources().add(a.getDatasourceName());
		}

		return ret.values();
	}

	/**
	 * Returns if an attribute is a feature, by comparing "yes" values from config
	 * 
	 * @param e
	 * @return
	 */
	private boolean isFeatureAttribute(Attribute e, AttributesConfig attributesConfig) {
		return e.getRawValue() == null ? false : attributesConfig.getFeaturedValues().contains(e.getRawValue().trim().toUpperCase());
	}

	/**
	 * Handles referential attributes of a data fragment and updates the product
	 * output accordingly. This method updates or adds referential attributes, while
	 * also handling conflicts and logging them.
	 *
	 * @param fragment The data fragment containing referential attributes.
	 * @param output   The product output to which referential attributes are to be
	 *                 added or updated.
	 */
	private void handleReferentielAttributes(DataFragment fragment, Product output) {
		for (Entry<ReferentielKey, String> attr : fragment.getReferentielAttributes().entrySet()) {
			ReferentielKey key = attr.getKey();
			String value = attr.getValue().toUpperCase();

			if (StringUtils.isEmpty(value)) {
				continue;
			}

			String existing = output.getAttributes().getReferentielAttributes().get(key);

			if (StringUtils.isEmpty(existing)) {
				output.getAttributes().addReferentielAttribute(key, value);
			} else if (!existing.equals(value)) {
				// We have a variation !
				switch (key) {
				case MODEL:
					dedicatedLogger.info("Adding different {} name as alternate id. Existing is {}, would have erased with {}", key, existing, value);
					output.addModel(value);
					break;
				case BRAND:				
					output.getAkaBrands().put(fragment.getDatasourceName(), value);
					break;
				case GTIN:
					if (value != null && !existing.equals(value)) {
						try {
							long existingGtin = Long.parseLong(existing);
							long newGtin = Long.parseLong(value);
							if (existingGtin != newGtin) {
								dedicatedLogger.error("Overriding GTIN from {} to {}", existing, newGtin);
								output.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN, value);
							} else {
								dedicatedLogger.error("Cannot override GTIN from {} to {}", existing, value);
							}
						} catch (NumberFormatException e) {
							dedicatedLogger.error("Invalid GTIN format: existing = {}, new = {}", existing, value, e);
						}
					}
					break;
				default:
					dedicatedLogger.warn("Skipping referential attribute erasure for {}. Existing is {}, would have erased with {}", key, existing, value);
					break;
				}
			}
		}
	}

	/**
	 * Type attribute and apply parsing rules. Return null if the Attribute fail to
	 * exact parsing rules
	 *
	 * @param translated
	 * @param attributeConfigByKey
	 * @return
	 * @throws ValidationException
	 */
	public String parseAttributeValue(final String source, final AttributeConfig conf) throws ValidationException {

		String string = source;
		///////////////////
		// To upperCase / lowerCase
		///////////////////

		if (conf.getParser().getLowerCase()) {
			string = string.toLowerCase();
		}

		if (conf.getParser().getUpperCase()) {
			string = string.toUpperCase();
		}

		//////////////////////////////
		// Deleting arbitrary tokens
		//////////////////////////////

		if (null != conf.getParser().getDeleteTokens()) {
			for (final String token : conf.getParser().getDeleteTokens()) {
				string = string.replace(token, "");
			}
		}

		///////////////////
		// removing parenthesis tokens
		///////////////////
		if (conf.getParser().isRemoveParenthesis()) {
			string = string.replace("\\(.*\\)", "");
		}

		///////////////////
		// Normalisation
		///////////////////
		if (conf.getParser().getNormalize()) {
			string = StringUtils.normalizeSpace(source);
		}

		///////////////////
		// Trimming
		///////////////////
		if (conf.getParser().getTrim()) {
			string = source.trim();
		}

		///////////////////
		// Exact match option
		///////////////////

		if (null != conf.getParser().getTokenMatch()) {
			boolean found = false;

			final String val = string;
			for (final String match : conf.getParser().getTokenMatch()) {
				if (val.contains(match)) {
					string = match;
					found = true;
					break;
				}
			}

			if (!found) {
				throw new ValidationException("Token " + string + " does not match  any fixed attribute ");
			}

		}

		/////////////////////////////////
		// FIXED TEXT MAPPING
		/////////////////////////////////
		if (!conf.getMappings().isEmpty()) {
			string = conf.getMappings().get(string);
		}

		/////////////////////////////////
		// Checking preliminary result
//		/////////////////////////////////
//
//		if (null == string) {
//			throw new ValidationException("Null rawValue in attribute " + string);
//		}

		////////////////////////////////////
		// Applying specific parser instance
		/////////////////////////////////////

		if (!StringUtils.isEmpty(conf.getParser().getClazz())) {
			try {
				final AttributeParser parser = conf.getParserInstance();
				string  = parser.parse(string, conf);
			} catch (final ResourceNotFoundException e) {
				dedicatedLogger.warn("Error while applying specific parser for {}", conf.getParser().getClazz(), e);
				throw new ValidationException(e.getMessage());
			} catch (final Exception e) {
				dedicatedLogger.error("Unexpected exception while parsing {} with {}", string, conf.getParser().getClazz(), e);
				throw new ValidationException(e.getMessage());
			}
		}

		return string;

	}

}
