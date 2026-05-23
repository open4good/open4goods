package org.open4goods.api.services.completion;

import com.amazon.paapi5.v1.ApiClient;
import com.amazon.paapi5.v1.ApiException;
import com.amazon.paapi5.v1.GetItemsRequest;
import com.amazon.paapi5.v1.GetItemsResponse;
import com.amazon.paapi5.v1.SearchItemsRequest;
import com.amazon.paapi5.v1.SearchItemsResponse;
import com.amazon.paapi5.v1.api.DefaultApi;
import org.open4goods.api.config.yml.AmazonCompletionConfig;

/**
 * Small adapter around the generated Amazon PA-API client.
 *
 * <p>Keeping the generated SDK behind this interface lets completion tests
 * exercise request building and response mapping without performing network
 * calls or depending on PA-API credentials.
 */
public interface AmazonPaapiClient {

    /**
     * Fetches Amazon items by ASIN.
     *
     * @param request PA-API GetItems request
     * @return PA-API GetItems response
     * @throws ApiException when PA-API rejects or fails the request
     */
    GetItemsResponse getItems(GetItemsRequest request) throws ApiException;

    /**
     * Searches Amazon items.
     *
     * @param request PA-API SearchItems request
     * @return PA-API SearchItems response
     * @throws ApiException when PA-API rejects or fails the request
     */
    SearchItemsResponse searchItems(SearchItemsRequest request) throws ApiException;

    /**
     * Builds a PA-API SDK adapter from Amazon completion configuration.
     *
     * @param config Amazon completion configuration
     * @return generated SDK-backed PA-API client
     */
    static AmazonPaapiClient fromConfig(AmazonCompletionConfig config) {
        ApiClient client = new ApiClient();
        client.setAccessKey(config.getAccessKey());
        client.setSecretKey(config.getSecretKey());
        client.setHost(config.getHost());
        client.setRegion(config.getRegion());

        DefaultApi api = new DefaultApi(client);
        return new AmazonSdkPaapiClient(api);
    }

    /**
     * Generated SDK-backed PA-API client implementation.
     */
    final class AmazonSdkPaapiClient implements AmazonPaapiClient {
        private final DefaultApi api;

        private AmazonSdkPaapiClient(DefaultApi api) {
            this.api = api;
        }

        @Override
        public GetItemsResponse getItems(GetItemsRequest request) throws ApiException {
            return api.getItems(request);
        }

        @Override
        public SearchItemsResponse searchItems(SearchItemsRequest request) throws ApiException {
            return api.searchItems(request);
        }
    }
}
