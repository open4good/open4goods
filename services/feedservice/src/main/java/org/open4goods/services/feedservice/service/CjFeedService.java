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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Feed service implementation for CJ Affiliate (Commission Junction).
 * <p>
 * Covers all affiliation surfaces:
 * <ul>
 *   <li>Product feeds — via the CJ GraphQL Product Feed API</li>
 *   <li>Programs — via the CJ Advertiser Lookup REST API (XML)</li>
 *   <li>Promotions — via the CJ Link Search REST API (XML), coupon/voucher type</li>
 *   <li>Transactions — via the CJ Commission Detail GraphQL API</li>
 *   <li>Tracking links — via the CJ click-through deep link pattern</li>
 * </ul>
 * Authentication uses a Bearer token issued at developers.cj.com/account/personal-access-tokens.
 * </p>
 *
 * @author open4goods
 */
public class CjFeedService extends AbstractFeedService
{
    private static final String ADVERTISER_LOOKUP_URL = "https://advertiser-lookup.api.cj.com/v3/advertiser-lookup";
    private static final String LINK_SEARCH_URL = "https://link-search.api.cj.com/v2/link-search";
    private static final String ACCOUNTS_GRAPHQL_URL = "https://accounts.api.cj.com/graphql";
    private static final String COMMISSIONS_GRAPHQL_URL = "https://commissions.api.cj.com/query";

    /** Standard CJ click-through redirect domain used in tracking links. */
    private static final String CJ_CLICK_DOMAIN = "https://www.dpbolvw.net";

    private static final int PAGE_SIZE = 100;

    // Google Shopping format column candidates (used by CJ feeds migrated to Shopping format)
    private static final String[] CJ_PRODUCT_URL_COLUMNS = {"link", "Link", "buy-url", "buy_url"};
    private static final String[] CJ_TRACKED_URL_COLUMNS = {"link", "Link", "buy-url", "buy_url"};
    private static final String[] CJ_NAME_COLUMNS = {"title", "Title", "name", "Name"};
    private static final String[] CJ_PRICE_COLUMNS = {"price", "Price", "sale_price", "sale-price"};
    private static final String[] CJ_DESCRIPTION_COLUMNS = {"description", "Description"};
    private static final String[] CJ_IMAGE_COLUMNS = {"image_link", "image link", "Image Link", "image-url", "image_url"};
    private static final String[] CJ_STOCK_COLUMNS = {"availability", "Availability", "in-stock", "in_stock"};
    private static final String[] CJ_GTIN_COLUMNS = {"gtin", "GTIN", "upc", "UPC", "isbn", "ISBN"};
    private static final String[] CJ_BRAND_COLUMNS = {"brand", "Brand", "manufacturer-name", "manufacturer_name"};
    private static final String[] CJ_MODEL_COLUMNS = {"mpn", "MPN", "manufacturer-sku", "manufacturer_sku"};
    private static final String[] CJ_SKU_COLUMNS = {"id", "Id", "sku", "SKU", "catalog-id"};

