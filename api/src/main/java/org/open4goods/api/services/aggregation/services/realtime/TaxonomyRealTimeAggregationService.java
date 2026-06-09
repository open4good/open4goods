package org.open4goods.api.services.aggregation.services.realtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductTexts;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;

/**
 * Maps incoming product categories to a vertical and validates that the
 * product's offer names contain at least one canonical vertical name token.
 *
 * <p>Processing steps ({@link #onProduct(Product, VerticalConfig)}):
 * <ol>
 *   <li>Refresh the product's flat {@code datasourceCategories} set from its
 *       {@code categoriesByDatasources} map.</li>
 *   <li>Propagate the Google taxonomy ID from the vertical configuration.</li>
 *   <li>Look up which vertical best matches the product's categories.</li>
 *   <li>Confirm the vertical by checking that at least one offer name contains a
 *       canonical vertical name token; unset the vertical if none match.</li>
 *   <li>Clear the vertical when no categories are present at all.</li>
 * </ol>
 *
 * <p>The {@link #onDataFragment(DataFragment, Product, VerticalConfig)} hook stores
 * the incoming fragment's category in {@code categoriesByDatasources} and then
 * delegates to {@link #onProduct}.
 */
public class TaxonomyRealTimeAggregationService extends AbstractAggregationService {

	private final VerticalsConfigService verticalService;
	private final GoogleTaxonomyService taxonomyService;
	private final Map<String, Set<String>> verticalTokensCache = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * @param logger          dedicated aggregation logger
	 * @param verticalService service providing vertical-to-category mappings
	 * @param taxonomyService Google taxonomy resolver (used for name-token confirmation)
	 */
	public TaxonomyRealTimeAggregationService(final Logger logger, final VerticalsConfigService verticalService,
			final GoogleTaxonomyService taxonomyService) {
		super(logger);
		this.verticalService = verticalService;
		this.taxonomyService = taxonomyService;
	}

	/**
	 * Registers the fragment's category for its datasource, then delegates to
	 * {@link #onProduct(Product, VerticalConfig)}.
	 */
	@Override
	public void onDataFragment(final DataFragment input, final Product output,
			final VerticalConfig vConf) throws AggregationSkipException {

		String category = input.getCategory();
		if (!StringUtils.isEmpty(category)) {
			Map<String, String> categoriesByDatasources = output.getCategoriesByDatasources();
			if (categoriesByDatasources == null) {
				categoriesByDatasources = new HashMap<>();
				output.setCategoriesByDatasources(categoriesByDatasources);
			}
			categoriesByDatasources.put(input.getDatasourceConfigName(), category);
		}

		onProduct(output, vConf);
	}

	/**
	 * Resolves or clears the product's vertical based on its accumulated categories
	 * and offer names.
	 *
	 * <p>A product whose offer names do not contain any canonical vertical name token
	 * has its vertical unset, even if category matching succeeded, to prevent
	 * mis-classification from broad category strings.
	 *
	 * TODO: The language used for name-token lookup is hard-coded to French
	 * ({@code GoogleTaxonomyService.byId(...).getGoogleNames()}). This will break
	 * for non-French verticals once multi-language support is needed.
	 */
	@Override
	public void onProduct(final Product data, final VerticalConfig vConf) throws AggregationSkipException {

		// Rebuild the flat category set from the per-datasource map
		data.getDatasourceCategories().clear();
		data.getDatasourceCategories().addAll(data.getCategoriesByDatasources().values());

		// Propagate the Google taxonomy ID from vertical configuration
		data.setGoogleTaxonomyId(vConf.getGoogleTaxonomyId());

		// Resolve vertical from categories
		VerticalConfig vertical = verticalService.getVerticalForCategories(data.getCategoriesByDatasources());
		if (vertical != null) {
			if (data.getVertical() != null && !vertical.getId().equals(data.getVertical())) {
				dedicatedLogger.warn("Will erase existing vertical {} with {} for product {}, because of category {}",
						data.getVertical(), vertical.getId(), data.bestName());
			}
			data.setVertical(vertical.getId());
		} else {
			// Unsetting the vertical also requires cleaning previously generated
			// texts that may no longer be relevant when the product has no
			// associated category.
			data.setVertical(null);
			data.setNames(new ProductTexts());
		}

		// Confirm vertical by checking that at least one offer name contains a canonical token
		if (data.getVertical() != null) {

			// Build normalised offer-name list once; check each name individually to
			// avoid false positives from cross-name boundary matches in a concatenated bag.
			List<String> normalizedOfferNames = data.getOfferNames().stream()
					.map(n -> StringUtils.stripAccents(n).toLowerCase())
					.toList();

			try {
				if (vConf.getId() != null) {
					Set<String> verticalNames = verticalTokensCache.computeIfAbsent(vConf.getId(), id -> {
						try {
							return vConf.getTokenNames(
									taxonomyService.byId(vConf.getGoogleTaxonomyId()).getGoogleNames().values()
											.stream().map(e -> StringUtils.stripAccents(e.toLowerCase())).toList());
						} catch (Exception ex) {
							dedicatedLogger.error("Failed to compute vertical token names for " + id, ex);
							return Set.of();
						}
					});

					boolean confirmed = verticalNames.stream()
							.anyMatch(token -> normalizedOfferNames.stream().anyMatch(n -> n.contains(token)));
					if (confirmed) {
						dedicatedLogger.info("Vertical {} confirmed by product names match for {}", vConf, data);
						data.setVertical(vConf.getId());
					} else {
						data.setVertical(null);
						dedicatedLogger.info(
								"Vertical {} failed on product names match, unsetting vertical for {}", vConf, data);
					}
				}
			} catch (Exception e) {
				dedicatedLogger.error("Error while handling names vertical filtering", e);
			}
		}

		// Clear vertical when no categories are present
		if (data.getDatasourceCategories().isEmpty()) {
			dedicatedLogger.info("No category in {}, removing vertical", data);
			data.setVertical(null);
		}
	}

}
