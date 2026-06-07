package org.open4goods.api.services.aggregation.services.realtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.icecat.services.IcecatFeatureResolver;
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
import org.open4goods.model.util.ProductModelCandidateHelper.ModelCandidateSource;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributeParser;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;

/**
 * Merges, parses, and normalises product attributes from incoming
 * {@link DataFragment}s and from Icecat taxonomy data.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Applies attribute exclusions defined in the vertical configuration.</li>
 *   <li>Resolves and deduplicates brand names using alias/exclusion rules.</li>
 *   <li>Extracts manufacturer model numbers from offer titles when no explicit
 *       model attribute is present.</li>
 *   <li>Classifies raw attributes as features or indexed numeric attributes.</li>
 *   <li>Parses numeric attribute values (with unit conversion) and stores them
 *       in the product's {@code indexed} map for downstream scoring.</li>
 *   <li>Merges descriptions from all datasources into the product.</li>
 * </ul>
 *
 * <p>TODO(p3,perf): Attribute exclusions in {@code onProduct} are applied on
 * every sanitisation run. Once an initial cleanup batch has run they could be
 * skipped in realtime mode.
 * <p>TODO: Add BRAND / MODEL from attribute-match candidates (currently only
 * title-extraction is used).
 */
public class AttributeRealtimeAggregationService extends AbstractAggregationService {

	private static final Pattern MODEL_TOKEN_PATTERN =
			Pattern.compile("(?i)(?<![A-Z0-9])[A-Z0-9][A-Z0-9._/\\-]{3,}[A-Z0-9](?![A-Z0-9])");
	private static final Pattern MEASURE_UNIT_PATTERN =
			Pattern.compile("^(\\d{2,5})([A-Z]{1,10})$");
	private static final Pattern NUMERIC_EXTRACT_PATTERN =
			Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?");
	private static final Pattern EMPTY_VALUE_SEPARATOR_PATTERN =
			Pattern.compile("[()_./-]+");
	private static final Pattern RESOLUTION_PATTERN =
			Pattern.compile(".*\\d{3,4}X\\d{3,4}.*");
	private static final Pattern NON_ALPHANUMERIC_PATTERN =
			Pattern.compile("[^A-Z0-9]");
	private static final Pattern MODEL_SEPARATOR_PATTERN =
			Pattern.compile("[._/\\-]");
	private static final Pattern EDGE_PUNCTUATION_PATTERN =
			Pattern.compile("^[\\p{Punct}]+|[\\p{Punct}]+$");
	private static final Pattern PARENTHESIS_CONTENT_PATTERN =
			Pattern.compile("\\(.*?\\)");
	private static final Pattern QUOTE_MARK_PATTERN =
			Pattern.compile("[\"”“´’]|''|´´");
	private static final Set<String> MEASURE_SUFFIXES = Set.of("POUCE", "POUCES", "INCH", "INCHES", "CM", "MM",
			"HZ", "W", "KW", "V", "AH", "MAH", "GB", "TB", "MB", "MP", "FPS", "NITS", "LUMENS", "K");

	private final BrandService brandService;
	private final VerticalsConfigService verticalConfigService;
	private final IcecatFeatureResolver featureResolver;

