package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.InStock;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests stock parsing of Awin rows in {@link FeedIndexingWorker}.
 */
class FeedIndexingWorkerStockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedIndexingWorkerStockTest.class);

    /**
     * Back Market publishes {@code search_price=1} on out-of-stock rows, with an
     * empty {@code in_stock} and a non-zero {@code stock_quantity}. The row must be
     * flagged out of stock so price aggregation ignores the placeholder price.
     *
     * @throws Exception when parsing fails
     */
    @Test
    void flagsBackMarketOutOfStockRow() throws Exception {
        DataFragment fragment = worker().parseCsvLine(awinConfig(), backMarketRow("out of stock"), "backmarket.fr",
                LOGGER, "https://example.com/feed.csv.gz");

        assertThat(fragment.getInStock()).isEqualTo(InStock.OUTOFSTOCK);
        assertThat(fragment.getQuantityInStock()).isEqualTo(10);
        assertThat(fragment.getProductState()).isEqualTo(ProductCondition.OCCASION);
        assertThat(fragment.getPrice().getPrice()).isEqualTo(1.0);
    }

    /**
     * Verifies an in-stock Back Market row stays in stock.
     *
     * @throws Exception when parsing fails
     */
    @Test
    void keepsBackMarketInStockRow() throws Exception {
        DataFragment fragment = worker().parseCsvLine(awinConfig(), backMarketRow("in stock"), "backmarket.fr",
                LOGGER, "https://example.com/feed.csv.gz");

        assertThat(fragment.getInStock()).isEqualTo(InStock.INSTOCK);
    }

    private static FeedIndexingWorker worker() {
        return new FeedIndexingWorker(mock(FeedIndexingService.class), mock(DataFragmentCompletionService.class),
                mock(DataFragmentIndexer.class), 0, "target/logs", mock(RemoteFileCachingService.class));
    }

    /**
     * Mirrors the stock, price and condition mapping of the Awin {@code defaultCsvProperties}.
     */
    private static DataSourceProperties awinConfig() {
        CsvDataSourceProperties csv = new CsvDataSourceProperties();
        csv.setInStock(new LinkedHashSet<>(List.of("in_stock", "stock_status", "availability")));
        csv.setPrice(new LinkedHashSet<>(List.of("search_price")));
        csv.setProductState(new LinkedHashSet<>(List.of("condition")));
        csv.setCurrency(Currency.EUR);
        DataSourceProperties config = new DataSourceProperties();
        config.setCsvDatasource(csv);
        return config;
    }

    private static Map<String, String> backMarketRow(String stockStatus) {
        Map<String, String> row = new HashMap<>();
        row.put("aw_deep_link", "https://www.awin1.com/pclick.php?p=1&a=1&m=25197");
        row.put("product_name", "Galaxy A70 128 Go - Noir - Débloqué");
        row.put("brand_name", "Samsung");
        row.put("product_GTIN", "8801643871277");
        row.put("currency", "EUR");
        row.put("search_price", "1");
        row.put("rrp_price", "620");
        row.put("condition", "used");
        row.put("in_stock", "");
        row.put("stock_status", stockStatus);
        row.put("stock_quantity", "10");
        return row;
    }
}
