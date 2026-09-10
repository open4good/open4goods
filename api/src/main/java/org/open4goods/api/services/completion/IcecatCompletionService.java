package org.open4goods.api.services.completion;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.commons.services.AbstractCompletionService;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.icecat.config.yml.IcecatCompletionConfig;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeatureLogos;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeaturesGroups;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Gallery;
import org.open4goods.icecat.model.IcecatLiveApiResponse.GeneralInfo;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Image;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Multimedia;
import org.open4goods.icecat.model.IcecatLiveApiResponse.ReasonsToBuy;
import org.open4goods.icecat.model.IcecatLiveApiResponse.VariantIdentifier;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Variants;
import org.open4goods.icecat.services.IcecatLiveClient;
import org.open4goods.icecat.services.IcecatLiveLookupResult;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.product.Product;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.util.ProductModelCandidateHelper.ModelCandidateSource;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;




public class IcecatCompletionService extends AbstractCompletionService {

	/**
	 * Suffix appended to {@link #getDatasourceName()} to record the negative-cache timestamp
	 * (NOT_FOUND / RESTRICTED / ERROR outcomes), kept distinct from the success timestamp so
	 * that {@link #shouldProcess(VerticalConfig, Product)} can gate each independently.
	 */
	private static final String MISS_SUFFIX = ".miss";

    private final IcecatCompletionConfig icecatConfig;
    private final StandardAggregator aggregator;
    private final IcecatLiveClient liveClient;

