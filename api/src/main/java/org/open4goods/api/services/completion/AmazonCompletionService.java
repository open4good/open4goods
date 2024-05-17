package org.open4goods.api.services.completion;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.AmazonCompletionConfig;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.api.services.aggregation.services.realtime.PriceAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.constants.ProductCondition;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Price;
import org.open4goods.model.data.UnindexedKeyVal;
import org.open4goods.model.product.Product;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.paapi5.v1.ApiClient;
import com.amazon.paapi5.v1.ApiException;
import com.amazon.paapi5.v1.BrowseNodeInfo;
import com.amazon.paapi5.v1.ByLineInfo;
import com.amazon.paapi5.v1.Classifications;
import com.amazon.paapi5.v1.ContentInfo;
import com.amazon.paapi5.v1.ContentRating;
import com.amazon.paapi5.v1.CustomerReviews;
import com.amazon.paapi5.v1.ErrorData;
import com.amazon.paapi5.v1.ExternalIds;
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
import com.amazon.paapi5.v1.TradeInInfo;
import com.amazon.paapi5.v1.VariationAttribute;
import com.amazon.paapi5.v1.api.DefaultApi;

public class AmazonCompletionService extends AbstractCompletionService {

	protected static final Logger logger = LoggerFactory.getLogger(AmazonCompletionService.class);

	private AmazonCompletionConfig amazonConfig;

	// We use the price agg service to store amazon price
	private PriceAggregationService priceAggregationService;
	// The amazon api
	private DefaultApi api;

	private ArrayList<GetItemsResource> getItemsResources;
	private ArrayList<SearchItemsResource> searchItemsResources;

	private DataSourceConfigService dataSourceConfigService;

