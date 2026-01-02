package org.open4goods.api.services.aggregation.services.realtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributeParser;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
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
		// TODO(p3,perf) / Usefull for batch mode, could remove once initial sanitization
		vConf.getAttributesConfig().getExclusions().forEach(e -> {
			data.getAttributes().getAll().remove(e);
		});



		/////////////////////////////////////////////////
		// Cleaning brands
		// NOTE : Should be disabled after recovery batch, but need to be run each time to
		// take in account modifications of configurable brandAlias() and brandExclusions()
		/////////////////////////////////////////////////

		if (null != data.getEprelDatas()) {
			String supplier = data.getEprelDatas().getSupplierOrTrademark();
			data.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, supplier);
			data.addBrand("eprel", supplier, vConf.getBrandsExclusion(), vConf.getBrandsAlias());

			String model = data.getEprelDatas().getModelIdentifier();
			data.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, model);
			data.addModel(model);



		} else 	{
			String actualBrand = data.brand();
			Map<String,String> akaBrands = new HashMap<>(data.getAkaBrands());

			data.getAttributes().getReferentielAttributes().remove(ReferentielKey.BRAND);
			data.akaBrands().clear();
			// NOTE : No datasource for first, cause first will be set as referentiel brand
			data.addBrand(null, actualBrand, vConf.getBrandsExclusion(), vConf.getBrandsAlias());

			akaBrands.entrySet().forEach(e -> {
				data.addBrand(e.getKey(), e.getValue(), vConf.getBrandsExclusion(), vConf.getBrandsAlias());
			});
			// Adding model from title
			extractModelFromTitles(data);

		}


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
			AttributeConfig attrConfig = attributesConfig.resolveFromProductAttribute(attr);

			// We have a "raw" attribute that matches an aggregationconfig
			if (null != attrConfig) {

				try {

					// Applying parsing rule
					String cleanedValue =  parseAttributeValue(attr, attrConfig, vConf);

					if (StringUtils.isEmpty(cleanedValue)) {
						dedicatedLogger.error("Empty indexed attribute value {}:{}",attrConfig.getKey(),attr.getValue());
						continue;
					}

                                        IndexedAttribute indexedAttr = indexed.get(attrConfig.getKey());
                                        if (null != indexedAttr) {
                                                dedicatedLogger.info("Duplicate attribute candidate for indexation, for GTIN : {} and attrs {}",data.getId(), attrConfig.getKey());
                                                if (!cleanedValue.equals(indexedAttr.getValue() )) {
                                                        // TODO(p3,design) : Means we have multiple attributes matching for indexed . Have a merge strategy
                                                        dedicatedLogger.error("Value mismatch for attribute {} : {}<>{}",attr.getName(),cleanedValue, indexedAttr.getValue());
                                                }
                                        } else {
                                                 indexedAttr = new IndexedAttribute(attrConfig.getKey(), cleanedValue);

                                                 // Todo : force value through referenced datasources order
                                                 // TO


                                        }

                                        mergeSourcesAndRefreshValue(indexedAttr, attr, attrConfig);
                                        indexed.put(attrConfig.getKey(), indexedAttr);

                                } catch (Exception e) {
                                        dedicatedLogger.error("Attribute parsing fail for matched attribute {}", attrConfig.getKey(),e);
                                }
			}
		}


		// Replacing all previously indexed
		data.getAttributes().setIndexed(indexed);



		///////////////////////////////////////////
                // Setting excluded state
                //////////////////////////////////////////

                updateExcludeStatus(data,vConf);

        }

        private void mergeSourcesAndRefreshValue(IndexedAttribute indexedAttr, ProductAttribute attr, AttributeConfig attrConf) throws ValidationException
        {
                indexedAttr.getSource().addAll(attr.getSource());

                String bestValue = indexedAttr.bestValue();
                if (null == bestValue) {
                        return;
                }

                if (AttributeType.NUMERIC.equals(attrConf.getFilteringType())) {
                        bestValue = sanitizeNumericValue(bestValue);
                }

                indexedAttr.setValue(bestValue);
                indexedAttr.setBoolValue(IndexedAttribute.getBool(bestValue));

                if (AttributeType.NUMERIC.equals(attrConf.getFilteringType())) {
                        indexedAttr.setNumericValue(indexedAttr.numericOrNull(bestValue));
                } else {
                        try {
                                indexedAttr.setNumericValue(indexedAttr.numericOrNull(bestValue));
                        } catch (NumberFormatException e) {
                                indexedAttr.setNumericValue(null);
                        }
                }
        }


        /**
         * Extracts a manufacturer-like model identifier from offer titles (brand/model agnostic).
         *
         * Heuristics (conservative to reduce false positives):
         * - Candidate token must be mostly [A-Za-z0-9] with optional separators (- _ / .)
         * - Must contain digits; and either:
         *     (a) have >= 2 alpha<->digit transitions (e.g. HG32EJ690WE, TX25QUE), OR
         *     (b) have enough letters/digits and length to look like a true model (e.g. AB1234)
         * - Rejects common size/unit/resolution patterns (e.g. 42pouces, 55inch, 1920x1080, 144Hz, 1000W)
         *
         * If a best model is found, it updates data.forceModel(best) and stores alternates in akaModels.
         *
         * @param data the product from which to extract model information
         */
        public void extractModelFromTitles(Product data) {
            if (data == null || data.getOfferNames() == null || data.getOfferNames().isEmpty()) {
                dedicatedLogger.info("No offer titles available; cannot extract model.");
                return;
            }

            // Broad token finder: "model-ish" chunks including separators, min length 5
            // Examples matched: "HG32EJ690WE", "TX-25QUE", "AB1234", "SM-G991B"
            java.util.regex.Pattern tokenPattern =
                    java.util.regex.Pattern.compile("(?i)(?<![A-Z0-9])[A-Z0-9][A-Z0-9._/\\-]{3,}[A-Z0-9](?![A-Z0-9])");

            java.util.Map<String, Integer> freq = new java.util.HashMap<>();
            java.util.Map<String, String> originalByNorm = new java.util.HashMap<>();

            for (String offerName : data.getOfferNames()) {
                if (offerName == null || offerName.isBlank()) continue;

                java.util.regex.Matcher m = tokenPattern.matcher(offerName);
                while (m.find()) {
                    String raw = m.group();
                    String candidate = trimEdgePunct(raw);
                    if (candidate.isEmpty()) continue;

                    if (!isLikelyManufacturerModel(candidate)) continue;

                    String norm = candidate.toUpperCase();
                    freq.put(norm, freq.getOrDefault(norm, 0) + 1);
                    originalByNorm.putIfAbsent(norm, norm);
                }
            }

            if (freq.isEmpty()) {
                dedicatedLogger.info("No manufacturer-like model found in offer titles.");
                return;
            }

            // Pick best: highest frequency, then shortest, then lexical
            String best = null;
            int bestCount = -1;

            for (java.util.Map.Entry<String, Integer> e : freq.entrySet()) {
                String cand = e.getKey();
                int count = e.getValue();

                if (best == null) {
                    best = cand;
                    bestCount = count;
                    continue;
                }

                if (count > bestCount) {
                    best = cand;
                    bestCount = count;
                } else if (count == bestCount) {
                    int lenCand = stripSeparators(cand).length();
                    int lenBest = stripSeparators(best).length();
                    if (lenCand < lenBest || (lenCand == lenBest && cand.compareTo(best) < 0)) {
                        best = cand;
                    }
                }
            }

            String currentModel = data.model(); // can be null; we still update if best exists
            if (currentModel == null || !best.equalsIgnoreCase(currentModel)) {
                data.forceModel(best);
                dedicatedLogger.info("Model updated from '" + currentModel + "' to '" + best + "'.");
            }

            // Store alternates (including other frequent candidates)
            for (String cand : freq.keySet()) {
                if (cand.equalsIgnoreCase(best)) continue;
                if (!data.getAkaModels().contains(cand)) {
                    data.getAkaModels().add(cand);
                    dedicatedLogger.info("Added alternate model: " + cand);
                }
            }
        }

        /** Conservative validator for manufacturer-like models. */
        private static boolean isLikelyManufacturerModel(String token) {
            String up = token.toUpperCase();

            // Quick rejects: resolutions like 1920x1080, 3840X2160, etc.
            if (up.matches(".*\\d{3,4}[X]\\d{3,4}.*")) return false;

            // Extract alnum-only for analysis
            String alnum = up.replaceAll("[^A-Z0-9]", "");
            if (alnum.length() < 5) return false;

            int letters = 0;
            int digits = 0;
            for (int i = 0; i < alnum.length(); i++) {
                char c = alnum.charAt(i);
                if (c >= 'A' && c <= 'Z') letters++;
                else if (c >= '0' && c <= '9') digits++;
            }

            // digits-only models allowed only if long enough (avoid years, sizes, etc.)
            if (letters == 0) return digits >= 5;

            // Must contain at least one digit
            if (digits == 0) return false;

            // Reject "size/unit" single-suffix patterns like 42POUCES / 55INCH / 1000W / 144HZ / 500GB etc.
            if (looksLikeMeasureOrUnit(alnum)) return false;

            boolean hasSeparator = up.matches(".*[._/\\-].*");
            int transitions = countAlphaDigitTransitions(alnum);

            // Strong signal: at least 2 transitions (letters->digits->letters or digits->letters->digits)
            if (transitions >= 2) return true;

            // Allow some common manufacturer formats with one transition if “dense enough”
            // e.g. AB1234, E2100, RX7800XT (though RX7800XT has 2 transitions; AB1234 has 1)
            if (letters >= 2 && digits >= 3 && alnum.length() >= 6) return true;

            // If it has separators, be a bit more permissive (still requires some density)
            if (hasSeparator && letters >= 2 && digits >= 2 && alnum.length() >= 5) return true;

            return false;
        }

        private static boolean looksLikeMeasureOrUnit(String alnum) {
            // Pattern: digits + unit word (single transition), e.g. 42POUCES, 55INCH, 144HZ, 1000W, 500GB
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d{2,5})([A-Z]{1,10})$").matcher(alnum);
            if (!m.matches()) return false;

            String suffix = m.group(2);

            // Common units/specs that often appear in titles and are NOT models
            java.util.Set<String> badSuffixes = java.util.Set.of(
                    "POUCE", "POUCES", "INCH", "INCHES", "CM", "MM",
                    "HZ", "W", "KW", "V", "AH", "MAH",
                    "GB", "TB", "MB",
                    "MP", "FPS", "NITS", "LUMENS",
                    "K" // catches 4K, 8K etc (we also avoid short length elsewhere)
            );

            if (badSuffixes.contains(suffix)) return true;

            // Also reject plural-ish / common French/English variants
            if (suffix.startsWith("POUC")) return true;
            if (suffix.startsWith("INCH")) return true;

            return false;
        }

        private static int countAlphaDigitTransitions(String alnum) {
            int transitions = 0;
            boolean prevIsDigit = Character.isDigit(alnum.charAt(0));
            for (int i = 1; i < alnum.length(); i++) {
                boolean isDigit = Character.isDigit(alnum.charAt(i));
                if (isDigit != prevIsDigit) transitions++;
                prevIsDigit = isDigit;
            }
            return transitions;
        }

        private static String stripSeparators(String s) {
            return s.replaceAll("[._/\\-]", "");
        }

        private static String trimEdgePunct(String s) {
            // trim common edge punctuation while keeping internal separators
            return s.replaceAll("^[\\p{Punct}]+|[\\p{Punct}]+$", "");
        }



	/**
	 * Set the product in excluded state (will not be exposed through indexation, searchservice,..)
	 * @param data
	 */
	private void updateExcludeStatus(Product data, VerticalConfig vConf) {
		boolean ret = false;
		// On brand
		if (StringUtils.isEmpty(data.brand())) {
			dedicatedLogger.info("Excluded because brand is missing : {}", data );
			ret =  true;
			data.getExcludedCauses().add("missing_brand");
		}

		// On model
		if (StringUtils.isEmpty(data.model())) {
			dedicatedLogger.info("Excluded because model is missing : {}", data );
			ret =  true;
			data.getExcludedCauses().add("missing_model");
		}

		// On eprel
		if (null == data.getEprelDatas()) {
			dedicatedLogger.info("Excluded because no EPREL association : {}", data );
			ret =  true;
			data.getExcludedCauses().add("missing_eprel");
		}


		Set<String> attrKeys = data.getAttributes().getattributesAsStringKeys();
                if (vConf.getRequiredAttributes() != null) {

                        Set<String> missing = new HashSet<>(vConf.getRequiredAttributes());
                        missing.removeAll(attrKeys);

                        if (!missing.isEmpty()) {
                                missing.forEach(e-> {
                                        data.getExcludedCauses().add("missing_attr_" + e);
                                });

                                dedicatedLogger.info("Excluded because required attributes are missing : {}", data );
                                ret =  true;
                        }

                }

		data.setExcluded(ret);

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

				agg.addSourceAttribute(new SourcedAttribute(attr, dataFragment.getDatasourceName()));

				// Replacing new AggAttribute in product
				product.getAttributes().getAll().put(agg.getName(), agg);

			}


			// Checking model name from product words
