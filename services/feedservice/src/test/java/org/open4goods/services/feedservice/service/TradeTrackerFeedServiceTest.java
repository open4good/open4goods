package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.w3c.dom.Document;

/**
 * Tests for {@link TradeTrackerFeedService} normalized SOAP mapping.
 */
class TradeTrackerFeedServiceTest
{
    @Test
    void parseTradeTrackerProgramsShouldReturnMappedCampaigns() throws Exception
    {
        TradeTrackerFeedService service = buildService();

        Collection<AffiliationProgram> mapped = service.parseTradeTrackerPrograms(service.parseXml("""
                <Envelope>
                  <Body>
                    <getCampaignsResponse>
                      <Campaign>
                        <ID>1234</ID>
                        <name>TradeTracker Merchant</name>
                        <URL>https://merchant.example</URL>
                        <info>
                          <assignmentStatus>accepted</assignmentStatus>
                          <imageURL>https://merchant.example/logo.png</imageURL>
                          <trackingURL>https://tc.tradetracker.net/?c=1234</trackingURL>
                          <campaignDescription>Campaign description</campaignDescription>
                          <clickToConversion>P30D</clickToConversion>
                          <category><name>Shopping</name></category>
                          <commission><click>0.01</click><lead>1.50</lead><sale>5.00</sale></commission>
                        </info>
                      </Campaign>
                    </getCampaignsResponse>
                  </Body>
                </Envelope>
                """));

        assertThat(mapped).hasSize(1);
        AffiliationProgram program = mapped.iterator().next();
        assertThat(program.getProviderName()).isEqualTo("TradeTracker");
        assertThat(program.getProgramId()).isEqualTo("1234");
        assertThat(program.getAdvertiserName()).isEqualTo("TradeTracker Merchant");
        assertThat(program.getStatus()).isEqualTo("accepted");
        assertThat(program.getPortalUrl()).isEqualTo("https://merchant.example");
        assertThat(program.getLogoUrl()).isEqualTo("https://merchant.example/logo.png");
        assertThat(program.getTrackingUrl()).isEqualTo("https://tc.tradetracker.net/?c=1234");
        assertThat(program.getDescription()).isEqualTo("Campaign description");
        assertThat(program.getCookieDurationDays()).isEqualTo(30);
        assertThat(program.getCategories()).containsExactly("Shopping");
        assertThat(program.getClickCommission()).isEqualByComparingTo("0.01");
        assertThat(program.getLeadCommission()).isEqualByComparingTo("1.50");
        assertThat(program.getSaleCommissionPercent()).isEqualByComparingTo("5.00");
    }

    @Test
    void parseTradeTrackerPromotionsShouldReturnMappedVoucherItems() throws Exception
    {
        TradeTrackerFeedService service = buildService();
        Document root = service.parseXml("""
                <Envelope>
                  <Body>
                    <getMaterialIncentiveVoucherItemsResponse>
                      <MaterialItem>
                        <ID>99</ID>
                        <campaign>
                          <ID>1234</ID>
                          <name>Merchant</name>
                          <URL>https://merchant.example</URL>
                          <info><trackingURL>https://tc.tradetracker.net/?c=1234</trackingURL></info>
                        </campaign>
                        <name>Voucher title</name>
                        <description>Voucher description</description>
                        <conditions>Terms apply</conditions>
                        <validFromDate>2026-01-01</validFromDate>
                        <validToDate>2026-12-31</validToDate>
                        <discountVariable>20</discountVariable>
                        <voucherCode>PROMO20</voucherCode>
                        <code>html code</code>
                      </MaterialItem>
                    </getMaterialIncentiveVoucherItemsResponse>
                  </Body>
                </Envelope>
                """);

        Collection<AffiliationPromotion> mapped = service.parseTradeTrackerPromotions(root);

        assertThat(mapped).hasSize(1);
        AffiliationPromotion promotion = mapped.iterator().next();
        assertThat(promotion.getProviderName()).isEqualTo("TradeTracker");
        assertThat(promotion.getProgramId()).isEqualTo("1234");
        assertThat(promotion.getAdvertiserName()).isEqualTo("Merchant");
        assertThat(promotion.getTitle()).isEqualTo("Voucher title");
        assertThat(promotion.getDescription()).isEqualTo("Voucher description");
        assertThat(promotion.getVoucherCode()).isEqualTo("PROMO20");
        assertThat(promotion.getDiscountType()).isEqualTo("percent");
        assertThat(promotion.getDiscountValue()).isEqualByComparingTo("20");
        assertThat(promotion.getStartDate().toString()).isEqualTo("2026-01-01");
        assertThat(promotion.getEndDate().toString()).isEqualTo("2026-12-31");
        assertThat(promotion.getLandingUrl()).isEqualTo("https://merchant.example");
        assertThat(promotion.getTrackingUrl()).isEqualTo("https://tc.tradetracker.net/?c=1234");
        assertThat(promotion.getConditions()).isEqualTo("Terms apply");
    }

    @Test
    void parseTradeTrackerTransactionsShouldReturnMappedConversions() throws Exception
    {
        TradeTrackerFeedService service = buildService();

        Collection<AffiliationTransaction> mapped = service.parseTradeTrackerTransactions(service.parseXml("""
                <Envelope>
                  <Body>
                    <getConversionTransactionsResponse>
                      <ConversionTransaction>
                        <ID>tx-1</ID>
                        <campaign><ID>1234</ID></campaign>
                        <transactionStatus>accepted</transactionStatus>
                        <currency>EUR</currency>
                        <orderAmount>120.50</orderAmount>
                        <commission>6.03</commission>
                        <registrationDate>2026-05-29T12:00:00Z</registrationDate>
                        <reference>sub-1</reference>
                      </ConversionTransaction>
                    </getConversionTransactionsResponse>
                  </Body>
                </Envelope>
                """));

        assertThat(mapped).hasSize(1);
        AffiliationTransaction transaction = mapped.iterator().next();
        assertThat(transaction.getProviderName()).isEqualTo("TradeTracker");
        assertThat(transaction.getTransactionId()).isEqualTo("tx-1");
        assertThat(transaction.getProgramId()).isEqualTo("1234");
        assertThat(transaction.getTransactionDate()).isEqualTo(Instant.parse("2026-05-29T12:00:00Z"));
        assertThat(transaction.getStatus()).isEqualTo("accepted");
        assertThat(transaction.getSaleAmount()).isEqualByComparingTo("120.50");
        assertThat(transaction.getCommissionAmount()).isEqualByComparingTo("6.03");
        assertThat(transaction.getCurrency()).isEqualTo("EUR");
        assertThat(transaction.getSubId()).isEqualTo("sub-1");
    }

    private TradeTrackerFeedService buildService()
    {
        FeedConfiguration config = new FeedConfiguration();
        config.setDefaultCsvProperties(new CsvDataSourceProperties());
        config.getTradetracker().setEnabled(true);
        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());
        return new TradeTrackerFeedService(
                config,
                mock(RemoteFileCachingService.class),
                dataSourceConfigService,
                new SerialisationService(),
                "123456",
                "passphrase");
    }
}
