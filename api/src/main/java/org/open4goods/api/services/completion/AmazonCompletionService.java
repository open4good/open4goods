package org.open4goods.api.services.completion;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.AmazonCompletionConfig;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.exceptions.TechnicalException;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.model.constants.ProductCondition;
import org.open4goods.commons.model.constants.ReferentielKey;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.data.Price;
import org.open4goods.commons.model.data.Resource;
import org.open4goods.commons.model.data.ResourceTag;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.paapi5.v1.ApiClient;
import com.amazon.paapi5.v1.ApiException;
import com.amazon.paapi5.v1.BrowseNodeInfo;
import com.amazon.paapi5.v1.ByLineInfo;
import com.amazon.paapi5.v1.ErrorData;
import com.amazon.paapi5.v1.GetItemsRequest;
import com.amazon.paapi5.v1.GetItemsResource;
import com.amazon.paapi5.v1.GetItemsResponse;
import com.amazon.paapi5.v1.Images;
import com.amazon.paapi5.v1.Item;
import com.amazon.paapi5.v1.ItemIdType;
import com.amazon.paapi5.v1.ItemInfo;
import com.amazon.paapi5.v1.ManufactureInfo;
import com.amazon.paapi5.v1.MultiValuedAttribute;
import com.amazon.paapi5.v1.OfferListing;
import com.amazon.paapi5.v1.Offers;
import com.amazon.paapi5.v1.PartnerType;
import com.amazon.paapi5.v1.ProductInfo;
import com.amazon.paapi5.v1.SearchItemsRequest;
import com.amazon.paapi5.v1.SearchItemsResource;
import com.amazon.paapi5.v1.SearchItemsResponse;
import com.amazon.paapi5.v1.SingleStringValuedAttribute;
import com.amazon.paapi5.v1.TechnicalInfo;
import com.amazon.paapi5.v1.UnitBasedAttribute;
import com.amazon.paapi5.v1.VariationAttribute;
import com.amazon.paapi5.v1.api.DefaultApi;


public class AmazonCompletionService extends AbstractCompletionService {

	protected static final Logger logger = LoggerFactory.getLogger(AmazonCompletionService.class);

	// Constants
	private static final String NOT_FOUND_ASIN_MARKUP = "NOT_FOUND";
	private static final String AMAZON_PRODUCTSTATE_NEW = "New";
	private static final String AMAZON_PRODUCTSTATE_OCCASION = "Occasion";

	// The standard datasource (yaml) for amazon
	private DataSourceProperties amazonDatasource;

	// The specific amazon fetching properties
	private AmazonCompletionConfig amazonConfig;

	// We re-use the realtime aggregator, from the AggregationFavcade
	private StandardAggregator aggregator;

	// The amazon api componsnets
	private DefaultApi api;
	private ArrayList<GetItemsResource> getItemsResources;
	private ArrayList<SearchItemsResource> searchItemsResources;


