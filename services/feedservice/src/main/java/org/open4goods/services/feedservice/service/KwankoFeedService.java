package org.open4goods.services.feedservice.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.util.HttpUtils;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.affiliation.AffiliationCapability;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.model.price.Currency;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.scheduling.annotation.Scheduled;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Feed service implementation for Kwanko.
 * 
 * @author open4goods
 */
public class KwankoFeedService extends AbstractFeedService
{
    private static final String BASE_URL = "https://api.kwanko.com";
    private static final String PRODUCT_FEED_AD_TYPE = "product_feed";
    private static final String VOUCHER_CODE_AD_TYPE = "voucher_code";
    private static final String[] KWANKO_PRODUCT_URL_COLUMNS = {"product_url", "Product page URL", "Product URL", "url", "URL", "link", "merchant_deep_link"};
    private static final String[] KWANKO_TRACKED_URL_COLUMNS = {"tracking_url", "tracked_url", "deeplink", "tracking_link", "merchant_deep_link"};
    private static final String[] KWANKO_NAME_COLUMNS = {"product_name", "Product name", "Name", "name", "title", "Title", "product_title"};
    private static final String[] KWANKO_PRICE_COLUMNS = {"price", "Current price", "product_price", "sale_price", "search_price", "price_vat_inc"};
    private static final String[] KWANKO_DESCRIPTION_COLUMNS = {"description", "Product description", "product_description", "short_description", "long_description"};
    private static final String[] KWANKO_IMAGE_COLUMNS = {"image_url", "URL related to the big image", "Image URL", "image", "picture", "picture_url", "product_image", "large_image"};
    private static final String[] KWANKO_STOCK_COLUMNS = {"availability", "Stock status", "in_stock", "stock", "stock_status"};
    private static final String[] KWANKO_GTIN_COLUMNS = {"gtin", "GTIN", "ean", "EAN", "ean13", "EAN13", "isbn", "ISBN", "barcode", "product_gtin"};
    private static final String[] KWANKO_BRAND_COLUMNS = {"brand", "Brand", "brand_name", "Brand name", "manufacturer", "Manufacturer"};
    private static final String[] KWANKO_MODEL_COLUMNS = {"mpn", "MPN", "model", "Model", "sku", "SKU", "reference", "Internal reference", "Manufacturer reference", "product_reference"};