	public AmazonCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService,
			ApiProperties apiProperties, DataSourceConfigService dataSourceConfigService) {
		// TODO : Should set a specific log level here (not "agg(regation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());
		this.amazonConfig = apiProperties.getAmazonConfig();
		this.dataSourceConfigService = dataSourceConfigService;
		this.priceAggregationService = new PriceAggregationService(logger, dataSourceConfigService);
		
		
		// The amazon api client
		ApiClient client = new ApiClient();

		// Setting credentials
		client.setAccessKey(amazonConfig.getAccessKey());
		client.setSecretKey(amazonConfig.getSecretKey());
		client.setHost(amazonConfig.getHost());
		client.setRegion(amazonConfig.getRegion());
		api = new DefaultApi(client);

		// Setting the itemresources to be used for direct product retrieving
		// https://webservices.amazon.com/paapi5/documentation/get-items.html#resources-parameter
		this.getItemsResources = new ArrayList<GetItemsResource>();
		// Adding all
		for (GetItemsResource gr : GetItemsResource.values()) {
			getItemsResources.add(gr);

		}
//        
//        getItemsResources.add(GetItemsResource.ITEMINFO_CLASSIFICATIONS);
//        getItemsResources.add(GetItemsResource.ITEMINFO_FEATURES);
//        getItemsResources.add(GetItemsResource.ITEMINFO_MANUFACTUREINFO);
//        getItemsResources.add(GetItemsResource.ITEMINFO_PRODUCTINFO);
//        getItemsResources.add(GetItemsResource.ITEMINFO_TECHNICALINFO);
//        getItemsResources.add(GetItemsResource.ITEMINFO_TRADEININFO);                
//        getItemsResources.add(GetItemsResource.OFFERS_LISTINGS_PRICE);        
//        getItemsResources.add(GetItemsResource.IMAGES_PRIMARY_LARGE);
//        getItemsResources.add(GetItemsResource.IMAGES_VARIANTS_LARGE);
//        

		// Setting the itemresources to be used for product search
		// https://webservices.amazon.com/paapi5/documentation/search-items.html#resources-parameter
		this.searchItemsResources = new ArrayList<SearchItemsResource>();
		// Adding all
		for (SearchItemsResource sr : SearchItemsResource.values()) {
			searchItemsResources.add(sr);
		}
	}

	/**
	 * Trigger amazon call on a product
	 */
	public void processProduct(VerticalConfig vertical, Product data) {
		logger.info("Amazon completion for {}", data.getId());
		String asin = data.getExternalId().getAsin();
		if (StringUtils.isEmpty(asin)) {
			// First time API Call, we operate through the search method
			logger.info("Initial amazon call (get) for {}", data.gtin());
			completeSearch(vertical, data);
		} else {
			
			if (!asin.equals("NOT_FOUND")) {
			
				// If we already have the ASIN, we operate a direct get request
				logger.info("Further amazon call (get) for {}", data.gtin());
				// TODO : Do not proceed, have a delay threshold
				completeGet(vertical, data);
			} else {
//				TODO : Could hve a strategy to try back items, after a while
				logger.info("Amazon fetch of {} skipped because failed in a previous attempt", data.gtin());
				return;
			}
		}
		// Indexing the result
		dataRepository.index(data);

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
	private void completeGet(VerticalConfig vertical, Product data) {
		GetItemsRequest getItemsRequest = new GetItemsRequest().itemIdType(ItemIdType.ASIN)
				.itemIds(List.of(data.gtin())).partnerTag(amazonConfig.getPartnerTag())
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
				processAmazonItem(item, vertical, data);
			}

		} catch (ApiException exception) {
			logger.error("Amazon API error {} : {} \n {} ", exception.getCode(), exception.getResponseBody(), exception.getMessage());
		}
	}

	/**
	 * Proceed to the search api call on amazon
	 * 
	 * @param vertical
	 * @param data
	 */
	private void completeSearch(VerticalConfig vertical, Product data) {
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
				data.getExternalId().setAsin("NOT_FOUND");
				return;
			} else if (response.getSearchResult().getItems().size() > 1) {
				logger.warn("Multiple amazon product for {}", data.gtin());
			}

			for (Item item : response.getSearchResult().getItems()) {
				processAmazonItem(item, vertical, data);
			}

		} catch (ApiException exception) {
			logger.error("Amazon API error {} : {} \n {} ", exception.getCode(), exception.getResponseBody(),	exception.getMessage());
		}
	}

	private void processAmazonItem(Item item, VerticalConfig vertical, Product data) {
		
		logger.info("Setting amazon data for {}:{}", vertical.getId(), data.gtin());
		
		// Setting the ASIN
		String asin = item.getASIN();
		if (!StringUtils.isEmpty(asin)) {
			data.getExternalId().setAsin(asin);
		} else {
			logger.warn("Empty ASIN returned for {}", data.gtin());
		}
		
		 // Handling images		
		Images images = item.getImages();		
		if (null != images) {
			if (null != images.getPrimary()) {	
				logger.info("Adding primary image for {} : {}", data.gtin(), images.getPrimary().getLarge());
				data.addImage(images.getPrimary().getLarge().getURL(), "amazon");
			}
			
			if (null != images.getVariants()) {						
				images.getVariants().forEach(e -> {					
					logger.info("Adding variant image for {} : {}", data.gtin(), e.getLarge().getURL());
					data.addImage(e.getLarge().getURL(), "amazon");										
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
				
				//o.getAvailability();
				// TODO : As constant
				if (o.getCondition().getLabel().equals("OCCASION")) {
					// Handling occasion product
					if (minOccasionPrice == null) {
						minOccasionPrice = o;
					} else {
						if (minOccasionPrice.getPrice().getAmount().doubleValue() > o.getPrice().getAmount().doubleValue()) {
							minOccasionPrice = o;
						}
					}
				} else {
					// Handling occasion product
					if (minNewPrice == null) {
						minNewPrice = o;
					} else {
						if (minNewPrice.getPrice().getAmount().doubleValue() > o.getPrice().getAmount().doubleValue()) {
							minNewPrice = o;
						}
					}				
				}			
			}
			
			// Handling the best new offer if any
			if (null != minNewPrice) {
				DataFragment df = mapOfferToDataFragment(minNewPrice, detailPageUrl);
				try {
					priceAggregationService.onDataFragment(df, data, vertical);
				} catch (AggregationSkipException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			// Handling the best occasion offer if any
			if (null != minOccasionPrice) {
				DataFragment df = mapOfferToDataFragment(minOccasionPrice, detailPageUrl);
				try {
					priceAggregationService.onDataFragment(df, data, vertical);
				} catch (AggregationSkipException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
//			offers.getSummaries().getFirst().getLowestPrice();			
		}
		
		
		
//		// Handling customer reviews
//		CustomerReviews customerReviews = item.getCustomerReviews();
//		if (null != customerReviews) {
//			customerReviews.getCount(); 
//		}
		
		// Handling product infos
		ItemInfo itemInfo = item.getItemInfo();
		if (null != itemInfo) {
			ByLineInfo lineInfo = itemInfo.getByLineInfo();
			if (null != lineInfo) {
				String brand = lineInfo.getBrand().getDisplayValue();
				String manufacturer = lineInfo.getManufacturer().getDisplayValue();
			}
			
			
			
			Classifications classifications = itemInfo.getClassifications();
			ContentInfo contentInfo = itemInfo.getContentInfo();
			ContentRating contentRating = itemInfo.getContentRating();
			ExternalIds externalIds = itemInfo.getExternalIds();
			MultiValuedAttribute features = itemInfo.getFeatures();
			ManufactureInfo manufactureInfo = itemInfo.getManufactureInfo();
			ProductInfo productInfo = itemInfo.getProductInfo();
			TechnicalInfo technicalInfo = itemInfo.getTechnicalInfo();
			SingleStringValuedAttribute title = itemInfo.getTitle();
			TradeInInfo tradeInfo = itemInfo.getTradeInInfo();
		}
		
		
		// ParentASIN
		String parentAsin = item.getParentASIN();
		if (!StringUtils.isEmpty(parentAsin)) {
			logger.info("Found a parent ASIN for {}", data.gtin());
		}
		
		List<VariationAttribute> variationAttributes = item.getVariationAttributes();
		if (null != variationAttributes) {
			variationAttributes.forEach(e ->  {
				String varName = e.getName();
				String varValue = e.getValue();
				logger.info("Found a variation attribute for {} : {}, {}", data.gtin(), varName, varValue);				
			});
		}
		
		
		
		////////////////////
		// Adding category
		////////////////////
		BrowseNodeInfo browseNodeInfo = item.getBrowseNodeInfo();
		String category = IdHelper.getCategoryName(StringUtils.join(browseNodeInfo.getBrowseNodes().stream().map(e->e.getDisplayName()).toList()," > "));
		
		data.getDatasourceCategories().add(category);
		data.getMappedCategories().add(new UnindexedKeyVal("amazon",category));
	
		BigDecimal score = item.getScore();
		if (null != score) {
			logger.info("Score : {}", score);
		}
		
		// RentalOffers rentalOffers = item.getRentalOffers();

		
		logger.info("Amazon completion done for {}", data.gtin());
		
	}

	/**
	 * Map an amazon offer to a DataFragment
	 * @param o
	 * @param url
	 * @return
	 */
	private DataFragment mapOfferToDataFragment(OfferListing o, String url) {
		DataFragment df = new DataFragment();
		Price p = new Price();
		if (o.getCondition().getLabel().equals("OCCASION")) {
			df.setProductState(ProductCondition.OCCASION);
		} else if (o.getCondition().getLabel().equals("NEW")) {
			df.setProductState(ProductCondition.NEW);
		} else {
			logger.warn("Unlnow amazon product condition : {}", o.getCondition().getLabel());
		}
		
		p.setPrice(o.getPrice().getAmount().doubleValue());
		try {
			p.setCurrency(o.getPrice().getCurrency());
		} catch (ParseException e) {
			logger.warn("Error setting amazoncurrency", e);
		}
				
		df.setAffiliatedUrl(url);		
		df.setPrice(p);
		
		return df;
	}

}
