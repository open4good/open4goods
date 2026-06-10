package org.open4goods.crawler.services.fetching;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.InStock;
import org.open4goods.model.product.ProductCondition;

/**
 * Tests for CSV data-fragment parsing helpers.
 */
class CsvIndexationWorkerTest
{
    @Test
    void keepsAffiliatedUrlWhenUrlAndAffiliatedUrlShareTheSameColumn() throws Exception
    {
        CsvIndexationWorker worker = new CsvIndexationWorker(null, null, null, null, null, 0, null, null);
        DataSourceProperties datasource = new DataSourceProperties();
        CsvDataSourceProperties csv = new CsvDataSourceProperties();
        csv.setUrl("urlficheproduit");
        csv.setAffiliatedUrl("urlficheproduit");
        csv.setExtractUrlFromParam("url");
        csv.setAffiliatedUrlReplacementTokens(Map.of("ID_COMPTEUR", "23243634"));
        datasource.setCsvDatasource(csv);

        Map<String, String> row = new LinkedHashMap<>();
        row.put("urlficheproduit",
                "https://track.effiliation.com/servlet/effi.redir?id_compteur=ID_COMPTEUR&url="
                        + "https%3A%2F%2Ffr.shopping.rakuten.com%2Fmfp%2F13356001%2Fapple-ipad-a16-wi-fi%3Fpid%3D13401260207");

        DataFragment fragment = new DataFragment();
        invoke(worker, "setAffiliatedUrl", fragment, row, datasource);
        invoke(worker, "setUrl", fragment, row, datasource);
        invoke(worker, "enforceAffiliatedUrl", fragment);

        assertThat(fragment.getAffiliatedUrl())
                .isEqualTo("https://track.effiliation.com/servlet/effi.redir?id_compteur=23243634&url="
                        + "https%3A%2F%2Ffr.shopping.rakuten.com%2Fmfp%2F13356001%2Fapple-ipad-a16-wi-fi%3Fpid%3D13401260207");
        assertThat(fragment.getUrl()).isEqualTo(fragment.getAffiliatedUrl());
        assertThat(row).doesNotContainKey("urlficheproduit");
    }

    @Test
    void parsesRowWhenConfiguredColumnsDifferOnlyByHeaderDecoration() throws Exception
    {
        CsvIndexationWorker worker = new CsvIndexationWorker(
                null,
                new DataFragmentCompletionService(null),
                null,
                null,
                null,
                0,
                null,
                null);
        DataSourceProperties datasource = new DataSourceProperties();
        datasource.setName("MDA Electromenager");
        datasource.setLanguage("fr");
        datasource.setValidationFields(Set.of("price", "names"));

        CsvDataSourceProperties csv = new CsvDataSourceProperties();
        csv.setUrl("product_url");
        csv.setName("name");
        csv.setPrice(Set.of("sale_price"));
        csv.setCurrency(Currency.EUR);
        datasource.setCsvDatasource(csv);

        Map<String, String> row = new LinkedHashMap<>();
        row.put("\ufeff\"Product URL\"", "https://www.mda-electromenager.com/fr/a/tv-led-50-thomson-50ua5s13");
        row.put("\"Name\"", "TV LED 50 Thomson");
        row.put("\"Sale Price\"", "399,99");

        DataFragment fragment = invokeParseCsvLine(worker, row, datasource);

        assertThat(fragment.getUrl()).isEqualTo("https://www.mda-electromenager.com/fr/a/tv-led-50-thomson-50ua5s13");
        assertThat(fragment.getNames()).contains("TV LED 50 Thomson");
        assertThat(fragment.getPrice().getPrice()).isEqualTo(399.99);
    }

