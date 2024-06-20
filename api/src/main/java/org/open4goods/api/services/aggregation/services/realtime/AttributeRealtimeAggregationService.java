package org.open4goods.api.services.aggregation.services.realtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.attributes.AttributeParser;
import org.open4goods.config.yml.ui.AttributesConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.exceptions.ParseException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.ResourceHelper;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.Brand;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.UnindexedKeyValTimestamp;
import org.open4goods.model.product.AggregatedAttribute;
import org.open4goods.model.product.AggregatedFeature;
import org.open4goods.model.product.Product;
import org.open4goods.services.BrandService;
import org.open4goods.services.IcecatService;
import org.open4goods.services.VerticalsConfigService;
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

		//////////////////////////////////////////////////////////////////////////		
		// Checking if all mandatory attributes are present for this product
		//////////////////////////////////////////////////////////////////////////
		if (!data.getAttributes().getAggregatedAttributes().keySet().containsAll(vConf.getAttributesConfig().getMandatory())) {
			// Missing attributes.
			
			Set<String> missing = vConf.getAttributesConfig().getMandatory();
			missing.removeAll(data.getAttributes().getAggregatedAttributes().keySet());
			
			dedicatedLogger.warn("{} excluded from {}. Missing mandatory attributes : {}",  data.getId(), vConf.getId(), missing);
			data.setExcluded(true);
		} else {
			data.setExcluded(false);			
		}

		
		// Attributing taxomy to attributes
		data.getAttributes().getUnmapedAttributes().forEach(a -> {
			Set<Integer> icecatTaxonomyIds = featureService.resolveFeatureName(a.getName());
			if (null != icecatTaxonomyIds) {
				dedicatedLogger.info("Found icecat taxonomy for {} : {}", a.getName(), icecatTaxonomyIds);
				a.setIcecatTaxonomyIds(icecatTaxonomyIds );
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
				
				// We have a "raw" attribute that matches a aggragationconfig								
				
				if (ResourceHelper.isImage(attr.getValue())) {
					product.addImage(attr.getValue(), attr.getName());
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

						AggregatedAttribute agg = product.getAttributes().getAggregatedAttributes().get(attr.getName());
						
						
						if (null == agg) {
							// A first time match
							agg = new AggregatedAttribute();
							agg.setName(attr.getName());
						} 
							
						
						
						toRemoveFromUnmatched.add(translated.getName());
					
						
						
						agg.addAttribute(translated,attrConfig, new UnindexedKeyValTimestamp(dataFragment.getDatasourceName(), translated.getValue()));
						
						// Replacing new AggAttribute in product
						product.getAttributes().getAggregatedAttributes().put(agg.getName(), agg);
					} catch (Exception e) {

						dedicatedLogger.error("Attribute parsing fail for matched attribute {}", translated);
					}				
				}
			}

			
			// Checking model name from product words
//			completeModelNames(product, dataFragment.getReferentielAttributes().get(ReferentielKey.MODEL));
			
			/////////////////////////////////////////
			// Update referentiel attributes
			/////////////////////////////////////////
			handleReferentielAttributes(dataFragment , product);
			// TODO : Add BRAND / MODEL from matches from attributes

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
				
				AggregatedAttribute agg = product.getAttributes().getUnmapedAttributes().stream().filter(e->e.getName().equals(attr.getName())).findAny().orElse(null);
				
				if (null == agg) {
					// A first time match
					agg = new AggregatedAttribute();
					agg.setName(attr.getName());
				} 
				agg.addAttribute(attr, new UnindexedKeyValTimestamp(dataFragment.getDatasourceName(), attr.getValue()));
				
				product.getAttributes().getUnmapedAttributes().add(agg);			
			}

			
			// Removing 
			product.getAttributes().setUnmapedAttributes(product.getAttributes().getUnmapedAttributes().stream()
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
	 * Aggregate ReferentielAttributes
	 * @param refAttrs
	 * @param aa
	 * @param output
	 */
	private void handleReferentielAttributes(DataFragment fragement, Product output) {

		
		for (Entry<ReferentielKey, String> attr : fragement.getReferentielAttributes().entrySet()) {

			ReferentielKey key = attr.getKey();

			String value = attr.getValue();

			String existing = output.getAttributes().getReferentielAttributes().get(key);

			if (!StringUtils.isEmpty(existing) && !existing.equals(value)) {
				//TODO(0.5,p2,feature) : handle conflicts and "best value" election on referentiel attributes
				if (key.equals(ReferentielKey.MODEL)) {
					dedicatedLogger.info("Adding different {} name as alternate id. Existing is {}, would have erased with {}",key,existing, value);					
					output.addModel(value);					
					
				} else if (key.equals(ReferentielKey.BRAND)) {
					
					// TODO : Not good.... Here is in fact a company resolution
					Brand model = brandService.resolveCompanyFromBrandName(value);
					if (null != model && !existing.equals(model.getName())) {
						//TODO (gof) : elect best brand, exclude "non categorisée", ...
						dedicatedLogger.info("Adding different {} name as BRAND. Existing is {}, would have erased with {}",key,existing, model.getName());						
						// Adding the old one in alternate brand
						output.getAlternativeBrands().add(new UnindexedKeyValTimestamp(fragement.getDatasourceName(), model.getName()));
						
						
						// Adding the new one
						output.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, model.getName());

						// Removing the current brand in any case
						output.getAlternativeBrands().removeIf(b -> b.getValue().equals(output.brand()));
					}
				} else if (key.equals(ReferentielKey.GTIN)) {
					if (null != value && !existing.equals(value)) {
						// If the same gtin but in a different UPC form
						if (Long.valueOf(value).longValue() == Long.valueOf(existing).longValue()) {	
							dedicatedLogger.info("Overiding GTIN from {} to {} ",existing, value);
							output.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN, value);
						} else {							
							dedicatedLogger.error("Cannot overide GTIN from {} to {} ",existing, value);						
						}
					}
				} 
				else {
					dedicatedLogger.warn("Skipping referentiel attribute erasure for {}. Existing is {}, would have erased with {}",key,existing, value);
				}
			} 
			
			
			else {
				// TODO : Bad design of this method
				if (key.equals(ReferentielKey.BRAND)) {
					Brand b = brandService.resolveCompanyFromBrandName(value);
					if (null == b) {
						dedicatedLogger.error("Should nor ! Unresolvable already set brand : {}", value);
					} else {
						value = b.getName();;						
					}
				}
				
				
				if (key.equals(ReferentielKey.MODEL)) {
					output.addModel(value);
				} else {
					output.getAttributes().addReferentielAttribute( key, value);
				}
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
