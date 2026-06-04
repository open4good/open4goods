package org.open4goods.services.feedservice.service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.util.HttpUtils;
import org.open4goods.model.affiliation.AffiliationCapability;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Feed service implementation for TradeTracker.
 * <p>
 * TradeTracker exposes affiliate data through a SOAP API. This implementation keeps the network
 * calls credential-gated so local environments without TradeTracker credentials return empty data
 * instead of failing startup.
 * </p>
 *
 * @author open4goods
 */
public class TradeTrackerFeedService extends AbstractFeedService
{
    private static final String SOAP_ENDPOINT = "https://ws.tradetracker.com/soap/affiliate";
    private static final String SOAP_NAMESPACE = "https://ws.tradetracker.com/soap/affiliate";
    private static final String DEFAULT_LOCALE = "fr_FR";
    private static final int DEFAULT_LIMIT = 500;

    private final String customerId;
    private final String apiKey;
    private final HttpClient httpClient;

    public TradeTrackerFeedService(FeedConfiguration feedConfig,
                                   RemoteFileCachingService remoteFileCachingService,
                                   DataSourceConfigService dataSourceConfigService,
                                   SerialisationService serialisationService,
                                   String customerId,
                                   String apiKey)
    {
        super(feedConfig, remoteFileCachingService, dataSourceConfigService, serialisationService);
        this.customerId = customerId;
        this.apiKey = apiKey;
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder().cookieHandler(cookieManager).build();
    }

    @Scheduled(cron = "${feed.tradetracker.cron:-}")
    public void scheduledLoad()
    {
        if (!isEnabled())
        {
            logger.info("TradeTracker feed service is disabled. Skipping scheduled load.");
            return;
        }
        logger.info("Scheduled refresh of TradeTracker datasources initiated.");
        load();
    }

    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception
    {
        Set<DataSourceProperties> result = new HashSet<>();
        if (!canCallApi())
        {
            logger.info("TradeTracker feed service is disabled or credentials are blank. Returning empty datasource set.");
            return result;
        }

        authenticate();
        for (String affiliateSiteId : retrieveAffiliateSiteIds())
        {
            Document root = call("getFeeds", """
                    <affiliateSiteID>%s</affiliateSiteID>
                    <options>
                        <assignmentStatus>accepted</assignmentStatus>
                        <limit>%d</limit>
                    </options>
                    """.formatted(escapeXml(affiliateSiteId), DEFAULT_LIMIT));
            for (Element feed : containerItems(root, "feeds"))
            {
                String feedUrl = toCsvUrl(directText(feed, "URL"));
                if (isBlank(feedUrl))
                {
                    continue;
                }
                Element campaign = firstDirectChild(feed, "campaign");
                String campaignName = directText(campaign, "name");
                String feedName = directText(feed, "name");
                String feedKey = firstNonBlank(campaignName, feedName, directText(feed, "ID"));
                if (isBlank(feedKey))
                {
                    logger.warn("Skipping TradeTracker feed because no campaign/feed identifier was found.");
                    continue;
                }

                DataSourceProperties ds = getVolatileDatasource(feedKey, feedConfig, feedUrl);
                ds.setDatasourceConfigName(firstNonBlank(campaignName, feedName, feedKey));
                ds.setPortalUrl(directText(campaign, "URL"));
                ds.setName(extractNameAndTld(ds.getPortalUrl()));
                Element info = firstDirectChild(campaign, "info");
                if (info != null)
                {
                    ds.setLogo(directText(info, "imageURL"));
                    ds.setAffiliatedPortalUrl(directText(info, "trackingURL"));
                    ds.setDescription(firstNonBlank(directText(info, "campaignDescription"), directText(info, "shopDescription")));
                }
                result.add(ds);
            }
        }
        logger.info("TradeTracker datasources loaded: {} entries", result.size());
        return result;
    }

    @Override
    public String getProviderName()
    {
        return "TradeTracker";
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
        if (!canCallApi())
        {
            return List.of();
        }
        authenticate();
        Map<String, AffiliationProgram> byId = new java.util.LinkedHashMap<>();
        for (String affiliateSiteId : retrieveAffiliateSiteIds())
        {
            Document root = call("getCampaigns", """
                    <affiliateSiteID>%s</affiliateSiteID>
                    <options>
                        <assignmentStatus>accepted</assignmentStatus>
                        <limit>%d</limit>
                    </options>
                    """.formatted(escapeXml(affiliateSiteId), DEFAULT_LIMIT));
            for (AffiliationProgram p : parseTradeTrackerPrograms(root))
            {
                byId.putIfAbsent(p.getProgramId(), p);
            }
        }
        return byId.values();
    }