	/**
	 * Builds the realtime attribute aggregation service.
	 *
	 * @param verticalConfigService vertical configuration lookup service
	 * @param brandService          brand alias/exclusion service
	 * @param logger                logger dedicated to aggregation diagnostics
	 * @param featureResolver       Icecat feature-name resolver
	 */
	public AttributeRealtimeAggregationService(final VerticalsConfigService verticalConfigService,
			final BrandService brandService, final Logger logger, final IcecatFeatureResolver featureResolver) {
		super(logger);
		this.verticalConfigService = verticalConfigService;
		this.brandService = brandService;
		this.featureResolver = featureResolver;
	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {

		//////////////////////////////////////////
		// Cleaning attributes that must be discarded
		//////////////////////////////////////////

		// Remove excluded attributes.
		// TODO(p3,perf) / Usefull for batch mode, could remove once initial
		// sanitization
		Set<String> exclusions = vConf.getAttributesConfig().getExclusions();
		if (exclusions != null && !exclusions.isEmpty()) {
			data.getAttributes().getAll().keySet().removeAll(exclusions);
		}

		/////////////////////////////////////////////////
		// Cleaning brands
		// NOTE : Should be disabled after recovery batch, but need to be run each time
		///////////////////////////////////////////////// to
		// take in account modifications of configurable brandAlias() and
		///////////////////////////////////////////////// brandExclusions()
		/////////////////////////////////////////////////

		if (data.getEprelDatas() != null) {
			String supplier = data.getEprelDatas().getSupplierOrTrademark();
			data.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, supplier);
			data.addBrand("eprel", supplier, vConf.getBrandsExclusion(), vConf.getBrandsAlias());

			String model = data.getEprelDatas().getModelIdentifier();
			data.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, model);
			data.addModel(model, ModelCandidateSource.EPREL);

		} else {
			String actualBrand = data.brand();
			Map<String, String> akaBrands = new HashMap<>(data.getAkaBrands());

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

		// Attribute names often repeat across products; avoid resolving the same name
		// twice in this product pass when aliases point to the same raw attribute.
		Map<String, Set<Integer>> taxonomyByAttributeName = new HashMap<>();
		data.getAttributes().getAll().values().forEach(a -> {
			Set<Integer> icecatTaxonomyIds = taxonomyByAttributeName.computeIfAbsent(a.getName(),
					featureResolver::resolveFeatureName);
			if (!icecatTaxonomyIds.isEmpty()) {
				dedicatedLogger.info("Found icecat taxonomy for {} : {}", a.getName(), icecatTaxonomyIds);
				a.setIcecatTaxonomyIds(icecatTaxonomyIds);
			}
		});

		///////////////////////////////////////////////////
		// Extracting indexed attributes
		//////////////////////////////////////////////////
		AttributesConfig attributesConfig = vConf.getAttributesConfig();

		Map<String, IndexedAttribute> indexed = new HashMap<>();

		for (ProductAttribute attr : data.getAttributes().getAll().values()) {

			// Checking if a potential AggregatedAttribute
			AttributeConfig attrConfig = attributesConfig.resolveFromProductAttribute(attr);

			// We have a "raw" attribute that matches an aggregationconfig
			if (attrConfig != null) {

				try {

					// Applying parsing rule
					String cleanedValue = parseAttributeValue(attr, attrConfig, vConf);

					if (StringUtils.isEmpty(cleanedValue)) {
						if (isKnownEmptyAttributeValue(attr.getValue())) {
							dedicatedLogger.debug("Ignoring empty indexed attribute value {}:{}", attrConfig.getKey(),
									attr.getValue());
						} else {
							dedicatedLogger.warn("Empty indexed attribute value {}:{}", attrConfig.getKey(),
									attr.getValue());
						}
						continue;
					}

					IndexedAttribute indexedAttr = indexed.get(attrConfig.getKey());
					if (indexedAttr != null) {
						dedicatedLogger.info("Duplicate attribute candidate for indexation, for GTIN : {} and attrs {}",
								data.getId(), attrConfig.getKey());
						if (!cleanedValue.equals(indexedAttr.getValue())) {
							dedicatedLogger.warn("Value mismatch for attribute {} : {}<>{}", attr.getName(),
									cleanedValue, indexedAttr.getValue());
						}
					} else {
						indexedAttr = new IndexedAttribute(attrConfig.getKey(), cleanedValue);
					}

					mergeSourcesAndRefreshValue(indexedAttr, attr, attrConfig, vConf);
					indexed.put(attrConfig.getKey(), indexedAttr);

				} catch (ValidationException e) {
					dedicatedLogger.warn("Attribute parsing fail for matched attribute {}: {}", attrConfig.getKey(),
							e.getMessage());
				} catch (Exception e) {
					dedicatedLogger.error("Unexpected attribute parsing fail for matched attribute {}", attrConfig.getKey(), e);
				}
			}
		}

		// Replacing all previously indexed
		data.getAttributes().setIndexed(indexed);

		///////////////////////////////////////////
		// Setting excluded state
		//////////////////////////////////////////

		updateExcludeStatus(data, vConf);

	}

