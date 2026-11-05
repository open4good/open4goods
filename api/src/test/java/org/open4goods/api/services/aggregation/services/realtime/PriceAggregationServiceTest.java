package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

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

    /**
     * Verifies a merchant re-sending an offer at the same price refreshes its timestamp (GOU-1699). Offers are
     * kept in a {@code HashSet} and {@code Price} defines equality on price and currency only, so the stored and
     * the incoming offer used to tie and the stored one could survive, expiring 2 days after it was first seen
     * although the merchant kept listing it. Repeated because the tie depended on hash-set iteration order.
     *
     * @throws Exception when aggregation fails
     */
    @Test
    void sameDatasourceSamePriceKeepsMostRecentOffer() throws Exception {
        PriceAggregationService service = new PriceAggregationService(LOGGER);
        long now = System.currentTimeMillis();
        long seenBefore = now - Duration.ofHours(36).toMillis();

        for (int i = 0; i < 50; i++) {
            Product product = new Product(123L);
            DataFragment first = offer("darty.com", "https://example.com/darty", 849.99, 0.0, 2, 4);
            first.setLastIndexationDate(seenBefore);
            service.onDataFragment(first, product, new VerticalConfig());

            DataFragment again = offer("darty.com", "https://example.com/darty", 849.99, 0.0, 2, 4);
            again.setLastIndexationDate(now);
            service.onDataFragment(again, product, new VerticalConfig());

            assertThat(product.getPrice().getOffers()).singleElement()
                    .extracting(AggregatedPrice::getTimeStamp).isEqualTo(now);
            assertThat(product.getOffersCount()).isEqualTo(1);
        }
    }

    /**
     * Verifies the date an offer was last seen survives the expiry of the offer, per condition, and never moves
     * backwards (GOU-1699).
     *
     * @throws Exception when aggregation fails
     */
    @Test
    void keepsLastOfferSeenAfterOffersExpire() throws Exception {
        PriceAggregationService service = new PriceAggregationService(LOGGER);
        Product product = new Product(123L);
        long seen = System.currentTimeMillis() - Duration.ofDays(3).toMillis();

        DataFragment darty = offer("darty.com", "https://example.com/darty", 849.99, 0.0, 2, 4);
        darty.setLastIndexationDate(seen);
        product.getPrice().getOffers().add(new AggregatedPrice(darty));
        product.getPrice().getLastOfferSeen().put(ProductCondition.NEW, seen - 1000);
        product.getPrice().getLastOfferSeen().put(ProductCondition.OCCASION, seen + 1000);

        // Any later fragment of the product triggers the re-aggregation that drops the expired offer
        service.onDataFragment(offer("fnac.com", "https://example.com/fnac", 0.0, 0.0, 2, 4), product,
                new VerticalConfig());

        assertThat(product.getPrice().getOffers()).isEmpty();
        assertThat(product.getOffersCount()).isZero();
        assertThat(product.getPrice().getLastOfferSeen())
                .containsEntry(ProductCondition.NEW, seen)
                .containsEntry(ProductCondition.OCCASION, seen + 1000);
    }

    /**
     * Verifies a lower price from the same merchant and condition still wins over a more recent higher one.
     *
     * @throws Exception when aggregation fails
     */
    @Test
    void sameDatasourceKeepsLowestPrice() throws Exception {
        PriceAggregationService service = new PriceAggregationService(LOGGER);
        Product product = new Product(123L);

        service.onDataFragment(offer("darty.com", "https://example.com/a", 799.0, 0.0, 2, 4), product,
                new VerticalConfig());
        service.onDataFragment(offer("darty.com", "https://example.com/b", 849.99, 0.0, 2, 4), product,
                new VerticalConfig());

        assertThat(product.getPrice().getOffers()).singleElement()
                .extracting(AggregatedPrice::getPrice).isEqualTo(799.0);
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