    Collection<AffiliationProgram> parseTradeTrackerPrograms(Document root)
    {
        List<AffiliationProgram> list = new ArrayList<>();
        for (Element campaign : containerItems(root, "campaigns"))
        {
            AffiliationProgram program = new AffiliationProgram();
            program.setProviderName(getProviderName());
            program.setProgramId(directText(campaign, "ID"));
            program.setAdvertiserName(directText(campaign, "name"));
            program.setPortalUrl(directText(campaign, "URL"));

            Element info = firstDirectChild(campaign, "info");
            if (info != null)
            {
                program.setStatus(directText(info, "assignmentStatus"));
                program.setLogoUrl(directText(info, "imageURL"));
                program.setTrackingUrl(directText(info, "trackingURL"));
                program.setDescription(firstNonBlank(directText(info, "campaignDescription"), directText(info, "shopDescription")));
                program.setCookieDurationDays(parseDurationDays(directText(info, "clickToConversion")));
                Element category = firstDirectChild(info, "category");
                String categoryName = directText(category, "name");
                if (!isBlank(categoryName))
                {
                    program.setCategories(Set.of(categoryName));
                }
                Element commission = firstDirectChild(info, "commission");
                program.setClickCommission(parseBigDecimal(firstNonBlank(directText(commission, "clickCommission"), directText(commission, "click"))));
                program.setLeadCommission(parseBigDecimal(firstNonBlank(directText(commission, "leadCommission"), directText(commission, "lead"))));
                program.setSaleCommissionPercent(parseBigDecimal(firstNonBlank(directText(commission, "saleCommissionVariable"), directText(commission, "sale"))));
            }
            list.add(program);
        }
        return list;
    }

    @Override
    protected Collection<AffiliationPromotion> loadPromotionsInternal() throws Exception
    {
        if (!canCallApi())
        {
            return List.of();
        }
        authenticate();
        List<AffiliationPromotion> result = new ArrayList<>();
        for (String affiliateSiteId : retrieveAffiliateSiteIds())
        {
            Document root = call("getMaterialIncentiveVoucherItems", """
                    <affiliateSiteID>%s</affiliateSiteID>
                    <materialOutputType>html</materialOutputType>
                    <options>
                        <limit>%d</limit>
                    </options>
                    """.formatted(escapeXml(affiliateSiteId), DEFAULT_LIMIT));
            result.addAll(parseTradeTrackerPromotions(root));
        }
        return result;
    }

    Collection<AffiliationPromotion> parseTradeTrackerPromotions(Document root)
    {
        List<AffiliationPromotion> list = new ArrayList<>();
        for (Element material : containerItems(root, "materialItems"))
        {
            Element campaign = firstDirectChild(material, "campaign");
            AffiliationPromotion promotion = new AffiliationPromotion();
            promotion.setProviderName(getProviderName());
            promotion.setProgramId(directText(campaign, "ID"));
            promotion.setAdvertiserName(directText(campaign, "name"));
            promotion.setTitle(directText(material, "name"));
            promotion.setDescription(directText(material, "description"));
            promotion.setVoucherCode(firstNonBlank(directText(material, "voucherCode"), directText(material, "code")));
            promotion.setStartDate(parseLocalDate(directText(material, "validFromDate")));
            promotion.setEndDate(parseLocalDate(directText(material, "validToDate")));
            promotion.setLandingUrl(directText(campaign, "URL"));
            Element campaignInfo = firstDirectChild(campaign, "info");
            promotion.setTrackingUrl(directText(campaignInfo, "trackingURL"));
            promotion.setConditions(directText(material, "conditions"));
            if (!isBlank(directText(material, "discountFixed")))
            {
                promotion.setDiscountType("fixed");
                promotion.setDiscountValue(parseBigDecimal(directText(material, "discountFixed")));
            }
            else if (!isBlank(directText(material, "discountVariable")))
            {
                promotion.setDiscountType("percent");
                promotion.setDiscountValue(parseBigDecimal(directText(material, "discountVariable")));
            }
            list.add(promotion);
        }
        return list;
    }