	/**
	 * Adds all sources from one raw attribute to the indexed attribute and elects a
	 * stable value from the cleaned source values.
	 *
	 * @param indexedAttr indexed attribute being built for the product
	 * @param attr        raw product attribute whose sources should be merged
	 * @param attrConf    vertical attribute parsing configuration
	 * @param vConf       vertical configuration owning the attribute
	 * @throws ValidationException when the elected value cannot be converted to the
	 *                             configured attribute type
	 */
	private void mergeSourcesAndRefreshValue(IndexedAttribute indexedAttr, ProductAttribute attr,
			AttributeConfig attrConf, VerticalConfig vConf) throws ValidationException {

		for (SourcedAttribute source : attr.getSource()) {
			try {
				String parsed = parseValue(source.getValue(), attrConf, vConf);
				source.setCleanedValue(parsed);
			} catch (ValidationException e) {
				dedicatedLogger.warn("Failed to parse source value {} for attribute {}", source.getValue(), attrConf.getKey());
			}
		}

		indexedAttr.getSource().addAll(attr.getSource());

		String bestValue = null;
		if (attrConf.getParser() != null && !StringUtils.isEmpty(attrConf.getParser().getClazz())) {
			try {
				bestValue = attrConf.getParserInstance().parse(attr, attrConf, vConf);
			} catch (Exception e) {
				dedicatedLogger.error("Error parsing attribute with custom parser", e);
			}
		}

		if (bestValue == null) {
			bestValue = indexedAttr.bestValue();
		}

		if (bestValue == null) {
			return;
		}

		if (AttributeType.NUMERIC.equals(attrConf.getFilteringType())) {
			bestValue = sanitizeNumericValue(bestValue);
		}

		indexedAttr.setValue(bestValue);

		if (AttributeType.NUMERIC.equals(attrConf.getFilteringType())) {
			indexedAttr.setNumericValue(indexedAttr.numericOrNull(bestValue));
		} else {
			try {
				indexedAttr.setNumericValue(indexedAttr.numericOrNull(bestValue));
			} catch (NumberFormatException e) {
				indexedAttr.setNumericValue(null);
			}
		}

		attr.setValue(bestValue);
	}

	/**
	 * Identifies merchant placeholders that mean the attribute is absent rather
	 * than invalid.
	 *
	 * @param value raw attribute value
	 * @return {@code true} when the value is a known placeholder
	 */
	private boolean isKnownEmptyAttributeValue(String value) {
		if (StringUtils.isBlank(value)) {
			return true;
		}
		String normalized = StringUtils.stripAccents(value).toLowerCase(Locale.ROOT);
		normalized = EMPTY_VALUE_SEPARATOR_PATTERN.matcher(normalized).replaceAll(" ");
		normalized = StringUtils.normalizeSpace(normalized);
		return Set.of("donnee non specifiee", "donnees non specifiees", "non specifie", "non specifiee", "false", "n a",
				"na", "nc", "null", "-").contains(normalized);
	}