    @Test
    void parsesPriceFromDefaultColumnFallbackWhenDatasourceMappingMissesHeader() throws Exception
    {
        CsvIndexationWorker worker = new CsvIndexationWorker(
                null,
                new DataFragmentCompletionService(null),
                null,
                null,
                null,
                0,
                null,
                null);
        DataSourceProperties datasource = new DataSourceProperties();
        datasource.setName("Kwanko Merchant");
        datasource.setLanguage("fr");
        datasource.setValidationFields(Set.of("price", "names"));

        CsvDataSourceProperties csv = new CsvDataSourceProperties();
        csv.setUrl("product page URL");
        csv.setName("name of the product");
        csv.setPrice(Set.of("price_vat_inc", "price", "product_price", "sale_price"));
        csv.setCurrency(Currency.EUR);
        datasource.setCsvDatasource(csv);

        Map<String, String> row = new LinkedHashMap<>();
        row.put("product page URL", "https://example.com/product/1");
        row.put("name of the product", "Example product");
        row.put("current price", "149,90");
        row.put("condition", "Très bon état");
        row.put("stock indicator", "In stock");
        row.put("stock quantity", "12+");
        row.put("shipping costs", "4,99 EUR");
        row.put("Delais de livraison", "Livraison sous 3 à 5 jours");
        row.put("EAN or ISBN", "1234567890123");
        row.put("manufacturer reference", "ABC-1234");
        row.put("sku", "SKU-1");
        row.put("brand", "Example Brand");

        DataFragment fragment = invokeParseCsvLine(worker, row, datasource);

        assertThat(fragment.getPrice().getPrice()).isEqualTo(149.90);
        assertThat(fragment.getProductState()).isEqualTo(ProductCondition.OCCASION);
        assertThat(fragment.getInStock()).isEqualTo(InStock.INSTOCK);
        assertThat(fragment.getQuantityInStock()).isEqualTo(12);
        assertThat(fragment.getShippingCost()).isEqualTo(4.99);
        assertThat(fragment.getShippingTime()).isEqualTo(5);
        assertThat(fragment.getReferentielAttributes()).containsEntry(ReferentielKey.GTIN, "1234567890123");
        assertThat(fragment.getReferentielAttributes()).containsEntry(ReferentielKey.BRAND, "EXAMPLE BRAND");
        assertThat(fragment.getReferentielAttributes()).containsEntry(ReferentielKey.MODEL, "ABC-1234");
        assertThat(fragment.getExternalIds().getSku()).contains("SKU-1");
        assertThat(fragment.getExternalIds().getMpn()).contains("ABC-1234");
    }

    @Test
    void parsesMdaEffiliationRowUsingBroadHeaderFallbacks() throws Exception
    {
        CsvIndexationWorker worker = new CsvIndexationWorker(
                null,
                new DataFragmentCompletionService(null),
                null,
                null,
                null,
                0,
                null,
                null);
        DataSourceProperties datasource = new DataSourceProperties();
        datasource.setName("MDA Electromenager");
        datasource.setLanguage("fr");
        datasource.setValidationFields(Set.of("price", "names", "GTIN"));

        CsvDataSourceProperties csv = new CsvDataSourceProperties();
        csv.setUrl("product_url");
        csv.setAffiliatedUrl("tracking_url");
        csv.setName("product_name");
        csv.setPrice(Set.of("price_vat_inc", "price", "product_price", "sale_price"));
        csv.setImage(Set.of("image_url"));
        csv.setCurrency(Currency.EUR);
        datasource.setCsvDatasource(csv);

        Map<String, String> row = new LinkedHashMap<>();
        row.put("EAN or ISBN", "0010942218654");
        row.put("name of the product", "Cafetière KRUPS KM468910");
        row.put("internal reference", "KM468910");
        row.put("current price", "134.0");
        row.put("product category", "Cafetière à filtre");
        row.put("product page URL", "https://qby.mda-electromenager.com/?P5122F758B41F15S1UC41363f044849V4");
        row.put("big image", "https://www.mda-electromenager.com/media/cache/product.jpg");
        row.put("manufacturer reference", "0010942218654");
        row.put("brand", "krups");
        row.put("description", "Arrêt automatique : Oui - Couleur : Noir");
        row.put("product availability", "in stock");
        row.put("condition", "neuf");
        row.put("shipping costs", "4.90");
        row.put("Delais de livraison", "1 à 2 jours");
        row.put("sku", "150527000067747");

        DataFragment fragment = invokeParseCsvLine(worker, row, datasource);

        assertThat(fragment.getUrl()).isEqualTo("https://qby.mda-electromenager.com/?P5122F758B41F15S1UC41363f044849V4");
        assertThat(fragment.getAffiliatedUrl()).isEqualTo(fragment.getUrl());
        assertThat(fragment.getNames()).contains("Cafetière KRUPS KM468910");
        assertThat(fragment.getPrice().getPrice()).isEqualTo(134.0);
        assertThat(fragment.getResources()).extracting("url")
                .contains("https://www.mda-electromenager.com/media/cache/product.jpg");
        assertThat(fragment.getReferentielAttributes()).containsEntry(ReferentielKey.GTIN, "0010942218654");
        assertThat(fragment.getReferentielAttributes()).containsEntry(ReferentielKey.BRAND, "KRUPS");
        assertThat(fragment.getExternalIds().getSku()).contains("150527000067747");
    }

