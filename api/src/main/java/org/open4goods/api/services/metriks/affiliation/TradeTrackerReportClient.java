package org.open4goods.api.services.metriks.affiliation;

import java.io.StringReader;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Reads affiliate conversions from the TradeTracker SOAP API ({@code https://ws.tradetracker.com/soap/affiliate}).
 * <p>
 * Flow, per call of {@link #fetchTransactions}: {@code authenticate(customerID, passphrase)} (the session is a cookie,
 * kept by the client's {@link CookieManager} for the following calls), {@code getAffiliateSites}, then
 * {@code getConversionTransactions} for every affiliate site on the window. Same operations as
 * {@code TradeTrackerFeedService}. By default the credentials are those of the TradeTracker feed
 * ({@code affiliation-config.tradetracker-customer-id} and {@code tradetracker-api-key}, the passphrase).
 * <p>
 * The window is requested on {@code registrationDate}, one day wider on the upper bound, and filtered locally on the
 * report's time zone. Pages of {@value #PAGE_SIZE} are read with {@code offset} as long as they are full and bring
 * new transaction IDs.
 * <p>
 * Status mapping ({@code transactionStatus}, case-insensitive): {@code accepted} &rarr;
 * {@link AffiliationReportStatus#VALIDATED}; {@code pending}, {@code onhold} &rarr;
 * {@link AffiliationReportStatus#PENDING}; {@code rejected}, {@code declined} &rarr;
 * {@link AffiliationReportStatus#DECLINED}; any other value &rarr; error. The commission is {@code commission}, in
 * {@code currency}.
 */
@Service
public class TradeTrackerReportClient extends AbstractAffiliationReportClient {

    static final String CUSTOMER_ID_PROPERTY = "metriks.affiliation.tradetracker.customer-id";
    static final String PASSPHRASE_PROPERTY = "metriks.affiliation.tradetracker.passphrase";
    static final String SOAP_ENDPOINT = "https://ws.tradetracker.com/soap/affiliate";
    static final int PAGE_SIZE = 500;
    /** Safety cap on the number of pages read per site. */
    static final int MAX_PAGES = 50;
    private static final String LOCALE = "fr_FR";

    private final String customerId;
    private final String passphrase;

    @Autowired
    public TradeTrackerReportClient(@Value("${" + CUSTOMER_ID_PROPERTY + ":}") String customerId,
            @Value("${" + PASSPHRASE_PROPERTY + ":}") String passphrase) {
        this(customerId, passphrase, HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT)
                .cookieHandler(new CookieManager()).build());
    }

    /**
     * Package-private constructor allowing tests to inject a fake {@link HttpClient}.
     */
    TradeTrackerReportClient(String customerId, String passphrase, HttpClient httpClient) {
        super(httpClient);
        this.customerId = customerId == null ? "" : customerId.trim();
        this.passphrase = passphrase == null ? "" : passphrase.trim();
    }

    @Override
    public String network() {
        return "tradetracker";
    }

    @Override
    public String displayName() {
        return "TradeTracker";
    }

    @Override
    public List<String> missingCredentials() {
        return missing(CUSTOMER_ID_PROPERTY, customerId, PASSPHRASE_PROPERTY, passphrase);
    }

    @Override
    public List<String> secretValues() {
        return nonBlank(customerId, passphrase);
    }

    @Override
    public List<AffiliationReportTransaction> fetchTransactions(LocalDate startDate, LocalDate endDate, ZoneId zone)
            throws AffiliationReportException {
        call("authenticate", "<customerID>" + escape(customerId) + "</customerID><passphrase>" + escape(passphrase)
                + "</passphrase><sandbox>false</sandbox><locale>" + LOCALE + "</locale><demo>false</demo>");

        List<String> siteIds = new ArrayList<>();
        for (Element site : container(call("getAffiliateSites", "<options><limit>" + PAGE_SIZE + "</limit></options>"),
                "affiliateSites")) {
            String id = childText(site, "ID");
            if (id != null) {
                siteIds.add(id);
            }
        }

        List<AffiliationReportTransaction> transactions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String siteId : siteIds) {
            for (int page = 0; ; page++) {
                if (page >= MAX_PAGES) {
                    throw failure("trop de pages (plus de " + MAX_PAGES + ")");
                }
                Document document = call("getConversionTransactions", "<affiliateSiteID>" + escape(siteId)
                        + "</affiliateSiteID><options><registrationDateFrom>" + startDate + "</registrationDateFrom>"
                        + "<registrationDateTo>" + endDate.plusDays(1) + "</registrationDateTo><limit>" + PAGE_SIZE
                        + "</limit><offset>" + page * PAGE_SIZE + "</offset></options>");
                List<Element> conversions = container(document, "conversionTransactions");
                int added = 0;
                for (Element conversion : conversions) {
                    String id = childText(conversion, "ID");
                    if (id != null && !seen.add(siteId + "/" + id)) {
                        continue;
                    }
                    added++;
                    AffiliationReportTransaction transaction = parse(conversion, zone);
                    if (!transaction.transactionDate().isBefore(startDate) && !transaction.transactionDate().isAfter(endDate)) {
                        transactions.add(transaction);
                    }
                }
                if (conversions.size() < PAGE_SIZE || added == 0) {
                    break;
                }
            }
        }
        return transactions;
    }

    private AffiliationReportTransaction parse(Element conversion, ZoneId zone) throws AffiliationReportException {
        AffiliationReportStatus status = mapStatus(childText(conversion, "transactionStatus"));
        LocalDate date = parseDate(childText(conversion, "registrationDate"), zone);
        String commission = childText(conversion, "commission");
        String currency = childText(conversion, "currency");
        BigDecimal amount = null;
        if (commission != null) {
            try {
                amount = new BigDecimal(commission.replace(',', '.'));
            } catch (NumberFormatException e) {
                throw failure("montant illisible dans le champ commission");
            }
        }
        if (status != AffiliationReportStatus.DECLINED && (amount == null || currency == null)) {
            throw failure("commission incomplète (montant ou devise absent)");
        }
        return new AffiliationReportTransaction(date, status, amount, currency);
    }

    private AffiliationReportStatus mapStatus(String state) throws AffiliationReportException {
        if (state == null) {
            throw failure("transactionStatus absent");
        }
        return switch (state.toLowerCase(Locale.ROOT)) {
            case "accepted" -> AffiliationReportStatus.VALIDATED;
            case "pending", "onhold" -> AffiliationReportStatus.PENDING;
            case "rejected", "declined" -> AffiliationReportStatus.DECLINED;
            default -> throw failure("statut inconnu : " + echo(state));
        };
    }

    private LocalDate parseDate(String value, ZoneId zone) throws AffiliationReportException {
        if (value == null) {
            throw failure("registrationDate absent");
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDate();
        } catch (DateTimeParseException e) {
            // no offset: TradeTracker dates are then read as UTC instants or local date-times of the report zone
        }
        try {
            return Instant.parse(value).atZone(zone).toLocalDate();
        } catch (DateTimeParseException e) {
            // fall through
        }
        try {
            return java.time.LocalDateTime.parse(value.replace(' ', 'T')).atZone(zone).toLocalDate();
        } catch (DateTimeParseException e) {
            throw failure("registrationDate illisible : " + echo(value));
        }
    }

    /**
     * Sends a SOAP operation and returns the parsed response. A SOAP fault is an error whose text is not quoted.
     */
    private Document call(String operation, String body) throws AffiliationReportException {
        String envelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:aff=\""
                + SOAP_ENDPOINT + "\"><soapenv:Body><aff:" + operation + ">" + body + "</aff:" + operation
                + "></soapenv:Body></soapenv:Envelope>";
        HttpRequest request = HttpRequest.newBuilder(URI.create(SOAP_ENDPOINT))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", SOAP_ENDPOINT + "/" + operation)
                .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                .build();
        Document document = parseXml(send(request, operation), operation);
        if (!items(document, "Fault").isEmpty()) {
            throw failure("faute SOAP (" + operation + ")");
        }
        return document;
    }

    private Document parseXml(String xml, String step) throws AffiliationReportException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml == null ? "" : xml)));
        } catch (Exception e) {
            throw failure("réponse XML illisible (" + step + ")");
        }
    }

    /** All descendants of the document with the given local name. */
    private static List<Element> items(Document document, String localName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element element && localName.equals(localName(element))) {
                result.add(element);
            }
        }
        return result;
    }

    /** Entries ({@code item}) of the SOAP array with the given local name; empty when the array is absent or empty. */
    private static List<Element> container(Document document, String localName) {
        List<Element> result = new ArrayList<>();
        List<Element> containers = items(document, localName);
        if (!containers.isEmpty()) {
            for (Node node = containers.get(0).getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element element) {
                    result.add(element);
                }
            }
        }
        return result;
    }

    private static String childText(Element parent, String localName) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && localName.equals(localName(element))) {
                String text = element.getTextContent();
                return StringUtils.hasText(text) ? text.trim() : null;
            }
        }
        return null;
    }

    private static String localName(Element element) {
        String name = element.getLocalName() != null ? element.getLocalName() : element.getTagName();
        return name.substring(name.indexOf(':') + 1);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
