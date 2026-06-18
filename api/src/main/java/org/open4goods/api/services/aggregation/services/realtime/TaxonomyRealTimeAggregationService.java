package org.open4goods.api.services.aggregation.services.realtime;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;

/**
 * Maps incoming product categories to a vertical.
 *
 * <p>The vertical association is a deterministic function of the product's
 * accumulated categories: it is resolved through
 * {@link VerticalsConfigService#getVerticalForCategories(Map)} and nothing else.
 * Keeping it purely category-driven matters for SEO — an unstable vertical changes
 * the canonical product URL and triggers 301 redirects.
 *
 * <p>Processing steps ({@link #onProduct(Product, VerticalConfig)}):
 * <ol>
 *   <li>Refresh the product's flat {@code datasourceCategories} set from its
 *       {@code categoriesByDatasources} map.</li>
 *   <li>Resolve the best-matching vertical from those categories.</li>
 *   <li>When a vertical matches, set it and propagate its Google taxonomy ID;
 *       otherwise clear both. Generated names are left untouched — the URL slug is
 *       (re)generated downstream by {@code NamesAggregationService}.</li>
 * </ol>
 *
 * <p>The {@link #onDataFragment(DataFragment, Product, VerticalConfig)} hook stores
 * the incoming fragment's category in {@code categoriesByDatasources} and then
 * delegates to {@link #onProduct}.
 */
public class TaxonomyRealTimeAggregationService extends AbstractAggregationService {

	private final VerticalsConfigService verticalService;

	/**
	 * @param logger          dedicated aggregation logger
	 * @param verticalService service providing vertical-to-category mappings
	 */
	public TaxonomyRealTimeAggregationService(final Logger logger, final VerticalsConfigService verticalService) {
		super(logger);
		this.verticalService = verticalService;
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
	 * Resolves or clears the product's vertical based solely on its accumulated
	 * categories.
	 *
	 * <p>The vertical is set when {@link VerticalsConfigService#getVerticalForCategories(Map)}
	 * returns a match, and cleared only when it does not (no categories, no matching
	 * category, or an excluding token). It is never unset on the basis of transient
	 * data such as offer names, so a correctly-categorized product keeps a stable
	 * vertical (and therefore a stable URL) across re-aggregations.
	 */
	@Override
	public void onProduct(final Product data, final VerticalConfig vConf) throws AggregationSkipException {

		// Rebuild the flat category set from the per-datasource map
		data.getDatasourceCategories().clear();
		data.getDatasourceCategories().addAll(data.getCategoriesByDatasources().values());

		// Resolve the vertical deterministically from the accumulated categories
		VerticalConfig vertical = verticalService.getVerticalForCategories(data.getCategoriesByDatasources());

		if (vertical != null) {
			if (data.getVertical() != null && !vertical.getId().equals(data.getVertical())) {
				dedicatedLogger.warn("Will erase existing vertical {} with {} for product {}",
						data.getVertical(), vertical.getId(), data.bestName());
			}
			data.setVertical(vertical.getId());
			data.setGoogleTaxonomyId(vertical.getGoogleTaxonomyId());
		} else {
			// No category matched a vertical: clear the association. Generated names
			// are left intact; NamesAggregationService regenerates the URL slug.
			data.setVertical(null);
			data.setGoogleTaxonomyId(null);
		}
	}

}
