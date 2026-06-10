package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import tools.jackson.databind.ObjectMapper;

/**
 * Tests for {@link CjFeedService} normalized affiliation mapping.
 */
class CjFeedServiceTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseAdvertisersShouldReturnMappedPrograms() throws Exception
    {
        CjFeedService service = buildService();

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <cj-api>
                  <advertisers total="1">
                    <advertiser>
                      <advertiser-id>12345</advertiser-id>
                      <advertiser-name>Electronics Store</advertiser-name>
                      <program-url>https://electronics.example.com</program-url>
                      <account-status>active</account-status>
                      <relationship-status>joined</relationship-status>
                      <language>FR</language>
                      <primary-category id="cat1">
                        <parent>Electronics</parent>
                        <child>Phones</child>
                      </primary-category>
                    </advertiser>
                  </advertisers>
                </cj-api>
                """;

        Collection<AffiliationProgram> programs = service.parseAdvertisers(List.of(parseXml(xml)));

        assertThat(programs).hasSize(1);
        AffiliationProgram program = programs.iterator().next();
        assertThat(program.getProviderName()).isEqualTo("CJ");
        assertThat(program.getProgramId()).isEqualTo("12345");
        assertThat(program.getAdvertiserName()).isEqualTo("Electronics Store");
        assertThat(program.getStatus()).isEqualTo("active");
        assertThat(program.getPortalUrl()).isEqualTo("https://electronics.example.com");
        assertThat(program.getCountryCodes()).containsExactly("FR");
        assertThat(program.getCategories()).containsExactlyInAnyOrder("Electronics", "Phones");
    }

    @Test
    void parseAdvertisersShouldHandleEmptyResponse() throws Exception
    {
        CjFeedService service = buildService();

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <cj-api>
                  <advertisers total="0"/>
                </cj-api>
                """;

        Collection<AffiliationProgram> programs = service.parseAdvertisers(List.of(parseXml(xml)));

        assertThat(programs).isEmpty();
    }

    @Test
    void parseLinkSearchPromotionsShouldReturnMappedCoupons() throws Exception
    {
        CjFeedService service = buildService();

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <cj-api>
                  <links total="1">
                    <link>
                      <link-id>987654</link-id>
                      <advertiser-id>12345</advertiser-id>
                      <advertiser-name>Electronics Store</advertiser-name>
                      <link-name>Save 10% on all orders</link-name>
                      <link-type>Text</link-type>
                      <promotion-type>coupon</promotion-type>
                      <promotion-start-date>01/01/2024</promotion-start-date>
                      <promotion-end-date>12/31/2024</promotion-end-date>
                      <coupon-code>SAVE10</coupon-code>
                      <description>Use coupon SAVE10 for 10% off</description>
                      <link-code-html>&lt;a href="https://www.dpbolvw.net/click-530033-987654"&gt;Shop Now&lt;/a&gt;</link-code-html>
                      <language>FR</language>
                    </link>
                  </links>
                </cj-api>
                """;

        Collection<AffiliationPromotion> promotions = service.parseLinkSearchPromotions(List.of(parseXml(xml)));

        assertThat(promotions).hasSize(1);
        AffiliationPromotion promotion = promotions.iterator().next();
        assertThat(promotion.getProviderName()).isEqualTo("CJ");
        assertThat(promotion.getProgramId()).isEqualTo("12345");
        assertThat(promotion.getAdvertiserName()).isEqualTo("Electronics Store");
        assertThat(promotion.getTitle()).isEqualTo("Save 10% on all orders");
        assertThat(promotion.getVoucherCode()).isEqualTo("SAVE10");
        assertThat(promotion.getDiscountType()).isEqualTo("coupon");
        assertThat(promotion.getDescription()).isEqualTo("Use coupon SAVE10 for 10% off");
        assertThat(promotion.getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(promotion.getEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(promotion.getTrackingUrl()).isEqualTo("https://www.dpbolvw.net/click-530033-987654");
        assertThat(promotion.getCountryCodes()).containsExactly("FR");
    }

    @Test
    void parseLinkSearchPromotionsShouldHandleEmptyResponse() throws Exception
    {
        CjFeedService service = buildService();

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <cj-api>
                  <links total="0"/>
                </cj-api>
                """;

        Collection<AffiliationPromotion> promotions = service.parseLinkSearchPromotions(List.of(parseXml(xml)));

        assertThat(promotions).isEmpty();
    }

    @Test
    void parseCommissionsShouldReturnMappedTransactions() throws Exception
    {
        CjFeedService service = buildService();

        String json = """
                {
                  "data": {
                    "publisherCommissions": {
                      "count": 1,
                      "payloadComplete": true,
                      "records": [
                        {
                          "id": "txn-001",
                          "advertiserId": "12345",
                          "advertiserName": "Electronics Store",
                          "postingDate": "2024-03-15T10:00:00Z",
                          "pubCommissionAmountUsd": "4.99",
                          "pubCommissionCurrencyCode": "USD",
                          "saleAmountPubCurrency": "49.99",
                          "orderId": "ORDER-XYZ",
                          "actionStatus": "approved",
                          "publisherSiteSubId": "subid-abc",
                          "items": [
                            {
                              "productId": "PROD-123",
                              "quantity": 1,
                              "totalCommissionPubCurrency": "4.99"
                            }
                          ]
                        }
                      ],
                      "maxCommissionId": "txn-001"
                    }
                  }
                }
                """;

        Collection<AffiliationTransaction> transactions = service.parseCommissions(objectMapper.readTree(json));

        assertThat(transactions).hasSize(1);
        AffiliationTransaction tx = transactions.iterator().next();
        assertThat(tx.getProviderName()).isEqualTo("CJ");
        assertThat(tx.getTransactionId()).isEqualTo("txn-001");
        assertThat(tx.getProgramId()).isEqualTo("12345");
        assertThat(tx.getTransactionDate()).isEqualTo(Instant.parse("2024-03-15T10:00:00Z"));
        assertThat(tx.getStatus()).isEqualTo("approved");
        assertThat(tx.getCommissionAmount()).isEqualByComparingTo("4.99");
        assertThat(tx.getSaleAmount()).isEqualByComparingTo("49.99");
        assertThat(tx.getCurrency()).isEqualTo("USD");
        assertThat(tx.getSubId()).isEqualTo("subid-abc");
        assertThat(tx.getProductId()).isEqualTo("PROD-123");
    }

    @Test
    void parseCommissionsShouldHandleEmptyPayload() throws Exception
    {
        CjFeedService service = buildService();

        Collection<AffiliationTransaction> transactions = service.parseCommissions(
                objectMapper.readTree("{\"data\":{\"publisherCommissions\":{\"count\":0,\"records\":[]}}}"));

        assertThat(transactions).isEmpty();
    }

    @Test
    void extractHrefShouldParseDoubleQuotedHref() throws Exception
    {
        CjFeedService service = buildService();

        String href = service.extractHref("<a href=\"https://www.dpbolvw.net/click-530033-12345\">Shop Now</a>");

        assertThat(href).isEqualTo("https://www.dpbolvw.net/click-530033-12345");
    }

    @Test
    void extractHrefShouldParseSingleQuotedHref() throws Exception
    {
        CjFeedService service = buildService();

        String href = service.extractHref("<a href='https://www.dpbolvw.net/click-530033-12345'>Shop Now</a>");

        assertThat(href).isEqualTo("https://www.dpbolvw.net/click-530033-12345");
    }

    @Test
    void extractHrefShouldReturnNullOnBlankInput() throws Exception
    {
        CjFeedService service = buildService();

        assertThat(service.extractHref(null)).isNull();
        assertThat(service.extractHref("")).isNull();
        assertThat(service.extractHref("no href here")).isNull();
    }

    @Test
    void parseMmDdYyyyDateShouldWork() throws Exception
    {
        CjFeedService service = buildService();

        assertThat(service.parseMmDdYyyy("03/15/2024")).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(service.parseMmDdYyyy("12/31/2024")).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(service.parseMmDdYyyy(null)).isNull();
        assertThat(service.parseMmDdYyyy("")).isNull();
        assertThat(service.parseMmDdYyyy("not-a-date")).isNull();
    }

    // ------------------------------------------------------------------ //
    // Test helpers
    // ------------------------------------------------------------------ //

    private CjFeedService buildService()
    {
        FeedConfiguration.CjConfig cjConfig = new FeedConfiguration.CjConfig();
        cjConfig.setEnabled(true);
        FeedConfiguration feedConfig = new FeedConfiguration();
        feedConfig.setCj(cjConfig);

        return new CjFeedService(
                feedConfig,
                mock(RemoteFileCachingService.class),
                mock(DataSourceConfigService.class),
                mock(SerialisationService.class),
                "test-token",
                "pub123",
                "site456");
    }

    private Document parseXml(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}
