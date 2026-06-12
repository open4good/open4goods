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
import org.open4goods.brand.model.Brand;
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
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.util.ProductModelCandidateHelper;
import org.open4goods.model.util.ProductModelCandidateHelper.ModelCandidateSource;
import org.open4goods.model.util.ProductModelCandidateHelper.TitleModelExtraction;
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
 * <p>TODO(p3,perf): Attribute exclusions in {@code onProduct} currently scan
 * and mutate the product attribute map on every realtime sanitisation pass,
 * even when neither the product attributes nor the vertical exclusion set
 * changed. This is useful while running cleanup batches because old indexed
 * products may still carry attributes that were excluded after ingestion. In
 * steady-state realtime aggregation, we can avoid the repeated map mutation
 * once products carry a sanitisation/config marker such as the vertical
 * exclusion version or hash. A safe optimization would be: run a one-time
 * cleanup batch for existing products, stamp products after exclusions are
 * applied, and skip this block only when the stamp matches the current vertical
 * config. Without that marker, skipping here can leave stale excluded
 * attributes in products that were indexed before a config change.
 * <p>TODO: Add BRAND / MODEL from attribute-match candidates (currently only
 * title-extraction is used).
 */
public class AttributeRealtimeAggregationService extends AbstractAggregationService {

	private static final Pattern NUMERIC_EXTRACT_PATTERN =
			Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?");
	private static final Pattern EMPTY_VALUE_SEPARATOR_PATTERN =
			Pattern.compile("[()_./-]+");
	private static final Pattern PARENTHESIS_CONTENT_PATTERN =
			Pattern.compile("\\(.*?\\)");
	private static final Pattern QUOTE_MARK_PATTERN =
			Pattern.compile("[\u201c\u201d\"\u00b4\u2019]|\u2019\u2019|\u00b4\u00b4");

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

		// Remove excluded attributes. This is intentionally repeated until product
		// sanitisation can be tied to a vertical-config version/hash; otherwise old
		// products can retain attributes excluded after their first ingestion.
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
			String canonicalSupplier = resolveBrandName(supplier, vConf);
			data.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, canonicalSupplier);
			data.addBrand("eprel", canonicalSupplier, vConf.getBrandsExclusion(), vConf.getBrandsAlias());

			String model = data.getEprelDatas().getModelIdentifier();
			data.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, model);
			data.addModel(model, ModelCandidateSource.EPREL);

		} else {
			String actualBrand = data.brand();
			Map<String, String> akaBrands = new HashMap<>(data.getAkaBrands());

			data.getAttributes().getReferentielAttributes().remove(ReferentielKey.BRAND);
			data.akaBrands().clear();
			// NOTE : No datasource for first, cause first will be set as referentiel brand
			data.addBrand(null, resolveBrandName(actualBrand, vConf), vConf.getBrandsExclusion(), vConf.getBrandsAlias());

			akaBrands.entrySet().forEach(e -> {
				data.addBrand(e.getKey(), resolveBrandName(e.getValue(), vConf), vConf.getBrandsExclusion(), vConf.getBrandsAlias());
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

		String cleanVal = bestValue == null ? null : bestValue.trim().replace(',', '.');
		if (IdHelper.isPureDouble(cleanVal)) {
			try {
				indexedAttr.setNumericValue(indexedAttr.numericOrNull(bestValue));
			} catch (NumberFormatException e) {
				indexedAttr.setNumericValue(null);
			}
		} else {
			if (AttributeType.NUMERIC.equals(attrConf.getFilteringType())) {
				throw new NumberFormatException("For input string: \"" + bestValue + "\"");
			} else {
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
	 * Extracts manufacturer-like model candidates from offer titles and promotes the
	 * best into the product identity.
	 *
	 * <p>Delegates all extraction and validation to
	 * {@link ProductModelCandidateHelper#extractModelsFromTitles} which applies the
	 * union of alpha-digit transition-density heuristics and
	 * {@link ProductModelCandidateHelper#isPersistableModelCandidate} guards.
	 *
	 * <p>When no canonical model is set yet, the best candidate is promoted via
	 * {@link Product#forceModel}. When a model already exists, all title candidates
	 * that differ from it are added as cleaned alternates in {@code akaModels}.
	 *
	 * @param data the product from which to extract model information
	 */
	public void extractModelFromTitles(Product data) {
		if (data == null || data.getOfferNames() == null || data.getOfferNames().isEmpty()) {
			dedicatedLogger.info("No offer titles available; cannot extract model.");
			return;
		}
		TitleModelExtraction extraction =
				ProductModelCandidateHelper.extractModelsFromTitles(data.getOfferNames());
		if (extraction.isEmpty()) {
			dedicatedLogger.info("No manufacturer-like model found in offer titles.");
			return;
		}
		String currentModel = data.model();
		if (StringUtils.isEmpty(currentModel)) {
			data.forceModel(extraction.best());
			dedicatedLogger.info("Model set from offer title: '{}'.", extraction.best());
		} else {
			for (String cand : extraction.ranked()) {
				if (cand.equalsIgnoreCase(currentModel)) {
					continue;
				}
				if (!data.getAkaModels().contains(cand)) {
					data.getAkaModels().add(cand);
					dedicatedLogger.info("Added alternate model from offer title: {}", cand);
				}
			}
		}
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
			output.addBrand(fragment.getDatasourceName(), resolveBrandName(brand, vConf), vConf.getBrandsExclusion(), vConf.getBrandsAlias());
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

	private String resolveBrandName(String rawBrand, VerticalConfig vConf) {
		if (StringUtils.isBlank(rawBrand)) {
			return rawBrand;
		}
		Brand brand = brandService.resolve(rawBrand, vConf.getBrandsAlias());
		return brand.getBrandName();
	}

}
