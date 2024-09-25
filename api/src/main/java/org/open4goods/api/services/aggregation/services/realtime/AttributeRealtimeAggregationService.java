package org.open4goods.api.services.aggregation.services.realtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.config.yml.attributes.AttributeConfig;
import org.open4goods.commons.config.yml.attributes.AttributeParser;
import org.open4goods.commons.config.yml.ui.AttributesConfig;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.exceptions.ParseException;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.helper.ResourceHelper;
import org.open4goods.commons.model.attribute.Attribute;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.data.Resource;
import org.open4goods.commons.model.product.AggregatedAttribute;
import org.open4goods.commons.model.product.AggregatedFeature;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.IcecatService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.slf4j.Logger;

public class AttributeRealtimeAggregationService extends AbstractAggregationService {


	
	private final BrandService brandService;

	private VerticalsConfigService verticalConfigService;
	private IcecatService featureService;

	public AttributeRealtimeAggregationService(final VerticalsConfigService verticalConfigService,  BrandService brandService, final Logger logger, IcecatService featureService) {
		super(logger);
		this.verticalConfigService = verticalConfigService;
		this.brandService = brandService;
		this.featureService = featureService;
	}


	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {

		
		//////////////////////////////////////////
		// Cleaning attributes names (normalisation)		
		// TODO(p3, optimisation) : Could remove once full sanitisation batch, all new attribute names are clean
		//////////////////////////////////////////
		Set<AggregatedAttribute> attrs = new HashSet<AggregatedAttribute>();
		data.getAttributes().getUnmatchedAttributes().stream().forEach(a -> {
			// Dedup is ensured with the set and hashcode / equals override
			a.setName(IdHelper.normalizeAttributeName(a.getName()));
			attrs.add(a);
		});
		data.getAttributes().setUnmatchedAttributes(attrs);
		
		
		//////////////////////////////////////////////////////////////////////////		
		// Checking if all mandatory attributes are present for this product
		//////////////////////////////////////////////////////////////////////////
		if (!data.getAttributes().getAttributes().keySet().containsAll(vConf.getAttributesConfig().getMandatory())) {
			// Missing attributes.
			
			Set<String> missing = vConf.getAttributesConfig().getMandatory();
			missing.removeAll(data.getAttributes().getAttributes().keySet());
			
			dedicatedLogger.warn("{} excluded from {}. Missing mandatory attributes : {}",  data.getId(), vConf.getId(), missing);
			data.setExcluded(true);
		} else {
			data.setExcluded(false);			
		}

		
		// Attributing taxomy to attributes
		data.getAttributes().getUnmatchedAttributes().forEach(a -> {
			Set<Integer> icecatTaxonomyIds = featureService.resolveFeatureName(a.getName());
			if (null != icecatTaxonomyIds) {
				dedicatedLogger.info("Found icecat taxonomy for {} : {}", a.getName(), icecatTaxonomyIds);
				a.setIcecatTaxonomyIds(icecatTaxonomyIds );
				a.setNumericValue(null);
			}
		});
		
		// Post computing attr value from best value. Should not occcurs, but due to RDO, sometimes attribute value is not computed
		data.getAttributes().getAttributes().entrySet().stream().forEach(a -> {
			if (null == a.getValue().getValue()) {
				a.getValue().setValue(a.getValue().bestValue());
			}
			
			
		});
		
		
		
		
	}

	
	/**
	 * Associate and match a set of nativ attributes from a datafragment into  a product
	 *
	 * @param dataFragment
	 * @param p
	 * @param match2
	 */
	@Override
	public void onDataFragment(final DataFragment dataFragment, final Product product, VerticalConfig vConf) throws AggregationSkipException {

		if (dataFragment.getAttributes().size() == 0) {
			return;
		}
		
		try {
			
			AttributesConfig attributesConfig = vConf.getAttributesConfig();

			// Adding the list of "to be removed" attributes
			Set<String> toRemoveFromUnmatched = new HashSet<>(attributesConfig.getExclusions());
					
			/////////////////////////////////////////
			// Converting to AggregatedAttributes for matches from config
			/////////////////////////////////////////
			
			List<Attribute> all = new ArrayList<>();
			// Handling attributes in datafragment
			all.addAll(dataFragment.getAttributes());
			// Add unmatched attributes from the product (case configuration change)
			
			// TODO : BIG BUG : Override the providername. Must be only in batch mode
			//all.addAll(product.getAttributes().getUnmapedAttributes().stream().map(e -> new Attribute(e.getName(),e.getValue(),e.getLanguage())).toList());
			
			
			for (Attribute attr :  all) {
				
				// Checking if a potential AggregatedAttribute
				Attribute translated = attributesConfig.translateAttribute(attr,  dataFragment.getDatasourceName());
				
				// We have a "raw" attribute that matches a aggregationconfig								
				
				if (ResourceHelper.isImage(attr.getValue())) {
					Resource r = new Resource(attr.getValue());
					r.getTags().add(attr.getName());
					product.addResource(r);
					toRemoveFromUnmatched.add(attr.getName());
					continue;
				}
				
				if (null != translated) {
					
					try {
						AttributeConfig attrConfig = attributesConfig.getConfigFor(translated);
						
						// Applying parsing rule
						translated = parseAttributeValue(translated, attrConfig);
						
						if (translated.getRawValue() == null) {
							continue;
						}

						AggregatedAttribute agg = product.getAttributes().getAttributes().get(attr.getName());
						
						
						if (null == agg) {
							// A first time match
							agg = new AggregatedAttribute();
							agg.setName(attr.getName());
						} 
							
						
						
						toRemoveFromUnmatched.add(translated.getName());
					
						
						
						agg.addAttribute(translated, dataFragment.getDatasourceName(), translated.getValue());
						
						// Replacing new AggAttribute in product
						product.getAttributes().getAttributes().put(agg.getName(), agg);
					} catch (Exception e) {

						dedicatedLogger.error("Attribute parsing fail for matched attribute {}", translated);
					}				
				}
			}

			
			// Checking model name from product words
//			completeModelNames(product, dataFragment.getReferentielAttributes().get(ReferentielKey.MODEL));
			
			/////////////////////////////////////////
			// EXTRACTING FEATURES 
			/////////////////////////////////////////
			
			List<Attribute> matchedFeatures = dataFragment.getAttributes().stream()
					.filter(e -> isFeatureAttribute(e, attributesConfig))
					.collect(Collectors.toList());

			toRemoveFromUnmatched.addAll(matchedFeatures.stream().map(Attribute::getName).collect(Collectors.toSet()));
			

			Collection<AggregatedFeature> af = aggregateFeatures(matchedFeatures);
			product.getAttributes().getFeatures().addAll(af);
			

			
			//////////////////////////
			// Aggregating unmatched attributes
			///////////////////////////
			
			for (Attribute attr : dataFragment.getAttributes()) {
				// Checking if to be removed
//				if (toRemoveFromUnmatched.contains(attr.getName())) {
//					continue;
//				}
				
				// TODO : remove from a config list
				
				AggregatedAttribute agg = product.getAttributes().getUnmatchedAttributes().stream().filter(e->e.getName().equals(attr.getName())).findAny().orElse(null);
				
				if (null == agg) {
					// A first time match
					agg = new AggregatedAttribute();
					agg.setName(attr.getName());
				} 
				agg.addAttribute(attr, dataFragment.getDatasourceName(), attr.getValue());
				
				product.getAttributes().getUnmatchedAttributes().add(agg);			
			}

			
			// Removing 
			product.getAttributes().setUnmatchedAttributes(product.getAttributes().getUnmatchedAttributes().stream()
					// TODO : Should be from path
					// TODO : apply from sanitisation
					.filter(e -> !e.getName().contains("CATEGORY")) 
//					.filter(e -> !toRemoveFromUnmatched.contains(e.getName())) 
					.collect(Collectors.toSet()));
			
			
			
			// TODO : Removing matchlist again to handle remove of old attributes in case of configuration change
//		product.getAttributes().getUnmapedAttributes().
		} catch (Exception e) {
			// TODO
			dedicatedLogger.error("Unexpected error",e);
			e.printStackTrace();
		}
		
		
		onProduct(product, vConf);
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
	private boolean isFeatureAttribute(Attribute e, AttributesConfig attributesConfig) {
		return e.getRawValue() == null ? false :  attributesConfig.getFeaturedValues().contains(e.getRawValue().trim().toUpperCase());
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


			final String val = attr.getRawValue();
			for (final String match : conf.getParser().getTokenMatch()) {
				if (val.contains(match)) {
					attr.setRawValue(match);
					found = true;
					break;
				}
			}
		

			if (!found) {
				throw new ValidationException("Token "+ attr.stringValue() + " does not match  attribute " + attr.getName() + " specifiction");
			}

		}

		
		
		
		/////////////////////////////////
		// FIXED TEXT MAPPING
		/////////////////////////////////
		if (!conf.getMappings().isEmpty() ) {			
			String replacement = conf.getMappings().get(attr.getRawValue());
			attr.setRawValue(replacement);			
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
				final String parserRes = parser.parse(attr.getRawValue(), attr, conf);
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


		return attr;

	}






}
