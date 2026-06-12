package org.open4goods.api.services.completion;

import java.text.ParseException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.AmazonCompletionConfig;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.services.AbstractCompletionService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.datafragment.ProviderSupportType;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.price.Price;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.util.ProductModelCandidateHelper.ModelCandidateSource;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;

import com.amazon.paapi5.v1.ApiException;
import com.amazon.paapi5.v1.ByLineInfo;
import com.amazon.paapi5.v1.Condition;
import com.amazon.paapi5.v1.DimensionBasedAttribute;
import com.amazon.paapi5.v1.ErrorData;
import com.amazon.paapi5.v1.GetItemsRequest;
import com.amazon.paapi5.v1.GetItemsResource;
import com.amazon.paapi5.v1.GetItemsResponse;
import com.amazon.paapi5.v1.ImageSize;
import com.amazon.paapi5.v1.Images;
import com.amazon.paapi5.v1.Item;
import com.amazon.paapi5.v1.ItemIdType;
import com.amazon.paapi5.v1.ItemInfo;
import com.amazon.paapi5.v1.ManufactureInfo;
import com.amazon.paapi5.v1.OfferListing;
import com.amazon.paapi5.v1.Offers;
import com.amazon.paapi5.v1.PartnerType;
import com.amazon.paapi5.v1.ProductInfo;
import com.amazon.paapi5.v1.SearchItemsRequest;
import com.amazon.paapi5.v1.SearchItemsResource;
import com.amazon.paapi5.v1.SearchItemsResponse;
import com.amazon.paapi5.v1.SingleBooleanValuedAttribute;
import com.amazon.paapi5.v1.SingleStringValuedAttribute;
import com.amazon.paapi5.v1.TechnicalInfo;
import com.amazon.paapi5.v1.UnitBasedAttribute;

/**
 * Completes products from Amazon Product Advertising API v5.
 *
 * <p>The service follows the existing completion pipeline contract:
 * {@link #shouldProcess(VerticalConfig, Product)} checks the product-level cache
 * in {@code Product.datasourceCodes}, {@link #processProduct(VerticalConfig, Product)}
 * fetches PA-API data, maps it to {@link DataFragment} instances, applies the
 * realtime aggregator, and stores the latest attempt timestamp.
 */
public class AmazonCompletionService extends AbstractCompletionService {

    static final String DATASOURCE_NAME = "amazon.fr";
    private static final String AMAZON_PRODUCT_STATE_NEW = "New";
    private static final String AMAZON_PRODUCT_STATE_USED = "Used";
    private static final String AMAZON_PRODUCT_STATE_COLLECTIBLE = "Collectible";
    private static final String AMAZON_PRODUCT_STATE_REFURBISHED = "Refurbished";
    private static final String LANGUAGE = "fr";

    private static final List<GetItemsResource> GET_ITEMS_RESOURCES = List.of(
            GetItemsResource.IMAGES_PRIMARY_LARGE,
            GetItemsResource.IMAGES_VARIANTS_LARGE,
            GetItemsResource.ITEMINFO_BYLINEINFO,
            GetItemsResource.ITEMINFO_MANUFACTUREINFO,
            GetItemsResource.ITEMINFO_PRODUCTINFO,
            GetItemsResource.ITEMINFO_TECHNICALINFO,
            GetItemsResource.ITEMINFO_TITLE,
            GetItemsResource.OFFERS_LISTINGS_CONDITION,
            GetItemsResource.OFFERS_LISTINGS_DELIVERYINFO_SHIPPINGCHARGES,
            GetItemsResource.OFFERS_LISTINGS_MERCHANTINFO,
            GetItemsResource.OFFERS_LISTINGS_PRICE);

    private static final List<SearchItemsResource> SEARCH_ITEMS_RESOURCES = List.of(
            SearchItemsResource.IMAGES_PRIMARY_LARGE,
            SearchItemsResource.IMAGES_VARIANTS_LARGE,
            SearchItemsResource.ITEMINFO_BYLINEINFO,
            SearchItemsResource.ITEMINFO_MANUFACTUREINFO,
            SearchItemsResource.ITEMINFO_PRODUCTINFO,
            SearchItemsResource.ITEMINFO_TECHNICALINFO,
            SearchItemsResource.ITEMINFO_TITLE,
            SearchItemsResource.OFFERS_LISTINGS_CONDITION,
            SearchItemsResource.OFFERS_LISTINGS_DELIVERYINFO_SHIPPINGCHARGES,
            SearchItemsResource.OFFERS_LISTINGS_MERCHANTINFO,
            SearchItemsResource.OFFERS_LISTINGS_PRICE);

