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

    /**
     * Verifies out-of-stock placeholder prices (Back Market publishes 1 EUR on
     * out-of-stock rows) never become offers nor the occasion minimum price.
     *
     * @throws Exception when aggregation fails
     */
    @Test
    void ignoresOutOfStockOffers() throws Exception {
        PriceAggregationService service = new PriceAggregationService(LOGGER);
        Product product = new Product(123L);

        DataFragment outOfStock = offer("backmarket.fr", "https://example.com/oos", 1.0, 0.0, 2, 10,
                ProductCondition.OCCASION, InStock.OUTOFSTOCK);
        DataFragment inStock = offer("certideal.fr", "https://example.com/ok", 249.0, 0.0, 2, 3,
                ProductCondition.OCCASION, InStock.INSTOCK);

        service.onDataFragment(outOfStock, product, new VerticalConfig());
        assertThat(product.getPrice().getOffers()).isEmpty();
        assertThat(product.getPrice().getMinPrice()).isNull();
        assertThat(product.getPrice().getOccasionPricehistory()).isEmpty();

        service.onDataFragment(inStock, product, new VerticalConfig());

        assertThat(product.getOffersCount()).isEqualTo(1);
        assertThat(product.bestPrice().getDatasourceName()).isEqualTo("certideal.fr");
        assertThat(product.bestPrice().getPrice()).isEqualTo(249.0);
    }

    /**
     * Verifies fragments without explicit stock information are still aggregated.
     *
     * @throws Exception when aggregation fails
     */
    @Test
    void keepsOffersWithUnknownStock() throws Exception {
        PriceAggregationService service = new PriceAggregationService(LOGGER);
        Product product = new Product(123L);

        service.onDataFragment(offer("merchant-a", "https://example.com/a", 100.0, 0.0, 2, 4,
                ProductCondition.NEW, null), product, new VerticalConfig());

        assertThat(product.getOffersCount()).isEqualTo(1);
    }

    private static DataFragment offer(String datasource, String url, double priceValue, double shippingCost,
            int shippingTime, int quantityInStock) {
        return offer(datasource, url, priceValue, shippingCost, shippingTime, quantityInStock,
                ProductCondition.NEW, InStock.INSTOCK);
    }

    private static DataFragment offer(String datasource, String url, double priceValue, double shippingCost,
            int shippingTime, int quantityInStock, ProductCondition condition, InStock stock) {
        DataFragment fragment = DataFragment.newOffer(url, datasource);
        fragment.addName("Offer " + datasource);
        fragment.setProductState(condition);
        fragment.setInStock(stock);
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
