package org.open4goods.api.services.aggregation.services.realtime;

import java.util.ArrayList;
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
 * Service in charge of mapping product categories to verticals
 *
 * @author goulven
 *
 */
public class TaxonomyRealTimeAggregationService extends AbstractAggregationService {

	private VerticalsConfigService verticalService;
	private GoogleTaxonomyService taxonomyService;

	public TaxonomyRealTimeAggregationService(final Logger logger, final VerticalsConfigService verticalService,
			GoogleTaxonomyService taxonomyService) {
		super(logger);
		this.verticalService = verticalService;
		this.taxonomyService = taxonomyService;

	}

	@Override
	public Map<String, Object> onDataFragment(final DataFragment input, final Product output, VerticalConfig vConf)
			throws AggregationSkipException {

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

		return new HashMap<String, Object>();
	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {

		////////////////////////////
		// Updating the categories
		////////////////////////////
		data.getDatasourceCategories().clear();
		data.getDatasourceCategories().addAll(data.getCategoriesByDatasources().values());

		////////////////////////////
		// Setting google taxonomy
		////////////////////////////
		data.setGoogleTaxonomyId(vConf.getGoogleTaxonomyId());
//		if (data.getDatasourceCategories().size() != 0) {
//			Integer taxonomy =   googleTaxonomy(data);
//			if (null != taxonomy) {
//				data.setGoogleTaxonomyId(taxonomy);
//				dedicatedLogger.info("Detected taxonomy {} for categories : {}", taxonomy, data.getDatasourceCategories());
//			} else {
//				dedicatedLogger.info("No taxonomy found for categories : {}", data.getDatasourceCategories());
//			}
//		}

		////////////////////////////
		// Setting vertical from category
		////////////////////////////
		VerticalConfig vertical = verticalService.getVerticalForCategories(data.getCategoriesByDatasources());
		if (null != vertical) {
			if (null != data.getVertical() && !vertical.getId().equals(data.getVertical())) {
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

		/////////////////////////////////////
		// A hard filtering on datasource categories, that must at least contains one of
		///////////////////////////////////// the
		// vertical natural names
		////////////////////////////////////
		if (null != data.getVertical()) {

			// Building offer name bags
			StringBuilder productNameBag = new StringBuilder();
			data.getOfferNames().forEach(name -> {
				productNameBag.append(StringUtils.stripAccents(name).toLowerCase());
			});
			String pNames = productNameBag.toString();

			try {

				if (null != vConf.getId()) {

					Set<String> verticalNames = vConf
							.getTokenNames(taxonomyService.byId(vConf.getGoogleTaxonomyId()).getGoogleNames().values()
									.stream().map(e -> StringUtils.stripAccents(e.toLowerCase())).toList());

					for (String term : verticalNames) {

						if (pNames.contains(term)) {
							dedicatedLogger.info("Vertical {} confirmed by product names match for {}", vConf, data);
							data.setVertical(vConf.getId());
							break;
						} else {
							data.setVertical(null);
						}

					}

					if (null == data.getVertical()) {
						dedicatedLogger.info("Vertical {} failed on product names match, unsetting vertical for {}",
								vConf, data);
					}
				}
			} catch (Exception e) {
				dedicatedLogger.error("Error while handling names vertical filtering", e);
			}
		}

//		// Setting no vertical if no category
		if (data.getDatasourceCategories().size() == 0) {
			dedicatedLogger.info("No category in {}, removing vertical", data);
			data.setVertical(null);
		}

		// TODO : Disabling google taxonomy for now

		// if (null != vertical && null != vertical.getTaxonomyId()) {
//			data.setGoogleTaxonomyId(vertical.getTaxonomyId());
//		} else {
//			if (data.getDatasourceCategories().size() != 0) {
//				Integer taxonomy =   googleTaxonomy(data);
//				if (null != taxonomy) {
//					data.setGoogleTaxonomyId(taxonomy);
//					dedicatedLogger.info("No taxonomy found for categories : {}", data.getDatasourceCategories());
//
//				} else {
//					dedicatedLogger.info("No taxonomy found for categories : {}", data.getDatasourceCategories());
//				}
//			}
//		}

	}

	/**
	 * Try to detect the google taxonomy id
	 *
	 * @param input
	 * @return
	 */
	private Integer googleTaxonomy(final Product input) {
		Integer taxonomyId = null;

		List<Integer> taxons = new ArrayList<>();

		input.getAttributes().getAll().values().forEach(a -> {
			String i = a.getName();

			if (i.contains("CATEGORY")) {
				Integer t = taxonomyService.resolve(a.getValue());
				if (null != t) {
					taxons.add(t);
				}
			}
		});

		if (taxons.size() == 1) {
			taxonomyId = taxons.stream().findAny().orElse(null);
		} else if (taxons.size() > 1) {
			// TODO : The language (should not be needed), will bug when other languages
			taxonomyId = taxonomyService.selectDeepest("fr", taxons);
		}

		return taxonomyId;
	}

}
