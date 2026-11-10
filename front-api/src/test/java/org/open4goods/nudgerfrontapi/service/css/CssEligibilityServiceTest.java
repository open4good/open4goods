package org.open4goods.nudgerfrontapi.service.css;

import static org.assertj.core.api.Assertions.assertThat;
import static org.open4goods.nudgerfrontapi.service.css.CssTestFixtures.offer;
import static org.open4goods.nudgerfrontapi.service.css.CssTestFixtures.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.open4goods.model.product.Product;

class CssEligibilityServiceTest {

    private final CssEligibilityService service = CssTestFixtures.eligibility();

    private CssEligibility evaluate(Product p) {
        return service.evaluate(p, CssTestFixtures.vertical(), CssTestFixtures.resolver());
    }

    private Product eligible() {
        return product(offer("darty.com", 100), offer("boulanger.com", 120));
    }

    @Test
    void twoMerchantDomainsAreEligible() {
        CssEligibility result = evaluate(eligible());
        assertThat(result.eligible()).isTrue();
        assertThat(result.merchantDomains()).containsExactly("boulanger.com", "darty.com");
    }

    @Test
    void sameRegistrableDomainTwiceIsExcluded() {
        CssEligibility result = evaluate(product(offer("fr.aliexpress.com", 10), offer("aliexpress.com", 12)));
        assertThat(result.reasons()).containsExactly(CssExclusionReason.NOT_ENOUGH_MERCHANT_DOMAINS);
    }

    @Test
    void affiliationNetworkOnlyIsExcluded() {
        CssEligibility result = evaluate(product(offer("awin1.com", 10), offer("awin1.com", 12)));
        assertThat(result.reasons()).containsExactly(CssExclusionReason.AFFILIATION_NETWORK_ONLY);
    }

    @Test
    void unresolvedDomainsAreExcluded() {
        CssEligibility result = evaluate(product(offer("?a", 10), offer("?b", 12)));
        assertThat(result.reasons()).containsExactly(CssExclusionReason.UNRESOLVED_MERCHANT_DOMAIN);
    }

    @Test
    void staleOffersDoNotCount() {
        var stale = offer("boulanger.com", 120);
        stale.setTimeStamp(1L);
        assertThat(evaluate(product(offer("darty.com", 100), stale)).reasons())
                .containsExactly(CssExclusionReason.NOT_ENOUGH_MERCHANT_DOMAINS);
        assertThat(evaluate(product(stale)).reasons()).containsExactly(CssExclusionReason.NO_FRESH_OFFER);
    }

    @Test
    void missingImageIsExcluded() {
        Product p = eligible();
        p.setCoverImagePath(null);
        assertThat(evaluate(p).reasons()).containsExactly(CssExclusionReason.NO_IMAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = { CssTestFixtures.GTIN, "Produit", "  ", "12345" })
    void gtinAloneOrGenericTitleIsExcluded(String title) {
        Product p = eligible();
        p.getNames().getDisplayName().put("fr", title);
        assertThat(evaluate(p).reasons()).containsExactly(CssExclusionReason.NO_REAL_TITLE);
    }

    @Test
    void invalidGtinIsExcluded() {
        Product p = eligible();
        p.getAttributes().getReferentielAttributes().put(org.open4goods.model.attribute.ReferentielKey.GTIN,
                "4006381333932");
        assertThat(evaluate(p).reasons()).containsExactly(CssExclusionReason.INVALID_GTIN);
    }

    @Test
    void missingSlugIsExcluded() {
        Product p = eligible();
        p.getNames().getUrl().clear();
        assertThat(evaluate(p).reasons()).containsExactly(CssExclusionReason.NO_CANONICAL_URL);
    }

    @Test
    void gtinChecksum() {
        assertThat(CssEligibilityService.isValidGtin(CssTestFixtures.GTIN)).isTrue();
        assertThat(CssEligibilityService.isValidGtin("96385074")).isTrue();
        assertThat(CssEligibilityService.isValidGtin("abc")).isFalse();
        assertThat(CssEligibilityService.isValidGtin(null)).isFalse();
    }
}