	/**
	 * Extracts a manufacturer-like model identifier from offer titles (brand/model
	 * agnostic).
	 *
	 * Heuristics (conservative to reduce false positives): - Candidate token must
	 * be mostly [A-Za-z0-9] with optional separators (- _ / .) - Must contain
	 * digits; and either: (a) have >= 2 alpha<->digit transitions (e.g.
	 * HG32EJ690WE, TX25QUE), OR (b) have enough letters/digits and length to look
	 * like a true model (e.g. AB1234) - Rejects common size/unit/resolution
	 * patterns (e.g. 42pouces, 55inch, 1920x1080, 144Hz, 1000W)
	 *
	 * If a best model is found, it updates data.forceModel(best) and stores
	 * alternates in akaModels.
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
		Map<String, Integer> freq = new HashMap<>();

		for (String offerName : data.getOfferNames()) {
			if (offerName == null || offerName.isBlank())
				continue;

			Matcher m = MODEL_TOKEN_PATTERN.matcher(offerName);
			while (m.find()) {
				String raw = m.group();
				String candidate = trimEdgePunct(raw);
				if (candidate.isEmpty())
					continue;

				if (!isLikelyManufacturerModel(candidate))
					continue;

				String norm = candidate.toUpperCase();
				freq.merge(norm, 1, Integer::sum);
			}
		}

		if (freq.isEmpty()) {
			dedicatedLogger.info("No manufacturer-like model found in offer titles.");
			return;
		}

		// Pick best: highest frequency, then shortest, then lexical
		String best = null;
		int bestCount = -1;

		for (Map.Entry<String, Integer> e : freq.entrySet()) {
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

		String currentModel = data.model();
		if (StringUtils.isEmpty(currentModel)) {
			// No model set yet (null or empty string): promote the best candidate.
			data.forceModel(best);
			dedicatedLogger.info("Model updated from '{}' to '{}'.", currentModel, best);
		} else {
			// Store all title candidates, including the elected one, when they differ from
			// the referential model already present on the product.
			for (String cand : freq.keySet()) {
				if (cand.equalsIgnoreCase(currentModel)) {
					continue;
				}
				if (!data.getAkaModels().contains(cand)) {
					data.getAkaModels().add(cand);
					dedicatedLogger.info("Added alternate model: {}", cand);
				}
			}
		}
	}

	/** Conservative validator for manufacturer-like models. */
	private static boolean isLikelyManufacturerModel(String token) {
		String up = token.toUpperCase();

		// Quick rejects: resolutions like 1920x1080, 3840X2160, etc.
		if (RESOLUTION_PATTERN.matcher(up).matches())
			return false;

		// Extract alnum-only for analysis
		String alnum = NON_ALPHANUMERIC_PATTERN.matcher(up).replaceAll("");
		if (alnum.length() < 5)
			return false;

		int letters = 0;
		int digits = 0;
		for (int i = 0; i < alnum.length(); i++) {
			char c = alnum.charAt(i);
			if (c >= 'A' && c <= 'Z')
				letters++;
			else if (c >= '0' && c <= '9')
				digits++;
		}

		// digits-only models allowed only if long enough (avoid years, sizes, etc.)
		if (letters == 0)
			return digits >= 5;

		// Must contain at least one digit
		if (digits == 0)
			return false;

		// Reject "size/unit" single-suffix patterns like 42POUCES / 55INCH / 1000W /
		// 144HZ / 500GB etc.
		if (looksLikeMeasureOrUnit(alnum))
			return false;

		boolean hasSeparator = MODEL_SEPARATOR_PATTERN.matcher(up).find();
		int transitions = countAlphaDigitTransitions(alnum);

		// Strong signal: at least 2 transitions (letters->digits->letters or
		// digits->letters->digits)
		if (transitions >= 2)
			return true;

		// Allow some common manufacturer formats with one transition if “dense enough”
		// e.g. AB1234, E2100, RX7800XT (though RX7800XT has 2 transitions; AB1234 has
		// 1)
		if (letters >= 2 && digits >= 3 && alnum.length() >= 6)
			return true;

		// If it has separators, be a bit more permissive (still requires some density)
		if (hasSeparator && letters >= 2 && digits >= 2 && alnum.length() >= 5)
			return true;

		return false;
	}

	private static boolean looksLikeMeasureOrUnit(String alnum) {
		// Pattern: digits + unit word (single transition), e.g. 42POUCES, 55INCH,
		// 144HZ, 1000W, 500GB
		Matcher m = MEASURE_UNIT_PATTERN.matcher(alnum);
		if (!m.matches())
			return false;

		String suffix = m.group(2);

		if (MEASURE_SUFFIXES.contains(suffix))
			return true;

		// Also reject plural-ish / common French/English variants
		if (suffix.startsWith("POUC"))
			return true;
		if (suffix.startsWith("INCH"))
			return true;

		return false;
	}

