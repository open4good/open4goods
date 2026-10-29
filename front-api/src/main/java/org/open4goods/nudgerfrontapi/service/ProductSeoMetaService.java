package org.open4goods.nudgerfrontapi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.PrefixedAttrText;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Builds the SEO title and meta description of product pages.
 * <p>
 * front-api is the single source of truth for these two strings: the Nuxt
 * product page renders {@code names.metaTitle} and {@code names.metaDescription}
 * as-is and only falls back to its own i18n templates when the API returns
 * {@code null} (no usable product name). See
 * {@code frontend/docs/seo-metadata.md}.
 * </p>
 * <ul>
 * <li>Title (at most {@value #MAX_TITLE_LENGTH} characters, never cut by an
 * ellipsis): {@code <Brand> <Model> : <type> <key features> – avis, prix et impact}.
 * The manufacturer reference number that some sources append to the model
 * (e.g. {@code E62LB200S 911572124}) is removed. When the title is too long,
 * the variants degrade: second feature, then long suffix, then first feature,
 * then type are dropped.</li>
 * <li>Description (at most {@value #MAX_DESCRIPTION_LENGTH} characters): impact
 * score out of 20, two key features and the price range of the current offers.
 * Missing data is omitted, never invented.</li>
 * </ul>
 * <p>
 * The product type is the first alternative of the vertical URL prefix
 * ({@code "|| lave-vaisselle | lave-vaisselles ||"} gives {@code lave-vaisselle}),
 * key features come from the vertical {@code popularAttributes}, formatted with
 * the attribute unit/suffix.
 * </p>
 */
@Service
public class ProductSeoMetaService {

    /** Maximum SERP-safe title length. */
    public static final int MAX_TITLE_LENGTH = 60;

    /** Maximum SERP-safe meta description length. */
    public static final int MAX_DESCRIPTION_LENGTH = 160;

    /** A trailing all-digit token of 6+ digits, separated by a space or an underscore. */
    private static final Pattern TRAILING_REFERENCE = Pattern.compile("^(.*?[^\\s_])[\\s_]+(\\d{6,})$");

    /** A word written in capitals that is long enough to be prose rather than an acronym. */
    private static final Pattern SHOUTED_WORD = Pattern.compile("\\p{Lu}{5,}");

    /** Short words kept lower case when a brand is title-cased (De Dietrich). */
    private static final Set<String> BRAND_PARTICLES = Set.of("DE", "DA", "DI", "DU", "LA", "LE", "VAN", "VON");

    private static final int MAX_TITLE_FEATURES = 2;
    private static final int MAX_DESCRIPTION_FEATURES = 2;

    private final OfferFreshnessService offerFreshnessService;

    /**
     * @param offerFreshnessService filter keeping only the offers that can be served
     */
    public ProductSeoMetaService(OfferFreshnessService offerFreshnessService) {
        this.offerFreshnessService = offerFreshnessService;
    }

    /**
     * Build the SEO title of a product page.
     *
     * @param product        product being mapped
     * @param fallbackName   localised product name used when brand or model is missing
     * @param vConfig        vertical configuration, may be {@code null}
     * @param domainLanguage requested domain language
     * @return a title of at most {@value #MAX_TITLE_LENGTH} characters, or {@code null}
     *         when no product name is available
     */
    public String buildMetaTitle(Product product, String fallbackName, VerticalConfig vConfig,
            DomainLanguage domainLanguage) {
        String label = resolveProductLabel(product, fallbackName);
        if (!StringUtils.hasText(label)) {
            return null;
        }
        boolean fr = isFrench(domainLanguage);
        String separator = fr ? " : " : ": ";
        String longSuffix = fr ? " – avis, prix et impact" : " – reviews, prices and impact";
        String shortSuffix = fr ? " – avis et prix" : " – reviews and prices";

        String type = resolveProductType(vConfig, domainLanguage);
        List<String> features = resolveFeatures(product, vConfig, domainLanguage, true, MAX_TITLE_FEATURES);

        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(type)) {
            String typed = label + separator + type;
            String twoFeatures = features.isEmpty() ? null : typed + " " + String.join(", ", features);
            String oneFeature = features.isEmpty() ? null : typed + " " + features.get(0);
            addCandidate(candidates, twoFeatures, longSuffix);
            addCandidate(candidates, oneFeature, longSuffix);
            addCandidate(candidates, twoFeatures, shortSuffix);
            addCandidate(candidates, oneFeature, shortSuffix);
            addCandidate(candidates, typed, longSuffix);
            addCandidate(candidates, typed, shortSuffix);
            addCandidate(candidates, typed, "");
        }
        addCandidate(candidates, label, longSuffix);
        addCandidate(candidates, label, shortSuffix);
        addCandidate(candidates, label, "");

        return candidates.stream()
                .filter(candidate -> candidate.length() <= MAX_TITLE_LENGTH)
                .findFirst()
                .orElseGet(() -> cutAtWordBoundary(label, MAX_TITLE_LENGTH));
    }

    /**
     * Build the meta description of a product page.
     *
     * @param product        product being mapped
     * @param fallbackName   localised product name used when brand or model is missing
     * @param vConfig        vertical configuration, may be {@code null}
     * @param domainLanguage requested domain language
     * @return a description of at most {@value #MAX_DESCRIPTION_LENGTH} characters, or
     *         {@code null} when no product name is available
     */
    public String buildMetaDescription(Product product, String fallbackName, VerticalConfig vConfig,
            DomainLanguage domainLanguage) {
        String label = resolveProductLabel(product, fallbackName);
        if (!StringUtils.hasText(label)) {
            return null;
        }
        boolean fr = isFrench(domainLanguage);
        String type = resolveProductType(vConfig, domainLanguage);
        String head = StringUtils.hasText(type) ? label + ", " + type : label;
        String separator = fr ? " : " : ": ";
        String closing = fr ? " Comparez les offres sur Nudger." : " Compare offers on Nudger.";

        String score = Optional.ofNullable(product.ecoscore())
                .map(Score::getValue)
                .map(value -> (fr ? "score impact " : "impact score ") + formatScore(value) + "/20")
                .orElse(null);
        List<String> features = resolveFeatures(product, vConfig, domainLanguage, false, MAX_DESCRIPTION_FEATURES);
        String prices = formatPriceRange(product, fr);

        // Facts in priority order; the last ones are dropped first when too long.
        List<String> facts = new ArrayList<>();
        if (score != null) {
            facts.add(score);
        }
        if (prices != null) {
            facts.add(prices);
        }
        facts.addAll(features);

        for (int count = facts.size(); count >= 0; count--) {
            List<String> kept = orderFacts(facts.subList(0, count), score, prices);
            String body = kept.isEmpty()
                    ? head + separator + (fr ? "avis, prix et caractéristiques." : "reviews, prices and specifications.")
                    : head + separator + String.join(", ", kept) + ".";
            if ((body + closing).length() <= MAX_DESCRIPTION_LENGTH) {
                return body + closing;
            }
            if (body.length() <= MAX_DESCRIPTION_LENGTH) {
                return body;
            }
        }
        return cutAtWordBoundary(head, MAX_DESCRIPTION_LENGTH);
    }

    /**
     * Remove the manufacturer reference number carried by the product model from a
     * product name (used for the page H1).
     *
     * @param name    product name, may be {@code null}
     * @param product product owning the name
     * @return the name without the reference, or the name unchanged when the model has
     *         no reference or the name does not contain it
     */
    public String stripManufacturerReference(String name, Product product) {
        if (!StringUtils.hasText(name) || product == null) {
            return name;
        }
        String reference = manufacturerReference(product.model());
        if (reference == null) {
            return name;
        }
        String stripped = name.replaceAll("[\\s_]+" + Pattern.quote(reference) + "(?=\\s|$)", "")
                .trim()
                .replaceAll("\\s+", " ");
        return StringUtils.hasText(stripped) ? stripped : name;
    }

    /**
     * Resolve the "Brand Model" label of the product, without manufacturer reference.
     *
     * @param product      product being mapped
     * @param fallbackName name used when brand or model is missing
     * @return product label or {@code null}
     */
    String resolveProductLabel(Product product, String fallbackName) {
        if (product == null) {
            return null;
        }
        String brand = trimToNull(product.brand());
        String model = trimToNull(cleanModel(product.model()));
        if (brand != null && model != null) {
            String displayBrand = displayBrand(brand);
            if (model.toLowerCase(Locale.ROOT).startsWith(brand.toLowerCase(Locale.ROOT) + " ")) {
                model = model.substring(brand.length()).trim();
            }
            return normalizeSpaces(displayBrand + " " + model);
        }
        String fallback = trimToNull(stripManufacturerReference(fallbackName, product));
        if (fallback == null) {
            return null;
        }
        return brand == null ? normalizeSpaces(fallback) : normalizeSpaces(replaceBrand(fallback, brand));
    }

    /**
     * Remove a trailing manufacturer reference from a model ({@code E62LB200S 911572124}
     * gives {@code E62LB200S}).
     *
     * @param model raw model, may be {@code null}
     * @return model without reference
     */
    static String cleanModel(String model) {
        if (!StringUtils.hasText(model)) {
            return model;
        }
        Matcher matcher = TRAILING_REFERENCE.matcher(model.trim());
        return matcher.matches() ? matcher.group(1).trim() : model.trim();
    }

    /**
     * Extract the trailing manufacturer reference of a model, if any.
     *
     * @param model raw model
     * @return reference digits or {@code null}
     */
    private static String manufacturerReference(String model) {
        if (!StringUtils.hasText(model)) {
            return null;
        }
        Matcher matcher = TRAILING_REFERENCE.matcher(model.trim());
        return matcher.matches() ? matcher.group(2) : null;
    }

    /**
     * Title-case a brand written in capitals ({@code ELECTROLUX} gives
     * {@code Electrolux}); short acronyms ({@code LG}, {@code AEG}) and mixed-case
     * brands are kept unchanged.
     *
     * @param brand raw brand
     * @return display brand
     */
    static String displayBrand(String brand) {
        String trimmed = brand.trim();
        if (!trimmed.equals(trimmed.toUpperCase(Locale.ROOT)) || trimmed.replaceAll("[^\\p{L}]", "").length() < 4) {
            return trimmed;
        }
        return Arrays.stream(trimmed.split(" "))
                .map(word -> Arrays.stream(word.split("-", -1))
                        .map(ProductSeoMetaService::titleCaseWord)
                        .collect(Collectors.joining("-")))
                .collect(Collectors.joining(" "));
    }

    private static String titleCaseWord(String word) {
        if (word.length() >= 4 || BRAND_PARTICLES.contains(word)) {
            return word.substring(0, 1) + word.substring(1).toLowerCase(Locale.ROOT);
        }
        return word;
    }

    private static String replaceBrand(String name, String brand) {
        return name.replaceAll("(?i)" + Pattern.quote(brand), Matcher.quoteReplacement(displayBrand(brand)));
    }

    /**
     * Resolve the singular product type from the vertical URL prefix.
     *
     * @param vConfig        vertical configuration
     * @param domainLanguage requested language
     * @return product type or {@code null}
     */
    String resolveProductType(VerticalConfig vConfig, DomainLanguage domainLanguage) {
        if (vConfig == null) {
            return null;
        }
        ProductI18nElements i18n = vConfig.i18n(languageTag(domainLanguage));
        PrefixedAttrText url = i18n == null ? null : i18n.getUrl();
        if (url == null || !StringUtils.hasText(url.getPrefix())) {
            return null;
        }
        return Arrays.stream(url.getPrefix().split("\\|"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .findFirst()
                .map(type -> type.length() <= 2 ? type.toUpperCase(Locale.ROOT) : type)
                .orElse(null);
    }

    /**
     * Format the key features of the product from the vertical popular attributes.
     *
     * @param product        product being mapped
     * @param vConfig        vertical configuration
     * @param domainLanguage requested language
     * @param measuredOnly   keep only numeric values carrying a unit or suffix
     *                       (self-explanatory in a title)
     * @param limit          maximum number of features
     * @return formatted features, measured ones first
     */
    List<String> resolveFeatures(Product product, VerticalConfig vConfig, DomainLanguage domainLanguage,
            boolean measuredOnly, int limit) {
        if (product == null || vConfig == null || vConfig.getPopularAttributes() == null
                || product.getAttributes() == null || product.getAttributes().getIndexed() == null) {
            return List.of();
        }
        String lang = languageTag(domainLanguage);
        Locale locale = isFrench(domainLanguage) ? Locale.FRANCE : Locale.UK;
        Map<String, IndexedAttribute> indexed = product.getAttributes().getIndexed();
        List<String> measured = new ArrayList<>();
        List<String> textual = new ArrayList<>();
        for (String key : new LinkedHashSet<>(vConfig.getPopularAttributes())) {
            IndexedAttribute attribute = key == null ? null : indexed.get(key);
            if (attribute == null) {
                continue;
            }
            AttributeConfig config = vConfig.getAttributesConfig() == null ? null
                    : vConfig.getAttributesConfig().getAttributeConfigByKey(key);
            String unit = config == null ? null : localised(config.getUnit(), lang);
            String suffix = config == null ? null : localised(config.getSuffix(), lang);
            Double numeric = attribute.getNumericValue();
            if (numeric != null && Double.isFinite(numeric) && (unit != null || suffix != null)) {
                measured.add(withUnits(formatNumber(numeric, locale), unit, suffix));
            } else if (!measuredOnly) {
                String text = numeric != null && Double.isFinite(numeric) ? formatNumber(numeric, locale)
                        : mappedValue(attribute.getValue(), config);
                if (StringUtils.hasText(text)) {
                    textual.add(withUnits(quietCase(text.trim()), unit, suffix));
                }
            }
        }
        List<String> features = new ArrayList<>(measured);
        features.addAll(textual);
        return features.stream().distinct().limit(limit).toList();
    }

    /**
     * Format the price range of the current offers. Stale offers are ignored (see
     * {@link OfferFreshnessService}).
     *
     * @param product product being mapped
     * @param fr      French copy when {@code true}
     * @return price fact, or {@code null} when the product has no fresh priced offer
     */
    String formatPriceRange(Product product, boolean fr) {
        AggregatedPrices prices = offerFreshnessService.freshPrices(product);
        if (prices == null) {
            return null;
        }
        List<AggregatedPrice> offers = prices.getOffers().stream()
                .filter(Objects::nonNull)
                .filter(offer -> offer.getPrice() != null && offer.getPrice() > 0 && Double.isFinite(offer.getPrice()))
                .toList();
        if (offers.isEmpty()) {
            return null;
        }
        Currency currency = offers.get(0).getCurrency();
        List<BigDecimal> amounts = offers.stream()
                .filter(offer -> Objects.equals(offer.getCurrency(), currency))
                .map(offer -> BigDecimal.valueOf(offer.getPrice()).setScale(2, RoundingMode.HALF_UP))
                .sorted()
                .toList();
        BigDecimal min = amounts.get(0);
        BigDecimal max = amounts.get(amounts.size() - 1);
        if (min.compareTo(max) == 0) {
            return (fr ? "prix : " : "price: ") + formatMoney(min, currency, fr);
        }
        return fr ? "prix de " + formatAmount(min, true) + " à " + formatMoney(max, currency, true)
                : "prices from " + formatMoney(min, currency, false) + " to " + formatMoney(max, currency, false);
    }

    private static List<String> orderFacts(List<String> kept, String score, String prices) {
        // Keep the reading order "score, features, prices" whatever the drop order.
        List<String> ordered = new ArrayList<>();
        if (score != null && kept.contains(score)) {
            ordered.add(score);
        }
        kept.stream().filter(fact -> !fact.equals(score) && !fact.equals(prices)).forEach(ordered::add);
        if (prices != null && kept.contains(prices)) {
            ordered.add(prices);
        }
        return ordered;
    }

    private static void addCandidate(List<String> candidates, String base, String suffix) {
        if (base != null) {
            candidates.add(normalizeSpaces(base + suffix));
        }
    }

    private static String withUnits(String value, String unit, String suffix) {
        StringBuilder sb = new StringBuilder(value);
        for (String part : new String[] { unit, suffix }) {
            if (part == null) {
                continue;
            }
            // Symbols such as the inch mark stick to the number: 55"
            sb.append(Character.isLetterOrDigit(part.codePointAt(0)) ? " " : "").append(part);
        }
        return sb.toString();
    }

    private static String mappedValue(String value, AttributeConfig config) {
        if (!StringUtils.hasText(value) || config == null || config.getMappings() == null) {
            return value;
        }
        String normalized = value.trim();
        return config.getMappings().entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(normalized))
                .map(Map.Entry::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(value);
    }

    /**
     * Lower-case values written in capitals ({@code POSE LIBRE}) while keeping
     * acronyms ({@code OLED}, {@code A+++}).
     */
    private static String quietCase(String value) {
        return SHOUTED_WORD.matcher(value).find() ? value.toLowerCase(Locale.ROOT) : value;
    }

    private static String formatNumber(double value, Locale locale) {
        DecimalFormat format = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(locale));
        format.setGroupingUsed(false);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(BigDecimal.valueOf(value));
    }

    private static String formatScore(double score) {
        return formatNumber(score, Locale.ROOT);
    }

    private static String formatMoney(BigDecimal amount, Currency currency, boolean fr) {
        String symbol = currencySymbol(currency);
        return fr ? formatAmount(amount, true) + " " + symbol : symbol + formatAmount(amount, false);
    }

    private static String formatAmount(BigDecimal amount, boolean fr) {
        BigDecimal value = amount.stripTrailingZeros().scale() <= 0 ? amount.setScale(0, RoundingMode.UNNECESSARY)
                : amount.setScale(2, RoundingMode.HALF_UP);
        String plain = value.toPlainString();
        return fr ? plain.replace('.', ',') : plain;
    }

    private static String currencySymbol(Currency currency) {
        if (currency == null) {
            return "€";
        }
        return switch (currency) {
            case EUR -> "€";
            case USD -> "$";
            default -> currency.name();
        };
    }

    private static String localised(Localisable<String, String> value, String lang) {
        if (value == null) {
            return null;
        }
        return trimToNull(value.i18n(lang));
    }

    private static boolean isFrench(DomainLanguage domainLanguage) {
        return domainLanguage == null || domainLanguage == DomainLanguage.fr;
    }

    private static String languageTag(DomainLanguage domainLanguage) {
        return domainLanguage == null ? DomainLanguage.fr.languageTag() : domainLanguage.languageTag();
    }

    private static String cutAtWordBoundary(String value, int maxLength) {
        String normalized = normalizeSpaces(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        String cut = normalized.substring(0, maxLength);
        int lastSpace = cut.lastIndexOf(' ');
        return (lastSpace > maxLength / 2 ? cut.substring(0, lastSpace) : cut).stripTrailing();
    }

    private static String normalizeSpaces(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