    private final String token;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KwankoFeedService(FeedConfiguration feedConfig,
                             RemoteFileCachingService remoteFileCachingService,
                             DataSourceConfigService dataSourceConfigService,
                             SerialisationService serialisationService,
                             String token)
    {
        super(feedConfig, remoteFileCachingService, dataSourceConfigService, serialisationService);
        this.token = token;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(cron = "${feed.kwanko.cron:-}")
    public void scheduledLoad()
    {
        if (!isEnabled())
        {
            logger.info("Kwanko feed service is disabled. Skipping scheduled load.");
            return;
        }
        logger.info("Scheduled refresh of Kwanko datasources initiated.");
        load();
    }

    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception
    {
        Set<DataSourceProperties> result = new HashSet<>();
        if (!isEnabled() || isBlank(token))
        {
            logger.info("Kwanko feed service is disabled or token is blank. Returning empty datasource set.");
            return result;
        }

        JsonNode root = retrieveKwankoAds(PRODUCT_FEED_AD_TYPE);
        JsonNode adsNode = arrayNode(root, "ads", "data", "items");
        if (adsNode == null)
        {
            logger.warn("Kwanko product feed payload has no supported ads array.");
            return result;
        }

        for (JsonNode ad : adsNode)
        {
            String feedUrl = firstTrackedValue(ad.path("tracked_material_per_websites"), "product_feed");
            if (isBlank(feedUrl))
            {
                logger.warn("Skipping Kwanko product feed '{}' because no product_feed URL was found.", textOrNull(ad, "id"));
                continue;
            }

            String feedKey = firstNonBlank(textOrNull(ad, "name"), textOrNull(ad, "id"));
            if (isBlank(feedKey))
            {
                logger.warn("Skipping Kwanko product feed because no id/name was found.");
                continue;
            }

            DataSourceProperties ds = getVolatileDatasource(feedKey, feedConfig, feedUrl);
            applyKwankoCsvDefaults(ds);
            JsonNode campaign = ad.path("campaign");
            ds.setDatasourceConfigName(firstNonBlank(textOrNull(campaign, "name"), feedKey));
            ds.setName(extractNameAndTld(textOrNull(campaign, "url")));
            ds.setPortalUrl(textOrNull(campaign, "url"));
            ds.setLogo(textOrNull(campaign, "logo"));
            ds.setDescription(textOrNull(campaign, "description"));
            result.add(ds);
        }

        logger.info("Kwanko datasources loaded: {} entries", result.size());
        return result;
    }

    @Override
    public String getProviderName()
    {
        return "Kwanko";
    }

    @Override
    public Set<AffiliationCapability> getCapabilities()
    {
        return Set.of(
            AffiliationCapability.FEEDS,
            AffiliationCapability.PROGRAMS,
            AffiliationCapability.PROMOTIONS,
            AffiliationCapability.TRANSACTIONS,
            AffiliationCapability.TRACKING
        );
    }

    @Override
    protected Collection<AffiliationProgram> loadProgramsInternal() throws Exception
    {
        if (!isEnabled() || isBlank(token))
        {
            return Collections.emptyList();
        }
        return parseKwankoPrograms(retrieveKwankoCampaigns());
    }

    Collection<AffiliationProgram> parseKwankoPrograms(JsonNode root)
    {
        JsonNode campaignsNode = arrayNode(root, "campaigns", "data", "items");
        if (campaignsNode == null)
        {
            return Collections.emptyList();
        }

        List<AffiliationProgram> list = new ArrayList<>();
        for (JsonNode campaign : campaignsNode)
        {
            AffiliationProgram program = new AffiliationProgram();
            program.setProviderName(getProviderName());
            program.setProgramId(textOrNull(campaign, "id"));
            program.setAdvertiserName(textOrNull(campaign, "name"));
            program.setStatus(firstNonBlank(textOrNull(campaign, "state"), "active"));
            program.setPortalUrl(textOrNull(campaign, "url"));
            program.setLogoUrl(textOrNull(campaign, "logo"));
            program.setDescription(textOrNull(campaign, "description"));
            program.setCurrency(textOrNull(campaign, "currency"));
            program.setCookieDurationDays(firstInteger(campaign, "postclick_default", "postclick"));
            program.setCategories(stringSet(campaign.path("iab_categories")));
            program.setCountryCodes(countriesFromLanguages(campaign.path("languages")));

            JsonNode payout = campaign.path("payout");
            if (!payout.isMissingNode())
            {
                program.setCookieDurationDays(firstInteger(payout, "postclick", "postclick_default"));
            }

            list.add(program);
        }
        return list;
    }

    @Override
    protected Collection<AffiliationPromotion> loadPromotionsInternal() throws Exception
    {
        if (!isEnabled() || isBlank(token))
        {
            return Collections.emptyList();
        }
        return parseKwankoPromotions(retrieveKwankoAds(VOUCHER_CODE_AD_TYPE));
    }

    Collection<AffiliationPromotion> parseKwankoPromotions(JsonNode root)
    {
        JsonNode adsNode = arrayNode(root, "ads", "data", "items");
        if (adsNode == null)
        {
            return Collections.emptyList();
        }

        List<AffiliationPromotion> list = new ArrayList<>();
        for (JsonNode ad : adsNode)
        {
            JsonNode campaign = ad.path("campaign");
            AffiliationPromotion promotion = new AffiliationPromotion();
            promotion.setProviderName(getProviderName());
            promotion.setProgramId(textOrNull(campaign, "id"));
            promotion.setAdvertiserName(textOrNull(campaign, "name"));
            promotion.setTitle(textOrNull(ad, "name"));
            promotion.setDescription(textOrNull(ad, "description"));
            promotion.setVoucherCode(textOrNull(ad, "code"));
            promotion.setDiscountType(textOrNull(ad, "type"));
            promotion.setStartDate(parseLocalDate(textOrNull(ad.path("validity_date"), "start")));
            promotion.setEndDate(parseLocalDate(textOrNull(ad.path("validity_date"), "end")));
            promotion.setTrackingUrl(firstTrackedUrl(ad.path("tracked_material_per_websites"), "click"));
            promotion.setLandingUrl(textOrNull(campaign, "url"));
            promotion.setConditions(firstNonBlank(textOrNull(ad, "conditions"), textOrNull(ad, "group_name")));
            promotion.setCountryCodes(countriesFromLanguages(campaign.path("languages")));
            list.add(promotion);
        }
        return list;
    }

    @Override
    public Collection<AffiliationTransaction> getTransactions(Instant from, Instant to)
    {
        if (!isEnabled() || isBlank(token))
        {
            return Collections.emptyList();
        }
        try
        {
            return parseKwankoTransactions(retrieveKwankoConversions(from, to));
        }
        catch (Exception e)
        {
            logger.warn("Failed to retrieve/parse Kwanko conversions.", e);
            return Collections.emptyList();
        }
    }

    Collection<AffiliationTransaction> parseKwankoTransactions(JsonNode root)
    {
        JsonNode conversionsNode = arrayNode(root, "conversions", "data", "items");
        if (conversionsNode == null)
        {
            return Collections.emptyList();
        }

        List<AffiliationTransaction> list = new ArrayList<>();
        for (JsonNode conversion : conversionsNode)
        {
            JsonNode campaign = conversion.path("campaign");
            JsonNode websitePerLanguage = firstArrayElement(conversion.path("websites_per_language"));
            JsonNode earnings = firstExisting(conversion, "earnings", "spending");
            if (earnings.isEmpty())
            {
                earnings = firstExisting(websitePerLanguage, "earnings", "spending");
            }
            AffiliationTransaction transaction = new AffiliationTransaction();
            transaction.setProviderName(getProviderName());
            transaction.setTransactionId(firstNonBlank(textOrNull(conversion, "unique_conversion_id"), textOrNull(conversion, "kwanko_id")));
            transaction.setProgramId(textOrNull(campaign, "id"));
            transaction.setTransactionDate(parseInstant(textOrNull(conversion, "completed_at")));
            transaction.setStatus(textOrNull(conversion, "state"));
            transaction.setCommissionAmount(parseBigDecimal(firstNonBlank(textOrNull(earnings, "value"), textOrNull(earnings, "countervalue"))));
            transaction.setCurrency(firstNonBlank(textOrNull(campaign, "currency"), textOrNull(conversion, "countervalue_currency")));
            transaction.setSubId(firstArgsite(conversion.path("websites_per_language")));
            list.add(transaction);
        }
        return list;
    }

    @Override
    public String buildTrackingLink(String programId, String targetUrl, Map<String, String> subIds)
    {
        if (isBlank(token) || isBlank(programId) || isBlank(targetUrl))
        {
            return targetUrl;
        }
        try
        {
            JsonNode root = retrieveKwankoAds("deeplink", programId);
            JsonNode adsNode = arrayNode(root, "ads", "data", "items");
            if (adsNode == null)
            {
                return targetUrl;
            }
            for (JsonNode ad : adsNode)
            {
                String clickPattern = firstTrackedUrl(ad.path("tracked_material_per_websites"), "click");
                if (!isBlank(clickPattern))
                {
                    return clickPattern.replace("{XXX}", HttpUtils.urlEncode(targetUrl));
                }
            }
        }
        catch (Exception e)
        {
            logger.warn("Failed to build Kwanko tracking link from deeplink ad.", e);
        }
        return targetUrl;
    }

    private boolean isEnabled()
    {
        return feedConfig != null && feedConfig.getKwanko() != null && feedConfig.getKwanko().isEnabled();
    }

    /**
     * Applies Kwanko V4 product-feed defaults documented for publisher ads while preserving
     * any datasource-specific mapping already configured in YAML.
     *
     * @param datasource datasource generated from the product-feed ad
     */
    private void applyKwankoCsvDefaults(DataSourceProperties datasource)
    {
        if (datasource.getCsvDatasource() == null)
        {
            datasource.setCsvDatasource(new CsvDataSourceProperties());
        }
        CsvDataSourceProperties csv = datasource.getCsvDatasource();
        csv.setImportAllAttributes(true);
        if (csv.getCurrency() == null)
        {
            csv.setCurrency(Currency.EUR);
        }
        if (isBlank(csv.getUrl()))
        {
            csv.setUrl(firstHeader(KWANKO_PRODUCT_URL_COLUMNS));
        }
        if (isBlank(csv.getAffiliatedUrl()))
        {
            csv.setAffiliatedUrl(firstHeader(KWANKO_TRACKED_URL_COLUMNS));
        }
        if (isBlank(csv.getName()))
        {
            csv.setName(firstHeader(KWANKO_NAME_COLUMNS));
        }
        addAllIfEmpty(csv.getPrice(), csv::setPrice, KWANKO_PRICE_COLUMNS);
        addAllIfEmpty(csv.getDescription(), csv::setDescription, KWANKO_DESCRIPTION_COLUMNS);
        addAllIfEmpty(csv.getImage(), csv::setImage, KWANKO_IMAGE_COLUMNS);
        addAllIfEmpty(csv.getInStock(), csv::setInStock, KWANKO_STOCK_COLUMNS);
        addReferentielDefaults(csv, ReferentielKey.GTIN, KWANKO_GTIN_COLUMNS);
        addReferentielDefaults(csv, ReferentielKey.BRAND, KWANKO_BRAND_COLUMNS);
        addReferentielDefaults(csv, ReferentielKey.MODEL, KWANKO_MODEL_COLUMNS);
    }

    private String firstHeader(String[] headers)
    {
        return headers.length == 0 ? null : headers[0];
    }

    private void addAllIfEmpty(Set<String> existing, java.util.function.Consumer<Set<String>> setter, String... values)
    {
        if (existing == null || existing.isEmpty())
        {
            setter.accept(new LinkedHashSet<>(List.of(values)));
        }
    }

    private void addReferentielDefaults(CsvDataSourceProperties csv, ReferentielKey key, String... values)
    {
        Set<String> existing = csv.getReferentiel().computeIfAbsent(key, ignored -> new LinkedHashSet<>());
        if (existing.isEmpty())
        {
            existing.addAll(List.of(values));
        }
    }

    private JsonNode retrieveKwankoCampaigns() throws Exception
    {
        return readCachedJson(BASE_URL + "/publishers/campaigns?campaign_states=active");
    }

    private JsonNode retrieveKwankoAds(String adType) throws Exception
    {
        return retrieveKwankoAds(adType, null);
    }

    private JsonNode retrieveKwankoAds(String adType, String programId) throws Exception
    {
        String endpoint = BASE_URL + "/publishers/ads?ad_types=" + HttpUtils.urlEncode(adType)
                + (isBlank(programId) ? "" : "&campaigns=" + HttpUtils.urlEncode(programId));
        return readCachedJson(endpoint);
    }

    private JsonNode retrieveKwankoConversions(Instant from, Instant to) throws Exception
    {
        String endpoint = BASE_URL + "/publishers/conversions"
                + "?completion_date_from=" + HttpUtils.urlEncode(DateTimeFormatter.ISO_INSTANT.format(from))
                + "&completion_date_to=" + HttpUtils.urlEncode(DateTimeFormatter.ISO_INSTANT.format(to));
        return readCachedJson(endpoint);
    }

    private JsonNode readCachedJson(String endpoint) throws Exception
    {
        // Kwanko authentication is header-based; the generic URL cache cannot attach the access-token header.
        return getJson(endpoint);
    }

    private JsonNode getJson(String endpoint) throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .header("access-token", token)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
        {
            throw new IllegalStateException("Kwanko API call failed for " + HttpUtils.safeEndpointForLogs(endpoint)
                    + " with HTTP status " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private JsonNode arrayNode(JsonNode root, String... fields)
    {
        if (root == null)
        {
            return null;
        }
        if (root.isArray())
        {
            return root;
        }
        for (String field : fields)
        {
            JsonNode candidate = root.path(field);
            if (!candidate.isMissingNode() && candidate.isArray())
            {
                return candidate;
            }
        }
        return null;
    }

    private String firstTrackedUrl(JsonNode trackedMaterials, String field)
    {
        if (trackedMaterials == null || !trackedMaterials.isArray())
        {
            return null;
        }
        for (JsonNode trackedMaterial : trackedMaterials)
        {
            String value = textOrNull(trackedMaterial.path("urls"), field);
            if (!isBlank(value))
            {
                return value;
            }
        }
        return null;
    }

    private String firstTrackedValue(JsonNode trackedMaterials, String field)
    {
        if (trackedMaterials == null || !trackedMaterials.isArray())
        {
            return null;
        }
        for (JsonNode trackedMaterial : trackedMaterials)
        {
            String value = firstNonBlank(textOrNull(trackedMaterial, field), textOrNull(trackedMaterial.path("urls"), field));
            if (!isBlank(value))
            {
                return value;
            }
        }
        return null;
    }

    private Set<String> countriesFromLanguages(JsonNode languages)
    {
        Set<String> result = new HashSet<>();
        if (languages == null || !languages.isArray())
        {
            return result;
        }
        for (JsonNode language : languages)
        {
            String value = language.asText();
            if (!isBlank(value) && value.contains("_"))
            {
                result.add(value.substring(value.indexOf('_') + 1).toUpperCase());
            }
        }
        return result;
    }

    private Set<String> stringSet(JsonNode node)
    {
        Set<String> result = new HashSet<>();
        if (node == null || !node.isArray())
        {
            return result;
        }
        for (JsonNode value : node)
        {
            if (!isBlank(value.asText()))
            {
                result.add(value.asText());
            }
        }
        return result;
    }

    private Integer firstInteger(JsonNode node, String... fields)
    {
        for (String field : fields)
        {
            JsonNode value = node.path(field);
            if (value.isInt())
            {
                return value.asInt();
            }
            if (!value.isMissingNode() && !value.isNull())
            {
                try
                {
                    return Integer.valueOf(value.asText().trim());
                }
                catch (Exception ignored) {}
            }
        }
        return null;
    }

    private JsonNode firstExisting(JsonNode node, String... fields)
    {
        if (node == null)
        {
            return objectMapper.createObjectNode();
        }
        for (String field : fields)
        {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull())
            {
                return value;
            }
        }
        return objectMapper.createObjectNode();
    }

    private JsonNode firstArrayElement(JsonNode node)
    {
        if (node == null || !node.isArray() || node.isEmpty())
        {
            return objectMapper.createObjectNode();
        }
        return node.get(0);
    }

    private String firstArgsite(JsonNode websitesPerLanguage)
    {
        if (websitesPerLanguage == null || !websitesPerLanguage.isArray() || websitesPerLanguage.isEmpty())
        {
            return null;
        }
        for (JsonNode websitePerLanguage : websitesPerLanguage)
        {
            JsonNode argsites = websitePerLanguage.path("argsites");
            String value = firstNonBlank(textOrNull(argsites, "argsite"), textOrNull(argsites, "argsite1"), textOrNull(argsites, "argsite2"));
            if (!isBlank(value))
            {
                return value;
            }
        }
        return null;
    }

    private LocalDate parseLocalDate(String value)
    {
        if (isBlank(value))
        {
            return null;
        }
        try
        {
            return LocalDate.parse(value.trim().substring(0, 10));
        }
        catch (DateTimeParseException | StringIndexOutOfBoundsException ignored)
        {
            return null;
        }
    }

    private Instant parseInstant(String value)
    {
        if (isBlank(value))
        {
            return null;
        }
        try
        {
            return Instant.parse(value.trim());
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value)
    {
        if (isBlank(value))
        {
            return null;
        }
        try
        {
            return new BigDecimal(value.trim().replace(",", "."));
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private String textOrNull(JsonNode node, String field)
    {
        if (node == null || field == null)
        {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull())
        {
            return null;
        }
        String s = v.asText();
        return isBlank(s) ? null : s;
    }

    private String firstNonBlank(String... values)
    {
        if (values == null)
        {
            return null;
        }
        for (String value : values)
        {
            if (!isBlank(value))
            {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