    private final String apiToken;
    private final String publisherId;
    private final String websiteId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new CjFeedService.
     *
     * @param feedConfig feed configuration
     * @param remoteFileCachingService remote file caching service
     * @param dataSourceConfigService datasource config service
     * @param serialisationService serialisation service
     * @param apiToken CJ API bearer token
     * @param publisherId CJ publisher company ID (CID)
     * @param websiteId CJ publisher website ID (PID)
     */
    public CjFeedService(FeedConfiguration feedConfig,
                         RemoteFileCachingService remoteFileCachingService,
                         DataSourceConfigService dataSourceConfigService,
                         SerialisationService serialisationService,
                         String apiToken,
                         String publisherId,
                         String websiteId)
    {
        super(feedConfig, remoteFileCachingService, dataSourceConfigService, serialisationService);
        this.apiToken = apiToken;
        this.publisherId = publisherId;
        this.websiteId = websiteId;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(cron = "${feed.cj.cron:-}")
    public void scheduledLoad()
    {
        if (!isEnabled())
        {
            logger.info("CJ feed service is disabled. Skipping scheduled load.");
            return;
        }
        logger.info("Scheduled refresh of CJ datasources initiated.");
        load();
    }

    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception
    {
        Set<DataSourceProperties> result = new HashSet<>();
        if (!isEnabled() || isBlank(apiToken) || isBlank(publisherId))
        {
            logger.info("CJ feed service is disabled or credentials are blank. Returning empty datasource set.");
            return result;
        }

        JsonNode root = queryProductFeeds();
        JsonNode feeds = findFeedArray(root);
        if (feeds == null)
        {
            logger.warn("CJ product feed payload has no supported feeds array; skipping datasource load.");
            return result;
        }

        for (JsonNode feed : feeds)
        {
            String feedUrl = firstNonBlank(
                    textOrNull(feed, "feedUrl"),
                    textOrNull(feed, "url"),
                    textOrNull(feed, "downloadUrl"));
            if (isBlank(feedUrl))
            {
                logger.warn("Skipping CJ product feed '{}' because no download URL was found.", textOrNull(feed, "feedId"));
                continue;
            }

            String feedKey = firstNonBlank(
                    textOrNull(feed, "advertiserName"),
                    textOrNull(feed, "feedTitle"),
                    textOrNull(feed, "title"),
                    textOrNull(feed, "name"),
                    textOrNull(feed, "feedId"),
                    textOrNull(feed, "catalogId"));
            if (isBlank(feedKey))
            {
                logger.warn("Skipping CJ product feed because no identifier was found.");
                continue;
            }

            DataSourceProperties ds = getVolatileDatasource(feedKey, feedConfig, feedUrl);
            applyCjCsvDefaults(ds);
            ds.setDatasourceConfigName(feedKey);
            String advertiserId = firstNonBlank(textOrNull(feed, "advertiserId"), textOrNull(feed, "advertiser_id"));
            if (!isBlank(advertiserId))
            {
                ds.setPortalUrl("https://members.cj.com/member/publisher/advertisers/detail.do?advertiserId=" + advertiserId);
            }
            result.add(ds);
        }

        logger.info("CJ datasources loaded: {} entries", result.size());
        return result;
    }

    @Override
    public String getProviderName()
    {
        return "CJ";
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
        if (!isEnabled() || isBlank(apiToken) || isBlank(publisherId))
        {
            return Collections.emptyList();
        }
        return parseAdvertisers(retrieveAllJoinedAdvertisers());
    }

    /**
     * Parses the raw advertiser XML documents into {@link AffiliationProgram} objects.
     *
     * @param documents list of raw XML documents (one per page)
     * @return list of affiliation programs
     */
    List<AffiliationProgram> parseAdvertisers(List<Document> documents)
    {
        List<AffiliationProgram> list = new ArrayList<>();
        for (Document doc : documents)
        {
            NodeList nodes = doc.getElementsByTagName("advertiser");
            for (int i = 0; i < nodes.getLength(); i++)
            {
                Element advertiser = (Element) nodes.item(i);
                AffiliationProgram program = new AffiliationProgram();
                program.setProviderName(getProviderName());
                program.setProgramId(directText(advertiser, "advertiser-id"));
                program.setAdvertiserName(directText(advertiser, "advertiser-name"));
                program.setStatus(firstNonBlank(directText(advertiser, "account-status"), "active"));
                program.setPortalUrl(directText(advertiser, "program-url"));
                program.setDescription(directText(advertiser, "description"));

                String language = directText(advertiser, "language");
                if (!isBlank(language))
                {
                    program.setCountryCodes(Set.of(language.toUpperCase()));
                }

                Element primaryCategory = firstChild(advertiser, "primary-category");
                if (primaryCategory != null)
                {
                    String parent = directText(primaryCategory, "parent");
                    String child = directText(primaryCategory, "child");
                    Set<String> categories = new HashSet<>();
                    if (!isBlank(parent))
                    {
                        categories.add(parent);
                    }
                    if (!isBlank(child))
                    {
                        categories.add(child);
                    }
                    if (!categories.isEmpty())
                    {
                        program.setCategories(categories);
                    }
                }

                list.add(program);
            }
        }
        return list;
    }

    @Override
    protected Collection<AffiliationPromotion> loadPromotionsInternal() throws Exception
    {
        if (!isEnabled() || isBlank(apiToken) || isBlank(websiteId))
        {
            return Collections.emptyList();
        }
        return parseLinkSearchPromotions(retrieveAllCouponLinks());
    }

    /**
     * Parses raw link-search XML documents into {@link AffiliationPromotion} objects.
     *
     * @param documents list of raw XML documents (one per page)
     * @return list of affiliation promotions
     */
    List<AffiliationPromotion> parseLinkSearchPromotions(List<Document> documents)
    {
        List<AffiliationPromotion> list = new ArrayList<>();
        for (Document doc : documents)
        {
            NodeList nodes = doc.getElementsByTagName("link");
            for (int i = 0; i < nodes.getLength(); i++)
            {
                Element link = (Element) nodes.item(i);
                AffiliationPromotion promotion = new AffiliationPromotion();
                promotion.setProviderName(getProviderName());
                promotion.setProgramId(directText(link, "advertiser-id"));
                promotion.setAdvertiserName(directText(link, "advertiser-name"));
                promotion.setTitle(directText(link, "link-name"));
                promotion.setDescription(directText(link, "description"));
                promotion.setVoucherCode(directText(link, "coupon-code"));
                promotion.setDiscountType(directText(link, "promotion-type"));
                promotion.setStartDate(parseMmDdYyyy(directText(link, "promotion-start-date")));
                promotion.setEndDate(parseMmDdYyyy(directText(link, "promotion-end-date")));
                promotion.setTrackingUrl(extractHref(directText(link, "link-code-html")));

                String language = directText(link, "language");
                if (!isBlank(language))
                {
                    promotion.setCountryCodes(Set.of(language.toUpperCase()));
                }
                list.add(promotion);
            }
        }
        return list;
    }

    @Override
    public Collection<AffiliationTransaction> getTransactions(Instant from, Instant to)
    {
        if (!isEnabled() || isBlank(apiToken) || isBlank(publisherId))
        {
            return Collections.emptyList();
        }
        try
        {
            return parseCommissions(retrieveCommissions(from, to));
        }
        catch (Exception e)
        {
            logger.warn("Failed to retrieve/parse CJ commissions.", e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses the raw commission JSON into {@link AffiliationTransaction} objects.
     *
     * @param root raw GraphQL response root
     * @return list of affiliation transactions
     */
    List<AffiliationTransaction> parseCommissions(JsonNode root)
    {
        JsonNode records = findCommissionRecords(root);
        if (records == null)
        {
            return Collections.emptyList();
        }

        List<AffiliationTransaction> list = new ArrayList<>();
        for (JsonNode record : records)
        {
            AffiliationTransaction transaction = new AffiliationTransaction();
            transaction.setProviderName(getProviderName());
            transaction.setTransactionId(firstNonBlank(
                    textOrNull(record, "id"),
                    textOrNull(record, "orderId"),
                    textOrNull(record, "transactionId")));
            transaction.setProgramId(firstNonBlank(
                    textOrNull(record, "advertiserId"),
                    textOrNull(record, "advertiser_id")));
            transaction.setTransactionDate(parseInstant(firstNonBlank(
                    textOrNull(record, "postingDate"),
                    textOrNull(record, "eventDate"))));
            transaction.setStatus(firstNonBlank(
                    textOrNull(record, "actionStatus"),
                    textOrNull(record, "status")));
            transaction.setCommissionAmount(parseBigDecimal(firstNonBlank(
                    textOrNull(record, "pubCommissionAmountUsd"),
                    textOrNull(record, "publisherCommissionAmount"),
                    textOrNull(record, "commissionAmount"))));
            transaction.setSaleAmount(parseBigDecimal(firstNonBlank(
                    textOrNull(record, "saleAmountPubCurrency"),
                    textOrNull(record, "orderAmount"),
                    textOrNull(record, "amount"))));
            transaction.setCurrency(firstNonBlank(
                    textOrNull(record, "pubCommissionCurrencyCode"),
                    textOrNull(record, "advertiserCommissionCurrencyCode"),
                    textOrNull(record, "currency"),
                    "USD"));
            transaction.setSubId(textOrNull(record, "publisherSiteSubId"));

            JsonNode items = record.path("items");
            if (items.isArray() && !items.isEmpty())
            {
                transaction.setProductId(textOrNull(items.get(0), "productId"));
            }
            list.add(transaction);
        }
        return list;
    }

    @Override
    public String buildTrackingLink(String programId, String targetUrl, Map<String, String> subIds)
    {
        if (isBlank(apiToken) || isBlank(websiteId) || isBlank(programId) || isBlank(targetUrl))
        {
            return targetUrl;
        }
        try
        {
            String linkId = lookupLinkId(programId);
            if (isBlank(linkId))
            {
                logger.warn("No CJ text link found for advertiser '{}'; returning raw target URL.", programId);
                return targetUrl;
            }
            StringBuilder sb = new StringBuilder(CJ_CLICK_DOMAIN)
                    .append("/click-")
                    .append(HttpUtils.urlEncode(websiteId))
                    .append("-")
                    .append(HttpUtils.urlEncode(linkId))
                    .append("?url=")
                    .append(HttpUtils.urlEncode(targetUrl));
            if (subIds != null && !subIds.isEmpty())
            {
                String sid = subIds.values().iterator().next();
                if (!isBlank(sid))
                {
                    sb.append("&sid=").append(HttpUtils.urlEncode(sid));
                }
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            logger.warn("Failed to build CJ tracking link for advertiser '{}'.", programId, e);
            return targetUrl;
        }
    }

    // ------------------------------------------------------------------ //
    // Internal helpers — API retrieval
    // ------------------------------------------------------------------ //

    private JsonNode queryProductFeeds() throws Exception
    {
        String query = """
                {"query":"{ productFeeds(publisherId: \\"%s\\") { resultList { feedId title feedUrl advertiserId advertiserName isActive count } } }"}
                """.formatted(publisherId);
        return postGraphql(ACCOUNTS_GRAPHQL_URL, query);
    }

    private List<Document> retrieveAllJoinedAdvertisers() throws Exception
    {
        List<Document> pages = new ArrayList<>();
        int page = 1;
        while (true)
        {
            String url = ADVERTISER_LOOKUP_URL
                    + "?requestor-cid=" + HttpUtils.urlEncode(publisherId)
                    + "&advertiser-ids=joined"
                    + "&records-per-page=" + PAGE_SIZE
                    + "&page-number=" + page;
            Document doc = getXml(url);
            NodeList advertisers = doc.getElementsByTagName("advertiser");
            if (advertisers.getLength() == 0)
            {
                break;
            }
            pages.add(doc);
            if (advertisers.getLength() < PAGE_SIZE)
            {
                break;
            }
            page++;
        }
        return pages;
    }

    private List<Document> retrieveAllCouponLinks() throws Exception
    {
        List<Document> pages = new ArrayList<>();
        int page = 1;
        while (true)
        {
            String url = LINK_SEARCH_URL
                    + "?website-id=" + HttpUtils.urlEncode(websiteId)
                    + "&advertiser-ids=joined"
                    + "&link-type=Text"
                    + "&promotion-type=coupon"
                    + "&records-per-page=" + PAGE_SIZE
                    + "&page-number=" + page;
            Document doc = getXml(url);
            NodeList links = doc.getElementsByTagName("link");
            if (links.getLength() == 0)
            {
                break;
            }
            pages.add(doc);
            if (links.getLength() < PAGE_SIZE)
            {
                break;
            }
            page++;
        }
        return pages;
    }

    private JsonNode retrieveCommissions(Instant from, Instant to) throws Exception
    {
        String sinceDate = DateTimeFormatter.ISO_INSTANT.format(from);
        String beforeDate = DateTimeFormatter.ISO_INSTANT.format(to);
        String query = """
                {"query":"{ publisherCommissions(forPublishers: [\\"%s\\"], sincePostingDate: \\"%s\\", beforePostingDate: \\"%s\\") { count payloadComplete records { id actionTrackerName advertiserId advertiserName postingDate pubCommissionAmountUsd pubCommissionCurrencyCode saleAmountPubCurrency orderId websiteId publisherSiteSubId actionStatus items { productId quantity totalCommissionPubCurrency } } maxCommissionId } }"}
                """.formatted(publisherId, sinceDate, beforeDate);
        return postGraphql(COMMISSIONS_GRAPHQL_URL, query);
    }

    private String lookupLinkId(String advertiserId) throws Exception
    {
        String url = LINK_SEARCH_URL
                + "?website-id=" + HttpUtils.urlEncode(websiteId)
                + "&advertiser-ids=" + HttpUtils.urlEncode(advertiserId)
                + "&link-type=Text"
                + "&records-per-page=1"
                + "&page-number=1";
        Document doc = getXml(url);
        NodeList links = doc.getElementsByTagName("link");
        if (links.getLength() == 0)
        {
            return null;
        }
        return directText((Element) links.item(0), "link-id");
    }

    // ------------------------------------------------------------------ //
    // Internal helpers — HTTP
    // ------------------------------------------------------------------ //

    private JsonNode postGraphql(String endpoint, String body) throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
        {
            throw new IllegalStateException("CJ GraphQL call failed for " + HttpUtils.safeEndpointForLogs(endpoint)
                    + " with HTTP status " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private Document getXml(String endpoint) throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiToken)
                .header("Accept", "application/xml, text/xml")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
        {
            throw new IllegalStateException("CJ REST API call failed for " + HttpUtils.safeEndpointForLogs(endpoint)
                    + " with HTTP status " + response.statusCode());
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new java.io.StringReader(response.body())));
    }

    // ------------------------------------------------------------------ //
    // Internal helpers — JSON traversal
    // ------------------------------------------------------------------ //

    private JsonNode findFeedArray(JsonNode root)
    {
        if (root == null)
        {
            return null;
        }
        // GraphQL data wrapper
        JsonNode data = root.path("data");
        if (!data.isMissingNode())
        {
            root = data;
        }
        // Various field names used across CJ GraphQL API versions
        for (String field : new String[]{"productFeeds", "shoppingProductFeeds", "catalogs", "feeds"})
        {
            JsonNode node = root.path(field);
            if (!node.isMissingNode())
            {
                if (node.isArray())
                {
                    return node;
                }
                for (String inner : new String[]{"resultList", "items", "results", "list"})
                {
                    JsonNode list = node.path(inner);
                    if (list.isArray())
                    {
                        return list;
                    }
                }
            }
        }
        if (root.isArray())
        {
            return root;
        }
        return null;
    }

    private JsonNode findCommissionRecords(JsonNode root)
    {
        if (root == null)
        {
            return null;
        }
        JsonNode data = root.path("data");
        if (!data.isMissingNode())
        {
            root = data;
        }
        JsonNode commissions = root.path("publisherCommissions");
        if (!commissions.isMissingNode())
        {
            JsonNode records = commissions.path("records");
            if (records.isArray())
            {
                return records;
            }
        }
        if (root.isArray())
        {
            return root;
        }
        return null;
    }

    // ------------------------------------------------------------------ //
    // Internal helpers — XML traversal
    // ------------------------------------------------------------------ //

    private String directText(Element parent, String tagName)
    {
        if (parent == null)
        {
            return null;
        }
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0)
        {
            return null;
        }
        String text = nodes.item(0).getTextContent();
        return isBlank(text) ? null : text.trim();
    }

    private Element firstChild(Element parent, String tagName)
    {
        if (parent == null)
        {
            return null;
        }
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0)
        {
            return null;
        }
        return (Element) nodes.item(0);
    }

    // ------------------------------------------------------------------ //
    // Internal helpers — datasource defaults
    // ------------------------------------------------------------------ //

    /**
     * Applies CJ-standard column mappings to a freshly created datasource, leaving any existing
     * user-configured mappings untouched.
     *
     * @param datasource the datasource to configure
     */
    private void applyCjCsvDefaults(DataSourceProperties datasource)
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
            csv.setUrl(firstHeader(CJ_PRODUCT_URL_COLUMNS));
        }
        if (isBlank(csv.getAffiliatedUrl()))
        {
            csv.setAffiliatedUrl(firstHeader(CJ_TRACKED_URL_COLUMNS));
        }
        if (isBlank(csv.getName()))
        {
            csv.setName(firstHeader(CJ_NAME_COLUMNS));
        }
        addAllIfEmpty(csv.getPrice(), csv::setPrice, CJ_PRICE_COLUMNS);
        addAllIfEmpty(csv.getDescription(), csv::setDescription, CJ_DESCRIPTION_COLUMNS);
        addAllIfEmpty(csv.getImage(), csv::setImage, CJ_IMAGE_COLUMNS);
        addAllIfEmpty(csv.getInStock(), csv::setInStock, CJ_STOCK_COLUMNS);
        addReferentielDefaults(csv, ReferentielKey.GTIN, CJ_GTIN_COLUMNS);
        addReferentielDefaults(csv, ReferentielKey.BRAND, CJ_BRAND_COLUMNS);
        addReferentielDefaults(csv, ReferentielKey.MODEL, CJ_MODEL_COLUMNS);
        addAllIfEmpty(csv.getSku(), csv::setSku, CJ_SKU_COLUMNS);
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

    // ------------------------------------------------------------------ //
    // Internal helpers — misc
    // ------------------------------------------------------------------ //

    private boolean isEnabled()
    {
        return feedConfig != null && feedConfig.getCj() != null && feedConfig.getCj().isEnabled();
    }

    /**
     * Extracts the href attribute value from a raw HTML anchor string such as
     * {@code <a href="https://...">click</a>}.
     *
     * @param html raw link-code-html value
     * @return href value, or {@code null} if not found
     */
    String extractHref(String html)
    {
        if (isBlank(html))
        {
            return null;
        }
        int start = html.indexOf("href=\"");
        if (start < 0)
        {
            start = html.indexOf("href='");
        }
        if (start < 0)
        {
            return null;
        }
        char quote = html.charAt(start + 5);
        int valueStart = start + 6;
        int end = html.indexOf(quote, valueStart);
        if (end < 0)
        {
            return null;
        }
        String href = html.substring(valueStart, end).trim();
        return isBlank(href) ? null : href;
    }

    /**
     * Parses a CJ date in MM/DD/YYYY format (used in the Link Search API).
     *
     * @param value date string
     * @return parsed date, or {@code null} on failure
     */
    LocalDate parseMmDdYyyy(String value)
    {
        if (isBlank(value))
        {
            return null;
        }
        try
        {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        }
        catch (DateTimeParseException ignored)
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

    private static boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
