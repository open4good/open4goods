package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
import org.open4goods.model.affiliation.AffiliationPartner;
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
              "unnamed.yml": {
                "license": "Licence Ouverte 2.0"
              }
            }
            """;

    private AffiliationPartnerService partnerService;
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private DatasourceAttributionService service;

    @BeforeEach
    void setUp() {
        partnerService = mock(AffiliationPartnerService.class);
        builder = RestClient.builder().baseUrl("http://api.test").defaultHeader(UrlConstants.APIKEY_PARAMETER, "key");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new DatasourceAttributionService(builder.build(), partnerService);
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

    @Test
    void merchantFeedsTakeThePortalOfTheirAffiliationPartner() {
        server.expect(requestTo("http://api.test/datasources"))
                .andRespond(withSuccess(CONFIGS, MediaType.APPLICATION_JSON));
        given(partnerService.getPartners()).willReturn(List.of(
                partner("Darty.com", "https://www.darty.com"),
                partner("data-gouv", "https://partner.example"),
                partner("evil.example", "javascript:alert(1)")));

        service.refresh();

        assertThat(service.describe(List.of("darty.com", "data-gouv", "evil.example")))
                .extracting(ProductSourceDto::name, ProductSourceDto::portalUrl)
                .containsExactly(
                        tuple("darty.com", "https://www.darty.com"),
                        tuple("data-gouv", "https://www.data.gouv.fr/"),
                        tuple("evil.example", null));
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

    private static AffiliationPartner partner(String name, String portalUrl) {
        AffiliationPartner partner = new AffiliationPartner();
        partner.setName(name);
        partner.setPortalUrl(portalUrl);
        return partner;
    }
}
