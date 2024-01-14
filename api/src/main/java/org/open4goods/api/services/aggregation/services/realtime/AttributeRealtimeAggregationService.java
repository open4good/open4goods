package org.open4goods.api.services.aggregation.services.realtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractRealTimeAggregationService;
import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.attributes.AttributeParser;
import org.open4goods.config.yml.ui.AttributesConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.exceptions.ParseException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.ResourceHelper;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.UnindexedKeyValTimestamp;
import org.open4goods.model.product.AggregatedAttribute;
import org.open4goods.model.product.AggregatedFeature;
import org.open4goods.model.product.Product;
import org.open4goods.services.BrandService;
import org.open4goods.services.VerticalsConfigService;

public class AttributeRealtimeAggregationService extends AbstractRealTimeAggregationService {


	
	private final BrandService brandService;

	private VerticalsConfigService verticalConfigService;

	public AttributeRealtimeAggregationService(final VerticalsConfigService verticalConfigService,  BrandService brandService, final String logsFolder,boolean toConsole) {
		super(logsFolder,toConsole);
		this.verticalConfigService = verticalConfigService;
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

		if (dataFragment.getAttributes().size() == 0) {
			return;
		}
		
		try {
			


			AttributesConfig attributesConfig = verticalConfigService.getConfigByIdOrDefault(product.getVertical()).getAttributesConfig();

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
				if (toRemoveFromUnmatched.contains(attr.getName())) {
					continue;
				}
				
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
					.filter(e -> !toRemoveFromUnmatched.contains(e.getName())) 
					.collect(Collectors.toSet()));
			
			
			
			// TODO : Removing matchlist again to handle remove of old attributes in case of configuration change
//		product.getAttributes().getUnmapedAttributes().
		} catch (Exception e) {
			// TODO
			dedicatedLogger.error("Unexpected error",e);
			e.printStackTrace();
		}
	}



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
					output.getAlternativeIds().add(new UnindexedKeyValTimestamp(fragement.getDatasourceName(), value));				
					
					// Election 
					String shortest = null;
					
					for (UnindexedKeyValTimestamp model : output.getAlternativeIds()) {
						
						if (null == shortest) {
							shortest = model.getValue();
						} else  if (model.getValue().length() < shortest.length()) {
							shortest = model.getValue();
						}
					}
					
					output.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, shortest);					
					output.getAlternativeIds().removeIf(b -> b.getValue().equals(output.model()));
					
					
					
				} else if (key.equals(ReferentielKey.BRAND)) {
					
					value = brandService.normalizeBrand(value);
					if (null != value && !existing.equals(value)) {
						//TODO (gof) : elect best brand, exclude "non categorisée", ...
						dedicatedLogger.info("Adding different {} name as BRAND. Existing is {}, would have erased with {}",key,existing, value);						
						// Adding the old one in alternate brand
						output.getAlternativeBrands().add(new UnindexedKeyValTimestamp(fragement.getDatasourceName(), value));
						
						
						// Adding the new one
						output.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, value);

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
							dedicatedLogger.info("Cannot overide GTIN from {} to {} ",existing, value);						
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


	@Override
	public void handle(Product output) throws AggregationSkipException {

		
	}






}
