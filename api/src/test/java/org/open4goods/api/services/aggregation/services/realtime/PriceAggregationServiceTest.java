package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.Currency;
import org.open4goods.model.price.Price;
import org.open4goods.model.product.InStock;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests price aggregation behavior for offer-level logistics fields.
 */
class PriceAggregationServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceAggregationServiceTest.class);

    /**
     * Verifies logistics fields are copied while price ordering remains item-price based.
     *
     * @throws Exception when aggregation fails
     */
    @Test
    void aggregatesOfferLogisticsWithoutChangingBestPriceRanking() throws Exception {
        PriceAggregationService service = new PriceAggregationService(LOGGER);
        Product product = new Product(123L);

        DataFragment cheaperWithShipping = offer("merchant-a", "https://example.com/a", 100.0, 20.0, 2, 4);
        DataFragment expensiveFreeShipping = offer("merchant-b", "https://example.com/b", 105.0, 0.0, 1, 10);

        service.onDataFragment(cheaperWithShipping, product, new VerticalConfig());
        service.onDataFragment(expensiveFreeShipping, product, new VerticalConfig());

        AggregatedPrice best = product.bestPrice();

        assertThat(best.getDatasourceName()).isEqualTo("merchant-a");
        assertThat(best.getPrice()).isEqualTo(100.0);
        assertThat(best.getShippingCost()).isEqualTo(20.0);
        assertThat(best.getShippingTime()).isEqualTo(2);
        assertThat(best.getQuantityInStock()).isEqualTo(4);
        assertThat(product.getOffersCount()).isEqualTo(2);
    }

    private static DataFragment offer(String datasource, String url, double priceValue, double shippingCost,
            int shippingTime, int quantityInStock) {
        DataFragment fragment = DataFragment.newOffer(url, datasource);
        fragment.addName("Offer " + datasource);
        fragment.setProductState(ProductCondition.NEW);
        fragment.setInStock(InStock.INSTOCK);
        fragment.setShippingCost(shippingCost);
        fragment.setShippingTime(shippingTime);
        fragment.setQuantityInStock(quantityInStock);

        Price price = new Price();
        price.setCurrency(Currency.EUR);
        price.setPrice(priceValue);
        price.setTimeStamp(System.currentTimeMillis());
        fragment.setPrice(price);
        return fragment;
    }
}
