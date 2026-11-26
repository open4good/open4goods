package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.open4goods.nudgerfrontapi.config.properties.AffiliationClickRefProperties;

/**
 * Unit tests for {@link AffiliationClickRefService}: one URL shape per network, switched on and off.
 */
class AffiliationClickRefServiceTest {

    private static final String ID = "0b6e7a4c-3f1d-4e8a-9c2b-5d7f1a2e3b4c";

    private static AffiliationClickRefService allEnabled() {
        AffiliationClickRefProperties properties = new AffiliationClickRefProperties();
        properties.setAwinEnabled(true);
        properties.setTradetrackerEnabled(true);
        properties.setEffiliationEnabled(true);
        properties.setKwankoEnabled(true);
        return new AffiliationClickRefService(properties);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.awin1.com/pclick.php?p=44101192377&a=530033&m=17547",
            "https://www.awin1.com/cread.php?awinmid=1&awinaffid=2&ued=https%3A%2F%2Fwww.darty.com%2Fx%3Fa%3D1",
            "https://tc.tradetracker.net/?c=1&m=2&a=3&u=https%3A%2F%2Fx",
            "https://partner.aosom.fr/c?c=1&m=2&a=3&r=&u=https%3A%2F%2Fx",
            "https://track.effiliation.com/servlet/effi.redir?id_compteur=23242374&url=https%3A%2F%2Fwww.x.com%2Fp",
            "https://action.metaffiliation.com/trk.php?mclic=P4455B58B421211S1UE41280d02321035V4",
            "https://www.amazon.fr/s?k=0123456789012&tag=nudger-21",
            "https://www.carrefour.fr/p/pouf design \"miami\" 50cm naturel-5400924343156?s=2543"})
    void switchedOffShouldKeepTheUrlUnchanged(String url) {
        AffiliationClickRefService service = new AffiliationClickRefService(new AffiliationClickRefProperties());

        assertThat(service.withClickRef(url, ID)).isSameAs(url);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // Awin product link: appended (p is the product id, not a destination).
            "https://www.awin1.com/pclick.php?p=44101192377&a=530033&m=17547"
                    + "|https://www.awin1.com/pclick.php?p=44101192377&a=530033&m=17547&clickref=" + ID,
            // Awin deep link: before the encoded destination, which is kept byte for byte.
            "https://www.awin1.com/cread.php?awinmid=1&awinaffid=2&ued=https%3A%2F%2Fwww.darty.com%2Fx%3Fa%3D1%26b%3D2"
                    + "|https://www.awin1.com/cread.php?awinmid=1&awinaffid=2&clickref=" + ID
                    + "&ued=https%3A%2F%2Fwww.darty.com%2Fx%3Fa%3D1%26b%3D2",
            // Awin legacy deep link with an unencoded p= destination: before it.
            "https://www.awin1.com/cread.php?awinmid=1&awinaffid=2&p=https://www.darty.com/x?a=1&b=2"
                    + "|https://www.awin1.com/cread.php?awinmid=1&awinaffid=2&clickref=" + ID
                    + "&p=https://www.darty.com/x?a=1&b=2",
            // Awin link with its own clickref: replaced in place.
            "https://www.awin1.com/cread.php?awinmid=1&clickref=feed&awinaffid=2"
                    + "|https://www.awin1.com/cread.php?awinmid=1&clickref=" + ID + "&awinaffid=2",
            // TradeTracker: before u=.
            "https://tc.tradetracker.net/?c=1&m=2&a=3&u=https%3A%2F%2Fwww.x.fr%2Fp%3Fq%3D1"
                    + "|https://tc.tradetracker.net/?c=1&m=2&a=3&r=" + ID + "&u=https%3A%2F%2Fwww.x.fr%2Fp%3Fq%3D1",
            // TradeTracker on a merchant domain with an empty reference: replaced.
            "https://partner.aosom.fr/c?c=1&m=2&a=3&r=&u=https%3A%2F%2Fx"
                    + "|https://partner.aosom.fr/c?c=1&m=2&a=3&r=" + ID + "&u=https%3A%2F%2Fx",
            // Effiliation: before url=.
            "https://track.effiliation.com/servlet/effi.redir?id_compteur=23242374&url=https%3A%2F%2Fwww.x.com%2Fp"
                    + "|https://track.effiliation.com/servlet/effi.redir?id_compteur=23242374&effi_id=" + ID
                    + "&url=https%3A%2F%2Fwww.x.com%2Fp",
            // Effiliation with its own effi_id after the destination: replaced, before the destination.
            "https://track.effiliation.com/servlet/effi.redir?id_compteur=1&url=https%3A%2F%2Fx&effi_id=feed"
                    + "|https://track.effiliation.com/servlet/effi.redir?id_compteur=1&effi_id=" + ID
                    + "&url=https%3A%2F%2Fx",
            // Kwanko mclic link: appended.
            "https://action.metaffiliation.com/trk.php?mclic=P4455B58B421211S1UE41280d02321035V4"
                    + "|https://action.metaffiliation.com/trk.php?mclic=P4455B58B421211S1UE41280d02321035V4&argsite="
                    + ID,
            // The fragment stays at the end.
            "https://tc.tradetracker.net/?c=1&m=2&a=3&u=https%3A%2F%2Fx#offers"
                    + "|https://tc.tradetracker.net/?c=1&m=2&a=3&r=" + ID + "&u=https%3A%2F%2Fx#offers"})
    void switchedOnShouldWriteTheClickIdIntoTheNetworkReference(String url, String expected) {
        assertThat(allEnabled().withClickRef(url, ID)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // No sub-id per click.
            "https://www.amazon.fr/s?k=0123456789012&tag=nudger-21",
            "https://amzn.to/abc",
            // Direct merchant links.
            "https://www.carrefour.fr/p/x-5400924343156?s=2543",
            "https://fr.shopping.rakuten.com/offer/buy/1",
            // Kwanko on a merchant domain: the query is a bare token, no documented place for argsite.
            "https://mqs.centrale-brico.com/?P510CAF58B41F15S1UD41308b0218097V4",
            "not a url"})
    void networksWithoutReferenceShouldKeepTheUrlUnchanged(String url) {
        assertThat(allEnabled().withClickRef(url, ID)).isSameAs(url);
    }

    @Test
    void eachNetworkShouldHaveItsOwnSwitch() {
        AffiliationClickRefProperties properties = new AffiliationClickRefProperties();
        properties.setTradetrackerEnabled(true);
        AffiliationClickRefService service = new AffiliationClickRefService(properties);
        String awin = "https://www.awin1.com/pclick.php?p=1&a=2&m=3";

        assertThat(service.withClickRef(awin, ID)).isSameAs(awin);
        assertThat(service.withClickRef("https://tc.tradetracker.net/?c=1&m=2&a=3&u=x", ID))
                .isEqualTo("https://tc.tradetracker.net/?c=1&m=2&a=3&r=" + ID + "&u=x");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void blankClickIdShouldKeepTheUrlUnchanged(String clickId) {
        String url = "https://www.awin1.com/pclick.php?p=1&a=2&m=3";

        assertThat(allEnabled().withClickRef(url, clickId)).isSameAs(url);
    }

    @Test
    void nullUrlShouldStayNull() {
        assertThat(allEnabled().withClickRef(null, ID)).isNull();
    }

    @Test
    void theValueShouldBeUrlEncoded() {
        assertThat(AffiliationClickRefService.setParameter("https://x.example/?a=1", "r", "a b&c", java.util.Set.of()))
                .isEqualTo("https://x.example/?a=1&r=a+b%26c");
    }

    @Test
    void aUrlWithoutQueryShouldGetOne() {
        assertThat(AffiliationClickRefService.setParameter("https://x.example/p#f", "r", "1", java.util.Set.of()))
                .isEqualTo("https://x.example/p?r=1#f");
    }

    @Test
    void shippedDefaultsShouldEnableAwinOnly() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml")).get(0));
        AffiliationClickRefProperties properties = Binder.get(environment)
                .bind("front.affiliation-click-ref", AffiliationClickRefProperties.class).get();
        AffiliationClickRefService service = new AffiliationClickRefService(properties);

        assertThat(service.withClickRef("https://www.awin1.com/pclick.php?p=1&a=2&m=3", ID))
                .isEqualTo("https://www.awin1.com/pclick.php?p=1&a=2&m=3&clickref=" + ID);
        assertThat(properties.isTradetrackerEnabled()).isFalse();
        assertThat(properties.isEffiliationEnabled()).isFalse();
        assertThat(properties.isKwankoEnabled()).isFalse();
    }
}
