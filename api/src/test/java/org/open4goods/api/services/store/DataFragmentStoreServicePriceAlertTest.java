package org.open4goods.api.services.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.open4goods.api.dto.pricealert.InternalPriceEventDto;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.pricealert.PriceAlertingService;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.price.Currency;
import org.open4goods.model.price.Price;
import org.open4goods.services.productrepository.config.IndexationConfig;
import org.open4goods.services.productrepository.services.ProductRepository;

/**
 * Unit tests for price-drop event publication in
 * {@link DataFragmentStoreService}.
 */
class DataFragmentStoreServicePriceAlertTest
{
    private AggregationFacadeService aggregationFacadeService;
    private ProductRepository productRepository;
    private PriceAlertingService priceAlertingService;
    private DataFragmentStoreService storeService;

    @BeforeEach
    void setUp()
    {
        aggregationFacadeService = Mockito.mock(AggregationFacadeService.class);
        productRepository = Mockito.mock(ProductRepository.class);
        priceAlertingService = Mockito.mock(PriceAlertingService.class);

        IndexationConfig indexationConfig = new IndexationConfig();
        indexationConfig.setDataFragmentworkers(0);

        storeService = new DataFragmentStoreService(
                new StandardiserService()
                {
                    @Override
                    public void standarise(Price price, Currency currency)
                    {
                    }
                },
                aggregationFacadeService,
                productRepository,
                indexationConfig,
                priceAlertingService);
    }

    @Test
    void aggregateAndstorePublishesOneFinalDropEventPerGtin() throws Exception
    {
        Product existing = productWithBestPrice(1234567890128L, 120d, ProductCondition.NEW);
        Map<String, Product> existingProducts = new HashMap<>();
        existingProducts.put("1234567890128", existing);
        when(productRepository.multiGetById(anyCollection())).thenReturn(existingProducts);

        when(aggregationFacadeService.updateOne(any(), any())).thenAnswer(invocation -> {
            Product product = invocation.getArgument(1);
            DataFragment fragment = invocation.getArgument(0);
            if ("first-source".equals(fragment.getDatasourceName()))
            {
                copyBestPrice(product, 110d, ProductCondition.NEW);
            }
            else
            {
                copyBestPrice(product, 90d, ProductCondition.NEW);
            }
            return product;
        });

        storeService.aggregateAndstore(List.of(
                fragment("1234567890128", "first-source", 1),
                fragment("1234567890128", "second-source", 2)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InternalPriceEventDto>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(priceAlertingService).publishPriceDropEvents(eventsCaptor.capture());

        assertThat(eventsCaptor.getValue()).hasSize(1);
        assertThat(eventsCaptor.getValue().getFirst().previousPrice()).isEqualTo(120d);
        assertThat(eventsCaptor.getValue().getFirst().currentPrice()).isEqualTo(90d);
        verify(productRepository).addToFullindexationQueue(anyCollection());
        verify(productRepository, never()).addToPartialIndexationQueue(anyCollection());
    }

    @Test
    void aggregateAndstoreDoesNotPublishDropForNewProduct() throws Exception
    {
        when(productRepository.multiGetById(anyCollection())).thenReturn(Map.of());
        when(aggregationFacadeService.updateOne(any(), any())).thenAnswer(invocation -> {
            Product product = invocation.getArgument(1);
            product.setId(1234567890128L);
            copyBestPrice(product, 90d, ProductCondition.NEW);
            return product;
        });

        storeService.aggregateAndstore(List.of(fragment("1234567890128", "first-source", 1)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InternalPriceEventDto>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(priceAlertingService).publishPriceDropEvents(eventsCaptor.capture());
        assertThat(eventsCaptor.getValue()).isEmpty();
    }

    private DataFragment fragment(String gtin, String datasourceName, int hash)
    {
        DataFragment fragment = new DataFragment();
        fragment.setDatasourceName(datasourceName);
        fragment.setFragmentHashCode(hash);
        fragment.setLastIndexationDate(System.currentTimeMillis());
        Map<org.open4goods.model.attribute.ReferentielKey, String> referential = new HashMap<>();
        referential.put(org.open4goods.model.attribute.ReferentielKey.GTIN, gtin);
        fragment.setReferentielAttributes(referential);
        return fragment;
    }

    private Product productWithBestPrice(Long id, Double price, ProductCondition condition)
    {
        Product product = new Product();
        product.setId(id);
        copyBestPrice(product, price, condition);
        return product;
    }

    private void copyBestPrice(Product product, Double price, ProductCondition condition)
    {
        AggregatedPrice aggregatedPrice = new AggregatedPrice();
        aggregatedPrice.setDatasourceName("test-source");
        aggregatedPrice.setProductState(condition);
        aggregatedPrice.setPrice(price);
        aggregatedPrice.setCurrency(Currency.EUR);
        aggregatedPrice.setTimeStamp(System.currentTimeMillis());

        AggregatedPrices aggregatedPrices = new AggregatedPrices();
        aggregatedPrices.setOffers(new java.util.HashSet<>(List.of(aggregatedPrice)));
        product.setPrice(aggregatedPrices);
        product.setOffersCount(1);
    }
}