    @Override
    public Collection<AffiliationTransaction> getTransactions(Instant from, Instant to)
    {
        if (!canCallApi())
        {
            return List.of();
        }
        try
        {
            authenticate();
            Map<String, AffiliationTransaction> byId = new java.util.LinkedHashMap<>();
            for (String affiliateSiteId : retrieveAffiliateSiteIds())
            {
                Document root = call("getConversionTransactions", """
                        <affiliateSiteID>%s</affiliateSiteID>
                        <options>
                            <registrationDateFrom>%s</registrationDateFrom>
                            <registrationDateTo>%s</registrationDateTo>
                            <limit>%d</limit>
                        </options>
                        """.formatted(escapeXml(affiliateSiteId), DateTimeFormatter.ISO_INSTANT.format(from), DateTimeFormatter.ISO_INSTANT.format(to), DEFAULT_LIMIT));
                for (AffiliationTransaction t : parseTradeTrackerTransactions(root))
                {
                    byId.putIfAbsent(t.getTransactionId(), t);
                }
            }
            return byId.values();
        }
        catch (Exception e)
        {
            logger.warn("Failed to retrieve/parse TradeTracker transactions.", e);
            return List.of();
        }
    }

    Collection<AffiliationTransaction> parseTradeTrackerTransactions(Document root)
    {
        List<AffiliationTransaction> list = new ArrayList<>();
        for (Element conversion : containerItems(root, "conversionTransactions"))
        {
            Element campaign = firstDirectChild(conversion, "campaign");
            AffiliationTransaction transaction = new AffiliationTransaction();
            transaction.setProviderName(getProviderName());
            transaction.setTransactionId(directText(conversion, "ID"));
            transaction.setProgramId(directText(campaign, "ID"));
            transaction.setTransactionDate(parseInstant(directText(conversion, "registrationDate")));
            transaction.setStatus(directText(conversion, "transactionStatus"));
            transaction.setSaleAmount(parseBigDecimal(directText(conversion, "orderAmount")));
            transaction.setCommissionAmount(parseBigDecimal(directText(conversion, "commission")));
            transaction.setCurrency(directText(conversion, "currency"));
            transaction.setSubId(directText(conversion, "reference"));
            list.add(transaction);
        }
        return list;
    }

    @Override
    public String buildTrackingLink(String programId, String targetUrl, Map<String, String> subIds)
    {
        if (isBlank(programId))
        {
            return targetUrl;
        }

        StringBuilder sb = new StringBuilder("https://tc.tradetracker.net/");
        sb.append("?c=").append(HttpUtils.urlEncode(programId));
        if (!isBlank(targetUrl))
        {
            sb.append("&u=").append(HttpUtils.urlEncode(targetUrl));
        }
        if (subIds != null && !subIds.isEmpty())
        {
            subIds.values().stream()
                    .filter(value -> !isBlank(value))
                    .findFirst()
                    .ifPresent(value -> sb.append("&r=").append(HttpUtils.urlEncode(value)));
        }
        return sb.toString();
    }

    private boolean isEnabled()
    {
        return feedConfig.getTradetracker() != null && feedConfig.getTradetracker().isEnabled();
    }

    private boolean canCallApi()
    {
        return isEnabled() && !isBlank(customerId) && !isBlank(apiKey);
    }

    private void authenticate() throws Exception
    {
        call("authenticate", """
                <customerID>%s</customerID>
                <passphrase>%s</passphrase>
                <sandbox>false</sandbox>
                <locale>%s</locale>
                <demo>false</demo>
                """.formatted(escapeXml(customerId), escapeXml(apiKey), DEFAULT_LOCALE));
    }

    private List<String> retrieveAffiliateSiteIds() throws Exception
    {
        Document root = call("getAffiliateSites", """
                <options>
                    <limit>%d</limit>
                </options>
                """.formatted(DEFAULT_LIMIT));
        List<String> result = new ArrayList<>();
        for (Element site : containerItems(root, "affiliateSites"))
        {
            String id = directText(site, "ID");
            if (!isBlank(id))
            {
                result.add(id);
            }
        }
        return result;
    }

