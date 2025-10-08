package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.nudgerfrontapi.config.properties.AffiliationPartnersProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AffiliationPartnerServiceTest {

    private static final String BASE_URL = "https://backend.example";
    private static final String PARTNERS_PATH = "/partners";

    private AffiliationPartnerService service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        AffiliationPartnersProperties properties = new AffiliationPartnersProperties();
        properties.setApiBaseUrl(BASE_URL);
        properties.setApiKey("secret");
        properties.setPartnersPath(PARTNERS_PATH);

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new AffiliationPartnerService(builder, properties);
    }

    @Test
    void refreshPartnersLoadsLatestSnapshot() {
        mockServer.expect(requestTo(BASE_URL + PARTNERS_PATH))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(UrlConstants.APIKEY_PARAMETER, "secret"))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "p1",
                            "name": "Partner 1",
                            "logoUrl": "https://logo.example/p1.svg",
                            "affiliationLink": "https://aff.example/p1",
                            "portalUrl": "https://portal.example/p1",
                            "countryCodes": ["FR", "DE"]
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        service.refreshPartners();

        List<AffiliationPartner> partners = service.getPartners();
        assertThat(partners).hasSize(1);
        AffiliationPartner partner = partners.getFirst();
        assertThat(partner.getId()).isEqualTo("p1");
        assertThat(partner.getName()).isEqualTo("Partner 1");
        mockServer.verify();
    }

    @Test
    void refreshPartnersPreservesPreviousSnapshotOnFailure() {
        mockServer.expect(requestTo(BASE_URL + PARTNERS_PATH))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"id": "p1"},
                          {"id": "p2"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        service.refreshPartners();
        mockServer.verify();

        List<AffiliationPartner> initialPartners = service.getPartners();

        mockServer.reset();
        mockServer.expect(requestTo(BASE_URL + PARTNERS_PATH))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        service.refreshPartners();

        assertThat(service.getPartners()).isSameAs(initialPartners);
        mockServer.verify();
    }
}
