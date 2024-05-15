package org.open4goods.api.services.completion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.AmazonCompletionConfig;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.api.services.aggregation.services.realtime.PriceAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.model.product.Product;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.paapi5.v1.ApiClient;
import com.amazon.paapi5.v1.ApiException;
import com.amazon.paapi5.v1.BrowseNodeInfo;
import com.amazon.paapi5.v1.CustomerReviews;
import com.amazon.paapi5.v1.ErrorData;
import com.amazon.paapi5.v1.GetItemsRequest;
import com.amazon.paapi5.v1.GetItemsResource;
import com.amazon.paapi5.v1.GetItemsResponse;
import com.amazon.paapi5.v1.Images;
import com.amazon.paapi5.v1.Item;
import com.amazon.paapi5.v1.ItemIdType;
import com.amazon.paapi5.v1.ItemInfo;
import com.amazon.paapi5.v1.Offers;
import com.amazon.paapi5.v1.PartnerType;
import com.amazon.paapi5.v1.RentalOffers;
import com.amazon.paapi5.v1.SearchItemsRequest;
import com.amazon.paapi5.v1.SearchItemsResource;
import com.amazon.paapi5.v1.SearchItemsResponse;
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
		if (StringUtils.isEmpty(data.getExternalId().getAsin())) {
			// First time API Call, we operate through the search method
			logger.info("Initial amazon call (get) for {}", data.gtin());
			completeSearch(vertical, data);
		} else {
			// If we already have the ASIN, we operate a direct get request
			logger.info("Further amazon call (get) for {}", data.gtin());
			completeGet(vertical, data);
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

			response.getSearchResult().getItems();
			response.getSearchResult().getSearchRefinements();

			if (response.getSearchResult().getItems().size() == 0) {
				logger.warn("No amazon product for {}", data.gtin());
			} else if (response.getSearchResult().getItems().size() == 0) {
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
		
		
		
		String asin = item.getASIN();
		if (!StringUtils.isEmpty(asin)) {
			data.getExternalId().setAsin(asin);
		} else {
			logger.warn("Empty ASIN returned for {}", data.gtin());
		}
		
		Images images = item.getImages();
		
		String detailPageUrl = item.getDetailPageURL();
		Offers offers = item.getOffers();
		
		CustomerReviews customerReviews = item.getCustomerReviews();
		
		
		ItemInfo itemInfo = item.getItemInfo();
		String parentAsin = item.getParentASIN();
		RentalOffers rentalOffers = item.getRentalOffers();
		BigDecimal score = item.getScore();
		List<VariationAttribute> variationAttributes = item.getVariationAttributes();
		BrowseNodeInfo browseNodeInfo = item.getBrowseNodeInfo();
		
	}

}
