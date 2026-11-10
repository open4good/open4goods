package org.open4goods.nudgerfrontapi.service.css;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCanonicalUrl;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.config.properties.CssProperties;
import org.open4goods.nudgerfrontapi.service.MerchantDomainsService.MerchantDomain;
import org.open4goods.nudgerfrontapi.service.OfferFreshnessService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Tells whether a product can be sent to the Google CSS API, with one exclusion reason per failed rule (GOU-2080).
 * <p>
 * A product needs at least {@code front.css.min-merchant-domains} distinct merchant domains with a fresh offer
 * (offers read through {@link OfferFreshnessService}, domains resolved by
 * {@link org.open4goods.nudgerfrontapi.service.MerchantDomainsService#domainResolver()}, so the registrable domain
 * and the affiliation networks exclusion are the rules of the merchant domains count), an image, a real title, a
 * valid GTIN and a canonical URL.
 * </p>
 */
@Service
@ConditionalOnProperty(prefix = "front.css", name = "enabled", havingValue = "true")
public class CssEligibilityService {

    private static final DomainLanguage LANGUAGE = DomainLanguage.fr;
    private static final String IMAGES_PATH = "/images/";

    private final OfferFreshnessService offerFreshnessService;
    private final CssProperties properties;
    private final ApiProperties apiProperties;

    /**
     * @param offerFreshnessService single freshness filter of the offers
     * @param properties            CSS settings
     * @param apiProperties         resource root of the images
     */
    public CssEligibilityService(OfferFreshnessService offerFreshnessService, CssProperties properties,
            ApiProperties apiProperties) {
        this.offerFreshnessService = offerFreshnessService;
        this.properties = properties;
        this.apiProperties = apiProperties;
    }

    /**
     * Evaluates every rule.
     *
     * @param product        product to evaluate
     * @param verticalConfig configuration of its vertical, may be null
     * @param resolver       datasource to merchant domain resolver of the run
     * @return the failed rules and the merchant domains found
     */
    public CssEligibility evaluate(Product product, VerticalConfig verticalConfig,
            Function<String, MerchantDomain> resolver) {
        Set<CssExclusionReason> reasons = EnumSet.noneOf(CssExclusionReason.class);
        Set<String> domains = new TreeSet<>();
        merchantReason(product, resolver, domains, reasons);
        if (!StringUtils.hasText(imageUrl(product))) {
            reasons.add(CssExclusionReason.NO_IMAGE);
        }
        if (title(product) == null) {
            reasons.add(CssExclusionReason.NO_REAL_TITLE);
        }
        if (!isValidGtin(product.gtin())) {
            reasons.add(CssExclusionReason.INVALID_GTIN);
        }
        if (canonicalPath(product, verticalConfig) == null) {
            reasons.add(CssExclusionReason.NO_CANONICAL_URL);
        }
        return new CssEligibility(reasons, domains);
    }

    /**
     * Fresh offers usable for a price: with a price and a currency.
     *
     * @param product product to read
     * @return the offers served by the product page ({@link OfferFreshnessService}), without price-less ones
     */
    public List<AggregatedPrice> usableOffers(Product product) {
        AggregatedPrices fresh = offerFreshnessService.freshPrices(product);
        if (fresh == null) {
            return List.of();
        }
        return fresh.getOffers().stream()
                .filter(o -> o.getPrice() != null && o.getPrice() > 0 && o.getCurrency() != null)
                .toList();
    }

    private void merchantReason(Product product, Function<String, MerchantDomain> resolver, Set<String> domains,
            Set<CssExclusionReason> reasons) {
        List<AggregatedPrice> offers = usableOffers(product);
        if (offers.isEmpty()) {
            reasons.add(CssExclusionReason.NO_FRESH_OFFER);
            return;
        }
        int networks = 0;
        int unresolved = 0;
        for (AggregatedPrice offer : offers) {
            MerchantDomain merchant = resolver.apply(offer.getDatasourceName());
            if (merchant.domain() == null) {
                unresolved++;
            } else if (merchant.network()) {
                networks++;
            } else {
                domains.add(merchant.domain());
            }
        }
        if (domains.size() >= Math.max(1, properties.getMinMerchantDomains())) {
            return;
        }
        if (domains.isEmpty() && networks > 0) {
            reasons.add(CssExclusionReason.AFFILIATION_NETWORK_ONLY);
        } else if (domains.isEmpty() && unresolved > 0) {
            reasons.add(CssExclusionReason.UNRESOLVED_MERCHANT_DOMAIN);
        } else {
            reasons.add(CssExclusionReason.NOT_ENOUGH_MERCHANT_DOMAINS);
        }
    }

    /**
     * @param product product to read
     * @return the display title in French, or null when it is missing, the GTIN alone or a generic title
     */
    public String title(Product product) {
        if (product.getNames() == null) {
            return null;
        }
        for (java.util.Map<String, String> candidates : List.of(product.getNames().getDisplayName(),
                product.getNames().getCardName(), product.getNames().getPageTitle(),
                product.getNames().getSeoName())) {
            String value = localise(candidates);
            if (StringUtils.hasText(value)) {
                return isRealTitle(value, product.gtin()) ? value.trim() : null;
            }
        }
        return null;
    }

    private boolean isRealTitle(String title, String gtin) {
        String trimmed = title.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (trimmed.matches("\\d+") || trimmed.equals(gtin)) {
            return false;
        }
        return properties.getGenericTitles().stream().noneMatch(g -> g != null && g.trim().toLowerCase(Locale.ROOT).equals(lower));
    }

    private static String localise(java.util.Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        for (String key : ProductCanonicalUrl.languageKeys(LANGUAGE)) {
            String value = values.get(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * @param value a localisable value (title, description)
     * @return its French value, or null
     */
    public static String french(java.util.Map<String, String> value) {
        return localise(value);
    }

    /**
     * @param product product to read
     * @return the absolute URL of the cover image, or null when the product has none
     */
    public String imageUrl(Product product) {
        String cover = product.getCoverImagePath();
        if (!StringUtils.hasText(cover)) {
            return null;
        }
        return absolute(cover);
    }

    /**
     * @param product product to read
     * @return the absolute URLs of the other processed images, without the cover
     */
    public List<String> additionalImageUrls(Product product) {
        String root = apiProperties.getResourceRootPath();
        String cover = imageUrl(product);
        return product.images().stream()
                .filter(r -> StringUtils.hasText(r.getFileName()) && StringUtils.hasText(r.getCacheKey())
                        && StringUtils.hasText(r.getExtension()) && StringUtils.hasText(root))
                .map(r -> root + IMAGES_PATH + r.getFileName() + "_" + r.getCacheKey() + "." + r.getExtension())
                .filter(url -> !url.equals(cover))
                .distinct()
                .limit(10)
                .toList();
    }

    private String absolute(String path) {
        String root = apiProperties.getResourceRootPath();
        String url = StringUtils.hasText(root) ? root + path : path;
        return url.startsWith("http") ? url : trimTrailingSlash(properties.getSiteBaseUrl()) + url;
    }

    /**
     * @param product        product to address
     * @param verticalConfig its vertical configuration, may be null
     * @return the canonical path ({@link ProductCanonicalUrl#fullSlug}), or null without slug
     */
    public String canonicalPath(Product product, VerticalConfig verticalConfig) {
        return ProductCanonicalUrl.fullSlug(product, verticalConfig, LANGUAGE);
    }

    /**
     * @param product        product to address
     * @param verticalConfig its vertical configuration, may be null
     * @return the absolute canonical URL, or null without slug
     */
    public String canonicalUrl(Product product, VerticalConfig verticalConfig) {
        String path = canonicalPath(product, verticalConfig);
        return path == null ? null : trimTrailingSlash(properties.getSiteBaseUrl()) + path;
    }

    /**
     * Checks a GTIN (8, 12, 13 or 14 digits, GS1 check digit).
     *
     * @param gtin candidate
     * @return true when valid
     */
    public static boolean isValidGtin(String gtin) {
        if (gtin == null || !gtin.matches("\\d{8}|\\d{12,14}")) {
            return false;
        }
        int sum = 0;
        for (int i = gtin.length() - 2, weight = 3; i >= 0; i--, weight = 4 - weight) {
            sum += (gtin.charAt(i) - '0') * weight;
        }
        return (10 - sum % 10) % 10 == gtin.charAt(gtin.length() - 1) - '0';
    }

    private static String trimTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
