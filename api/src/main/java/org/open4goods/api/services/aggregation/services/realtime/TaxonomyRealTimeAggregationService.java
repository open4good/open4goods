package org.open4goods.api.services.aggregation.services.realtime;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.icecat.services.IcecatCategoryVerticalResolver;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;

/**
 * Maps incoming product categories to a vertical.
 *
 * <p>When the product carries an Icecat category id, the vertical is resolved
 * exclusively through the Git-authored Icecat-to-O4G mapping registry
 * ({@link IcecatCategoryVerticalResolver}): only a reviewed (approved) mapping may
 * assign a vertical, and an Icecat category with no approved mapping yields no
 * vertical — the legacy heuristic below is deliberately not used as a fallback for
 * it, so an unreviewed guess can never classify a product.
 *
 * <p>Otherwise (no Icecat category on the product), the vertical association
 * remains a deterministic function of the product's accumulated categories,
 * resolved through {@link VerticalsConfigService#getVerticalForCategories(Map)}.
 * Keeping it purely category-driven matters for SEO — an unstable vertical changes
 * the canonical product URL and triggers 301 redirects.
 *
 * <p>Processing steps ({@link #onProduct(Product, VerticalConfig)}):
 * <ol>
 *   <li>Refresh the product's flat {@code datasourceCategories} set from its
 *       {@code categoriesByDatasources} map.</li>
 *   <li>Resolve the best-matching vertical, from the reviewed Icecat registry when
 *       an Icecat category id is present, otherwise from category name matching.</li>
 *   <li>When a vertical matches, set it and propagate its Google taxonomy ID;
 *       otherwise clear both. Generated names are left untouched — the URL slug is
 *       (re)generated downstream by {@code NamesAggregationService}.</li>
 * </ol>
 *
 * <p>The {@link #onDataFragment(DataFragment, Product, VerticalConfig)} hook stores
 * the incoming fragment's category (and Icecat category id, if any) and then
 * delegates to {@link #onProduct}.
 */
public class TaxonomyRealTimeAggregationService extends AbstractAggregationService {

	private final VerticalsConfigService verticalService;
	private final IcecatCategoryVerticalResolver icecatCategoryVerticalResolver;

	/**
	 * @param logger                         dedicated aggregation logger
	 * @param verticalService                service providing vertical-to-category mappings
	 * @param icecatCategoryVerticalResolver resolver backed by the reviewed Icecat-to-O4G registry
	 */
	public TaxonomyRealTimeAggregationService(final Logger logger, final VerticalsConfigService verticalService,
			final IcecatCategoryVerticalResolver icecatCategoryVerticalResolver) {
		super(logger);
		this.verticalService = verticalService;
		this.icecatCategoryVerticalResolver = icecatCategoryVerticalResolver;
	}

	/**
	 * Registers the fragment's category (and Icecat category id) for its
	 * datasource, then delegates to {@link #onProduct(Product, VerticalConfig)}.
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

		if (input.getIcecatCategoryId() != null) {
			output.setIcecatCategoryId(input.getIcecatCategoryId());
		}

		onProduct(output, vConf);
	}

	/**
	 * Resolves or clears the product's vertical.
	 *
	 * <p>When the product has an Icecat category id, only a reviewed registry
	 * mapping can resolve a vertical for it; the category-matching heuristic is not
	 * consulted in that case, per the "no fallback" classification rule. Otherwise
	 * the vertical is set when {@link VerticalsConfigService#getVerticalForCategories(Map)}
	 * returns a match. The vertical is cleared only when nothing resolves it
	 * (no categories, no matching category, an excluding token, or no approved
	 * Icecat mapping). It is never unset on the basis of transient data such as
	 * offer names, so a correctly-categorized product keeps a stable vertical (and
	 * therefore a stable URL) across re-aggregations.
	 */
	@Override
	public void onProduct(final Product data, final VerticalConfig vConf) throws AggregationSkipException {

		// Rebuild the flat category set from the per-datasource map
		data.getDatasourceCategories().clear();
		data.getDatasourceCategories().addAll(data.getCategoriesByDatasources().values());

		VerticalConfig vertical;
		if (data.getIcecatCategoryId() != null) {
			vertical = icecatCategoryVerticalResolver.resolveVerticalId(data.getIcecatCategoryId(), LocalDate.now())
					.map(verticalService::getConfigById)
					.orElse(null);
		} else {
			// Resolve the vertical deterministically from the accumulated categories
			vertical = verticalService.getVerticalForCategories(data.getCategoriesByDatasources());
		}

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
