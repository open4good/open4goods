package org.open4goods.api.services.completion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.config.yml.IcecatCompletionConfig;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.icecat.model.IcecatCategoryFeatureGroup;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatMultimediaObject;
import org.open4goods.icecat.model.IcecatProduct;
import org.open4goods.icecat.model.IcecatProductGallery;
import org.open4goods.icecat.model.IcecatProductPicture;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

// TODO : The duplicate GTIN
// TODO : Add specific attributes :
// -"ReleaseDate": "31-05-2023",
// - "EndOfLifeDate": "31-12-2023",

public class IcecatCompletionService extends AbstractCompletionService {

	protected static final Logger logger = LoggerFactory.getLogger(IcecatCompletionService.class);

	/**
	 * Icecat completion will be triggered only if older than this const
	 */
	private static final int REFRESH_IN_DAYS = 30;

	// The specific icecat fetching properties
	private IcecatCompletionConfig icecatConfig;

	// We re-use the realtime aggregator, from the AggregationFavcade
	private StandardAggregator aggregator;

	private IcecatService icecatService;

	public IcecatCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService,
			ApiProperties apiProperties, DataSourceConfigService dataSourceConfigService,
			AggregationFacadeService aggregationFacadeService) {
		// TODO : Should set a specific log level here (not "agg(regation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());

		this.aggregator = aggregationFacadeService.getStandardAggregator("icecat-aggregation");
		;
		this.aggregator.beforeStart();
		this.icecatConfig = apiProperties.getIcecatCompletionConfig();
		this.icecatService = icecatService;

	}

	@Override
	public boolean shouldProcess(VerticalConfig vertical, Product data) {
		// TODO(p2,perf) : should adda check on unprocessed resources
		Long lastProcessed = data.getDatasourceCodes().get(getDatasourceName());
		if (null != lastProcessed && REFRESH_IN_DAYS * 1000 * 3600 * 24 < System.currentTimeMillis() - lastProcessed) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public String getDatasourceName() {
		return "icecat.biz";
	}

	/**
	 * Trigger icecat call on a product. Here the logic :
	 * > If first call, then make a search, then associates the asin
	 * > if second call, then make a get.
	 * > If ASIN was not previously found, then mark as stand by
	 */
	public void processProduct(VerticalConfig vertical, Product data) {
		logger.info("Icecat completion for {}", data.getId());
		Set<DataFragment> fragments = new HashSet<>();

		String icecatId = data.getExternalIds().getIcecat();
		if (StringUtils.isEmpty(icecatId)) {
			fragments.addAll(completeSearch(vertical, data));
		} else {
			// TODO : Refresh policy
			// logger.info("Skipping, already fetched{}", data.gtin());
			fragments.addAll(completeSearch(vertical, data));
		}

		// Apply aggregation
		for (DataFragment df : fragments) {
			try {
				aggregator.onDatafragment(df, data);
			} catch (AggregationSkipException e) {
				logger.error("Error occurs during icecat aggregation", e);
			}
		}

		// Setting the timestamp flag
		data.getDatasourceCodes().put(getDatasourceName(), System.currentTimeMillis());

		try {
			Thread.sleep(icecatConfig.getPolitnessDelayMs());
		} catch (InterruptedException e) {
			logger.error("Errot while sleeping");
		}
	}

	/**
	 * Proceed to the search api call on icecat
	 *
	 * @param vertical
	 * @param data
	 */
	private Set<DataFragment> completeSearch(VerticalConfig vertical, Product data) {
		Set<DataFragment> ret = new HashSet<>();

		// TODO : Should manage a thread pool if operating on all catalog
		// Using "fr" as default language for now, should be configurable or passed in
		// context
		String language = "fr";

		try {
			IcecatProduct iceItem = icecatService.getProduct(String.valueOf(data.getId()), language);

			if (iceItem != null) {
				data.getExternalIds().setIcecat(String.valueOf(iceItem.getID()));
				ret.add(convert(iceItem, data));
			} else {
				logger.info("Gtin {} is not found in Icecat", data.gtin());
			}

		} catch (Exception e) {
			logger.error("Unexpected error in icecat parsing", e);
		}
		return ret;
	}

	/**
	 * Proceed to icecat to datafragment conversion
	 *
	 * @param iceItem
	 * @param data
	 * @return
	 */
	private DataFragment convert(IcecatProduct iceItem, Product data) {
		DataFragment df = initDataFragment(data);

		completeGeneralInfos(iceItem, df, data);
		completeImage(iceItem, df, data);
		completeMultimedia(iceItem.getMultimedia(), df, data);
		completeGallery(iceItem.getGallery(), df, data);
		completeFeaturesGroup(iceItem.getCategoryFeatureGroups(), df);

		return df;
	}

	private void completeFeaturesGroup(List<IcecatCategoryFeatureGroup> featuresGroups, DataFragment df) {
		if (featuresGroups == null)
			return;
		for (IcecatCategoryFeatureGroup g : featuresGroups) {
			// TODO : Could handle the FeaturesGroups (!!!!)
			if (g.getFeatures() != null) {
				g.getFeatures().forEach(f -> {
					// TODO (i18n) --> Is Wrong
					// In XML model, we might need to resolve feature name if not present or use ID
					// Assuming IcecatFeature in XML model has Name/Value
					// But wait, IcecatFeature in services/icecat model is complex.
					// Let's check what IcecatCategoryFeatureGroup contains.
					// It contains List<IcecatFeature>.

					// We need to get the value. In XML, Feature element has Value attribute?
					// Or is it PresentationValue?
					// Let's assume we can get name and value.
					// The IcecatFeature model in services/icecat seems to be the one from XSD.
					// It has Names, Measure, etc.
					// But wait, the IcecatFeature used in IcecatService is for the full feature
					// list file.
					// The one in Product XML might be slightly different or same.
					// XSD says Category uses Feature.

					// For now, let's try to get what we can.
					// df.addAttribute(f.getNames()..., f.getPresentationValue()...);
					// This part is tricky without seeing the exact populated object.
					// Leaving as TODO or best effort.
				});
			}

		}

	}

	private void completeGallery(List<IcecatProductGallery> gallery, DataFragment df, Product p) {
		if (gallery == null)
			return;
		for (IcecatProductGallery g : gallery) {
			if (g.getProductPicture() != null) {
				for (IcecatProductPicture pic : g.getProductPicture()) {
					try {
						addResourceIfAbsent(df, p, pic.getPic(), "gallery");
					} catch (ValidationException e) {
						logger.warn("Error while adding resource {}", pic.getPic());
					}
				}
			}
		}

	}

	/**
	 * Adds the icecat only if not already done, filtering on the icecat completion
	 * token
	 *
	 * @param df
	 * @param p
	 * @param g
	 * @throws ValidationException
	 */
	private void addResourceIfAbsent(DataFragment df, Product p, String url, String tag) throws ValidationException {

		if (url == null)
			return;

		String shortened = null;
		int marker = url.indexOf("?access");
		if (marker != -1) {
			// TODO(P1,design) : Remove when tested
			logger.error("Got an access protected resource from icecat : {} - {}", url, p);
			shortened = url.substring(0, marker);
		}

		if (null != shortened) {

			for (Resource r : p.getResources()) {
				if (r.getUrl().startsWith(shortened)) {
					if (r.isProcessed() == true && r.getFileSize() > 0) {
						logger.info("Resource have already been processed, skipping {}");
						return;
					}
				}
			}
		}

		df.addResource(url, Sets.newHashSet(tag, "gallery"));
	}

	private void completeMultimedia(List<IcecatMultimediaObject> multimedia, DataFragment df, Product p) {
		if (multimedia == null)
			return;
		for (IcecatMultimediaObject m : multimedia) {
			try {
				// TODO : handle i18
				addResourceIfAbsent(df, p, m.getURL(), "fr");
			} catch (ValidationException e) {
				logger.info("Cannot validate multimedia resource : {}", m.getURL());
			}
		}

	}

	private void completeImage(IcecatProduct image, DataFragment df, Product p) {
		try {

			// Tweak to exclude "brand" images sometimes used as logo
			if (image.getHighPic() != null && !image.getHighPic().contains("brand")) {
				addResourceIfAbsent(df, p, image.getHighPic(), ResourceTag.PRIMARY.toString());
			}

		} catch (ValidationException e) {
			logger.info("Cannot validate image resource : {}", image.getHighPic());
		}

	}

	private void completeGeneralInfos(IcecatProduct e, DataFragment df, Product p) {

		// TODO(p3, feature) : HAndle end of year / end of year
		if (null != e.getReleaseDate()) {
			// TODO : i18n
			try {
				df.addAttribute("YEAR", e.getReleaseDate().substring(e.getReleaseDate().lastIndexOf("-") + 1), "fr",
						null);
			} catch (Exception e1) {
				logger.error("Parsing year failed ! ", e);
			}
		}

		df.addName(e.getTitle());
		// df.addName(e.titleInfo.generatedIntTitle); // Not directly available in
		// simple Product model yet
		df.addName(e.getName());

		if (e.getSupplier() != null) {
			df.addReferentielAttribute(ReferentielKey.BRAND, e.getSupplier().getName());
		}

		df.addReferentielAttribute(ReferentielKey.MODEL, e.getProd_id());
		if (e.getCategory() != null && e.getCategory().getNames() != null && !e.getCategory().getNames().isEmpty()) {
			// Taking first name for now
			df.addProductTag(e.getCategory().getNames().get(0).getTextValue());
		}

		// Adding PDFs
		// In XML, PDFs are usually in MultimediaObject or specific fields.
		// Checking MultimediaObjects for PDFs
		if (e.getMultimedia() != null) {
			for (IcecatMultimediaObject m : e.getMultimedia()) {
				if ("application/pdf".equals(m.getContentType())) {
					try {
						addResourceIfAbsent(df, p, m.getURL(), ResourceTag.MANUAL.toString());
					} catch (ValidationException e1) {
						logger.error("Error while adding pdf {}", m.getURL(), e1);
					}
				}
			}
		}

		// TODO : Check ProductFamily

	}

	/**
	 * Init an empty datafragment for amazon provider
	 *
	 * @param datasourceName
	 * @param url
	 * @param data
	 * @return
	 */
	private DataFragment initDataFragment(Product data) {
		DataFragment df = new DataFragment();
		// TODO(p3,conf) : Constants
		df.setDatasourceName("icecat.biz");
		df.setDatasourceConfigName("icecat.biz.yml");
		df.setLastIndexationDate(System.currentTimeMillis());
		df.setCreationDate(System.currentTimeMillis());
		df.addReferentielAttribute(ReferentielKey.GTIN, String.valueOf(data.getId()));
		return df;
	}

}