	private static int countAlphaDigitTransitions(String alnum) {
		int transitions = 0;
		boolean prevIsDigit = Character.isDigit(alnum.charAt(0));
		for (int i = 1; i < alnum.length(); i++) {
			boolean isDigit = Character.isDigit(alnum.charAt(i));
			if (isDigit != prevIsDigit)
				transitions++;
			prevIsDigit = isDigit;
		}
		return transitions;
	}

	private static String stripSeparators(String s) {
		return MODEL_SEPARATOR_PATTERN.matcher(s).replaceAll("");
	}

	private static String trimEdgePunct(String s) {
		// trim common edge punctuation while keeping internal separators
		return EDGE_PUNCTUATION_PATTERN.matcher(s).replaceAll("");
	}

	/**
	 * Set the product in excluded state (will not be exposed through indexation,
	 * searchservice,..)
	 *
	 * @param data
	 */
	private void updateExcludeStatus(Product data, VerticalConfig vConf) {
		data.getExcludedCauses().clear();
		boolean ret = false;
		// On brand
		if (StringUtils.isEmpty(data.brand())) {
			dedicatedLogger.info("Excluded because brand is missing : {}", data);
			ret = true;
			data.getExcludedCauses().add("missing_brand");
		}

		// On model
		if (StringUtils.isEmpty(data.model())) {
			dedicatedLogger.info("Excluded because model is missing : {}", data);
			ret = true;
			data.getExcludedCauses().add("missing_model");
		}

		// On eprel
		if (data.getEprelDatas() == null) {
			dedicatedLogger.info("Excluded because no EPREL association : {}", data);
			ret = true;
			data.getExcludedCauses().add("missing_eprel");
		}

		Set<String> attrKeys = data.getAttributes().getattributesAsStringKeys();
		if (vConf.getRequiredAttributes() != null) {

			Set<String> missing = new HashSet<>(vConf.getRequiredAttributes());
			missing.removeAll(attrKeys);

			if (!missing.isEmpty()) {
				missing.forEach(e -> {
					data.getExcludedCauses().add("missing_attr_" + e);
				});

				dedicatedLogger.info("Excluded because required attributes are missing : {}", data);
				ret = true;
			}

		}

		data.setExcluded(ret);

	}

	/**
	 * Merges attributes from the incoming fragment into the product, then delegates
	 * to {@link #onProduct} for full attribute classification and parsing.
	 */
	@Override
	public void onDataFragment(final DataFragment dataFragment, final Product product,
			final VerticalConfig vConf) throws AggregationSkipException {

		try {
			handleDescriptions(dataFragment, product, vConf);

			for (Attribute attr : dataFragment.getAttributes()) {

				ProductAttribute agg = product.getAttributes().getAll().get(attr.getName());

				if (agg == null) {
					agg = new ProductAttribute();
					agg.setName(attr.getName());
				}

				agg.addSourceAttribute(new SourcedAttribute(attr, dataFragment.getDatasourceName()));
				product.getAttributes().getAll().put(agg.getName(), agg);
			}

			mergeExternalIds(dataFragment, product);
			handleReferentielAttributes(dataFragment, product, vConf);

		} catch (Exception e) {
			dedicatedLogger.error("Unexpected error", e);
		}

		onProduct(product, vConf);
	}

	private void mergeExternalIds(final DataFragment dataFragment, final Product product) {
		if (dataFragment == null || dataFragment.getExternalIds() == null || product == null || product.getExternalIds() == null) {
			return;
		}
		product.getExternalIds().getMpn().addAll(dataFragment.getExternalIds().getMpn());
		product.getExternalIds().getSku().addAll(dataFragment.getExternalIds().getSku());
	}