//			completeModelNames(product, dataFragment.getReferentielAttributes().get(ReferentielKey.MODEL));

			/////////////////////////////////////////
			// Update referentiel attributes
			/////////////////////////////////////////
			handleReferentielAttributes(dataFragment, product, vConf);
			// TODO : Add BRAND / MODEL from matches from attributes


		} catch (Exception e) {
			dedicatedLogger.error("Unexpected error", e);
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
//	private void  extractFeatures(ProductAttributes attributes) {
//
//		attributes.getFeatures().clear();
//
//
//		Map<String, ProductAttribute> features = attributes.getAll().entrySet().stream()
//				.filter(e -> e.getValue().isFeature())
//			    .collect(Collectors.toMap(
//			            Map.Entry::getKey,    // key mapper: uses the key from each entry
//			            Map.Entry::getValue   // value mapper: uses the value from each entry
//			        ));
//
//		attributes.getFeatures().addAll(features.values());
//
//	}

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
	private void handleReferentielAttributes(DataFragment fragment, Product output, VerticalConfig vConf) {

		///////////////////////
		// Adding brand
		///////////////////////
		String brand = fragment.brand();
		if (!StringUtils.isEmpty(brand)) {
			output.addBrand(fragment.getDatasourceName(), brand, vConf.getBrandsExclusion(), vConf.getBrandsAlias());
		}


		///////////////////////
		// Adding model
		///////////////////////
		String model = fragment.getReferentielAttributes().get(ReferentielKey.MODEL);
		if (!StringUtils.isEmpty(model)) {
			output.addModel(model);
		}

		///////////////////////
		// Handling gtin (NOTE : useless since gtin is used as ID, so coupling is done previously
		///////////////////////
		String gtin = fragment.gtin();
		if (!StringUtils.isEmpty(gtin)) {
			String existing = output.gtin();

			if (!existing.equals(gtin)) {
				try {
					long existingGtin = Long.parseLong(existing);
					long newGtin = Long.parseLong(gtin);
					if (existingGtin != newGtin) {
						dedicatedLogger.error("Overriding GTIN from {} to {}", existing, newGtin);
						output.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN, gtin);
					}
				} catch (NumberFormatException e) {
					dedicatedLogger.error("Invalid GTIN format: existing = {}, new = {}", existing, gtin, e);
				}
			}

		}


	}

	/**
	 * Type attribute and apply parsing rules. Return null if the Attribute fail to
	 * exact parsing rules
	 * @param vConf
	 *
	 * @param translated
	 * @param attributeConfigByKey
	 * @return
	 * @throws ValidationException
	 */
	public String parseAttributeValue(final ProductAttribute attr, final AttributeConfig attrConf, VerticalConfig vConf) throws ValidationException {

		String string = attr.getValue();
		///////////////////
		// To upperCase / lowerCase
		///////////////////
		if (attrConf.getParser().getLowerCase()) {

			string = string.toLowerCase();
		}

		if (attrConf.getParser().getUpperCase()) {
			string = string.toUpperCase();
		}

		//////////////////////////////
		// Deleting arbitrary tokens
		//////////////////////////////

		if (null != attrConf.getParser().getDeleteTokens()) {
			for (final String token : attrConf.getParser().getDeleteTokens()) {
				string = string.replace(token, "");
			}
		}

		///////////////////
		// removing parenthesis tokens
		///////////////////
		if (attrConf.getParser().isRemoveParenthesis()) {
			string = string.replace("\\(.*\\)", "");
		}

		///////////////////
		// Normalisation
		///////////////////
		if (attrConf.getParser().getNormalize()) {
			string = StringUtils.normalizeSpace(string);
		}

		///////////////////
		// Trimming
		///////////////////
		if (attrConf.getParser().getTrim()) {
			string = string.trim();
		}

		///////////////////
		// Exact match option
		///////////////////

		if (null != attrConf.getParser().getTokenMatch()) {
			boolean found = false;

			final String val = string;
			for (final String match : attrConf.getParser().getTokenMatch()) {
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
		if (!attrConf.getMappings().isEmpty()) {

			string = attrConf.getMappings().get(string);
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

		if (!StringUtils.isEmpty(attrConf.getParser().getClazz())) {
			try {
				final AttributeParser parser = attrConf.getParserInstance();
				string  = parser.parse(attr, attrConf, vConf);
			} catch (final ResourceNotFoundException e) {
				dedicatedLogger.warn("Error while applying specific parser for {}", attrConf.getParser().getClazz(), e);
				throw new ValidationException(e.getMessage());
			} catch (final Exception e) {
				dedicatedLogger.error("Unexpected exception while parsing {} with {}", string, attrConf.getParser().getClazz(), e);
				throw new ValidationException(e.getMessage());
			}
		}

                if (AttributeType.NUMERIC.equals(attrConf.getFilteringType())) {
                        string = sanitizeNumericValue(string);
                }

                return string;

        }

        private String sanitizeNumericValue(String value) throws ValidationException {
                if (StringUtils.isBlank(value)) {
                        throw new ValidationException("Empty numeric attribute");
                }

                String stripped = value.replace("\"", "");
                stripped = stripped.replace("”", "");
                stripped = stripped.replace("“", "");
                stripped = stripped.replace("''", "");
                stripped = stripped.replace("´´", "");
                stripped = stripped.replace("´", "");
                stripped = stripped.replace("’", "");

                String normalized = StringUtils.normalizeSpace(stripped);
                Matcher matcher = Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?").matcher(normalized);

                if (!matcher.find()) {
                        throw new ValidationException("Attribute is expected to be numeric : " + value);
                }

                String numericCandidate = matcher.group().replace(",", ".");

                try {
                        Double.parseDouble(numericCandidate);
                } catch (NumberFormatException e) {
                        throw new ValidationException("Attribute is expected to be numeric : " + value);
                }

                return numericCandidate;
        }

}
