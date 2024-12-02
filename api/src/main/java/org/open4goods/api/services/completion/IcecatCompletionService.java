package org.open4goods.api.services.completion;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.config.yml.IcecatCompletionConfig;
import org.open4goods.api.model.IcecatData;
import org.open4goods.api.model.IcecatData.FeaturesGroups;
import org.open4goods.api.model.IcecatData.Gallery;
import org.open4goods.api.model.IcecatData.GeneralInfo;
import org.open4goods.api.model.IcecatData.IceDataItem;
import org.open4goods.api.model.IcecatData.Image;
import org.open4goods.api.model.IcecatData.Multimedia;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.model.constants.ReferentielKey;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.data.Resource;
import org.open4goods.commons.model.data.ResourceTag;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
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

	private ObjectMapper objectMapper = new ObjectMapper();
	
	// The specific icecat fetching properties
	private IcecatCompletionConfig icecatConfig;

	// We re-use the realtime aggregator, from the AggregationFavcade
	private StandardAggregator aggregator;




	public IcecatCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService,
			ApiProperties apiProperties, DataSourceConfigService dataSourceConfigService, AggregationFacadeService aggregationFacadeService)  {
		// TODO : Should set a specific log level here (not "agg(regation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());
		
		this.aggregator = aggregationFacadeService.getStandardAggregator("icecat-aggregation");;
		this.aggregator.beforeStart();
		this.icecatConfig = apiProperties.getIcecatCompletionConfig();
		
		
	}

	@Override
	public boolean shouldProcess(VerticalConfig vertical, Product data) {
		// TODO(p2,perf) : should adda check on unprocessed resources
		Long lastProcessed = data.getDatasourceCodes().get(getDatasourceName());
		if (null != lastProcessed &&  REFRESH_IN_DAYS * 1000 * 3600 * 24 < System.currentTimeMillis() - lastProcessed ) {
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
					//logger.info("Skipping, already fetched{}", data.gtin());
			fragments.addAll(completeSearch(vertical, data));
		}
		
		// Apply aggregation
		for (DataFragment df : fragments) {
			try {
				aggregator.onDatafragment(df, data);
			} catch (AggregationSkipException e) {
				logger.error("Error occurs during icecat aggregation",e);
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
		String url = icecatConfig.getIceCatUrlPrefix()+data.getId();
		logger.info("Loading icecat data {}",url);
		
		try {
			String content = IOUtils.toString(new URL(url));
			IceDataItem iceItem = objectMapper.readValue(content, IcecatData.class).data;
			
			data.getExternalIds().setIcecat(String.valueOf(iceItem.generalInfo.icecatId));
			
			ret.add(convert(iceItem, data));

			
		} catch (UnrecognizedPropertyException e) {
			logger.error("Unknown property at {} : {}",url,e.getOriginalMessage());
		}  catch (FileNotFoundException e) {
			logger.info("Gtin {} is not found",data.gtin());
		} catch (IOException e) {
			// TODO : Not nice, use real httpclient
			if (e.getMessage().contains("response code: 400 ")) {
				logger.info("Gtin {} does not exists in Icecat",data.gtin());
			} else if (e.getMessage().contains("response code: 403 ")) {
				logger.info("Gtin {} is restricted to upgraded plan in Icecat",data.gtin());
			} else {
				logger.error("Unexpected error in icecat parsing",e);
			}
		}catch (Exception e) {
			logger.error("Unexpected error in icecat parsing",e);
		}
		return ret;
	}

	
	/**
	 * Proceed to icecat to datafragment conversion
	 * @param iceItem
	 * @param data
	 * @return
	 */
	private DataFragment convert(IceDataItem iceItem, Product data) {
		DataFragment df = initDataFragment(data);
		
		completeGeneralInfos(iceItem.generalInfo, df,data);
		completeImage(iceItem.image, df, data);
		completeMultimedia(iceItem.multimedia,df,data);
		completeGallery(iceItem.gallery,df,data);
		completeFeaturesGroup(iceItem.featuresGroups,df);

		
		iceItem.taxonomyDescriptions.forEach(e->{
			
			// TODO : Handle taxonomy
		});
		
		
		
		iceItem.productRelated.forEach(e-> {
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
			// TODO : Could handle the FeaturesGroups (!!!!)
			g.features.forEach(f -> {
				// TODO (i18n) --> Is Wrong
				
				if (!f.rawValue.equals(f.value)) {
					logger.error("VALUE MISMATCH for {}", df.gtin());
				}
				df.addAttribute(f.featureDetail.name.value, f.rawValue,  f.featureDetail.name.language, f.featureDetail.id);
			});
			
			
			
		}
		
	}

	private void completeGallery(List<Gallery> gallery, DataFragment df, Product p) {

		for (Gallery g : gallery) {
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

	private void completeMultimedia(List<Multimedia> multimedia, DataFragment df, Product p) {

		for (Multimedia m : multimedia) {
			try {
				// TODO : handle i18
				addResourceIfAbsent(df, p, m.url, "fr");
			} catch (ValidationException e) {
				logger.info("Cannot validate multimedia resource : {}",m.url);
			}
		}
				
		
	}

	private void completeImage(Image image, DataFragment df, Product p) {
		try {
			
			// Tweak to exclude "brand" images sometimes used as logo
			if (!image.highPic.contains("brand")) {
				addResourceIfAbsent(df, p, image.highPic, ResourceTag.PRIMARY.toString());
			}
			
		} catch (ValidationException e) {
			logger.info("Cannot validate image resource : {}",image.highPic);
		}
		
	}

	private void completeGeneralInfos(GeneralInfo e, DataFragment df, Product p) {
		
		// TODO(p3, feature) : HAndle end of year / end of year
		if (null != e.releaseDate) {			
			// TODO : i18n
			try {
				df.addAttribute("YEAR",  e.releaseDate.substring(e.releaseDate.lastIndexOf("-")+1) , "fr", null);
			} catch (Exception e1) {
				logger.error("Parsing year failed ! ",e);
			}
		}
		
		df.addName(e.title);
		df.addName(e.titleInfo.generatedIntTitle);
		df.addName(e.productName);
		
		df.addReferentielAttribute(ReferentielKey.BRAND, e.brand);
		df.addReferentielAttribute(ReferentielKey.BRAND, e.brandInfo.brandName);
		
		df.addReferentielAttribute(ReferentielKey.MODEL, e.brandPartCode);
		df.addProductTag(e.category.name.value);
		
		
		
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
		
		// TODO : Check ProductFamily
		
		
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
