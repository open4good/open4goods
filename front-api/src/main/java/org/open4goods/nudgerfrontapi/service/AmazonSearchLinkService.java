package org.open4goods.nudgerfrontapi.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.open4goods.nudgerfrontapi.config.properties.AmazonSearchLinkProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Builds the "Search on Amazon" link of product pages without any offer (GOU-1396).
 * <p>
 * The link targets an Amazon search by EAN with the Associates tag
 * ({@code https://www.amazon.fr/s?k={gtin}&tag={tag}}) and is wrapped in the existing {@code /contrib/} token, so the
 * click is recorded as an {@code AffiliationClick} like any other offer click. Amazon is never called: no price, no
 * image, no availability. The link is not an offer and never feeds the offer count, the ranking or the JSON-LD.
 * </p>
 */
@Service
public class AmazonSearchLinkService {

    private final AmazonSearchLinkProperties properties;
    private final AffiliationService affiliationService;

    public AmazonSearchLinkService(AmazonSearchLinkProperties properties, AffiliationService affiliationService) {
        this.properties = properties;
        this.affiliationService = affiliationService;
    }

    /**
     * Builds the {@code /contrib/} link towards the Amazon search of the given product.
     *
     * @param gtin      product GTIN
     * @param hasOffers whether the product has at least one offer; the link is only built when it has none
     * @return {@code /contrib/<token>}, or {@code null} when the link is disabled, the product has offers, the
     *         configuration is incomplete or the GTIN is not a valid EAN/UPC (see {@link #validEan(String)})
     */
    public String buildContribLink(String gtin, boolean hasOffers) {
        if (hasOffers || !properties.isEnabled() || !StringUtils.hasText(properties.getTag())
                || !StringUtils.hasText(properties.getUrlTemplate())) {
            return null;
        }
        String ean = validEan(gtin);
        if (ean == null) {
            return null;
        }
        String url = properties.getUrlTemplate()
                .replace(AmazonSearchLinkProperties.GTIN_PLACEHOLDER, ean)
                .replace(AmazonSearchLinkProperties.TAG_PLACEHOLDER,
                        URLEncoder.encode(properties.getTag().trim(), StandardCharsets.UTF_8));
        return AffiliationPartnerService.CONTRIB_ENDPOINT
                + affiliationService.encryptAffiliationLink(properties.getMerchant(), url, gtin);
    }

    /**
     * Validates a GTIN for an Amazon search and normalises it to its EAN form.
     * <p>
     * Leading zeros do not change a GS1 check digit, and product ids are left-padded to 13 digits, so the value is
     * first stripped of them: at least 8 significant digits are required (a short internal id such as
     * {@code 0000000000123} is not a barcode). The key is then the GTIN-8 for at most 8 digits, the EAN-13 for 9 to 13
     * (a UPC-A becomes its EAN-13), or the GTIN-14.
     * </p>
     *
     * @param gtin raw product GTIN, possibly {@code null}
     * @return the normalised EAN when the GTIN is digits only with a valid GS1 check digit, {@code null} otherwise
     */
    static String validEan(String gtin) {
        if (!StringUtils.hasText(gtin)) {
            return null;
        }
        String digits = gtin.trim();
        if (digits.length() > 14 || !digits.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return null;
        }
        String significant = digits.replaceFirst("^0+", "");
        if (significant.length() < 8) {
            return null;
        }
        int length = significant.length() <= 8 ? 8 : Math.max(significant.length(), 13);
        String ean = "0".repeat(length - significant.length()) + significant;
        return hasValidCheckDigit(ean) ? ean : null;
    }

    /**
     * Checks the GS1 check digit: from the digit left of the check digit, weights alternate 3 and 1.
     *
     * @param ean digits only, check digit included
     * @return {@code true} when the last digit matches the weighted modulus-10 sum
     */
    private static boolean hasValidCheckDigit(String ean) {
        int sum = 0;
        boolean weightThree = true;
        for (int i = ean.length() - 2; i >= 0; i--) {
            sum += (ean.charAt(i) - '0') * (weightThree ? 3 : 1);
            weightThree = !weightThree;
        }
        return (10 - sum % 10) % 10 == ean.charAt(ean.length() - 1) - '0';
    }
}