    /**
     * Returns an empty list when an Icecat response omits an optional array.
     *
     * @param values the nullable Icecat response list
     * @param <T> the list item type
     * @return the original list or an empty immutable list
     */
    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }




	public IcecatCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService,
			ApiProperties apiProperties, DataSourceConfigService dataSourceConfigService, AggregationFacadeService aggregationFacadeService)  {
		// TODO : Should set a specific log level here (not "agg(regation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());
		
		this.aggregator = aggregationFacadeService.getStandardAggregator("icecat-aggregation");;
		this.aggregator.beforeStart();
		this.icecatConfig = apiProperties.getIcecatCompletionConfig();
		this.liveClient = new IcecatLiveClient(icecatConfig);
	}

        @Override
        public boolean shouldProcess(VerticalConfig vertical, Product data) {
                // TODO(p2,perf) : should adda check on unprocessed resources
                long now = System.currentTimeMillis();

                Long lastSuccess = data.getDatasourceCodes().get(getDatasourceName());
                if (null != lastSuccess) {
                        long refreshIntervalMs = Duration.ofDays(icecatConfig.getRefreshIntervalDays()).toMillis();
                        if (now - lastSuccess < refreshIntervalMs) {
                                return false;
                        }
                }

                Long lastMiss = data.getDatasourceCodes().get(missDatasourceName());
                if (null != lastMiss) {
                        long negativeCacheMs = Duration.ofDays(icecatConfig.getNegativeCacheIntervalDays()).toMillis();
                        if (now - lastMiss < negativeCacheMs) {
                                return false;
                        }
                }

                return true;
        }

	@Override
	public String getDatasourceName() {
		return "icecat.biz";
	}

	private String missDatasourceName() {
		return getDatasourceName() + MISS_SUFFIX;
	}

	/**
	 * Trigger icecat call on a product. Here the logic :
	 * > If no Icecat id is known yet, search by GTIN and associate the id on a match.
	 * > If an Icecat id is already known, refresh that exact product by id.
	 * The success timestamp ({@link #getDatasourceName()}) is set only when aggregation of the
	 * resulting DataFragment actually succeeds; a NOT_FOUND/RESTRICTED/ERROR outcome instead sets
	 * a distinct negative-cache timestamp ({@link #missDatasourceName()}), so {@link #shouldProcess}
	 * can gate retries on misses independently of the refresh interval for known matches.
	 */
	public void processProduct(VerticalConfig vertical, Product data) {
		logger.info("Icecat completion for {}", data.getId());

		if (null == data.getId()) {
			logger.warn("Skipping icecat completion, product has no id");
			return;
		}

		Set<DataFragment> fragments = new HashSet<>();

		String icecatId = data.getExternalIds().getIcecat();
		IcecatLiveLookupResult result = StringUtils.isEmpty(icecatId)
				? liveClient.fetchProduct(data.getId(), icecatConfig.getDomainLanguage())
				: liveClient.fetchProductByIcecatId(icecatId, icecatConfig.getDomainLanguage());

		switch (result.status()) {
			case FOUND -> {
				IceDataItem iceItem = result.product().orElseThrow();
				data.getExternalIds().setIcecat(String.valueOf(iceItem.generalInfo.icecatId));
				fragments.add(convert(iceItem, data, icecatConfig.getDomainLanguage()));
			}
			case NOT_FOUND, RESTRICTED -> data.getDatasourceCodes().put(missDatasourceName(), System.currentTimeMillis());
			case ERROR -> {
				logger.error("Icecat live lookup failed for gtin {} : {}", data.gtin(),
						result.errorMessage().orElse("unknown error"));
				data.getDatasourceCodes().put(missDatasourceName(), System.currentTimeMillis());
			}
		}

		// Apply aggregation; the success timestamp is set only if at least one fragment aggregates cleanly.
		boolean aggregationSucceeded = false;
		for (DataFragment df : fragments) {
			try {
				aggregator.onDatafragment(df, data);
				aggregationSucceeded = true;
			} catch (AggregationSkipException e) {
				logger.error("Error occurs during icecat aggregation",e);
			}
		}

		if (aggregationSucceeded) {
			data.getDatasourceCodes().put(getDatasourceName(), System.currentTimeMillis());
		}

        try {
            Thread.sleep(icecatConfig.getPolitenessDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Icecat politeness sleep interrupted");
        }
    }


	/**
	 * Proceed to icecat to datafragment conversion
	 * @param iceItem
	 * @param data
	 * @param language the domain language requested from the live API, used for language-tagged
	 *                 fields when the item itself carries no more specific language
	 * @return
	 */
	private DataFragment convert(IceDataItem iceItem, Product data, DomainLanguage language) {
		DataFragment df = initDataFragment(data);

		completeGeneralInfos(iceItem.generalInfo, df,data, language);
		completeImage(iceItem.image, df, data);
		completeMultimedia(nullToEmpty(iceItem.multimedia),df,data, language);
		completeGallery(nullToEmpty(iceItem.gallery),df,data);
		completeFeaturesGroup(nullToEmpty(iceItem.featuresGroups),df);
		completeFeatureLogos(nullToEmpty(iceItem.featureLogos), df, data);
		completeReasonsToBuy(nullToEmpty(iceItem.reasonsToBuy), df, language);
		completeVariants(nullToEmpty(iceItem.variants), df, language);


        nullToEmpty(iceItem.taxonomyDescriptions).forEach(e->{

            // TODO : Handle taxonomy
        });



        nullToEmpty(iceItem.productRelated).forEach(e-> {
            // TODO : HAndle related products
            //System.out.println("RELATED : " + e.icecatID);
        });
// TODO
//TaxonomyDescriptions
//ProductRelated


		return df;
	}

	
	
	private void completeFeaturesGroup(List<FeaturesGroups> featuresGroups, DataFragment df) {
        for (FeaturesGroups g : featuresGroups) {
            if (g == null || g.features == null) {
                continue;
            }
            // TODO : Could handle the FeaturesGroups (!!!!)
            g.features.forEach(f -> {
                // TODO (i18n) --> Is Wrong
                if (f == null || f.featureDetail == null || f.featureDetail.name == null) {
                    return;
                }
                
                if (f.rawValue != null && !f.rawValue.equals(f.value)) {
                    logger.error("VALUE MISMATCH for {}", df.gtin());
                }
                df.addAttribute(f.featureDetail.name.value, f.rawValue,  f.featureDetail.name.language, f.featureDetail.id);
			});
			
			
			
		}
		
	}

	private void completeGallery(List<Gallery> gallery, DataFragment df, Product p) {

        for (Gallery g : gallery) {
            if (g == null || StringUtils.isBlank(g.pic)) {
                continue;
            }
            try {
                addResourceIfAbsent(df, p, g.pic, g.type);
            } catch (ValidationException e) {
				logger.warn("Error while adding resource {}",g.pic);
			}
		}
		
	}

	/**
	 * Adds the icecat only if not already done, filtering on the icecat completion token
	 * @param df
	 * @param p
	 * @param g
	 * @throws ValidationException
	 */
	private void addResourceIfAbsent(DataFragment df, Product p, String url, String tag) throws ValidationException {
		
		
		String shortened = null;
		int marker = url.indexOf("?access");
		if (marker != -1) {
			// TODO(P1,design) : Remove when tested
			logger.error("Got an access protected resource from icecat : {} - {}",url,p );
			shortened = url.substring(0,marker);
		}
		
		if (null != shortened) {
			
			for (Resource r : p.getResources()) {
				if (r.getUrl().startsWith(shortened)) {
					if (r.isProcessed() == true && r.getFileSize() >0) {
						logger.info("Resource have already been processed, skipping {}");
						return;
					}
				}
			}
		}
		
		df.addResource(url ,  Sets.newHashSet(tag,"gallery"));
	}

	private void completeMultimedia(List<Multimedia> multimedia, DataFragment df, Product p, DomainLanguage language) {

        for (Multimedia m : multimedia) {
            if (m == null || StringUtils.isBlank(m.url)) {
                continue;
            }
            try {
                // Resource tag is the multimedia type (matches completeGallery's use of Gallery.type);
                // falls back to the requested language only when Icecat provides no type.
                String tag = StringUtils.isNotBlank(m.type) ? m.type : language.languageTag();
                addResourceIfAbsent(df, p, m.url, tag);
			} catch (ValidationException e) {
				logger.info("Cannot validate multimedia resource : {}",m.url);
			}
		}


	}

	private void completeFeatureLogos(List<FeatureLogos> featureLogos, DataFragment df, Product p) {
		for (FeatureLogos logo : featureLogos) {
			if (logo == null || StringUtils.isBlank(logo.logoPic)) {
				continue;
			}
			try {
				addResourceIfAbsent(df, p, logo.logoPic, "logo");
			} catch (ValidationException e) {
				logger.info("Cannot validate feature logo resource : {}", logo.logoPic);
			}
		}
	}

	private void completeReasonsToBuy(List<ReasonsToBuy> reasonsToBuy, DataFragment df, DomainLanguage language) {
		List<String> values = new ArrayList<>();
		String lang = language.languageTag();
		for (ReasonsToBuy reason : reasonsToBuy) {
			if (reason == null || StringUtils.isBlank(reason.value)) {
				continue;
			}
			values.add(reason.value);
			if (StringUtils.isNotBlank(reason.language)) {
				lang = reason.language;
			}
		}
		if (!values.isEmpty()) {
			df.addAttribute("REASONS_TO_BUY", String.join(" | ", values), lang, null);
		}
	}

	/**
	 * Variants (sibling products, e.g. by color) are captured as a single joined attribute of
	 * their identifiers rather than as alternate ids : {@code DataFragment.alternateIds} is for
	 * alternate names of the SAME product, and a variant is a DIFFERENT product.
	 */
	private void completeVariants(List<Variants> variants, DataFragment df, DomainLanguage language) {
		List<String> identifiers = new ArrayList<>();
		for (Variants variant : variants) {
			if (variant == null) {
				continue;
			}
			for (VariantIdentifier identifier : nullToEmpty(variant.variantIdentifiers)) {
				if (identifier == null || StringUtils.isBlank(identifier.value)) {
					continue;
				}
				identifiers.add(StringUtils.isNotBlank(identifier.identifierType)
						? identifier.identifierType + ":" + identifier.value
						: identifier.value);
			}
		}
		if (!identifiers.isEmpty()) {
			df.addAttribute("ICECAT_VARIANTS", String.join(" | ", identifiers), language.languageTag(), null);
		}
	}

    private void completeImage(Image image, DataFragment df, Product p) {
        if (image == null || StringUtils.isBlank(image.highPic)) {
            return;
        }
        try {
            
            // Tweak to exclude "brand" images sometimes used as logo
			if (!image.highPic.contains("brand")) {
				addResourceIfAbsent(df, p, image.highPic, ResourceTag.PRIMARY.toString());
			}
			
		} catch (ValidationException e) {
			logger.info("Cannot validate image resource : {}",image.highPic);
		}
		
	}

    private void completeGeneralInfos(GeneralInfo e, DataFragment df, Product p, DomainLanguage language) {
        if (e == null) {
            return;
        }
		String lang = language.languageTag();

		// TODO(p3, feature) : HAndle end of year / end of year
		if (null != e.releaseDate) {
			try {
				// ReleaseDate is "YYYY-MM-DD" or "YYYY-MM-DDTHH:MM:SS" ; the year is the leading segment.
				int dash = e.releaseDate.indexOf("-");
				df.addAttribute("YEAR", dash > 0 ? e.releaseDate.substring(0, dash) : e.releaseDate, lang, null);
			} catch (Exception e1) {
				logger.error("Parsing year failed ! ",e);
			}
		}
		if (null != e.endOfLifeDate) {
			df.addAttribute("END_OF_LIFE_DATE", e.endOfLifeDate, lang, null);
		}

		df.addName(e.title);
        if (e.titleInfo != null) {
            df.addName(e.titleInfo.generatedIntTitle);
        }
        df.addName(e.productName);

        df.addReferentielAttribute(ReferentielKey.BRAND, e.brand);
        if (e.brandInfo != null) {
            df.addReferentielAttribute(ReferentielKey.BRAND, e.brandInfo.brandName);
        }

        df.addReferentielAttribute(ReferentielKey.MODEL, e.brandPartCode, ModelCandidateSource.STRUCTURED_DATA);
        if (e.category != null && e.category.name != null) {
            df.addProductTag(e.category.name.value);
        }

        if (e.productFamily != null && StringUtils.isNotBlank(e.productFamily.value)) {
            df.addAttribute("PRODUCT_FAMILY", e.productFamily.value,
                    StringUtils.isNotBlank(e.productFamily.language) ? e.productFamily.language : lang, null);
        }
        if (e.productSeries != null && StringUtils.isNotBlank(e.productSeries.value)) {
            df.addAttribute("PRODUCT_SERIES", e.productSeries.value,
                    StringUtils.isNotBlank(e.productSeries.language) ? e.productSeries.language : lang, null);
        }

        completeSummaryAndBullets(e, df);

		// Adding PDFs
		try {

			if (e.description != null && e.description.leafletPDFURL != null) {
				addResourceIfAbsent(df, p, e.description.leafletPDFURL, ResourceTag.LEAFLET.toString());
			}

		} catch (ValidationException e1) {
			logger.error("Error while adding leaflet pdf {}", e.description.leafletPDFURL, e);
		}


		try {
			if (e.description != null && e.description.manualPDFURL != null) {
				addResourceIfAbsent(df, p, e.description.manualPDFURL, ResourceTag.MANUAL.toString());

			}
		} catch (ValidationException e1) {
			logger.error("Error while adding manual pdf {}", e.description.leafletPDFURL, e);

		}
	}

	/**
	 * DataFragment only holds one description slot per datasource ({@link DataFragment#addDescription}),
	 * so summary and bullet points are folded into a single text rather than competing for that
	 * slot: prefers SummaryDescription (long, then short), falls back to Description (long, then
	 * middle), then appends BulletPoints (or GeneratedBulletPoints if absent) as a bullet list.
	 */
	private void completeSummaryAndBullets(GeneralInfo e, DataFragment df) {
		String description = null;
		if (e.summaryDescription != null && StringUtils.isNotBlank(e.summaryDescription.longSummaryDescription)) {
			description = e.summaryDescription.longSummaryDescription;
		} else if (e.summaryDescription != null && StringUtils.isNotBlank(e.summaryDescription.shortSummaryDescription)) {
			description = e.summaryDescription.shortSummaryDescription;
		} else if (e.description != null && StringUtils.isNotBlank(e.description.longDesc)) {
			description = e.description.longDesc;
		} else if (e.description != null && StringUtils.isNotBlank(e.description.middleDesc)) {
			description = e.description.middleDesc;
		}

		List<String> bullets = null;
		if (e.bulletPoints != null && !nullToEmpty(e.bulletPoints.values).isEmpty()) {
			bullets = e.bulletPoints.values;
		} else if (e.generatedBulletPoints != null && !nullToEmpty(e.generatedBulletPoints.values).isEmpty()) {
			bullets = e.generatedBulletPoints.values;
		}

		if (bullets != null && !bullets.isEmpty()) {
			StringBuilder sb = new StringBuilder(StringUtils.defaultString(description));
			for (String bullet : bullets) {
				if (StringUtils.isNotBlank(bullet)) {
					if (sb.length() > 0) {
						sb.append('\n');
					}
					sb.append("- ").append(bullet);
				}
			}
			description = sb.toString();
		}

		df.addDescription(getDatasourceName(), description);
	}

	/**
	 * Init an empty datafragment for amazon provider
	 * @param datasourceName
	 * @param url
	 * @param data
	 * @return
	 */
	private DataFragment initDataFragment( Product data) {
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
