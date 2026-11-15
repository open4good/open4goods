package org.open4goods.nudgerfrontapi.service.css;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.product.ProductTexts;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.config.properties.CssProperties;
import org.open4goods.nudgerfrontapi.config.properties.OfferFreshnessProperties;
import org.open4goods.nudgerfrontapi.service.MerchantDomainsService.MerchantDomain;
import org.open4goods.nudgerfrontapi.service.OfferFreshnessService;

/** Builders shared by the CSS tests. */
final class CssTestFixtures {

    static final String GTIN = "4006381333931";

    private CssTestFixtures() {
    }

    static CssEligibilityService eligibility() {
        ApiProperties api = new ApiProperties();
        api.setResourceRootPath("https://static.nudger.fr");
        return new CssEligibilityService(new OfferFreshnessService(new OfferFreshnessProperties()), new CssProperties(),
                api);
    }

    /** Resolver reading the datasource name as the domain, network when it contains "awin". */
    static java.util.function.Function<String, MerchantDomain> resolver() {
        return name -> {
            if (name == null) {
                return new MerchantDomain(null, false);
            }
            String domain = name.replace("fr.", "").replace("www.", "");
            if (name.startsWith("?")) {
                return new MerchantDomain(null, false);
            }
            return new MerchantDomain(domain, domain.contains("awin"));
        };
    }

    static VerticalConfig vertical() {
        VerticalConfig vc = new VerticalConfig();
        vc.setId("tv");
        ProductI18nElements fr = new ProductI18nElements();
        fr.setVerticalHomeUrl("televisions");
        fr.setVerticalHomeTitle("Téléviseurs");
        vc.getI18n().put("fr", fr);
        return vc;
    }

    static AggregatedPrice offer(String datasource, double price) {
        AggregatedPrice o = new AggregatedPrice();
        o.setDatasourceName(datasource);
        o.setPrice(price);
        o.setCurrency(Currency.EUR);
        o.setProductState(ProductCondition.NEW);
        o.setTimeStamp(System.currentTimeMillis());
        o.setUrl("https://" + datasource + "/p");
        return o;
    }

    /** An eligible product: two merchants, image, title, valid GTIN, slug. */
    static Product product(AggregatedPrice... offers) {
        Product p = new Product();
        p.setId(Long.parseLong(GTIN));
        p.setVertical("tv");
        p.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN, GTIN);
        p.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "Acme");
        p.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "X-100");
        ProductTexts names = new ProductTexts();
        names.getDisplayName().put("fr", "Acme X-100 55 pouces");
        names.getUrl().put("fr", "acme-x-100-4006381333931");
        names.getMetaDescription().put("fr", "Un téléviseur.");
        p.setNames(names);
        p.setCoverImagePath("/images/acme_abc.webp");
        AggregatedPrices prices = new AggregatedPrices();
        Set<AggregatedPrice> set = new HashSet<>(java.util.List.of(offers));
        prices.setOffers(set);
        p.setPrice(prices);
        return p;
    }
}