    private final AmazonCompletionConfig amazonConfig;
    private final DataSourceProperties amazonDatasource;
    private final StandardAggregator aggregator;
    private final AmazonPaapiClient paapiClient;

    /**
     * Builds the Amazon completion service from API configuration.
     *
     * @param dataRepository product repository used by the completion base class
     * @param verticalConfigService vertical configuration service
     * @param apiProperties global API properties
     * @param dataSourceConfigService datasource configuration registry
     * @param aggregationFacadeService aggregation facade used to build a realtime aggregator
     */
    public AmazonCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService,
            ApiProperties apiProperties, DataSourceConfigService dataSourceConfigService,
            AggregationFacadeService aggregationFacadeService) {
        this(dataRepository, verticalConfigService, apiProperties, dataSourceConfigService, aggregationFacadeService,
                apiProperties.getAmazonConfig().isConfigured()
                        ? AmazonPaapiClient.fromConfig(apiProperties.getAmazonConfig())
                        : null);
    }

    AmazonCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService,
            ApiProperties apiProperties, DataSourceConfigService dataSourceConfigService,
            AggregationFacadeService aggregationFacadeService, AmazonPaapiClient paapiClient) {
        super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());
        this.amazonConfig = apiProperties.getAmazonConfig();
        this.amazonDatasource = resolveAmazonDatasource(dataSourceConfigService, amazonConfig);
        this.aggregator = aggregationFacadeService.getStandardAggregator("amazon-aggregation");
        this.aggregator.beforeStart();
        this.paapiClient = paapiClient;
    }

    @Override
    public boolean shouldProcess(VerticalConfig vertical, Product data) {
        if (!amazonConfig.isConfigured()) {
            return false;
        }

        Long lastProcessed = data.getDatasourceCodes().get(getDatasourceName());
        if (lastProcessed == null) {
            return true;
        }
        Duration refreshDuration = amazonConfig.getRefreshDuration();
        return System.currentTimeMillis() - lastProcessed >= refreshDuration.toMillis();
    }

    @Override
    public String getDatasourceName() {
        return DATASOURCE_NAME;
    }

    /**
     * Runs Amazon completion for all verticals using one global PA-API budget.
     *
     * <p>The base completion service applies {@code max} per vertical. Amazon
     * throttling is account-wide, so this override treats {@code max} as the
     * total number of products for the whole all-vertical run.
     *
     * @param max maximum products for this run; uses Amazon config when null
     * @param withExcluded whether excluded products should be included
     */
    @Override
    public void completeAll(Integer max, boolean withExcluded) {
        int remaining = max == null ? amazonConfig.getMaxCallsPerBatch() : max;
        logger.info("Amazon completion for all verticals, max {} products", remaining);
        for (VerticalConfig vertical : verticalConfigService.getConfigsWithoutDefault()) {
            if (remaining <= 0) {
                return;
            }

            List<Product> products = dataRepository.exportVerticalWithValidDate(vertical, withExcluded)
                    .limit(remaining)
                    .toList();
            products.forEach(product -> completeAndIndexProduct(vertical, product));
            remaining -= products.size();
        }
    }

    /**
     * Returns Amazon completion settings used by orchestration endpoints.
     *
     * @return Amazon completion configuration
     */
    public AmazonCompletionConfig getAmazonConfig() {
        return amazonConfig;
    }

    /**
     * Completes one product from PA-API and aggregates the resulting fragments.
     *
     * <p>If the product already has an ASIN, the service uses {@code GetItems};
     * otherwise it searches by GTIN and stores the returned ASIN for the next
     * refresh. The timestamp is stored even when no item is found, making misses
     * cacheable for the configured refresh duration.
     *
     * @param vertical vertical context
     * @param data product to complete
     */
    @Override
    public void processProduct(VerticalConfig vertical, Product data) {
        if (!amazonConfig.isConfigured() || paapiClient == null) {
            logger.info("Amazon completion is disabled or missing PA-API credentials");
            return;
        }

        logger.info("Amazon completion for {}", data.getId());
        Set<DataFragment> fragments = StringUtils.isBlank(data.getExternalIds().getAsin())
                ? completeSearch(data)
                : completeGet(data);

        for (DataFragment fragment : fragments) {
            try {
                aggregator.onDatafragment(fragment, data);
            } catch (AggregationSkipException e) {
                logger.error("Error occurred during Amazon aggregation for {}", data.gtin(), e);
            }
        }

        data.getDatasourceCodes().put(getDatasourceName(), System.currentTimeMillis());
        sleepAfterCall();
    }

    Set<DataFragment> completeGet(Product data) {
        GetItemsRequest request = new GetItemsRequest()
                .itemIdType(ItemIdType.ASIN)
                .itemIds(List.of(data.getExternalIds().getAsin()))
                .partnerTag(amazonConfig.getPartnerTag())
                .partnerType(PartnerType.ASSOCIATES)
                .resources(GET_ITEMS_RESOURCES)
                .condition(Condition.ANY)
                .marketplace(amazonConfig.getMarketplace());

        try {
            GetItemsResponse response = paapiClient.getItems(request);
            logErrors(response == null ? null : response.getErrors());
            List<Item> items = Optional.ofNullable(response)
                    .map(GetItemsResponse::getItemsResult)
                    .map(result -> nullToEmpty(result.getItems()))
                    .orElseGet(List::of);

            if (items.isEmpty()) {
                logger.info("No Amazon item found for known ASIN {} and GTIN {}", data.getExternalIds().getAsin(),
                        data.gtin());
            } else if (items.size() > 1) {
                logger.warn("Amazon returned {} items for ASIN {} and GTIN {}", items.size(),
                        data.getExternalIds().getAsin(), data.gtin());
            }
            return mapItems(items, data);
        } catch (ApiException e) {
            logApiException(e, data);
            return Set.of();
        }
    }

    Set<DataFragment> completeSearch(Product data) {
        SearchItemsRequest request = new SearchItemsRequest()
                .keywords(data.gtin())
                .itemCount(3)
                .partnerTag(amazonConfig.getPartnerTag())
                .partnerType(PartnerType.ASSOCIATES)
                .resources(SEARCH_ITEMS_RESOURCES)
                .condition(Condition.ANY)
                .searchIndex(amazonConfig.getSearchIndex())
                .marketplace(amazonConfig.getMarketplace());

        try {
            SearchItemsResponse response = paapiClient.searchItems(request);
            logErrors(response == null ? null : response.getErrors());
            List<Item> items = Optional.ofNullable(response)
                    .map(SearchItemsResponse::getSearchResult)
                    .map(result -> nullToEmpty(result.getItems()))
                    .orElseGet(List::of);

            if (items.isEmpty()) {
                logger.info("No Amazon item found for GTIN {}", data.gtin());
            } else if (items.size() > 1) {
                logger.warn("Amazon search returned {} candidates for GTIN {}", items.size(), data.gtin());
            }
            return mapItems(items, data);
        } catch (ApiException e) {
            logApiException(e, data);
            return Set.of();
        }
    }

    Set<DataFragment> mapItems(List<Item> items, Product data) {
        return nullToEmpty(items).stream()
                .filter(Objects::nonNull)
                .flatMap(item -> processAmazonItem(item, data).stream())
                .collect(java.util.stream.Collectors.toSet());
    }

    Set<DataFragment> processAmazonItem(Item item, Product data) {
        String detailPageUrl = item.getDetailPageURL();
        if (StringUtils.isBlank(detailPageUrl)) {
            detailPageUrl = amazonProductUrl(item.getASIN());
        }

        if (StringUtils.isNotBlank(item.getASIN())) {
            data.getExternalIds().setAsin(item.getASIN());
        } else {
            logger.warn("Amazon item for {} did not include an ASIN", data.gtin());
        }

        addImages(item.getImages(), data);

        String resolvedDetailPageUrl = detailPageUrl;
        Set<DataFragment> fragments = offerFragments(item.getOffers(), resolvedDetailPageUrl, data);
        if (fragments.isEmpty()) {
            DataFragment attributeFragment = initDataFragment(resolvedDetailPageUrl, data);
            addItemInfoAttributes(item.getItemInfo(), attributeFragment);

            if (hasCompletionContent(attributeFragment)) {
                fragments.add(attributeFragment);
            }
        } else {
            fragments.forEach(fragment -> addItemInfoAttributes(item.getItemInfo(), fragment));
        }
        return fragments;
    }

    private Set<DataFragment> offerFragments(Offers offers, String detailPageUrl, Product data) {
        List<OfferListing> listings = Optional.ofNullable(offers)
                .map(Offers::getListings)
                .map(AmazonCompletionService::nullToEmpty)
                .orElseGet(List::of);

        Optional<OfferListing> newOffer = listings.stream()
                .filter(this::hasMappablePrice)
                .filter(this::isNewCondition)
                .min(Comparator.comparing(offer -> offer.getPrice().getAmount()));

        Optional<OfferListing> usedOffer = listings.stream()
                .filter(this::hasMappablePrice)
                .filter(this::isUsedCondition)
                .min(Comparator.comparing(offer -> offer.getPrice().getAmount()));

        return Stream.of(newOffer, usedOffer)
                .flatMap(Optional::stream)
                .map(offer -> mapOfferToDataFragment(offer, detailPageUrl, data))
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Optional<DataFragment> mapOfferToDataFragment(OfferListing offer, String url, Product data) {
        if (!hasMappablePrice(offer)) {
            return Optional.empty();
        }

        DataFragment fragment = initDataFragment(url, data);
        ProductCondition condition = isUsedCondition(offer) ? ProductCondition.OCCASION : ProductCondition.NEW;
        fragment.setProductState(condition);
        fragment.setUrl(url + "#condition=" + condition.name().toLowerCase());

        Price price = new Price();
        price.setTimeStamp(System.currentTimeMillis());
        price.setPrice(offer.getPrice().getAmount().doubleValue());
        try {
            price.setCurrency(offer.getPrice().getCurrency());
        } catch (ParseException e) {
            logger.warn("Cannot map Amazon currency {} for {}", offer.getPrice().getCurrency(), data.gtin());
        }
        fragment.setPrice(price);
        mapShippingCost(offer, fragment);
        return Optional.of(fragment);
    }

    private void mapShippingCost(OfferListing offer, DataFragment fragment) {
        Optional.ofNullable(offer.getDeliveryInfo())
                .map(delivery -> nullToEmpty(delivery.getShippingCharges()).stream()
                        .filter(charge -> charge != null && charge.getAmount() != null)
                        .min(Comparator.comparing(charge -> charge.getAmount())))
                .flatMap(optional -> optional)
                .ifPresent(charge -> fragment.setShippingCost(charge.getAmount().doubleValue()));
    }

    private boolean hasMappablePrice(OfferListing offer) {
        return offer != null && offer.getPrice() != null && offer.getPrice().getAmount() != null;
    }

    private boolean isNewCondition(OfferListing offer) {
        String condition = conditionValue(offer);
        return condition == null || AMAZON_PRODUCT_STATE_NEW.equalsIgnoreCase(condition);
    }

    private boolean isUsedCondition(OfferListing offer) {
        String condition = conditionValue(offer);
        return AMAZON_PRODUCT_STATE_USED.equalsIgnoreCase(condition)
                || AMAZON_PRODUCT_STATE_COLLECTIBLE.equalsIgnoreCase(condition)
                || AMAZON_PRODUCT_STATE_REFURBISHED.equalsIgnoreCase(condition);
    }

    private String conditionValue(OfferListing offer) {
        return Optional.ofNullable(offer)
                .map(OfferListing::getCondition)
                .map(condition -> StringUtils.defaultIfBlank(condition.getValue(), condition.getDisplayValue()))
                .orElse(null);
    }

    private void addImages(Images images, Product data) {
        if (images == null) {
            return;
        }
        addImage(images.getPrimary(), data, ResourceTag.AMAZON_PRIMARY_TAG, ResourceTag.PRIMARY);
        nullToEmpty(images.getVariants()).forEach(image -> addImage(image, data, ResourceTag.AMAZON_VARIANT_TAG));
    }

    private void addImage(com.amazon.paapi5.v1.ImageType image, Product data, ResourceTag... hardTags) {
        String url = Optional.ofNullable(image)
                .map(com.amazon.paapi5.v1.ImageType::getLarge)
                .map(ImageSize::getURL)
                .orElse(null);
        if (StringUtils.isBlank(url)) {
            return;
        }

        try {
            Resource resource = new Resource(url);
            resource.setDatasourceName(amazonDatasource.getName());
            for (ResourceTag hardTag : hardTags) {
                resource.getHardTags().add(hardTag);
            }
            data.getResources().add(resource);
        } catch (ValidationException e) {
            logger.warn("Cannot add Amazon image {} for {}", url, data.gtin());
        }
    }

    private void addItemInfoAttributes(ItemInfo itemInfo, DataFragment fragment) {
        if (itemInfo == null) {
            return;
        }

        addByLineInfo(itemInfo.getByLineInfo(), fragment);
        addManufactureInfo(itemInfo.getManufactureInfo(), fragment);
        addProductInfo(itemInfo.getProductInfo(), fragment);
        addTechnicalInfo(itemInfo.getTechnicalInfo(), fragment);
        addName(itemInfo.getTitle(), fragment);
    }

    private void addByLineInfo(ByLineInfo byLineInfo, DataFragment fragment) {
        if (byLineInfo == null) {
            return;
        }
        addReferentielAttribute(fragment, ReferentielKey.BRAND, byLineInfo.getBrand());
    }

    private void addManufactureInfo(ManufactureInfo manufactureInfo, DataFragment fragment) {
        if (manufactureInfo == null) {
            return;
        }
        String model = displayValue(manufactureInfo.getModel());
        if (StringUtils.isNotBlank(model)) {
            if (model.length() > 15) {
                model = IdHelper.extractModelTokens(model).stream().findFirst().orElse(model);
            }
            fragment.addReferentielAttribute(ReferentielKey.MODEL, model, ModelCandidateSource.STRUCTURED_DATA);
        }
        addAttribute(fragment, "WARRANTY", manufactureInfo.getWarranty());
        addAttribute(fragment, "ITEM_PART_NUMBER", manufactureInfo.getItemPartNumber());
    }

    private void addProductInfo(ProductInfo productInfo, DataFragment fragment) {
        if (productInfo == null) {
            return;
        }
        addAttribute(fragment, "COLOR", productInfo.getColor());
        addBooleanAttribute(fragment, "ADULT", productInfo.getIsAdultProduct());
        addAttribute(fragment, "HEIGHT", displayUnit(Optional.ofNullable(productInfo.getItemDimensions())
                .map(DimensionBasedAttribute::getHeight).orElse(null)));
        addAttribute(fragment, "LENGTH", displayUnit(Optional.ofNullable(productInfo.getItemDimensions())
                .map(DimensionBasedAttribute::getLength).orElse(null)));
        addAttribute(fragment, "WEIGHT", displayUnit(Optional.ofNullable(productInfo.getItemDimensions())
                .map(DimensionBasedAttribute::getWeight).orElse(null)));
        addAttribute(fragment, "WIDTH", displayUnit(Optional.ofNullable(productInfo.getItemDimensions())
                .map(DimensionBasedAttribute::getWidth).orElse(null)));
        addAttribute(fragment, "SIZE", productInfo.getSize());

        String releaseDate = displayValue(productInfo.getReleaseDate());
        if (StringUtils.length(releaseDate) >= 4) {
            String year = releaseDate.substring(0, 4);
            if (StringUtils.isNumeric(year)) {
                addAttribute(fragment, "YEAR", year);
            }
        }
    }

    private void addTechnicalInfo(TechnicalInfo technicalInfo, DataFragment fragment) {
        if (technicalInfo == null) {
            return;
        }
        addAttribute(fragment, "CLASSE ENERGETIQUE", technicalInfo.getEnergyEfficiencyClass());
    }

    private void addName(SingleStringValuedAttribute title, DataFragment fragment) {
        String value = displayValue(title);
        if (StringUtils.isNotBlank(value)) {
            fragment.addName(value);
        }
    }

    private void addReferentielAttribute(DataFragment fragment, ReferentielKey key, SingleStringValuedAttribute value) {
        String displayValue = displayValue(value);
        if (StringUtils.isNotBlank(displayValue)) {
            fragment.addReferentielAttribute(key, displayValue);
        }
    }

    private void addAttribute(DataFragment fragment, String name, SingleStringValuedAttribute value) {
        addAttribute(fragment, name, displayValue(value));
    }

    private void addBooleanAttribute(DataFragment fragment, String name, SingleBooleanValuedAttribute value) {
        if (value != null && value.isDisplayValue() != null) {
            addAttribute(fragment, name, String.valueOf(value.isDisplayValue()));
        }
    }

    private void addAttribute(DataFragment fragment, String name, String value) {
        if (StringUtils.isNotBlank(value)) {
            fragment.addAttribute(name, value, LANGUAGE, null);
        }
    }

    private String displayValue(SingleStringValuedAttribute value) {
        return value == null ? null : value.getDisplayValue();
    }

    private String displayUnit(UnitBasedAttribute attribute) {
        if (attribute == null || attribute.getDisplayValue() == null) {
            return null;
        }
        return StringUtils.isBlank(attribute.getUnit())
                ? attribute.getDisplayValue().toPlainString()
                : attribute.getDisplayValue().toPlainString() + " " + attribute.getUnit();
    }

    private boolean hasCompletionContent(DataFragment fragment) {
        return !fragment.getNames().isEmpty()
                || !fragment.getAttributes().isEmpty()
                || fragment.getReferentielAttributes().size() > 1;
    }

    private DataFragment initDataFragment(String url, Product data) {
        DataFragment fragment = new DataFragment();
        fragment.setDatasourceName(amazonDatasource.getName());
        fragment.setDatasourceConfigName(amazonDatasource.getDatasourceConfigName());
        fragment.setProviderSupportType(ProviderSupportType.API);
        fragment.setAffiliatedUrl(url);
        fragment.setUrl(url);
        fragment.setLastIndexationDate(System.currentTimeMillis());
        fragment.setCreationDate(System.currentTimeMillis());
        fragment.addReferentielAttribute(ReferentielKey.GTIN, String.valueOf(data.getId()));
        return fragment;
    }

    private void logErrors(List<ErrorData> errors) {
        nullToEmpty(errors).forEach(error -> logger.warn("Amazon PA-API returned {}: {}",
                error.getCode(), error.getMessage()));
    }

    private void logApiException(ApiException exception, Product data) {
        logger.warn("Amazon PA-API failed for {} with status {}: {}", data.gtin(), exception.getCode(),
                exception.getResponseBody() == null ? exception.getMessage() : exception.getResponseBody());
    }

    private void sleepAfterCall() {
        try {
            Thread.sleep(amazonConfig.getSleepDuration().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Amazon politeness sleep interrupted");
        }
    }

    private String amazonProductUrl(String asin) {
        if (StringUtils.isBlank(asin)) {
            return "https://" + amazonConfig.getMarketplace();
        }
        return "https://" + amazonConfig.getMarketplace() + "/dp/" + asin + "?tag=" + amazonConfig.getPartnerTag();
    }

    private static DataSourceProperties resolveAmazonDatasource(DataSourceConfigService dataSourceConfigService,
            AmazonCompletionConfig config) {
        DataSourceProperties datasource = dataSourceConfigService.getDatasourceConfig(config.getDatasourceName());
        if (datasource == null) {
            datasource = dataSourceConfigService.getDatasourceConfig(DATASOURCE_NAME);
        }
        if (datasource != null) {
            return datasource;
        }

        DataSourceProperties fallback = new DataSourceProperties();
        fallback.setName(DATASOURCE_NAME);
        fallback.setDatasourceConfigName(config.getDatasourceName());
        return fallback;
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
