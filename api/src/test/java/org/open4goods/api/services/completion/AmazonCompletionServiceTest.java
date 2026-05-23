package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.AmazonCompletionConfig;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

import ch.qos.logback.classic.Level;

import com.amazon.paapi5.v1.ApiException;
import com.amazon.paapi5.v1.ByLineInfo;
import com.amazon.paapi5.v1.GetItemsRequest;
import com.amazon.paapi5.v1.GetItemsResponse;
import com.amazon.paapi5.v1.ImageSize;
import com.amazon.paapi5.v1.ImageType;
import com.amazon.paapi5.v1.Images;
import com.amazon.paapi5.v1.Item;
import com.amazon.paapi5.v1.ItemInfo;
import com.amazon.paapi5.v1.ItemsResult;
import com.amazon.paapi5.v1.OfferCondition;
import com.amazon.paapi5.v1.OfferListing;
import com.amazon.paapi5.v1.OfferPrice;
import com.amazon.paapi5.v1.Offers;
import com.amazon.paapi5.v1.SearchItemsRequest;
import com.amazon.paapi5.v1.SearchItemsResponse;
import com.amazon.paapi5.v1.SearchResult;
import com.amazon.paapi5.v1.SingleStringValuedAttribute;

class AmazonCompletionServiceTest {

    private AmazonCompletionService service;
    private RecordingAmazonPaapiClient paapiClient;
    private AmazonCompletionConfig config;

    @BeforeEach
    void setUp() {
        config = new AmazonCompletionConfig(true, "access", "secret", "tag-21", "webservices.amazon.fr",
                "eu-west-1", Duration.ZERO, Duration.ofDays(30), 10, "amazon.fr.yml", "All", "www.amazon.fr");
        paapiClient = new RecordingAmazonPaapiClient();

        ApiProperties apiProperties = mock(ApiProperties.class);
        when(apiProperties.getAmazonConfig()).thenReturn(config);
        when(apiProperties.logsFolder()).thenReturn("target/test-logs");
        when(apiProperties.aggLogLevel()).thenReturn(Level.INFO);

        DataSourceProperties datasource = new DataSourceProperties();
        datasource.setName(AmazonCompletionService.DATASOURCE_NAME);
        datasource.setDatasourceConfigName("amazon.fr.yml");

        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        when(dataSourceConfigService.getDatasourceConfig("amazon.fr.yml")).thenReturn(datasource);

        AggregationFacadeService aggregationFacadeService = mock(AggregationFacadeService.class);
        when(aggregationFacadeService.getStandardAggregator("amazon-aggregation")).thenReturn(mock(StandardAggregator.class));

        service = new AmazonCompletionService(mock(ProductRepository.class), mock(VerticalsConfigService.class),
                apiProperties, dataSourceConfigService, aggregationFacadeService, paapiClient);
    }

    @Test
    void shouldUseProductDatasourceCodeAsRefreshCache() {
        Product product = new Product(123L);
        VerticalConfig vertical = new VerticalConfig();

        assertThat(service.shouldProcess(vertical, product)).isTrue();

        product.getDatasourceCodes().put(service.getDatasourceName(), System.currentTimeMillis());
        assertThat(service.shouldProcess(vertical, product)).isFalse();

        product.getDatasourceCodes().put(service.getDatasourceName(),
                System.currentTimeMillis() - config.getRefreshDuration().toMillis() - 1_000L);
        assertThat(service.shouldProcess(vertical, product)).isTrue();
    }

    @Test
    void shouldNotProcessWhenAmazonCompletionIsDisabled() {
        AmazonCompletionConfig disabled = new AmazonCompletionConfig();
        ApiProperties apiProperties = mock(ApiProperties.class);
        when(apiProperties.getAmazonConfig()).thenReturn(disabled);
        when(apiProperties.logsFolder()).thenReturn("target/test-logs");
        when(apiProperties.aggLogLevel()).thenReturn(Level.INFO);

        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        AggregationFacadeService aggregationFacadeService = mock(AggregationFacadeService.class);
        when(aggregationFacadeService.getStandardAggregator("amazon-aggregation")).thenReturn(mock(StandardAggregator.class));

        AmazonCompletionService disabledService = new AmazonCompletionService(mock(ProductRepository.class),
                mock(VerticalsConfigService.class), apiProperties, dataSourceConfigService, aggregationFacadeService,
                null);

        assertThat(disabledService.shouldProcess(new VerticalConfig(), new Product(123L))).isFalse();
    }

