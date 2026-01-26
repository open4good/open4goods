package org.open4goods.api.services.aggregation.services.realtime;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.embedding.util.EmbeddingVectorUtils;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.PrefixedAttrText;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregation service in charge of:
 * <ul>
 *   <li>Collecting and normalizing raw offer names from {@link DataFragment}s</li>
 *   <li>Generating localized product naming elements (URL, H1, etc.) from templates/config</li>
 *   <li>Computing an embedding vector used for downstream similarity/search (DistilCamemBERT)</li>
 * </ul>
 *
 * <p>External contract preserved: same class name, same method signatures, same return types.</p>
 */
public class NamesAggregationService extends AbstractAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(NamesAggregationService.class);



	/**
	 * Kept for DI compatibility even if currently unused.
	 */
	@SuppressWarnings("unused")
	private final EvaluationService evaluationService;

	private final VerticalsConfigService verticalService;
	private final BlablaService blablaService;
	private final DjlTextEmbeddingService embeddingService;
	private final DjlEmbeddingProperties embeddingProperties;

	public NamesAggregationService(final Logger logger,
			final VerticalsConfigService verticalService,
			final EvaluationService evaluationService,
			final BlablaService blablaService,
			final DjlTextEmbeddingService embeddingService,
			final DjlEmbeddingProperties embeddingProperties) {
		super(logger);
		this.evaluationService = evaluationService;
		this.verticalService = verticalService;
		this.blablaService = blablaService;
		this.embeddingService = embeddingService;
		this.embeddingProperties = embeddingProperties;
	}

	/**
	 * Collects names from the incoming data fragment, normalizes them, and triggers product-level naming generation.
	 *
	 * @param df input fragment
	 * @param output target product being aggregated
	 * @param vConf vertical config provided to this aggregation pass
	 * @return contract kept as-is (null)
	 * @throws AggregationSkipException if aggregation must be skipped
	 */
	@Override
	public Map<String, Object> onDataFragment(final DataFragment df, final Product output, final VerticalConfig vConf)
			throws AggregationSkipException {

		// Adding raw offer names (not localized).
		// Defensive checks: df / df.getNames() can be null depending on upstream parsing.
		if (df != null && df.getNames() != null && output != null && output.getOfferNames() != null) {
			// Small perf gain: avoid creating an intermediate Set via Collectors.toSet().
			for (String raw : df.getNames()) {
				String normalized = normalizeName(raw);
				if (normalized != null) {
					output.getOfferNames().add(normalized);
				}
			}
		}

		onProduct(output, vConf);
		return null; // contract preserved
	}

	/**
	 * Generates localized naming fields (URL, H1, etc.) and computes embeddings.
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

		logger.info("Name generation for product {}", data.getId());

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

		// If no i18n config exists, we can still compute embedding later; skip name generation safely.
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

					if (vConf != null && vConf.isForceNameGeneration() || urlMissing) {
						logger.debug("Generating product url for productId={} lang={}", data.getId(), lang);

						final PrefixedAttrText urlPrefix = tConf.getUrl();
						final String url = generateUrl(data, urlPrefix, vConf);

						if (data.getNames() != null && data.getNames().getUrl() != null) {
							data.getNames().getUrl().put(lang, url);
						}
					} else {
						logger.debug("Skipping URL generation for productId={} lang={}", data.getId(), lang);
					}

					// ---- H1 Title ----
					final boolean h1Missing =
							data.getNames() == null
							|| data.getNames().getH1Title() == null
							|| data.getNames().getH1Title().get(lang) == null;

					if ((vConf != null && vConf.isForceNameGeneration()) || data.getVertical() == null || h1Missing) {
						if (data.getNames() != null && data.getNames().getH1Title() != null) {
							data.getNames().getH1Title().put(lang, computePrefixedText(data, tConf.getH1Title(), " "));
						}
					}

					// ---- Pretty Name ----
					final boolean prettyMissing =
							data.getNames() == null
							|| data.getNames().getPrettyName() == null
							|| data.getNames().getPrettyName().get(lang) == null;

					if ((vConf != null && vConf.isForceNameGeneration()) || prettyMissing) {
						if (data.getNames() != null && data.getNames().getPrettyName() != null) {
							data.getNames().getPrettyName().put(lang,
									computePrettyName(data, tConf.getPrettyName(), vConf, lang, " "));
						}
					}

					// ---- Singular ----
					final boolean singularMissing =
							data.getNames() == null
							|| data.getNames().getSingular() == null
							|| data.getNames().getSingular().get(lang) == null;

					if ((vConf != null && vConf.isForceNameGeneration()) || singularMissing) {
						if (data.getNames() != null && data.getNames().getSingular() != null) {
							data.getNames().getSingular().put(lang,
									computePrettyName(data, tConf.getSingular(), vConf, lang, " "));
						}
					}

					// ---- Singular Designation ----
					final boolean singularDesignationMissing =
							data.getNames() == null
							|| data.getNames().getSingularDesignation() == null
							|| data.getNames().getSingularDesignation().get(lang) == null;

					if ((vConf != null && vConf.isForceNameGeneration()) || singularDesignationMissing) {
						if (data.getNames() != null && data.getNames().getSingularDesignation() != null) {
							data.getNames().getSingularDesignation().put(lang,
									computePrettyName(data, tConf.getSingularDesignation(), vConf, lang, " "));
						}
					}

					// SEO meta generation intentionally left commented-out (as in original),
					// because of previous disk-space / stack-trace issues and template evaluation failures.
					// Keeping behavior unchanged.

					// ---- Card Title ----
					if (tConf.getCardTitle() != null) {
						String computed = computeTemplate(data, tConf.getCardTitle());
						if (StringUtils.isNotBlank(computed)) {
							data.getNames().getCardTitle().put(lang, computed);
						}
					}

					// ---- Short Name ----
					if (tConf.getShortName() != null) {
						String computed = computeTemplate(data, tConf.getShortName());
						if (StringUtils.isNotBlank(computed)) {
							data.getNames().getShortName().put(lang, computed);
						}
					}

					// ---- Long Name ----
					if (tConf.getLongName() != null) {
						String computed = computeTemplate(data, tConf.getLongName());
						if (StringUtils.isNotBlank(computed)) {
							data.getNames().getLongName().put(lang, computed);
						}
					}


				} catch (InvalidParameterException ex) {
					logger.error("Error while computing names for product {}", data.getId(), ex);
				} catch (RuntimeException ex) {
					// Guardrail: do not let one language/config issue break the whole aggregation.
					logger.error("Unexpected error while computing names for product {}", data.getId(), ex);
				}
			}
		}

		// ---- Embedding computation  ----
		// Compute embeddings whenever enough descriptive text is available.
		try {
			String textToEmbed = buildEmbeddingText(data, resolvedVertical);
			String prefixedText = applyEmbeddingPrefix(textToEmbed);

			if (StringUtils.isNotBlank(prefixedText)) {
				final float[] embedding = embeddingService.embed(prefixedText);
				if (embedding != null) {
					// Forcing to a 512 dims vector
					float[] padded = IdHelper.to512(embedding);
					data.setEmbedding(EmbeddingVectorUtils.normalizeL2(padded));
				}
			}
		} catch (Exception ex) {
			logger.error("Error computing embedding for product {}", data.getId(), ex);
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

	/**
	 * Computes a pretty name made of an optional prefix and a list of attributes,
	 * appending attribute suffixes when configured in the vertical catalog.
	 *
	 * @param data product
	 * @param config configuration (prefix + list of attributes)
	 * @param vConf vertical configuration used to resolve attribute suffixes
	 * @param lang current language key
	 * @param separator separator between chunks (e.g. " ")
	 * @return computed pretty name
	 * @throws InvalidParameterException if template evaluation fails
	 */
	private String computePrettyName(final Product data, final PrefixedAttrText config, final VerticalConfig vConf,
			final String lang, final String separator) throws InvalidParameterException {

		if (data == null || config == null) {
			return "";
		}

		final StringBuilder sb = new StringBuilder();

		final String p = config.getPrefix();
		if (StringUtils.isNotBlank(p)) {
			final String prefix = blablaService.generateBlabla(p, data);
			if (StringUtils.isNotBlank(prefix)) {
				sb.append(prefix);
			}
		}

		if (config.getAttrs() != null) {
			for (String attr : config.getAttrs()) {
				if (StringUtils.isBlank(attr) || data.getAttributes() == null) {
					continue;
				}
				final String refVal = data.getAttributes().val(attr);
				if (refVal != null) {
					if (sb.length() > 0) {
						sb.append(separator);
					}
					sb.append(IdHelper.azCharAndDigits(refVal).toLowerCase());

					String suffix = resolveAttributeSuffix(vConf, attr, lang);
					if (StringUtils.isNotBlank(suffix)) {
						sb.append(" ").append(suffix);
					}
				}
			}
		}

		return StringUtils.normalizeSpace(sb.toString());
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
	 * Prefixes the provided text with the configured passage prefix.
	 *
	 * @param textToEmbed text describing the product
	 * @return prefixed text suitable for embedding, or an empty string if no input is provided
	 */
	private String applyEmbeddingPrefix(final String textToEmbed) {
		if (StringUtils.isBlank(textToEmbed)) {
			return "";
		}

		final String prefix = embeddingProperties.getPassagePrefix();
		if (StringUtils.isBlank(prefix)) {
			return textToEmbed;
		}

		return prefix.trim() + " " + textToEmbed;
	}

	/**
	 * Builds a concatenated text payload for multimodal embedding:
	 * <ul>
	 *     <li>brand and model (referential attributes)</li>
	 *     <li>best computed name</li>
	 *     <li>top offer names (deduplicated)</li>
	 *     <li>popular attribute name/value pairs (vertical whitelists)</li>
	 *     <li>vertical-localized prefixes (if configured)</li>
	 * </ul>
	 * The resulting text is length-limited to prevent tokenizer overload.
	 *
	 * @param data  product to describe
	 * @param vConf resolved vertical configuration (may be null)
	 * @return concatenated text ready for embedding
	 */
	String buildEmbeddingText(final Product data, final VerticalConfig vConf) {
		if (data == null) {
			return "";
		}

		final Set<String> chunks = new LinkedHashSet<>();

		// Brand / model first to anchor identity
		if (data.getAttributes() != null && data.getAttributes().getReferentielAttributes() != null) {
			String brand = data.getAttributes().getReferentielAttributes().get(ReferentielKey.BRAND);
			String model = data.getAttributes().getReferentielAttributes().get(ReferentielKey.MODEL);
			if (StringUtils.isNotBlank(brand)) {
				chunks.add(brand);
			}
			if (StringUtils.isNotBlank(model)) {
				chunks.add(model);
			}
		}

		// Localized vertical prefix (e.g., H1 prefix)
		if (vConf != null && vConf.getI18n() != null) {
			vConf.getI18n().values().stream()
					.map(ProductI18nElements::getH1Title)
					.filter(java.util.Objects::nonNull)
					.map(PrefixedAttrText::getPrefix)
					.filter(StringUtils::isNotBlank)
					.forEach(chunks::add);
		}

		// Best computed name
		String bestName = data.preferredName(null);
		if (StringUtils.isNotBlank(bestName)) {
			chunks.add(bestName);
		}

		// Offer names (deduped, limited to avoid runaway payloads)
		if (data.getOfferNames() != null) {
			List<String> offers = data.getOfferNames().stream()
					.filter(StringUtils::isNotBlank)
					.limit(5)
					.collect(Collectors.toList());
			chunks.addAll(offers);
		}

		// TODO : Should be all attributes
		// Popular attribute name/value pairs (whitelisted per vertical)
		List<String> popularAttributeKeys = vConf != null ? vConf.getPopularAttributes() : List.of();
		if (!popularAttributeKeys.isEmpty() && data.getAttributes() != null) {
			for (String key : popularAttributeKeys.stream().filter(StringUtils::isNotBlank).limit(10).toList()) {
				String value = data.getAttributes().val(key);
				if (StringUtils.isBlank(value)) {
					continue;
				}

				String label = key;
				if (vConf != null && vConf.getAttributesConfig() != null) {
					AttributeConfig attributeConfig = vConf.getAttributesConfig().getAttributeConfigByKey(key);
					if (attributeConfig != null && attributeConfig.getName() != null) {
						String localized = attributeConfig.getName().i18n("default");
						if (StringUtils.isNotBlank(localized)) {
							label = localized;
						}
					}
				}

				chunks.add(label + " " + value);
			}
		}

		// Flatten and bound length
		String combined = chunks.stream()
				.map(String::trim)
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.joining(" "));

		if (combined.length() > 1000) {
			return combined.substring(0, 1000);
		}
		return combined;
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



		// Simple regex to find {KEY} pattern
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{([A-Z0-9_]+)\\}");
		java.util.regex.Matcher matcher = pattern.matcher(template);

		StringBuffer sb = new StringBuffer();
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
