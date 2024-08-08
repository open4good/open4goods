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
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.api.services.completion.IcecatData.FeaturesGroups;
import org.open4goods.api.services.completion.IcecatData.Gallery;
import org.open4goods.api.services.completion.IcecatData.GeneralInfo;
import org.open4goods.api.services.completion.IcecatData.IceDataItem;
import org.open4goods.api.services.completion.IcecatData.Image;
import org.open4goods.api.services.completion.IcecatData.Multimedia;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Resource;
import org.open4goods.model.data.ResourceTag;
import org.open4goods.model.product.Product;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.VerticalsConfigService;
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

	/**
	 * Trigger icecat call on a product. Here the logic : 
	 * > If first call, then make a search, then associates the asin
	 * > if second call, then make a get.
	 * > If ASIN was not previously found, then mark as stand by
	 */
	public void processProduct(VerticalConfig vertical, Product data) {
		logger.info("Icecat completion for {}", data.getId());
		Set<DataFragment> fragments = new HashSet<>();
		
		String icecatId = data.getExternalId().getIcecat();
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
			
			data.getExternalId().setIcecat(String.valueOf(iceItem.generalInfo.icecatId));
			
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
		
		completeGeneralInfos(iceItem.generalInfo, df);
		completeImage(iceItem.image, df);
		completeMultimedia(iceItem.multimedia,df);
		completeGallery(iceItem.gallery,df);
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
				df.addAttribute(f.featureDetail.name.value, f.rawValue,  f.featureDetail.name.language, false, null);
			});
			
			
			
		}
		
	}

	private void completeGallery(List<Gallery> gallery, DataFragment df) {

		for (Gallery g : gallery) {
			try {
				// TODO : mutualize tag
				df.addResource(g.pic ,  Sets.newHashSet(g.type,"gallery"));
			} catch (ValidationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private void completeMultimedia(List<Multimedia> multimedia, DataFragment df) {

		for (Multimedia m : multimedia) {
			try {
				// TODO : handle i18
				df.addResource(m.url ,  Sets.newHashSet(m.type,"fr"));
			} catch (ValidationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
		
	}

	private void completeImage(Image image, DataFragment df) {
		try {
			
			// Tweak to exclude "brand" images sometimes used as logo
			if (!image.highPic.contains("brand")) {
				Resource r = new Resource(image.highPic);
				r.getHardTags().add(ResourceTag.PRIMARY);
				df.addResource(r);
				
			}
			
		} catch (ValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void completeGeneralInfos(GeneralInfo e, DataFragment df) {
		
		// TODO : HAndle end of year / end of year
		if (null != e.releaseDate) {			
			// TODO : i18n
			try {
				df.addAttribute("YEAR",  e.releaseDate.substring(e.releaseDate.lastIndexOf("-")+1) , "fr", false, null);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
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
			
			Resource r = new Resource(e.description.leafletPDFURL);
			r.getHardTags().add(ResourceTag.LEAFLET);
			df.addResource(r);
			
			
		} catch (ValidationException e1) {
			logger.error("Error while adding leaflet pdf {}", e.description.leafletPDFURL, e);
		}
		try {
			Resource r = new Resource(e.description.manualPDFURL);
			r.getHardTags().add(ResourceTag.MANUAL);
			df.addResource(r);
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
		// TODO : Constants
		df.setDatasourceName("icecat.biz");
		df.setDatasourceConfigName("icecat.biz.yml");
		df.setLastIndexationDate(System.currentTimeMillis());
		df.setCreationDate(System.currentTimeMillis());
		df.addReferentielAttribute(ReferentielKey.GTIN, data.getId() );
		return df;
	}
	
	
	
}