    @Test
    void searchCompletionMapsAmazonItemToProductAndFragments() {
        Product product = new Product(1234567890123L);
        paapiClient.searchResponse = new SearchItemsResponse()
                .searchResult(new SearchResult().items(List.of(amazonItem())));

        Set<DataFragment> fragments = service.completeSearch(product);

        assertThat(paapiClient.lastSearchRequest.getKeywords()).isEqualTo(product.gtin());
        assertThat(paapiClient.lastSearchRequest.getItemCount()).isEqualTo(3);
        assertThat(product.getExternalIds().getAsin()).isEqualTo("B000TEST");
        assertThat(product.getResources())
                .anySatisfy(resource -> assertThat(resource.getHardTags()).contains(ResourceTag.AMAZON_PRIMARY_TAG));

        assertThat(fragments).hasSize(2);
        assertThat(fragments)
                .anySatisfy(fragment -> {
                    assertThat(fragment.getProductState()).isEqualTo(ProductCondition.NEW);
                    assertThat(fragment.getPrice().getPrice()).isEqualTo(19.99);
                    assertThat(fragment.getPrice().getCurrency()).isEqualTo(Currency.EUR);
                    assertThat(fragment.getReferentielAttributes()).containsEntry(ReferentielKey.BRAND, "Test Brand");
                    assertThat(fragment.getNames()).contains("Test Amazon Product");
                })
                .anySatisfy(fragment -> {
                    assertThat(fragment.getProductState()).isEqualTo(ProductCondition.OCCASION);
                    assertThat(fragment.getPrice().getPrice()).isEqualTo(12.50);
                });
    }

    @Test
    void getCompletionUsesKnownAsin() {
        Product product = new Product(1234567890123L);
        product.getExternalIds().setAsin("B000TEST");
        paapiClient.getResponse = new GetItemsResponse()
                .itemsResult(new ItemsResult().items(List.of(amazonItem())));

        Set<DataFragment> fragments = service.completeGet(product);

        assertThat(paapiClient.lastGetRequest.getItemIds()).containsExactly("B000TEST");
        assertThat(fragments).hasSize(2);
    }

    private Item amazonItem() {
        return new Item()
                .ASIN("B000TEST")
                .detailPageURL("https://www.amazon.fr/dp/B000TEST?tag=tag-21")
                .images(new Images().primary(new ImageType().large(new ImageSize()
                        .URL("https://m.media-amazon.com/images/I/test.jpg"))))
                .itemInfo(new ItemInfo()
                        .title(new SingleStringValuedAttribute().displayValue("Test Amazon Product"))
                        .byLineInfo(new ByLineInfo().brand(new SingleStringValuedAttribute()
                                .displayValue("Test Brand"))))
                .offers(new Offers().listings(List.of(
                        offer("New", "19.99"),
                        offer("Used", "12.50"))));
    }

    private OfferListing offer(String condition, String amount) {
        return new OfferListing()
                .condition(new OfferCondition().value(condition))
                .price(new OfferPrice().amount(new BigDecimal(amount)).currency("EUR"));
    }

    private static final class RecordingAmazonPaapiClient implements AmazonPaapiClient {
        private GetItemsRequest lastGetRequest;
        private SearchItemsRequest lastSearchRequest;
        private GetItemsResponse getResponse = new GetItemsResponse();
        private SearchItemsResponse searchResponse = new SearchItemsResponse();

        @Override
        public GetItemsResponse getItems(GetItemsRequest request) throws ApiException {
            lastGetRequest = request;
            return getResponse;
        }

        @Override
        public SearchItemsResponse searchItems(SearchItemsRequest request) throws ApiException {
            lastSearchRequest = request;
            return searchResponse;
        }
    }
}