    private Document call(String operation, String body) throws Exception
    {
        String envelope = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:aff="%s">
                    <soapenv:Body>
                        <aff:%s>
                            %s
                        </aff:%s>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(SOAP_NAMESPACE, operation, body, operation);
        HttpRequest request = HttpRequest.newBuilder(URI.create(SOAP_ENDPOINT))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", SOAP_NAMESPACE + "/" + operation)
                .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
        {
            throw new IllegalStateException("TradeTracker SOAP call failed for " + operation + " with HTTP status " + response.statusCode());
        }
        Document document = parseXml(response.body());
        Element fault = firstElement(document, "Fault");
        if (fault != null)
        {
            throw new IllegalStateException("TradeTracker SOAP fault for " + operation + ": " + directText(fault, "faultstring"));
        }
        return document;
    }

    Document parseXml(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    /** Returns TradeTracker collection items from array containers or normalized SOAP response payloads. */
    private List<Element> containerItems(Document root, String containerLocalName)
    {
        List<Element> containers = childElements(root, containerLocalName);
        if (!containers.isEmpty())
        {
            Element container = containers.get(0);
            List<Element> result = directCollectionChildren(container, containerLocalName);
            if (!result.isEmpty())
            {
                return result;
            }
        }

        List<Element> result = new ArrayList<>();
        String itemLocalName = singularCollectionName(containerLocalName);
        NodeList nodes = root.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node node = nodes.item(i);
            if (node instanceof Element element && sameLocalName(element, itemLocalName))
            {
                result.add(element);
            }
        }
        return result;
    }

    private List<Element> directCollectionChildren(Element container, String containerLocalName)
    {
        List<Element> result = new ArrayList<>();
        String itemLocalName = singularCollectionName(containerLocalName);
        NodeList nodes = container.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node node = nodes.item(i);
            if (node instanceof Element element && ("item".equals(localName(element)) || sameLocalName(element, itemLocalName)))
            {
                result.add(element);
            }
        }
        return result;
    }

    private List<Element> childElements(Document root, String localName)
    {
        List<Element> result = new ArrayList<>();
        NodeList nodes = root.getElementsByTagNameNS("*", localName);
        for (int i = 0; i < nodes.getLength(); i++)
        {
            if (nodes.item(i) instanceof Element element)
            {
                result.add(element);
            }
        }
        if (!result.isEmpty())
        {
            return result;
        }
        nodes = root.getElementsByTagName(localName);
        for (int i = 0; i < nodes.getLength(); i++)
        {
            if (nodes.item(i) instanceof Element element)
            {
                result.add(element);
            }
        }
        return result;
    }

    private Element firstElement(Document root, String localName)
    {
        List<Element> elements = childElements(root, localName);
        return elements.isEmpty() ? null : elements.get(0);
    }

    private Element firstDirectChild(Element parent, String localName)
    {
        if (parent == null)
        {
            return null;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node node = nodes.item(i);
            if (node instanceof Element element && localName.equals(element.getLocalName() != null ? element.getLocalName() : element.getNodeName()))
            {
                return element;
            }
        }
        return null;
    }

    private String directText(Element parent, String localName)
    {
        Element child = firstDirectChild(parent, localName);
        return child == null ? null : child.getTextContent();
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
        catch (Exception ignored)
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
            return new BigDecimal(value.trim());
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private Integer parseDurationDays(String value)
    {
        if (isBlank(value))
        {
            return null;
        }
        try
        {
            String trimmed = value.trim();
            if (trimmed.startsWith("P") && !trimmed.contains("T"))
            {
                return Math.toIntExact(java.time.Period.parse(trimmed).getDays());
            }
            return Math.toIntExact(java.time.Duration.parse(trimmed).toDays());
        }
        catch (Exception ignored)
        {
            return null;
        }
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
        return HttpUtils.isBlank(value);
    }

    private String singularCollectionName(String containerLocalName)
    {
        if (containerLocalName.endsWith("ies"))
        {
            return containerLocalName.substring(0, containerLocalName.length() - 3) + "y";
        }
        if (containerLocalName.endsWith("s"))
        {
            return containerLocalName.substring(0, containerLocalName.length() - 1);
        }
        return containerLocalName;
    }

    private boolean sameLocalName(Element element, String expected)
    {
        return localName(element).equalsIgnoreCase(expected);
    }

    private String localName(Element element)
    {
        String localName = element.getLocalName();
        return localName == null ? element.getNodeName() : localName;
    }

    /** Rewrites a TradeTracker product-feed URL to request CSV instead of XML v2. */
    private String toCsvUrl(String url)
    {
        if (isBlank(url))
        {
            return url;
        }
        return url.replace("type=xml-v2", "type=csv");
    }

    private String escapeXml(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
