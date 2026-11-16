package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.nudgerfrontapi.dto.product.ProductSourceDto;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests {@link DatasourceAttributionService}: reading the datasource configurations from the api and describing the
 * sources of a product (GOU-2302).
 */
class DatasourceAttributionServiceTest {

    private static final String CONFIGS = """
            {
              "data-gouv.yml": {
                "name": " Data-Gouv ",
                "portalUrl": "https://www.data.gouv.fr/",
                "license": " Licence Ouverte 2.0 ",
                "licenseUrl": "https://www.etalab.gouv.fr/licence-ouverte-open-licence/",
                "attribution": "Producteurs, via data.gouv.fr",
                "minimumIndexedItems": 23000,
                "logLevel": {"levelInt": 30000, "levelStr": "WARN"}
              },
              "icecat.biz.yml": {
                "name": "icecat.biz",
                "portalUrl": "",
                "license": "",
                "licenseUrl": "javascript:alert(1)",
                "attribution": null
              },
              "amazon.fr.yml": {
                "name": "amazon.fr",
                "portalUrl": "https://www.amazon.fr/b?_encoding=UTF8&tag=nudger-21&node=10737150031",
                "favico": "https://cdn.icon-icons.com/icons2/413/PNG/256/Amazon_41192.png"
              },
              "open-prices.yml": {
                "name": "open-prices",
                "portalUrl": "https://prices.openfoodfacts.org/",
                "licenseUrl": "https://opendatacommons.org/licenses/odbl/1-0/",
                "attribution": "Open Food Facts contributors"
              },
              "unnamed.yml": {
                "license": "Licence Ouverte 2.0"
              }
            }
            """;

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private DatasourceAttributionService service;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("http://api.test").defaultHeader(UrlConstants.APIKEY_PARAMETER, "key");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new DatasourceAttributionService(builder.build());
    }

    @Test
    void describesTheAttributionOfTheSourcesThatStateItAndNothingForTheOthers() {
        server.expect(requestTo("http://api.test/datasources"))
                .andExpect(header(UrlConstants.APIKEY_PARAMETER, "key"))
                .andRespond(withSuccess(CONFIGS, MediaType.APPLICATION_JSON));

        service.refresh();

        server.verify();
        assertThat(service.describe(List.of("icecat.biz", "data-gouv", "unknown.example"))).containsExactly(
                new ProductSourceDto("data-gouv", "https://www.data.gouv.fr/", "Licence Ouverte 2.0",
                        "https://www.etalab.gouv.fr/licence-ouverte-open-licence/", "Producteurs, via data.gouv.fr"),
                new ProductSourceDto("icecat.biz", null, null, null, null),
                new ProductSourceDto("unknown.example", null, null, null, null));
    }

    /**
     * Only a source declaring a licence or an attribution gets its link (GOU-2302): a merchant gets its name only,
     * even when its datasource file holds a portal URL (an affiliation link for {@code amazon.fr.yml}), and no link is
     * ever taken from the affiliation partners.
     */
    @Test
    void onlyTheSourcesDeclaringALicenceOrAnAttributionGetTheirLink() {
        server.expect(requestTo("http://api.test/datasources"))
                .andRespond(withSuccess(CONFIGS, MediaType.APPLICATION_JSON));

        service.refresh();

        assertThat(service.describe(List.of("amazon.fr", "darty.com", "data-gouv", "icecat.biz", "open-prices")))
                .containsExactly(
                        new ProductSourceDto("amazon.fr", null, null, null, null),
                        new ProductSourceDto("darty.com", null, null, null, null),
                        new ProductSourceDto("data-gouv", "https://www.data.gouv.fr/", "Licence Ouverte 2.0",
                                "https://www.etalab.gouv.fr/licence-ouverte-open-licence/",
                                "Producteurs, via data.gouv.fr"),
                        new ProductSourceDto("icecat.biz", null, null, null, null),
                        new ProductSourceDto("open-prices", "https://prices.openfoodfacts.org/", null,
                                "https://opendatacommons.org/licenses/odbl/1-0/", "Open Food Facts contributors"));
    }

    @Test
    void aFailedRefreshKeepsThePreviousSnapshot() {
        server.expect(requestTo("http://api.test/datasources"))
                .andRespond(withSuccess(CONFIGS, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://api.test/datasources")).andRespond(withServerError());

        service.refresh();
        service.refresh();

        server.verify();
        assertThat(service.describe(List.of("data-gouv")).getFirst().license()).isEqualTo("Licence Ouverte 2.0");
    }

    @Test
    void describesWithoutLicenceBeforeTheFirstRefresh() {
        assertThat(service.describe(Arrays.asList("rakuten.com", null, " ", "rakuten.com")))
                .containsExactly(new ProductSourceDto("rakuten.com", null, null, null, null));
        assertThat(service.describe(null)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "javascript:alert(1)", "/relative/path", "ftp://example.org/file", "https://"})
    void onlyAbsoluteHttpUrlsAreKept(String url) {
        assertThat(DatasourceAttributionService.httpUrl(url)).isNull();
    }

    @Test
    void absoluteHttpUrlsAreKeptTrimmed() {
        assertThat(DatasourceAttributionService.httpUrl(" HTTPS://www.data.gouv.fr/x ")).isEqualTo(
                "HTTPS://www.data.gouv.fr/x");
        assertThat(DatasourceAttributionService.httpUrl("http://example.org")).isEqualTo("http://example.org");
    }
}