	public AmazonCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService,
			ApiProperties apiProperties, DataSourceConfigService dataSourceConfigService, AggregationFacadeService aggregationFacadeService) throws TechnicalException {
		// TODO : Should set a specific log level here (not "agg(regation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());
		this.amazonConfig = apiProperties.getAmazonConfig();	
		this.amazonDatasource = dataSourceConfigService.getDatasourceConfig(amazonConfig.getDatasourceName());
		this.aggregator = aggregationFacadeService.getStandardAggregator("amazon");;
		this.aggregator.beforeStart();
		
		
		if (null == amazonDatasource) {
			logger.error("Amazon datasource config file {} not found", amazonConfig.getDatasourceName() );
		}
		
		// Set up the amazon api client
		ApiClient client = new ApiClient();

		// Setting credentials
		client.setAccessKey(amazonConfig.getAccessKey());
		client.setSecretKey(amazonConfig.getSecretKey());
		client.setHost(amazonConfig.getHost());
		client.setRegion(amazonConfig.getRegion());
		api = new DefaultApi(client);

		// Setting the itemresources to be used for direct product retrieving
		this.getItemsResources = new ArrayList<GetItemsResource>();
		// Adding all
		// https://webservices.amazon.com/paapi5/documentation/get-items.html#resources-parameter
		for (GetItemsResource gr : GetItemsResource.values()) {
			getItemsResources.add(gr);

		}

		// Setting the itemresources to be used for product search
		// https://webservices.amazon.com/paapi5/documentation/search-items.html#resources-parameter
		this.searchItemsResources = new ArrayList<SearchItemsResource>();
		// Adding all
		for (SearchItemsResource sr : SearchItemsResource.values()) {
			searchItemsResources.add(sr);
		}
	}

	/**
	 * Trigger amazon call on a product. Here the logic : 
	 * > If first call, then make a search, then associates the asin
	 * > if second call, then make a get.
	 * > If ASIN was not previously found, then mark as stand by
	 */
	public void processProduct(VerticalConfig vertical, Product data) {
		logger.info("Amazon completion for {}", data.getId());
		Set<DataFragment> fragments = new HashSet<>();
		
		String asin = data.getExternalIds().getAsin();
		if (StringUtils.isEmpty(asin)) {
			// First time API Call, we operate through the search method
			logger.info("Initial amazon call (get) for {}", data.gtin());
			fragments.addAll(completeSearch(vertical, data));
		} else {
				if (asin.equals(NOT_FOUND_ASIN_MARKUP)) {
					data.getExternalIds().setAsin(null);
				} else {
					// If we already have the ASIN, we operate a direct get request
					logger.info("Further amazon call (get) for {}", data.gtin());
					// TODO : Do not proceed, have a delay threshold
					fragments.addAll(completeGet(vertical, data));
				}
		}
		
		// Apply aggregation
		for (DataFragment df : fragments) {
			try {
				aggregator.onDatafragment(df, data);
			} catch (AggregationSkipException e) {
				logger.error("Error occurs during amazon aggregation",e);
			}
		}


		try {
			Thread.sleep(amazonConfig.getSleepDuration());
		} catch (InterruptedException e) {
			logger.error("Errot while sleeping");
		}
	}

	/**
	 * Proceed to the get api call on amazon
	 * 
	 * @param vertical
	 * @param data
	 */
	private Set<DataFragment> completeGet(VerticalConfig vertical, Product data) {
		Set<DataFragment> ret = new HashSet<>();
		GetItemsRequest getItemsRequest = new GetItemsRequest().itemIdType(ItemIdType.ASIN)
				.itemIds(List.of(data.getExternalIds().getAsin())).partnerTag(amazonConfig.getPartnerTag())
				.partnerType(PartnerType.ASSOCIATES).resources(getItemsResources);
		try {
			// Sending the request
			GetItemsResponse response = api.getItems(getItemsRequest);
			logger.info("Amazon get response is {}", response);

			if (response.getErrors() != null) {
				for (ErrorData error : response.getErrors()) {
					logger.error("Amazon API returned an error {} : {}", error.getCode(), error.getMessage());
				}
			}

			if (response.getItemsResult().getItems().size() == 0) {
				logger.warn("No amazon product for {}", data.gtin());
			} else if (response.getItemsResult().getItems().size() == 0) {
				logger.warn("Multiple amazon product for {}", data.gtin());
			}

			for (Item item : response.getItemsResult().getItems()) {
				ret.addAll(processAmazonItem(item, vertical, data));
			}

		} catch (ApiException exception) {
			logger.error("Amazon API error {} : {} \n {} ", exception.getCode(), exception.getResponseBody(),
					exception.getMessage());
		}
		return ret;
	}

	/**
	 * Proceed to the search api call on amazon
	 * 
	 * @param vertical
	 * @param data
	 */
	private Set<DataFragment> completeSearch(VerticalConfig vertical, Product data) {
		Set<DataFragment> ret = new HashSet<>();

		SearchItemsRequest searchItemsRequest = new SearchItemsRequest().keywords(data.gtin())
				.partnerTag(amazonConfig.getPartnerTag()).partnerType(PartnerType.ASSOCIATES)
				.resources(searchItemsResources);

		try {
			// Sending the request
			SearchItemsResponse response = api.searchItems(searchItemsRequest);
			logger.info("Amazon search response is {}", response);

			if (response.getErrors() != null) {
				for (ErrorData error : response.getErrors()) {
					logger.error("Amazon API returned an error {} : {}", error.getCode(), error.getMessage());
				}
			}

			if (null == response.getSearchResult() || response.getSearchResult().getItems().size() == 0) {
				logger.warn("No amazon product for {}", data.gtin());
				data.getExternalIds().setAsin(NOT_FOUND_ASIN_MARKUP);
				return ret;
			} else if (response.getSearchResult().getItems().size() > 1) {
				logger.warn("Multiple amazon product for {}", data.gtin());
			}

			for (Item item : response.getSearchResult().getItems()) {
				ret.addAll(processAmazonItem(item, vertical, data));
			}

		} catch (ApiException exception) {
			logger.error("Amazon API error {} : {} \n {} ", exception.getCode(), exception.getResponseBody(),
					exception.getMessage());
		}
		
		return ret;
	}

	private Set<DataFragment> processAmazonItem(Item item, VerticalConfig vertical, Product data) {

		logger.info("Setting amazon data for {}:{}", vertical.getId(), data.gtin());
		Set<DataFragment>  ret = new HashSet<>();
		
		// Setting the ASIN (directly in product)
		String asin = item.getASIN();
		if (!StringUtils.isEmpty(asin)) {
			data.getExternalIds().setAsin(asin);
		} else {
			logger.warn("Empty ASIN returned for {}", data.gtin());
		}

		// Handling images (directly in product)
		Images images = item.getImages();
		if (null != images) {
			if (null != images.getPrimary()) {

				try {
					logger.info("Adding primary image for {} : {}", data.gtin(), images.getPrimary().getLarge());
					Resource r = new Resource(images.getPrimary(). getLarge().getURL());
					r.getHardTags().add(ResourceTag.AMAZON_PRIMARY_TAG);
					r.getHardTags().add(ResourceTag.PRIMARY);
					r.setDatasourceName(amazonDatasource.getName());
					data.getResources().add(r);
				} catch (ValidationException e) {
					logger.error("Error while adding primary image", e);
				}
			}

			if (null != images.getVariants()) {
				images.getVariants().forEach(e -> {
					logger.info("Adding variant image for {} : {}", data.gtin(), e.getLarge().getURL());
					try {
						Resource r = new Resource(e.getLarge().getURL());
						r.getHardTags().add(ResourceTag.AMAZON_VARIANT_TAG);
						r.setDatasourceName(amazonDatasource.getName());
						data.getResources().add(r);
					} catch (ValidationException e1) {
						logger.error("Error while adding variant image", e1);
					}
				});
			}
		}

		////////////////////
		// Handling offers
		///////////////////
		String detailPageUrl = item.getDetailPageURL();
		Offers offers = item.getOffers();
		if (null != offers) {
			logger.info("Adding prices for {}", data.gtin());
			OfferListing minNewPrice = null;
			OfferListing minOccasionPrice = null;

			for (OfferListing o : offers.getListings()) {

				// o.getAvailability();
				// TODO : As constant
				if (o.getCondition().getValue().equals(AMAZON_PRODUCTSTATE_OCCASION)) {
					// Handling occasion product
					if (minOccasionPrice == null) {
						minOccasionPrice = o;
					} else {
						if (minOccasionPrice.getPrice().getAmount().doubleValue() > o.getPrice().getAmount()
								.doubleValue()) {
							minOccasionPrice = o;
						}
					}
				} else if (o.getCondition().getValue().equals(AMAZON_PRODUCTSTATE_NEW)) {
					// Handling new product
					if (minNewPrice == null) {
						minNewPrice = o;
					} else {
						if (minNewPrice.getPrice().getAmount().doubleValue() > o.getPrice().getAmount().doubleValue()) {
							minNewPrice = o;
						}
					}
				} else {
					logger.error("Unknown Amazon item condition : {}", o.getCondition().getValue());
				}
				
			}

			// Handling the best new offer if any
			if (null != minNewPrice) {
				DataFragment df = mapOfferToDataFragment(minNewPrice, detailPageUrl, data);
				ret.add(df);
			}

			// Handling the best occasion offer if any
			if (null != minOccasionPrice) {
				DataFragment df = mapOfferToDataFragment(minOccasionPrice, detailPageUrl, data);
				ret.add(df);
			}	
		}


		// We take one of the df to store attributes
		DataFragment current = ret.stream().findAny().orElse(initDataFragment("amazon.fr",detailPageUrl, data));
		
		
		
		// Handling product infos
		ItemInfo itemInfo = item.getItemInfo();
		if (null != itemInfo) {

			// Brand / manufacturer
			ByLineInfo lineInfo = itemInfo.getByLineInfo();
			if (null != lineInfo) {
				String brand = lineInfo.getBrand().getDisplayValue();
//				String manufacturer = lineInfo.getManufacturer().getDisplayValue();
//				
//				if (!StringUtils.equalsIgnoreCase(brand, manufacturer)) {
//					logger.error("Brand and manufacturer are not equals for {}", data.gtin());
//				}
				
				if (!StringUtils.isEmpty(brand)) {
					current.addReferentielAttribute(ReferentielKey.BRAND, brand);
				}
			}
			
			// NOTE : Features seems too noisy (on TV), disabled for now
//			MultiValuedAttribute features = itemInfo.getFeatures();			
//			for (String f : features.getDisplayValues()) {
//			
//				int pos = f.indexOf(":");
//				if (-1 != pos) {
//					String key = f.substring(0,pos).trim();
//					String val = f.substring(pos+1).trim();
//					current.addAttribute(key,val, "fr", false, null);
//				}
//			}
			
			ManufactureInfo manufactureInfo = itemInfo.getManufactureInfo();
			if (null != manufactureInfo) {
				if (null != manufactureInfo.getModel()) {
					String model = manufactureInfo.getModel().getDisplayValue();
					// TODO : From conf
					if (model.length() > 15) {
						model = IdHelper.extractBrandUids(model).stream().findFirst().orElse(null);
						logger.warn("Extracted model {} from amazon model : {}", model,manufactureInfo.getModel().getDisplayValue() );
						
					}
					if (null != model) {
						current.addReferentielAttribute(ReferentielKey.MODEL, model);				
					}
				}
				if (null != manufactureInfo.getWarranty()) current.addAttribute("WARRANTY",manufactureInfo.getWarranty().getDisplayValue(), "fr", false, null);
				//if (null != manufactureInfo.getItemPartNumber()) current.addAttribute("ITEM_PART_NUMBER",manufactureInfo.getItemPartNumber().getDisplayValue(), "fr", false, null);
			}
		
			
			ProductInfo productInfo = itemInfo.getProductInfo();
			productInfo.getColor();
			// TODO : Localisation
			if (null != productInfo.getColor()) current.addAttribute("COLOR",productInfo.getColor().getDisplayValue(), "fr", false, null);
			if (null != productInfo.getIsAdultProduct())  current.addAttribute("ADULT",productInfo.getIsAdultProduct().getLabel(), "fr", false, null);
			if (null != productInfo.getItemDimensions())  current.addAttribute("HEIGHT",getDisplayUnit(productInfo.getItemDimensions().getHeight()), "fr", false, null);
			if (null != productInfo.getItemDimensions()) current.addAttribute("WEIGHT",getDisplayUnit(productInfo.getItemDimensions().getWeight()), "fr", false, null);
			if (null != productInfo.getItemDimensions()) current.addAttribute("WIDTH",getDisplayUnit(productInfo.getItemDimensions().getWidth()), "fr", false, null);
			if (null != productInfo.getSize()) current.addAttribute("SIZE",productInfo.getSize().getDisplayValue() , "fr", false, null);
			
			if (null != productInfo.getReleaseDate()) {
				String year = productInfo.getReleaseDate().getDisplayValue().substring(0,4);			
				if (StringUtils.isNumeric(year)) {
					current.addAttribute("YEAR",year, "fr", false, null);
				}
			}
			
			
			// TODO : Add here also
			TechnicalInfo technicalInfo = itemInfo.getTechnicalInfo();
			
			if (null != technicalInfo) {
				SingleStringValuedAttribute energyClass = technicalInfo.getEnergyEfficiencyClass();
				if (null != energyClass) {
					current.addAttribute("CLASSE ENERGETIQUE",energyClass.getDisplayValue(), "fr", false, null);					
				}
				
				MultiValuedAttribute formats = technicalInfo.getFormats();
				if (null != formats) {
					logger.error("NOT NULL FORMATS {}",formats);
				}
				
			}
			
			
			// Adding the amazon name
			SingleStringValuedAttribute title = itemInfo.getTitle();
			if (null != title) {
				current.addName(title.getDisplayValue());
			}

			// TradeInInfo tradeInfo = itemInfo.getTradeInInfo();
		}

		// ParentASIN
//		String parentAsin = item.getParentASIN();
//		if (!StringUtils.isEmpty(parentAsin)) {
//			logger.info("Found a parent ASIN for {}", data.gtin());
//		}

		List<VariationAttribute> variationAttributes = item.getVariationAttributes();
		if (null != variationAttributes) {
			variationAttributes.forEach(e -> {
				String varName = e.getName();
				String varValue = e.getValue();
				logger.info("Found a variation attribute for {} : {}, {}", data.gtin(), varName, varValue);
			});
		}

		////////////////////
		// Adding category
		////////////////////
		BrowseNodeInfo browseNodeInfo = item.getBrowseNodeInfo();
			
//			if (null != item.getItemInfo().getContentInfo()) {
//				System.out.println(item.getItemInfo().getContentInfo());
//			}
//		browseNodeInfo.getBrowseNodes()
//		String category = IdHelper.getCategoryName(StringUtils
//				.join(browseNodeInfo.getBrowseNodes().stream().map(e -> e.getDisplayName()).toList(), " > "));
//
//		data.getDatasourceCategories().add(category);
//		data.getMappedCategories().add(new UnindexedKeyVal("amazon", category));

//		BigDecimal score = item.getScore();
//		if (null != score) {
//			logger.info("Score : {}", score);
//		}

		// RentalOffers rentalOffers = item.getRentalOffers();

		logger.warn("Amazon completion done for {}", data.gtin());
		return ret;

	}

	

	/**
	 * Map an amazon offer to a DataFragment
	 * 
	 * @param o
	 * @param url
	 * @return
	 */
	private DataFragment mapOfferToDataFragment(OfferListing o, String url, Product data) {

		DataFragment df = initDataFragment(url, url, data);
		
		Price p = new Price();
		p.setTimeStamp(System.currentTimeMillis());
		if (o.getCondition().getValue().equals(AMAZON_PRODUCTSTATE_OCCASION)) {
			df.setProductState(ProductCondition.OCCASION);
		} else if (o.getCondition().getValue().equals(AMAZON_PRODUCTSTATE_NEW)) {
			df.setProductState(ProductCondition.NEW);
		} else {
			logger.warn("Unknow amazon product condition : {}", o.getCondition().getLabel());
		}

		p.setPrice(o.getPrice().getAmount().doubleValue());
		try {
			p.setCurrency(o.getPrice().getCurrency());
		} catch (ParseException e) {
			logger.warn("Error setting amazoncurrency", e);
		}
	
		df.setPrice(p);
		
		return df;
	}
	
	
	/**
	 * Init an empty datafragment for amazon provider
	 * @param datasourceName
	 * @param url
	 * @param data
	 * @return
	 */
	private DataFragment initDataFragment(String datasourceName, String url, Product data) {
		DataFragment df = new DataFragment();
		df.setDatasourceName(amazonDatasource.getName());
		df.setDatasourceConfigName(amazonDatasource.getDatasourceConfigName());
		df.setAffiliatedUrl(url);
		df.setUrl(url);
		df.setLastIndexationDate(System.currentTimeMillis());
		df.setCreationDate(System.currentTimeMillis());
		df.addReferentielAttribute(ReferentielKey.GTIN, data.getId() );
		return df;
	}
	

	/**
	 * Display an amazon UnitBasedAttribute
	 * @param attr
	 * @return
	 */
	private String getDisplayUnit(UnitBasedAttribute attr) {
		if (null == attr) {
			return null;
		}
		return attr.getDisplayValue().toString() + " " + attr.getUnit();
	}

	
}