    @Test
    void parsesFrenchMerchantHeadersUsingGlobalFallbacks() throws Exception
    {
        CsvIndexationWorker worker = new CsvIndexationWorker(
                null,
                new DataFragmentCompletionService(null),
                null,
                null,
                null,
                0,
                null,
                null);
        DataSourceProperties datasource = new DataSourceProperties();
        datasource.setName("centralebrico.fr");
        datasource.setLanguage("fr");
        datasource.setValidationFields(Set.of("price", "names", "GTIN"));

        CsvDataSourceProperties csv = new CsvDataSourceProperties();
        csv.setPrice(Set.of("price_vat_inc", "price", "product_price", "sale_price"));
        csv.setCurrency(Currency.EUR);
        datasource.setCsvDatasource(csv);

        Map<String, String> row = new LinkedHashMap<>();
        row.put("URL produit", "https://mqs.centrale-brico.com/?P510CAF58B41F15S1UC41308b030214V4");
        row.put("URL image", "https://www.centrale-brico.com/195963/gardena-regard-d-arrosage-enterre-pre-monte-1-voie.jpg");
        row.put("Marque", "GARDENA");
        row.put("Prix barré TTC", "56.10");
        row.put("Description", "Regard pré-montable V1 GARDENA");
        row.put("Référence fabricant", "GA-125420");
        row.put("Indicateur de stock", "1");
        row.put("Catégorie", "Accueil > Jardin > Arrosage-Pompage");
        row.put("Désignation", "Regard d'arrosage enterré prè-monté 1 voie");
        row.put("Prix actuel TTC", "32.55");
        row.put("Référence interne", "6");
        row.put("EAN13", "4078500125406");

        DataFragment fragment = invokeParseCsvLine(worker, row, datasource);

        assertThat(fragment.getUrl()).isEqualTo("https://mqs.centrale-brico.com/?P510CAF58B41F15S1UC41308b030214V4");
        assertThat(fragment.getAffiliatedUrl()).isEqualTo(fragment.getUrl());
        assertThat(fragment.getNames()).contains("Regard d'arrosage enterré prè-monté 1 voie");
        assertThat(fragment.getPrice().getPrice()).isEqualTo(32.55);
        assertThat(fragment.getReferentielAttributes()).containsEntry(ReferentielKey.GTIN, "4078500125406");
        assertThat(fragment.getReferentielAttributes()).containsEntry(ReferentielKey.BRAND, "GARDENA");
        assertThat(fragment.getReferentielAttributes()).containsEntry(ReferentielKey.MODEL, "GA-125420");
        assertThat(fragment.getExternalIds().getSku()).contains("6");
        assertThat(fragment.getResources()).extracting("url")
                .contains("https://www.centrale-brico.com/195963/gardena-regard-d-arrosage-enterre-pre-monte-1-voie.jpg");
    }

    private static void invoke(CsvIndexationWorker worker, String method, DataFragment fragment) throws Exception
    {
        Method reflected = CsvIndexationWorker.class.getDeclaredMethod(method, DataFragment.class);
        reflected.setAccessible(true);
        reflected.invoke(worker, fragment);
    }

    private static void invoke(
            CsvIndexationWorker worker,
            String method,
            DataFragment fragment,
            Map<String, String> row,
            DataSourceProperties datasource) throws Exception
    {
        Method reflected;
        if ("setAffiliatedUrl".equals(method))
        {
            reflected = CsvIndexationWorker.class.getDeclaredMethod(
                    method,
                    DataFragment.class,
                    Map.class,
                    DataSourceProperties.class,
                    org.slf4j.Logger.class);
        }
        else
        {
            reflected = CsvIndexationWorker.class.getDeclaredMethod(
                    method,
                    DataFragment.class,
                    Map.class,
                    DataSourceProperties.class,
                    org.slf4j.Logger.class,
                    String.class);
        }
        reflected.setAccessible(true);
        if ("setAffiliatedUrl".equals(method))
        {
            reflected.invoke(worker, fragment, row, datasource, org.slf4j.LoggerFactory.getLogger(CsvIndexationWorkerTest.class));
        }
        else
        {
            reflected.invoke(worker, fragment, row, datasource, org.slf4j.LoggerFactory.getLogger(CsvIndexationWorkerTest.class), "test.csv");
        }
    }

    private static DataFragment invokeParseCsvLine(
            CsvIndexationWorker worker,
            Map<String, String> row,
            DataSourceProperties datasource) throws Exception
    {
        Method reflected = CsvIndexationWorker.class.getDeclaredMethod(
                "parseCsvLine",
                DataFragmentWebCrawler.class,
                edu.uci.ics.crawler4j.crawler.CrawlController.class,
                DataSourceProperties.class,
                Map.class,
                String.class,
                org.slf4j.Logger.class,
                String.class);
        reflected.setAccessible(true);
        return (DataFragment) reflected.invoke(
                worker,
                null,
                null,
                datasource,
                row,
                "mda-electromenager",
                org.slf4j.LoggerFactory.getLogger(CsvIndexationWorkerTest.class),
                "classpath:mda.csv");
    }
}
