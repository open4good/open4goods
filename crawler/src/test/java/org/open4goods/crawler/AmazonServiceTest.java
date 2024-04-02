package org.open4goods.crawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.amazon.paapi5.v1.ApiClient;
import com.amazon.paapi5.v1.ApiException;
import com.amazon.paapi5.v1.ErrorData;
import com.amazon.paapi5.v1.GetItemsRequest;
import com.amazon.paapi5.v1.GetItemsResource;
import com.amazon.paapi5.v1.GetItemsResponse;
import com.amazon.paapi5.v1.Item;
import com.amazon.paapi5.v1.ItemIdType;
import com.amazon.paapi5.v1.PartnerType;
import com.amazon.paapi5.v1.api.DefaultApi;



public class AmazonServiceTest {

	@Test
	public void test() {}
	
	
    
    private static Map<String, Item> parse_response(List<Item> items) {
        Map<String, Item> mappedResponse = new HashMap<String, Item>();
        for (Item item : items) {
            mappedResponse.put(item.getASIN(), item);
        }
        return mappedResponse;
    }

    public static void main(String[] args) {
        ApiClient client = new ApiClient();

        // Add your credentials
        // Please add your access key here
        client.setAccessKey("");
        client.setSecretKey("");
        // Enter your partner tag (store/tracking id)
        String partnerTag = "nudger-21";

        /*
         * PAAPI Host and Region to which you want to send request. For more
         * details refer:
         * https://webservices.amazon.com/paapi5/documentation/common-request-parameters.html#host-and-region
         */
        client.setHost("webservices.amazon.fr");
        client.setRegion("eu-west-1");

        DefaultApi api = new DefaultApi(client);
 
        // Request initialization
        /*
         * Choose resources you want from GetItemsResource enum For more
         * details, refer:
         * https://webservices.amazon.com/paapi5/documentation/get-items.html#resources-parameter
         */
        List<GetItemsResource> getItemsResources = new ArrayList<GetItemsResource>();
        getItemsResources.add(GetItemsResource.ITEMINFO_TITLE);
        getItemsResources.add(GetItemsResource.ITEMINFO_CLASSIFICATIONS);
        getItemsResources.add(GetItemsResource.ITEMINFO_FEATURES);
        getItemsResources.add(GetItemsResource.ITEMINFO_MANUFACTUREINFO);
        getItemsResources.add(GetItemsResource.ITEMINFO_PRODUCTINFO);
        getItemsResources.add(GetItemsResource.ITEMINFO_TECHNICALINFO);
        getItemsResources.add(GetItemsResource.ITEMINFO_TRADEININFO);                
        getItemsResources.add(GetItemsResource.OFFERS_LISTINGS_PRICE);

        // Choose item id(s)
        List<String> itemIds = new ArrayList<String>();
        itemIds.add("B0C7K1YKSV");
//        itemIds.add("B00X4WHP55");
//        itemIds.add("1401263119");

        // Forming the request
        GetItemsRequest getItemsRequest = new GetItemsRequest().itemIdType(ItemIdType.ASIN). itemIds(itemIds).partnerTag(partnerTag)
                .resources(getItemsResources).partnerType(PartnerType.ASSOCIATES);

        try {
            // Sending the request
            GetItemsResponse response = api.getItems(getItemsRequest);

            System.out.println("API called successfully");
            System.out.println("Complete response: " + response);

            // Parsing the request
            if (response.getItemsResult() != null) {
                System.out.println("Printing all item information in ItemsResult:");
                Map<String, Item> responseList = parse_response(response.getItemsResult().getItems());
                for (String itemId : itemIds) {
                    if (response.getItemsResult().getItems() != null) {
                        System.out.println("Printing information about the ASIN: " + itemId);
                        if (responseList.get(itemId) != null) {
                            Item item = responseList.get(itemId);
                            if (item.getASIN() != null) {
                                System.out.println("ASIN: " + item.getASIN());
                            }
                            if (item.getDetailPageURL() != null) {
                                System.out.println("DetailPageURL: " + item.getDetailPageURL());
                            }
                            if (item.getItemInfo() != null && item.getItemInfo().getTitle() != null
                                    && item.getItemInfo().getTitle().getDisplayValue() != null) {
                                System.out.println("Title: " + item.getItemInfo().getTitle().getDisplayValue());
                            }
                            if (item.getOffers() != null && item.getOffers().getListings() != null
                                    && item.getOffers().getListings().get(0).getPrice() != null
                                    && item.getOffers().getListings().get(0).getPrice().getDisplayAmount() != null) {
                                System.out.println("Buying price: "
                                        + item.getOffers().getListings().get(0).getPrice().getDisplayAmount());
                            }
                            

							
							item.getBrowseNodeInfo();
							
                        } else {
                            System.out.println("Item not found, check errors");
                        }
                    }
                }
            }
            if (response.getErrors() != null) {
                System.out.println("Printing errors:\nPrinting Errors from list of Errors");
                for (ErrorData error : response.getErrors()) {
                    System.out.println("Error code: " + error.getCode());
                    System.out.println("Error message: " + error.getMessage());
                }
            }
        } catch (ApiException exception) {
            // Exception handling
            System.out.println("Error calling PA-API 5.0!");
            System.out.println("Status code: " + exception.getCode());
            System.out.println("Errors: " + exception.getResponseBody());
            System.out.println("Message: " + exception.getMessage());
            if (exception.getResponseHeaders() != null) {
                // Printing request reference
                System.out.println("Request ID: " + exception.getResponseHeaders().get("x-amzn-RequestId"));
            }
            // exception.printStackTrace();
        } catch (Exception exception) {
            System.out.println("Exception message: " + exception.getMessage());
            // exception.printStackTrace();
        }
    }

}
