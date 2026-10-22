package org.open4goods.api.services.aggregation.services.realtime;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.model.Localisable;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductNameResolver;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.PrefixedAttrText;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregation service in charge of:
 * <ul>
 *   <li>Collecting and normalizing raw offer names from {@link DataFragment}s</li>
 *   <li>Generating localized product naming elements (URL, H1, etc.) from templates/config</li>
 * </ul>
 *
 * <p>External contract preserved: same class name, same method signatures, same return types.</p>
 */
public class NamesAggregationService extends AbstractAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(NamesAggregationService.class);

	/** Pattern for template placeholders like {@code {BRAND}}, {@code {DIAGONALE_POUCES}}. */
	private static final Pattern TEMPLATE_PLACEHOLDER = Pattern.compile("\\{([A-Z0-9_]+)\\}");

	private final VerticalsConfigService verticalService;
	private final BlablaService blablaService;

	public NamesAggregationService(final Logger logger,
			final VerticalsConfigService verticalService,
			final BlablaService blablaService) {
		super(logger);
		this.verticalService = verticalService;
		this.blablaService = blablaService;
	}

	/**
	 * Collects names from the incoming data fragment, normalizes them, and triggers product-level naming generation.
	 *
	 * @param df input fragment
	 * @param output target product being aggregated
	 * @param vConf vertical config provided to this aggregation pass
	 * @throws AggregationSkipException if aggregation must be skipped
	 */
	@Override
	public void onDataFragment(final DataFragment df, final Product output, final VerticalConfig vConf)
			throws AggregationSkipException {

		// Adding raw offer names (not localized).
		if (df != null && df.getNames() != null && output != null && output.getOfferNames() != null) {
			for (String raw : df.getNames()) {
				String normalized = normalizeName(raw);
				if (normalized != null) {
					output.getOfferNames().add(normalized);
				}
			}
		}

		onProduct(output, vConf);
	}

	/**
	 * Generates localized naming fields (URL, H1, etc.).
	 *
	 * @param data product to enrich
	 * @param vConf vertical configuration for this run
	 * @throws AggregationSkipException if aggregation must be skipped
	 */
	@Override
	public void onProduct(final Product data, final VerticalConfig vConf) throws AggregationSkipException {

		if (data == null) {
			return; // Nothing to do
		}

		logger.debug("Name generation for product {}", data.getId());

		// Cleaning offer names too long (can happen on some CSV parsing bugs)
		if (data.getOfferNames() != null && !data.getOfferNames().isEmpty()) {
			data.setOfferNames(
				data.getOfferNames()
					.stream()
					.filter(e -> e != null && e.length() < 200)
					.collect(java.util.stream.Collectors.toSet())
			);
		}

		// Getting the config for the category/vertical (fallback to defaults handled by service)
		final VerticalConfig resolvedVertical = verticalService.getConfigByIdOrDefault(data.getVertical());
		final Map<String, ProductI18nElements> i18nConfs =
				(resolvedVertical != null) ? resolvedVertical.getI18n() : null;

		// If no i18n config exists, skip name generation safely.
		if (i18nConfs != null && !i18nConfs.isEmpty()) {

			for (Map.Entry<String, ProductI18nElements> entry : i18nConfs.entrySet()) {
				final String lang = entry.getKey();
				final ProductI18nElements tConf = entry.getValue();
				if (StringUtils.isBlank(lang) || tConf == null) {
					continue;
				}

				try {




					// ---- URL ----
					// High cognitive load note:
					// data.getNames().getUrl() is assumed existing by domain model, but we still guard nulls
					// to avoid unexpected NPEs if upstream product object is partially initialized.
					final boolean urlMissing =
							data.getNames() == null
							|| data.getNames().getUrl() == null
							|| data.getNames().getUrl().get(lang) == null;

					if (urlMissing || (vConf != null && vConf.isForceNameGeneration())) {
						logger.debug("Generating product url for productId={} lang={}", data.getId(), lang);

						final PrefixedAttrText urlPrefix = tConf.getUrl();
						final String url = generateUrl(data, urlPrefix, vConf);

						if (data.getNames() != null && data.getNames().getUrl() != null) {
							data.getNames().getUrl().put(lang, url);
						}
					} else {
						logger.debug("Skipping URL generation for productId={} lang={}", data.getId(), lang);
					}


					// ---- Canonical product names ----
					if (data.getNames() != null) {
						final boolean forceNames = vConf != null && vConf.isForceNameGeneration();
						final String generatedDisplayName = computeSafeTemplate(data, tConf.getDisplayName());
						final String generatedCardName = computeSafeTemplate(data, tConf.getCardName());
						final String generatedPageTitle = computeSafeTemplate(data, tConf.getPageTitle());
						final String generatedSeoName = computeSafeTemplate(data, tConf.getSeoName());

						putCanonicalName(data, lang, data.getNames().getDisplayName(), generatedDisplayName, forceNames);
						putCanonicalName(data, lang, data.getNames().getCardName(), generatedCardName, forceNames);
						putCanonicalName(data, lang, data.getNames().getPageTitle(), generatedPageTitle, forceNames);
						putCanonicalName(data, lang, data.getNames().getSeoName(), generatedSeoName, forceNames);
					}


				} catch (InvalidParameterException ex) {
					logger.error("Error while computing names for product {}", data.getId(), ex);
				} catch (RuntimeException ex) {
					// Guardrail: do not let one language/config issue break the whole aggregation.
					logger.error("Unexpected error while computing names for product {}", data.getId(), ex);
				}
			}
		}

	}

	/**
	 * Generates a URL slug from GTIN + configured naming parts.
	 *
	 * @param data product
	 * @param urlPrefix url configuration (prefix + attrs)
	 * @param vConf current vertical config (used to decide strategy when vertical is undefined)
	 * @return sanitized url slug
	 * @throws InvalidParameterException if template generation fails
	 */
	private String generateUrl(final Product data, final PrefixedAttrText urlPrefix, final VerticalConfig vConf)
			throws InvalidParameterException {

		// Defensive: gtin can be null; original code would NPE.
		String url = "";

		final String urlSuffix;
		if (vConf == null || vConf.getId() == null) {
			// Undefined vertical: apply shortest offer name (as original).
			urlSuffix = data.shortestOfferName();
		} else {
			// Defined vertical: apply computed names from config.
			urlSuffix = StringUtils.stripAccents(computePrefixedText(data, urlPrefix, "-"));
		}

		url += data.gtin();

		if (StringUtils.isNotBlank(urlSuffix)) {
			if (StringUtils.isNotBlank(url)) {
				url += "-";
			}
			url += urlSuffix;
		}

		// Url sanitisation
		url = IdHelper.azCharAndDigits(url, "-");
		url = StringUtils.normalizeSpace(url).replace(' ', '-');
		url = url.replaceAll("-{2,}", "-");
		url = url.toLowerCase();

		if (url.endsWith("-")) {
			url = url.substring(0, url.length() - 1);
		}

		return url;
	}

	/**
	 * Computes a text made of:
	 * <ul>
	 *   <li>an optional template prefix (generated via {@link BlablaService})</li>
	 *   <li>followed by configured product attributes present on the product</li>
	 * </ul>
	 *
	 * @param data product
	 * @param config configuration (prefix + list of attributes)
	 * @param separator separator between chunks (e.g. " " or "-")
	 * @return computed text (possibly empty)
	 * @throws InvalidParameterException if template evaluation fails
	 */
	private String computePrefixedText(final Product data, final PrefixedAttrText config, final String separator)
			throws InvalidParameterException {

		if (data == null || config == null) {
			return "";
		}

		final StringBuilder sb = new StringBuilder();

		// Adding the prefix (template)
		final String p = config.getPrefix();
		if (StringUtils.isNotBlank(p)) {
			final String prefix = blablaService.generateBlabla(p, data);
			if (StringUtils.isNotBlank(prefix)) {
				sb.append(prefix);
			}
		}

		// Adding configured attrs if present
		if (config.getAttrs() != null) {
			for (String attr : config.getAttrs()) {
				if (StringUtils.isBlank(attr) || data.getAttributes() == null) {
					continue;
				}
				final String refVal = data.getAttributes().val(attr);
				if (refVal != null) {
					// Note: azCharAndDigits() already filters characters; lowercasing is kept (original behavior).
					if (sb.length() > 0) {
						sb.append(separator);
					}
					sb.append(IdHelper.azCharAndDigits(refVal).toLowerCase());
				}
			}
		}

		return sb.toString();
	}

	private String resolveAttributeSuffix(final VerticalConfig vConf, final String attr, final String lang) {
		if (vConf == null || vConf.getAttributesConfig() == null) {
			return "";
		}
		AttributeConfig attributeConfig = vConf.getAttributesConfig().getAttributeConfigByKey(attr);
		if (attributeConfig == null || attributeConfig.getSuffix() == null) {
			return "";
		}
		String suffix = attributeConfig.getSuffix().i18n(lang);
		return StringUtils.defaultString(suffix);
	}

	/**
	 * Persist a canonical localized product name when missing or forced.
	 *
	 * @param data product to name
	 * @param lang localization key
	 * @param target target localisable field
	 * @param generatedName optional generated name from vertical configuration
	 * @param forceNames whether existing names must be replaced
	 */
	private void putCanonicalName(final Product data, final String lang, final Localisable<String, String> target,
			final String generatedName, final boolean forceNames) {
		if (target == null || (!forceNames && StringUtils.isNotBlank(target.get(lang)))) {
			return;
		}

		String resolved = ProductNameResolver.resolve(data, generatedName);
		if (StringUtils.isNotBlank(resolved) && !ProductNameResolver.containsUnresolvedTemplate(resolved)) {
			target.put(lang, resolved);
		}
	}

	private String computeSafeTemplate(final Product data, final String template) {
		String computed = computeTemplate(data, template);
		return ProductNameResolver.isSafeProductName(computed, data) ? computed : "";
	}

	/**
	 * Normalizes a raw offer name.
	 *
	 * @param name raw name
	 * @return trimmed name, or null if blank / null
	 */
	private String normalizeName(final String name) {
		if (name == null) {
			return null;
		}
		final String trimmed = name.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/**
	 * Computes a text from a template string with placeholders like {BRAND}, {MODEL}, {ATTRIBUTE_KEY}.
	 * Placeholders are replaced by product attribute values.
	 * If a placeholder cannot be resolved, it is removed.
	 *
	 * @param data product data
	 * @param template the template string
	 * @return computed text
	 */
	private String computeTemplate(final Product data, final String template) {
		if (data == null || StringUtils.isBlank(template)) {
			return "";
		}



		java.util.regex.Matcher matcher = TEMPLATE_PLACEHOLDER.matcher(template);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			String key = matcher.group(1);
			String value = null;

			if ("BRAND".equals(key)) {
				value = data.brand();
			} else if ("MODEL".equals(key)) {
				value = data.model();
			} else {
				// Fallback to indexed attributes
				if (data.getAttributes() != null) {
					value = data.getAttributes().val(key);
				}
			}

			if (value != null) {
				// Sanitize value
				matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(value.trim()));
			} else {
				// If not found, replace with empty string
				matcher.appendReplacement(sb, "");
			}
		}
		matcher.appendTail(sb);

		return StringUtils.normalizeSpace(sb.toString());
	}

}