	private void handleDescriptions(final DataFragment dataFragment, final Product product, final VerticalConfig vConf) {
		if (dataFragment == null || product == null || vConf == null) {
			return;
		}
		Set<String> descriptionAttributes = vConf.getDescriptionAttributes();
		Set<String> normalized = new HashSet<>();
		if (descriptionAttributes != null) {
			descriptionAttributes.stream()
					.filter(StringUtils::isNotBlank)
					.map(name -> name.trim().toUpperCase(Locale.ROOT))
					.forEach(normalized::add);
		}
		if (!normalized.isEmpty() && dataFragment.getAttributes() != null) {
			dataFragment.getAttributes().removeIf(attr -> {
				String name = attr.getName();
				if (StringUtils.isBlank(name)) {
					return false;
				}
				String normalizedName = name.trim().toUpperCase(Locale.ROOT);
				if (!normalized.contains(normalizedName)) {
					return false;
				}
				mergeDescription(product, dataFragment.getDatasourceName(), attr.stringValue());
				return true;
			});
		}
		if (dataFragment.getDescriptionsByDatasource() != null
					&& !dataFragment.getDescriptionsByDatasource().isEmpty()) {
			dataFragment.getDescriptionsByDatasource().forEach((source, description) ->
					mergeDescription(product, source, description));
		}
	}

	private void mergeDescription(final Product product, final String datasourceName, final String description) {
		if (product == null || StringUtils.isBlank(datasourceName) || StringUtils.isBlank(description)) {
			return;
		}
		Map<String, String> descriptions = product.getDescriptionsByDatasource();
		if (descriptions == null) {
			descriptions = new HashMap<>();
			product.setDescriptionsByDatasource(descriptions);
		}
		String existing = descriptions.get(datasourceName);
		if (StringUtils.isBlank(existing) || description.length() > existing.length()) {
			descriptions.put(datasourceName, description);
		}
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
			output.addModel(model, ModelCandidateSource.DATASOURCE_REFERENTIAL);
		}

