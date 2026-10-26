package org.open4goods.services.contribution.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link AffiliationNetwork}: network deduced from the target host, and {@link AffiliationClick#of} keeping
 * only the host of the target URL.
 */
class AffiliationNetworkTest {

    @ParameterizedTest
    @CsvSource({
            "https://www.awin1.com/cread.php?awinmid=1&awinaffid=2&ued=https%3A%2F%2Fshop.example, awin",
            "https://awin1.com/pclick.php?p=1, awin",
            "HTTPS://WWW.AWIN1.COM./cread.php, awin",
            "https://track.effiliation.com/servlet/effi.redir?id_compteur=1, effiliation",
            "https://effiliation.com/x, effiliation",
            "https://track.effinity.fr/r?x=1, effiliation",
            "https://www.darty.com/nav/achat/123.html, other",
            "https://awin1.com.evil.example/cread.php, other",
            "https://notawin1.com/cread.php, other",
            "https://www.example.com/?u=https://www.awin1.com/, other",
    })
    void fromUrl_deducesTheNetworkFromTheHost(String url, String expected) {
        assertThat(AffiliationNetwork.fromUrl(url).key()).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not a url", "http://", "/relative/path", "https://exa mple.com/"})
    void fromUrl_isOtherWithoutReadableHost(String url) {
        assertThat(AffiliationNetwork.fromUrl(url)).isEqualTo(AffiliationNetwork.OTHER);
        assertThat(AffiliationNetwork.hostOf(url)).isNull();
    }

    @Test
    void fromHost_matchesSubDomainsOnly() {
        assertThat(AffiliationNetwork.fromHost("a.b.awin1.com")).isEqualTo(AffiliationNetwork.AWIN);
        assertThat(AffiliationNetwork.fromHost("xawin1.com")).isEqualTo(AffiliationNetwork.OTHER);
        assertThat(AffiliationNetwork.fromHost(null)).isEqualTo(AffiliationNetwork.OTHER);
    }

    @Test
    void click_keepsOnlyTheHostAndTheDeducedNetwork() {
        AffiliationClick click = AffiliationClick.of("darty.com",
                "https://www.awin1.com/cread.php?awinmid=1&clickref=secret", "0123456789012", "/fours/foo", 42L);

        assertThat(click.getId()).isNotBlank();
        assertThat(click.getTs()).isEqualTo(42L);
        assertThat(click.getNetwork()).isEqualTo("awin");
        assertThat(click.getTargetHost()).isEqualTo("www.awin1.com");
        assertThat(click.getMerchant()).isEqualTo("darty.com");
        assertThat(click.getGtin()).isEqualTo("0123456789012");
        assertThat(click.getOriginPath()).isEqualTo("/fours/foo");
    }
}
