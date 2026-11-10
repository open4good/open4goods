package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.price.DatasourceOfferActivity;
import org.open4goods.model.price.DatasourceOfferActivityReport;
import org.open4goods.nudgerfrontapi.config.properties.MerchantDomainsProperties;
import org.open4goods.nudgerfrontapi.config.properties.OfferFreshnessProperties;
import org.open4goods.nudgerfrontapi.model.stats.ActiveMerchantDomains;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests {@link MerchantDomainsService}: domain normalisation and the active merchant filter (GOU-2037).
 */
class MerchantDomainsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final long TRACKED_SINCE = NOW.minus(Duration.ofDays(10)).toEpochMilli();

    private MerchantDomainsProperties properties;
    private OfferFreshnessService freshness;
    private AffiliationPartnerService partnerService;
    private MerchantDomainsService service;

    @BeforeEach
    void setUp() {
        properties = new MerchantDomainsProperties();
        MerchantDomainsProperties.DatasourceDomain carrefour = new MerchantDomainsProperties.DatasourceDomain();
        carrefour.setDatasource("Carrefour - marketplace");
        carrefour.setDomain("carrefour.fr");
        properties.setDatasourceDomains(List.of(carrefour));
        OfferFreshnessProperties freshnessProperties = new OfferFreshnessProperties();
        freshnessProperties.setMaxAge(Duration.ofDays(2));
        freshness = new OfferFreshnessService(freshnessProperties, CLOCK);
        partnerService = mock(AffiliationPartnerService.class);
        service = new MerchantDomainsService(mock(RestClient.class), properties, partnerService, freshness, CLOCK);
    }

    @ParameterizedTest
    @CsvSource({
            "https://www.Darty.com/, darty.com",
            "www.darty.com, darty.com",
            "darty.com., darty.com",
            "http://www.joom.com/fr, joom.com",
            "https://www.mobile24.fr/shop/frontpage.html, mobile24.fr",
            "https://fr.aliexpress.com, aliexpress.com",
            "https://store.acer.com/fr-fr/, acer.com",
            "https://www.e.leclerc, e.leclerc",
            "https://www.bbc.co.uk/news, bbc.co.uk",
            "HTTPS://WWW.FNAC.COM:443/, fnac.com"
    })
    void normalizesToTheRegistrableDomain(String value, String expected) {
        assertThat(MerchantDomainsService.normalizeDomain(value)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "Carrefour - marketplace", "Standard", "192.168.1.10", "localhost", "com"})
    void returnsNullWithoutAPublicDomain(String value) {
        assertThat(MerchantDomainsService.normalizeDomain(value)).isNull();
    }

    @Test
    void domainResolverAppliesTheSameRulesAsTheCount() {
        given(partnerService.getPartners()).willReturn(List.of());
        var resolver = service.domainResolver();

        assertThat(resolver.apply("fr.aliexpress.com")).isEqualTo(new MerchantDomainsService.MerchantDomain("aliexpress.com", false));
        assertThat(resolver.apply("aliexpress.com").domain()).isEqualTo("aliexpress.com");
        assertThat(resolver.apply("awin1.com")).isEqualTo(new MerchantDomainsService.MerchantDomain("awin1.com", true));
        assertThat(resolver.apply("Carrefour - marketplace").domain()).isEqualTo("carrefour.fr");
        assertThat(resolver.apply("Standard").domain()).isNull();
        assertThat(resolver.apply(null).domain()).isNull();
    }

    @Test
    void countsDistinctDomainsOfDatasourcesWithAFreshOffer() {
        List<AffiliationPartner> partners = List.of(
                partner("darty.com", "https://www.darty.com"),
                partner("Darty Marketplace", "http://darty.com/"),
                partner("fnac.com", "https://www.fnac.com/"),
                partner("Vevor FR ", null),
                partner("vevor.fr", "https://vevor.fr/"),
                partner("Carrefour - marketplace", null),
                partner("Standard", null),
                partner("tradetracker.com", "https://tradetracker.com/fr/"),
                partner("Awin promo", "https://www.awin1.com/cread.php?id=1"),
                partner("rakuten.com", "https://fr.shopping.rakuten.com"));

        ActiveMerchantDomains result = service.compute(report(
                fresh("darty.com"),
                fresh("Darty Marketplace"),
                fresh("fnac.com"),
                fresh("vevor.fr"),
                fresh("Carrefour - marketplace"),
                fresh("Standard"),
                fresh("tradetracker.com"),
                fresh("Awin promo"),
                fresh("cdiscount.com"),
                new DatasourceOfferActivity("rakuten.com", NOW.minus(Duration.ofDays(3)).toEpochMilli())),
                partners);

        // www. duplicates and two datasources of one merchant count once
        assertThat(result.domains()).containsEntry("darty.com", List.of("Darty Marketplace", "darty.com"));
        // Name fallback (cdiscount.com is no partner), configured domain (Carrefour), portal URL
        assertThat(result.domains().keySet())
                .containsExactly("carrefour.fr", "cdiscount.com", "darty.com", "fnac.com", "vevor.fr");
        assertThat(result.count()).isEqualTo(5);
        // Affiliation networks are not merchants
        assertThat(result.excluded()).containsOnlyKeys("tradetracker.com", "Awin promo");
        assertThat(result.excluded().get("Awin promo")).startsWith(MerchantDomainsService.REASON_NETWORK);
        // Without domain: not counted, reported
        assertThat(result.unresolved()).containsExactly("Standard");
        // rakuten.com's last offer is expired
        assertThat(result.domains()).doesNotContainKey("rakuten.com");
        assertThat(result.computedAt()).isEqualTo(NOW);
        assertThat(result.trackedSince()).isEqualTo(Instant.ofEpochMilli(TRACKED_SINCE));
        // No partner states its countries: France delivery is unknown
        assertThat(result.franceOnly()).isFalse();
    }

    @Test
    void appliesTheOfferFreshnessBoundary() {
        long maxAge = Duration.ofDays(2).toMillis();
        ActiveMerchantDomains result = service.compute(report(
                new DatasourceOfferActivity("darty.com", NOW.toEpochMilli() - maxAge),
                new DatasourceOfferActivity("fnac.com", NOW.toEpochMilli() - maxAge - 1)), List.of());

        assertThat(result.domains().keySet()).containsExactly("darty.com");
    }

    @Test
    void excludesMerchantsOutsideFranceAndIsFranceOnlyWhenEveryCountryIsKnown() {
        List<AffiliationPartner> partners = List.of(
                partner("darty.com", "https://www.darty.com", "FR"),
                partner("bol.com", "https://www.bol.com", "FR", "BE"),
                partner("Pneus-Online BEfr", "https://www.pneus-online.be", "BE"));

        ActiveMerchantDomains result = service.compute(
                report(fresh("darty.com"), fresh("bol.com"), fresh("Pneus-Online BEfr")), partners);

        assertThat(result.domains().keySet()).containsExactly("bol.com", "darty.com");
        assertThat(result.excluded()).containsOnlyKeys("Pneus-Online BEfr");
        assertThat(result.excluded().get("Pneus-Online BEfr")).startsWith(MerchantDomainsService.REASON_OUTSIDE_FRANCE);
        assertThat(result.franceOnly()).isTrue();
    }

    @Test
    void emptyActivityCountsNothingAndIsNotFranceOnly() {
        ActiveMerchantDomains result = service.compute(report(), null);

        assertThat(result.count()).isZero();
        assertThat(result.franceOnly()).isFalse();
    }

    @Test
    void refreshReadsTheApiAndKeepsThePreviousCountOnFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test")
                .defaultHeader(UrlConstants.APIKEY_PARAMETER, "key");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        given(partnerService.getPartners()).willReturn(List.of(partner("darty.com", "https://www.darty.com")));
        MerchantDomainsService refreshing = new MerchantDomainsService(builder.build(), properties, partnerService,
                freshness, CLOCK);
        server.expect(requestTo("http://api.test/merchants/offer-activity"))
                .andExpect(header(UrlConstants.APIKEY_PARAMETER, "key"))
                .andRespond(withSuccess("{\"trackedSince\":" + TRACKED_SINCE + ",\"datasources\":[{\"datasourceName\":"
                        + "\"darty.com\",\"lastOfferSeen\":" + NOW.toEpochMilli() + "}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://api.test/merchants/offer-activity")).andRespond(withServerError());

        assertThat(refreshing.current()).isNull();
        refreshing.refresh();
        ActiveMerchantDomains first = refreshing.current();
        refreshing.refresh();

        server.verify();
        assertThat(first.domains().keySet()).containsExactly("darty.com");
        assertThat(refreshing.current()).isSameAs(first);
    }

    private static DatasourceOfferActivity fresh(String name) {
        return new DatasourceOfferActivity(name, NOW.minus(Duration.ofHours(5)).toEpochMilli());
    }

    private static DatasourceOfferActivityReport report(DatasourceOfferActivity... activities) {
        return new DatasourceOfferActivityReport(TRACKED_SINCE, List.of(activities));
    }

    private static AffiliationPartner partner(String name, String portalUrl, String... countries) {
        AffiliationPartner partner = new AffiliationPartner();
        partner.setId(name);
        partner.setName(name);
        partner.setPortalUrl(portalUrl);
        partner.setCountryCodes(countries.length == 0 ? null : Set.of(countries));
        return partner;
    }
}
