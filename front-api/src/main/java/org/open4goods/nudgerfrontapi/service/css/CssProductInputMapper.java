package org.open4goods.nudgerfrontapi.service.css;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.CssProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.protobuf.Timestamp;
import com.google.shopping.css.v1.Attributes;
import com.google.shopping.css.v1.Certification;
import com.google.shopping.css.v1.CssProductInput;
import com.google.shopping.css.v1.ProductDetail;
import com.google.shopping.type.Price;

/**
 * Maps a {@link Product} to a Google CSS {@link CssProductInput} (GOU-2080). Pure mapping: no client, no call to Google.
 * <p>
 * Prices come from the same fresh offers as the product page ({@link CssEligibilityService#usableOffers}). The
 * mapper never sets {@code rating}, {@code reviewCount}, {@code minRating} or {@code maxRating}: the ImpactScore
 * is not a review rating.
 * </p>
 */
@Service
@ConditionalOnProperty(prefix = "front.css", name = "enabled", havingValue = "true")
public class CssProductInputMapper {

    private static final String DETAILS_SECTION = "Caractéristiques";
    private static final String CERTIFICATION_NAME = "EPREL";
    private static final String CERTIFICATION_AUTHORITY = "European_Commission";

    private final CssEligibilityService eligibility;
    private final CssProperties properties;
    private final Clock clock;

    /**
     * @param eligibility eligibility rules and shared product fields
     * @param properties  CSS settings
     */
    public CssProductInputMapper(CssEligibilityService eligibility, CssProperties properties) {
        this(eligibility, properties, Clock.systemUTC());
    }

    CssProductInputMapper(CssEligibilityService eligibility, CssProperties properties, Clock clock) {
        this.eligibility = eligibility;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Maps an eligible product.
     *
     * @param product        eligible product
     * @param verticalConfig configuration of its vertical, may be null
     * @return the input to send
     * @throws IllegalArgumentException when the product has no usable offer or no canonical URL
     */
    public CssProductInput map(Product product, VerticalConfig verticalConfig) {
        List<AggregatedPrice> offers = eligibility.usableOffers(product);
        String canonicalUrl = eligibility.canonicalUrl(product, verticalConfig);
        if (offers.isEmpty() || canonicalUrl == null) {
            throw new IllegalArgumentException("Product " + product.gtin() + " has no usable offer or canonical URL");
        }
        AggregatedPrice lowest = offers.stream().min(Comparator.comparingDouble(AggregatedPrice::getPrice)).orElseThrow();
        AggregatedPrice highest = offers.stream().max(Comparator.comparingDouble(AggregatedPrice::getPrice)).orElseThrow();

        Attributes.Builder attributes = Attributes.newBuilder()
                .setCppLink(canonicalUrl)
                .setTitle(eligibility.title(product))
                .setImageLink(eligibility.imageUrl(product))
                .addAllAdditionalImageLinks(eligibility.additionalImageUrls(product))
                .setGtin(product.gtin())
                .setLowPrice(price(lowest.getPrice(), lowest.getCurrency().name()))
                .setHighPrice(price(highest.getPrice(), highest.getCurrency().name()))
                .setNumberOfOffers(offers.size())
                .setHeadlineOfferPrice(price(lowest.getPrice(), lowest.getCurrency().name()))
                .setHeadlineOfferLink(canonicalUrl)
                .setExpirationDate(timestamp(clock.instant().plus(properties.getExpiration())))
                .addAllIncludedDestinations(properties.getIncludedDestinations());
        if (lowest.getProductState() != null) {
            attributes.setHeadlineOfferCondition(lowest.getProductState() == ProductCondition.NEW ? "new" : "used");
        }
        if (lowest.getShippingCost() != null) {
            attributes.setHeadlineOfferShippingPrice(price(lowest.getShippingCost(), lowest.getCurrency().name()));
        }
        String description = CssEligibilityService.french(product.getNames() == null ? null
                : product.getNames().getMetaDescription());
        if (StringUtils.hasText(description)) {
            attributes.setDescription(description.trim());
        }
        if (StringUtils.hasText(product.brand())) {
            attributes.setBrand(product.brand());
        }
        if (StringUtils.hasText(product.model())) {
            attributes.setMpn(product.model());
        }
        String taxonomy = product.getGoogleTaxonomyId() != null ? String.valueOf(product.getGoogleTaxonomyId())
                : verticalConfig != null && verticalConfig.getGoogleTaxonomyId() != null
                        ? String.valueOf(verticalConfig.getGoogleTaxonomyId()) : null;
        if (taxonomy != null) {
            attributes.setGoogleProductCategory(taxonomy);
        }
        String productType = productType(product, verticalConfig);
        if (productType != null) {
            attributes.addProductTypes(productType);
        }
        details(product).forEach(attributes::addProductDetails);
        if (product.getEprelDatas() != null && StringUtils.hasText(product.getEprelDatas().getEnergyClass())) {
            attributes.addCertifications(Certification.newBuilder().setName(CERTIFICATION_NAME)
                    .setAuthority(CERTIFICATION_AUTHORITY).setCode(product.getEprelDatas().getEnergyClass().trim()));
        }
        return CssProductInput.newBuilder()
                .setRawProvidedId(product.gtin())
                .setContentLanguage(properties.getContentLanguage())
                .setFeedLabel(properties.getFeedLabel())
                .setAttributes(attributes)
                .build();
    }

    private String productType(Product product, VerticalConfig verticalConfig) {
        if (verticalConfig != null && verticalConfig.getI18n() != null) {
            Map<String, ProductI18nElements> i18n = verticalConfig.getI18n();
            ProductI18nElements fr = i18n.getOrDefault("fr", i18n.get("default"));
            if (fr != null && StringUtils.hasText(fr.getVerticalHomeTitle())) {
                return fr.getVerticalHomeTitle().trim();
            }
        }
        return StringUtils.hasText(product.getVertical()) ? product.getVertical() : null;
    }

    private List<ProductDetail> details(Product product) {
        if (product.getAttributes() == null || product.getAttributes().getAll() == null) {
            return List.of();
        }
        return product.getAttributes().getAll().values().stream()
                .filter(a -> a != null && StringUtils.hasText(a.getName()) && StringUtils.hasText(a.getValue()))
                .sorted(Comparator.comparing(ProductAttribute::getName))
                .limit(properties.getMaxProductDetails())
                .map(a -> ProductDetail.newBuilder().setSectionName(DETAILS_SECTION)
                        .setAttributeName(a.getName()).setAttributeValue(a.getValue()).build())
                .toList();
    }

    /**
     * Converts an amount to micros with decimal arithmetic, never a floating point multiplication.
     */
    static Price price(double amount, String currencyCode) {
        long micros = BigDecimal.valueOf(amount).movePointRight(6).setScale(0, RoundingMode.HALF_UP).longValueExact();
        return Price.newBuilder().setAmountMicros(micros).setCurrencyCode(currencyCode).build();
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }
}