		///////////////////////
		// Handling gtin (NOTE : useless since gtin is used as ID, so coupling is done
		/////////////////////// previously
		///////////////////////
		String gtin = fragment.gtin();
		if (!StringUtils.isEmpty(gtin)) {
			output.getGtinInfos().addGtinString(gtin);
			String existing = output.gtin();

			if (StringUtils.isBlank(existing)) {
				output.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN, gtin);
			} else if (!existing.equals(gtin)) {
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
	 * Parses a product attribute with its configured parser and type rules.
	 *
	 * @param attr     product attribute to parse
	 * @param attrConf matching attribute configuration
	 * @param vConf    vertical configuration owning the attribute
	 * @return cleaned value, or an empty string for known empty placeholders
	 * @throws ValidationException when the value cannot satisfy the parser or type
	 *                             constraints
	 */
	public String parseAttributeValue(final ProductAttribute attr, final AttributeConfig attrConf, VerticalConfig vConf)
			throws ValidationException {

		if (attrConf.getParser() != null && !StringUtils.isEmpty(attrConf.getParser().getClazz())) {
			try {
				final AttributeParser parser = attrConf.getParserInstance();
				String string = parser.parse(attr, attrConf, vConf);
				if (AttributeType.NUMERIC.equals(attrConf.getFilteringType()) && string != null) {
					string = sanitizeNumericValue(string);
				}
				return string;
			} catch (final ResourceNotFoundException e) {
				dedicatedLogger.warn("Error while applying specific parser for {}", attrConf.getParser().getClazz(), e);
				throw new ValidationException(e.getMessage());
			} catch (final Exception e) {
				dedicatedLogger.error("Unexpected exception while parsing with {}",
						attrConf.getParser().getClazz(), e);
				throw new ValidationException(e.getMessage());
			}
		}

		return parseValue(attr.getValue(), attrConf, vConf);

	}

	/**
	 * Applies the generic parser options configured on an attribute value.
	 *
	 * @param rawValue raw value from a datasource
	 * @param attrConf attribute configuration containing parser options
	 * @param vConf    vertical configuration owning the attribute
	 * @return normalized and type-sanitized value, or an empty string for known
	 *         empty placeholders
	 * @throws ValidationException when the value is null or cannot satisfy parser
	 *                             constraints
	 */
	public String parseValue(final String rawValue, final AttributeConfig attrConf, VerticalConfig vConf)
			throws ValidationException {

		if (rawValue == null) {
			throw new ValidationException("Null rawValue in attribute " + attrConf.getKey());
		}

		String string = rawValue;
		///////////////////
		// To upperCase / lowerCase
		///////////////////
		if (Boolean.TRUE.equals(attrConf.getParser().getLowerCase())) {

			string = string.toLowerCase();
		}

		if (Boolean.TRUE.equals(attrConf.getParser().getUpperCase())) {
			string = string.toUpperCase();
		}

		//////////////////////////////
		// Deleting arbitrary tokens
		//////////////////////////////

		if (attrConf.getParser().getDeleteTokens() != null) {
			for (String token : attrConf.getParser().getDeleteTokens()) {
				if (Boolean.TRUE.equals(attrConf.getParser().getLowerCase())) {
					token = token.toLowerCase();
				}
				if (Boolean.TRUE.equals(attrConf.getParser().getUpperCase())) {
					token = token.toUpperCase();
				}
				string = string.replace(token, "");
			}
		}

		///////////////////
		// removing parenthesis tokens
		///////////////////
		if (attrConf.getParser().isRemoveParenthesis()) {
			string = PARENTHESIS_CONTENT_PATTERN.matcher(string).replaceAll("");
		}

		///////////////////
		// Normalisation
		///////////////////
		if (Boolean.TRUE.equals(attrConf.getParser().getNormalize())) {
			string = StringUtils.normalizeSpace(string);
		}

		///////////////////
		// Trimming
		///////////////////
		if (Boolean.TRUE.equals(attrConf.getParser().getTrim())) {
			string = string.trim();
		}

		if (isKnownEmptyAttributeValue(string)) {
			return "";
		}

		/////////////////////////////////
		// FIXED TEXT MAPPING
		/////////////////////////////////
		if (!attrConf.getMappings().isEmpty() && attrConf.getMappings().containsKey(string)) {

			string = attrConf.getMappings().get(string);
		}

		if (isKnownEmptyAttributeValue(string)) {
			return "";
		}

		///////////////////
		// Exact match option
		///////////////////

		if (attrConf.getParser().getTokenMatch() != null) {
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

		////////////////////////////////////
		// Applying specific parser instance
		/////////////////////////////////////

		if (!StringUtils.isEmpty(attrConf.getParser().getClazz())) {
			try {
				final AttributeParser parser = attrConf.getParserInstance();
				string = parser.parse(rawValue, attrConf, vConf);
			} catch (final ResourceNotFoundException e) {
				dedicatedLogger.warn("Error while applying specific parser for {}", attrConf.getParser().getClazz(), e);
				throw new ValidationException(e.getMessage());
			} catch (final Exception e) {
				dedicatedLogger.error("Unexpected exception while parsing {} with {}", string,
						attrConf.getParser().getClazz(), e);
				throw new ValidationException(e.getMessage());
			}
		}

		if (AttributeType.NUMERIC.equals(attrConf.getFilteringType())) {
			string = sanitizeNumericValue(string);
		}

		return string;

	}

	/**
	 * Extracts the first decimal-compatible number from a value and normalizes the
	 * decimal separator.
	 *
	 * @param value parsed numeric candidate
	 * @return Java-compatible decimal string
	 * @throws ValidationException when no parseable number exists
	 */
	private String sanitizeNumericValue(String value) throws ValidationException {
		if (StringUtils.isBlank(value)) {
			throw new ValidationException("Empty numeric attribute");
		}

		String stripped = QUOTE_MARK_PATTERN.matcher(value).replaceAll("");

		String normalized = StringUtils.normalizeSpace(stripped);
		Matcher matcher = NUMERIC_EXTRACT_PATTERN.matcher(normalized);

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
