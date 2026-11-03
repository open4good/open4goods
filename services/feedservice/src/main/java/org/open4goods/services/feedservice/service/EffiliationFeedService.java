package org.open4goods.services.feedservice.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.util.HttpUtils;
import org.open4goods.model.affiliation.AffiliationCapability;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.feedservice.model.EffiliationProgram;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Feed service implementation for Effiliation (now Effinity).
 *
 * <p>This service reads the Effinity publisher API v3 (JSON), which replaces the API v2 shut down by Effiliation
 * on 11/01/2027 (dd/MM/yyyy), caches the responses locally using {@link RemoteFileCachingService}, and maps them to
 * {@link DataSourceProperties} and to the normalised affiliation models.</p>
 *
 * <h3>Endpoints</h3>
 * All of them are {@code GET https://api.effinity.fr/apiv3/{apikey}/...}, paginated with {@code page} /
 * {@code perPage} and read until {@code pagination.lastPage}:
 * <ul>
 *   <li>{@code publisher/programs?status=active&status=inactive}: the programs the publisher is subscribed to
 *       (the {@code filter=mines} of the API v2);</li>
 *   <li>{@code publisher/links/productfeeds}: the product feeds, one datasource per feed;</li>
 *   <li>{@code publisher/links?linkType=voucherOrDeal}: the promotional offers (the {@code commercialtrades} of the
 *       API v2);</li>
 *   <li>{@code publisher/conversions}: the sales and leads, by windows of at most 90 days as required by the API
 *       (the {@code transaction} of the API v2).</li>
 * </ul>
 *
 * <h3>Mapping</h3>
 * <ul>
 *   <li>Program identifier exposed to callers: the {@code merchantId} (advertiser), as the {@code id_affilieur} of
 *       the API v2 was; feeds, promotions and conversions are joined on it.</li>
 *   <li>Product feed (root array {@code feeds}, not {@code links}): datasource name from {@code feedName} (the v2
 *       {@code nom}), feed URL from {@code feedUrl} (the v2 {@code code}). The feed carries no {@code merchantUrl}:
 *       the portal falls back on the program's {@code merchantUrl}. A feed whose program is {@code frozen} or
 *       {@code closed} is skipped.</li>
 *   <li>Fields the API v3 no longer provides (program click, sale, lead and display commissions, EPC, conversion
 *       rate, signup URL; transaction product id) stay {@code null}: an absent value is never reported as zero.</li>
 * </ul>
 *
 * <h3>Secrets</h3>
 * The API only accepts the publisher key as a path segment, so every request URL carries it. Those URLs are never
 * logged, put in a message or attached to an exception: logs use the {@code {apikey}} placeholder instead, and the
 * cache is given the same label (see {@link RemoteFileCachingService#getResource(String, Integer, String)}). The
 * hypermedia {@code links} of the payloads, which also carry the key, are never mapped.
 *
 * @see <a href="https://api.effinity.fr/apiv3/docs">Effinity API v3, OpenAPI description</a>
 */
public class EffiliationFeedService extends AbstractFeedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EffiliationFeedService.class);

    /** Base URL of the Effinity API v3; the publisher API key is the next path segment. */
    static final String API_BASE_URL = "https://api.effinity.fr/apiv3/";

    /** Replaces the API key in everything that is logged. */
    private static final String API_KEY_PLACEHOLDER = "{apikey}";

    static final String PROGRAMS_RESOURCE = "publisher/programs?status=active&status=inactive";
    static final String PRODUCT_FEEDS_RESOURCE = "publisher/links/productfeeds";
    static final String PROMOTIONS_RESOURCE = "publisher/links?linkType=voucherOrDeal";
    static final String CONVERSIONS_RESOURCE = "publisher/conversions";

    /** Elements per page, the API maximum default. */
    static final int PER_PAGE = 1000;

    /** Guard against a pagination that never ends. */
    static final int MAX_PAGES = 50;

    /** Longest {@code start}..{@code end} span accepted by the conversions endpoint, in days. */
    static final int MAX_CONVERSION_WINDOW_DAYS = 90;

    /** Time zone of the dates returned by the API, which carry no offset. */
    private static final ZoneId API_ZONE = ZoneId.of("Europe/Paris");

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern LEADING_INTEGER_PATTERN = Pattern.compile("^\\s*(\\d+)");

    /** Program states for which feeds are still ingested ({@code frozen} and {@code closed} are not). */
    private static final Set<String> ACTIVE_PROGRAM_STATES = Set.of("active", "closing");

    private static final List<DateTimeFormatter> LOCAL_DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    private final ObjectMapper objectMapper;
    private final String effiliationApiKey;

    /**
     * Constructor.
     *
     * @param feedConfig Effiliation feed configuration
     * @param remoteFileCachingService service for caching remote files
     * @param dataSourceConfigService service to retrieve existing datasource properties
     * @param serialisationService service to deep copy datasource properties
     * @param effiliationApiKey the Effiliation (Effinity) publisher API key
     */
    public EffiliationFeedService(FeedConfiguration feedConfig,
                                 RemoteFileCachingService remoteFileCachingService,
                                 DataSourceConfigService dataSourceConfigService,
                                 SerialisationService serialisationService,
                                 String effiliationApiKey) {
        super(feedConfig, remoteFileCachingService, dataSourceConfigService, serialisationService);
        this.objectMapper = new ObjectMapper();
        this.effiliationApiKey = effiliationApiKey;
    }

    /**
     * Scheduled refresh of Effiliation datasource properties according to configuration.
     *
     * <p>A random jitter is applied before each execution to avoid synchronized bursts
     * when several instances are running simultaneously.</p>
     */
    @Scheduled(cron = "${feed.effiliation.cron:-}")
    public void scheduledLoad() {
        if (!isEnabled()) {
            LOGGER.info("Effiliation feed service is disabled. Skipping scheduled load.");
            return;
        }
        applySchedulingJitter();
        LOGGER.info("Scheduled refresh of Effiliation datasources initiated.");
        load();
    }

    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception {
        Set<DataSourceProperties> result = new HashSet<>();

        if (!isEnabled()) {
            LOGGER.info("Effiliation feed service is disabled. Returning empty datasource set.");
            return result;
        }

        if (isBlank(effiliationApiKey)) {
            LOGGER.error("Effiliation API key is blank; skipping datasource load.");
            return result;
        }

        // Programs are mandatory to enrich datasources: without them, no datasource is returned.
        Map<String, EffiliationProgram> programs = retrieveEffiliationPrograms(effiliationApiKey);
        if (programs.isEmpty()) {
            LOGGER.error("Effiliation programs list is empty/unavailable; returning no datasources (per policy).");
            return result;
        }

        // One datasource per feed name, keeping the first occurrence.
        Map<String, JsonNode> feedsByName = new LinkedHashMap<>();
        for (JsonNode feedNode : retrieveEffiliationFeeds(effiliationApiKey)) {
            String name = textOrNull(feedNode, "feedName");
            if (!isBlank(name)) {
                feedsByName.putIfAbsent(name, feedNode);
            }
        }

        if (feedsByName.isEmpty()) {
            LOGGER.warn("Effiliation productfeeds returned no usable feeds.");
            return result;
        }

        for (Map.Entry<String, JsonNode> e : feedsByName.entrySet()) {
            JsonNode feedNode = e.getValue();
            String feedKey = e.getKey();

            String feedUrl = textOrNull(feedNode, "feedUrl");
            if (isBlank(feedUrl)) {
                LOGGER.warn("Skipping Effiliation feed '{}' due to missing feed URL field (feedUrl).", feedKey);
                continue;
            }
            feedUrl = HttpUtils.normalizeUrl(feedUrl);

            String merchantId = textOrNull(feedNode, "merchantId");
            if (!isValidMerchantId(merchantId)) {
                LOGGER.warn("Skipping Effiliation feed '{}' due to invalid 'merchantId' value '{}'.", feedKey, merchantId);
                continue;
            }

            EffiliationProgram program = programs.get(merchantId.trim());
            if (program == null) {
                LOGGER.warn("Program of merchant {} not found (feed='{}').", merchantId, feedKey);
                continue;
            }

            if (!isProgramActive(program)) {
                LOGGER.info("Skipping Effiliation feed '{}' because the program of merchant {} is not active (state='{}').",
                        feedKey, merchantId, program.state());
                continue;
            }

            DataSourceProperties ds = getVolatileDatasource(feedKey, feedConfig, feedUrl);
            ds.setPortalUrl(HttpUtils.normalizeUrl(firstNonBlank(textOrNull(feedNode, "merchantUrl"), program.merchantUrl())));
            ds.setAffiliatedPortalUrl(HttpUtils.normalizeUrl(program.defaultTrackedLink()));
            ds.setLogo(HttpUtils.normalizeUrl(program.programLogoUrl()));

            // Keep existing behavior: derive name from portal URL.
            ds.setName(extractNameAndTld(ds.getPortalUrl()));

            result.add(ds);
        }

        LOGGER.info("Effiliation datasources loaded: {} entries", result.size());
        return result;
    }

    /**
     * Retrieves the programs the publisher is subscribed to.
     *
     * <p>Programs are mandatory: callers treat an empty map as a hard failure.</p>
     *
     * @param apiKey Effinity publisher API key
     * @return map of merchantId -> program, first program kept for a merchant
     */
    private Map<String, EffiliationProgram> retrieveEffiliationPrograms(String apiKey) {
        if (isBlank(apiKey)) {
            LOGGER.error("Effiliation API key is blank; cannot retrieve programs.");
            return Collections.emptyMap();
        }

        try {
            Map<String, EffiliationProgram> programs = new LinkedHashMap<>();
            for (JsonNode node : readAllPages(apiKey, PROGRAMS_RESOURCE, "programs")) {
                EffiliationProgram program = objectMapper.convertValue(node, EffiliationProgram.class);
                if (program != null && program.merchantId() != null) {
                    programs.putIfAbsent(String.valueOf(program.merchantId()), program);
                }
            }
            return programs;
        } catch (Exception e) {
            LOGGER.warn("Failed to retrieve/parse Effiliation programs ({}).", describe(e));
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves the product feeds of the programs the publisher is subscribed to, all pages merged.
     *
     * @param apiKey Effinity publisher API key
     * @return the product feeds (items of the {@code feeds} array)
     * @throws IllegalStateException on missing cache file, invalid JSON or unexpected payload
     */
    List<JsonNode> retrieveEffiliationFeeds(String apiKey) {
        assertEnabled();
        if (isBlank(apiKey)) {
            throw new IllegalArgumentException("Effiliation API key is blank.");
        }
        return readAllPages(apiKey, PRODUCT_FEEDS_RESOURCE, "feeds");
    }

    /**
     * Retrieves the promotional offers (vouchers and deals), all pages merged.
     *
     * @param apiKey Effinity publisher API key
     * @return the promotional offer links ({@code LinkRestDto})
     * @throws IllegalStateException on missing cache file, invalid JSON or unexpected payload
     */
    List<JsonNode> retrieveEffiliationPromotionalOffers(String apiKey) {
        assertEnabled();
        return readAllPages(apiKey, PROMOTIONS_RESOURCE, "links");
    }

    /**
     * Retrieves the conversions (sales and leads, any status) whose conversion date is in the given range,
     * querying the API by windows of at most {@value #MAX_CONVERSION_WINDOW_DAYS} days.
     *
     * @param apiKey Effinity publisher API key
     * @param start first day, inclusive
     * @param end last day, inclusive
     * @return the conversions ({@code ConversionApiDto})
     * @throws IllegalStateException on missing cache file, invalid JSON or unexpected payload
     */
    List<JsonNode> retrieveEffiliationConversions(String apiKey, LocalDate start, LocalDate end) {
        assertEnabled();
        List<JsonNode> conversions = new ArrayList<>();
        for (LocalDate windowStart = start; !windowStart.isAfter(end);
                windowStart = windowStart.plusDays(MAX_CONVERSION_WINDOW_DAYS)) {
            LocalDate windowEnd = windowStart.plusDays(MAX_CONVERSION_WINDOW_DAYS - 1L);
            if (windowEnd.isAfter(end)) {
                windowEnd = end;
            }
            conversions.addAll(readAllPages(apiKey, CONVERSIONS_RESOURCE
                    + "?start=" + windowStart + "&end=" + windowEnd
                    + "&dateType=conversionDate&conversionType=all&status=all", "conversions"));
        }
        return conversions;
    }

    /* ==========================================================================================
     * Paginated, cached read
     * ========================================================================================== */

    /**
     * Reads every page of a paginated API v3 resource and merges the items of its list field.
     *
     * @param apiKey Effinity publisher API key
     * @param resource resource path relative to the key segment, with its query string if any
     * @param listField name of the array holding the items
     * @return the items of all pages
     * @throws IllegalStateException on missing cache file, invalid JSON, missing list or runaway pagination
     */
    private List<JsonNode> readAllPages(String apiKey, String resource, String listField) {
        List<JsonNode> items = new ArrayList<>();
        int lastPage = 1;
        for (int page = 1; page <= lastPage; page++) {
            if (page > MAX_PAGES) {
                throw new IllegalStateException("Effiliation " + listField + ": more than " + MAX_PAGES + " pages.");
            }
            String pagedResource = resource + (resource.contains("?") ? "&" : "?") + "page=" + page + "&perPage=" + PER_PAGE;
            JsonNode root = readCachedJson(apiKey, pagedResource);
            JsonNode list = root.path(listField);
            if (!list.isArray()) {
                throw new IllegalStateException("Effiliation payload has no '" + listField + "' array (page " + page + ").");
            }
            list.forEach(items::add);
            lastPage = root.path("pagination").path("lastPage").asInt(1);
        }
        return items;
    }

    /**
     * Reads a resource through the cache, falling back to a stale copy when the refresh fails.
     *
     * @param apiKey Effinity publisher API key, only placed in the request URL
     * @param resource resource path relative to the key segment, with its query string
     * @return the parsed response
     * @throws IllegalStateException when no cached copy exists or it cannot be read or parsed; it never carries a
     *         cause, whose message could quote the URL and therefore the key
     */
    private JsonNode readCachedJson(String apiKey, String resource) {
        if (isBlank(apiKey)) {
            throw new IllegalArgumentException("Effiliation API key is blank.");
        }
        // The URL carries the key: only the label is ever logged.
        String url = API_BASE_URL + urlEncode(apiKey.trim()) + "/" + resource;
        String label = API_BASE_URL + API_KEY_PLACEHOLDER + "/" + resource;
        int cacheTtl = feedConfig.getEffiliation().getCacheTtlDays();
        LOGGER.info("Retrieving Effiliation resource '{}'.", label);

        File cachedResponse;
        try {
            cachedResponse = remoteFileCachingService.getResource(url, cacheTtl, label);
        } catch (Exception exception) {
            LOGGER.warn("Effiliation live cache refresh failed ({}), retrying using stale cache (resource='{}').",
                    exception.getClass().getSimpleName(), label);
            cachedResponse = null;
        }

        if (cachedResponse == null || !cachedResponse.exists()) {
            LOGGER.warn("Effiliation response unavailable with configured TTL, retrying using stale cache (resource='{}').",
                    label);
            try {
                cachedResponse = remoteFileCachingService.getResource(url, Integer.MAX_VALUE, label);
            } catch (Exception exception) {
                throw new IllegalStateException("Effiliation stale cache read failed (" + exception.getClass().getSimpleName()
                        + ", resource='" + label + "').");
            }
        }

        if (cachedResponse == null || !cachedResponse.exists()) {
            throw new IllegalStateException("Effiliation cached response file not found (resource='" + label + "').");
        }

        String jsonResponse;
        try {
            jsonResponse = Files.readString(cachedResponse.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Effiliation cached response cannot be read (resource='" + label + "').");
        }
        if (isBlank(jsonResponse)) {
            throw new IllegalStateException("Effiliation cached response is empty (resource='" + label + "').");
        }
        try {
            return objectMapper.readTree(jsonResponse);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Effiliation response is not valid JSON (resource='" + label + "').");
        }
    }

    /* ==========================================================================================
     * Helpers
     * ========================================================================================== */

    /**
     * Describes a failure for the logs: the message of the exceptions raised by this class, which only carry the
     * {@code {apikey}} label, otherwise the exception type alone.
     */
    private static String describe(Exception e) {
        if (e instanceof IllegalStateException || e instanceof IllegalArgumentException) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }

    private boolean isEnabled() {
        return feedConfig.getEffiliation().isEnabled();
    }

    private void assertEnabled() {
        if (!isEnabled()) {
            throw new IllegalStateException("Effiliation feed service is disabled.");
        }
    }

    private void applySchedulingJitter() {
        int maxJitterSeconds = Math.max(0, feedConfig.getEffiliation().getMaxJitterSeconds());
        if (maxJitterSeconds == 0) {
            return;
        }

        int jitter = ThreadLocalRandom.current().nextInt(maxJitterSeconds + 1);
        if (jitter == 0) {
            return;
        }

        try {
            LOGGER.info("Applying Effiliation scheduler jitter: {} second(s).", jitter);
            Thread.sleep(jitter * 1000L);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Effiliation scheduler jitter interrupted; skipping this run.");
        }
    }

    private boolean isValidMerchantId(String merchantId) {
        return !isBlank(merchantId) && NUMERIC_PATTERN.matcher(merchantId.trim()).matches();
    }

    private boolean isProgramActive(EffiliationProgram program) {
        if (program == null || isBlank(program.state())) {
            return true;
        }
        return ACTIVE_PROGRAM_STATES.contains(program.state().trim().toLowerCase(Locale.ROOT));
    }

    private String urlEncode(String value) {
        return HttpUtils.urlEncode(value);
    }

    private boolean isBlank(String s) {
        return HttpUtils.isBlank(s);
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || v.isContainer()) {
            return null;
        }
        String s = v.asText();
        return isBlank(s) ? null : s.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (!isBlank(v)) {
                return v;
            }
        }
        return null;
    }

    private BigDecimal decimalOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Set<String> lowerCaseSet(List<String> values) {
        if (values == null) {
            return null;
        }
        Set<String> out = new HashSet<>();
        for (String value : values) {
            if (!isBlank(value)) {
                out.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    /** Reads the number of days of a duration such as {@code "30 days"}. */
    private Integer parseDays(String duration) {
        if (isBlank(duration)) {
            return null;
        }
        Matcher matcher = LEADING_INTEGER_PATTERN.matcher(duration);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private LocalDate parseLocalDate(String value) {
        if (isBlank(value) || value.trim().length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim().substring(0, 10));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /** Reads an API date-time, local to {@link #API_ZONE} unless it carries an offset. */
    private Instant parseInstant(String value) {
        if (isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (DateTimeParseException ignored) {
            // Documented format: a local date-time.
        }
        for (DateTimeFormatter formatter : LOCAL_DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(trimmed, formatter).atZone(API_ZONE).toInstant();
            } catch (DateTimeParseException ignored) {
                // Try the next format.
            }
        }
        return null;
    }

    @Override
    public String getProviderName()
    {
        return "Effiliation";
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
        Map<String, EffiliationProgram> raw = retrieveEffiliationPrograms(effiliationApiKey);
        if (raw.isEmpty())
        {
            return Collections.emptyList();
        }
        List<AffiliationProgram> list = new ArrayList<>();
        for (Map.Entry<String, EffiliationProgram> entry : raw.entrySet())
        {
            EffiliationProgram ep = entry.getValue();
            AffiliationProgram ap = new AffiliationProgram();
            ap.setProviderName(getProviderName());
            ap.setProgramId(entry.getKey());
            ap.setAdvertiserName(firstNonBlank(ep.merchantName(), ep.programName()));
            ap.setStatus(firstNonBlank(ep.state(), ep.status()));
            ap.setCountryCodes(lowerCaseSet(ep.coverageCountries()));
            ap.setCurrency("EUR"); // Default currency for Effiliation
            ap.setCategories(lowerCaseSet(ep.programCategories()));
            ap.setCookieDurationDays(parseDays(ep.cookieDurationPostClick()));
            ap.setLogoUrl(ep.programLogoUrl());
            ap.setPortalUrl(ep.merchantUrl());
            ap.setTrackingUrl(ep.defaultTrackedLink());
            ap.setDescription(ep.merchantDescription());
            list.add(ap);
        }
        return list;
    }

    @Override
    protected Collection<AffiliationPromotion> loadPromotionsInternal() throws Exception
    {
        if (isBlank(effiliationApiKey))
        {
            return Collections.emptyList();
        }

        List<AffiliationPromotion> list = new ArrayList<>();
        for (JsonNode node : retrieveEffiliationPromotionalOffers(effiliationApiKey))
        {
            JsonNode promo = node.path("promo");
            AffiliationPromotion ap = new AffiliationPromotion();
            ap.setProviderName(getProviderName());
            ap.setProgramId(textOrNull(node, "merchantId"));
            ap.setAdvertiserName(firstNonBlank(textOrNull(node, "merchantName"), textOrNull(node, "programName")));
            ap.setTitle(textOrNull(node, "linkName"));
            ap.setDescription(textOrNull(promo, "description"));
            ap.setVoucherCode(firstNonBlank(textOrNull(promo, "voucher"), textOrNull(promo, "code")));
            ap.setDiscountType(firstNonBlank(textOrNull(promo, "promoType"), textOrNull(promo, "valueType")));
            ap.setDiscountValue(decimalOrNull(textOrNull(promo, "value")));
            ap.setStartDate(parseLocalDate(firstNonBlank(textOrNull(promo, "validityStartDate"), textOrNull(node, "startDate"))));
            ap.setEndDate(parseLocalDate(firstNonBlank(textOrNull(promo, "validityEndDate"), textOrNull(node, "endDate"))));
            ap.setLandingUrl(textOrNull(node, "urlRedir"));
            ap.setTrackingUrl(textOrNull(node, "trackedLink"));
            list.add(ap);
        }
        return list;
    }

    @Override
    public Collection<AffiliationTransaction> getTransactions(Instant from, Instant to)
    {
        if (isBlank(effiliationApiKey))
        {
            return Collections.emptyList();
        }
        try
        {
            LocalDate start = LocalDate.ofInstant(from, API_ZONE);
            LocalDate end = LocalDate.ofInstant(to, API_ZONE);
            List<AffiliationTransaction> list = new ArrayList<>();
            for (JsonNode node : retrieveEffiliationConversions(effiliationApiKey, start, end))
            {
                AffiliationTransaction tx = new AffiliationTransaction();
                tx.setProviderName(getProviderName());
                tx.setTransactionId(textOrNull(node, "conversionId"));
                tx.setProgramId(textOrNull(node, "merchantId"));
                tx.setClickDate(parseInstant(textOrNull(node, "clicDate")));
                tx.setTransactionDate(parseInstant(textOrNull(node, "conversionDate")));
                tx.setStatus(textOrNull(node, "status"));
                tx.setSaleAmount(decimalOrNull(textOrNull(node, "amount")));
                tx.setCommissionAmount(decimalOrNull(textOrNull(node, "commissionAmount")));
                tx.setCurrency(firstNonBlank(textOrNull(node, "currency"), "EUR"));
                tx.setSubId(textOrNull(node, "effiId"));
                list.add(tx);
            }
            return list;
        }
        catch (Exception e)
        {
            LOGGER.warn("Failed to retrieve/parse Effiliation transactions ({}).", describe(e));
            return Collections.emptyList();
        }
    }

    @Override
    public String buildTrackingLink(String programId, String targetUrl, Map<String, String> subIds)
    {
        String trackingUrl = null;
        Collection<AffiliationProgram> programs = getPrograms();
        if (programs != null)
        {
            for (AffiliationProgram p : programs)
            {
                if (p.getProgramId().equals(programId))
                {
                    trackingUrl = p.getTrackingUrl();
                    break;
                }
            }
        }
        
        if (trackingUrl == null || trackingUrl.trim().isEmpty())
        {
            return targetUrl;
        }
        
        StringBuilder sb = new StringBuilder(trackingUrl);
        if (targetUrl != null && !targetUrl.trim().isEmpty())
        {
            if (sb.indexOf("?") < 0)
            {
                sb.append("?");
            }
            else
            {
                sb.append("&");
            }
            sb.append("url=").append(urlEncode(targetUrl));
        }
        
        if (subIds != null && !subIds.isEmpty())
        {
            for (Map.Entry<String, String> entry : subIds.entrySet())
            {
                if (sb.indexOf("?") < 0)
                {
                    sb.append("?");
                }
                else
                {
                    sb.append("&");
                }
                sb.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
            }
        }
        return sb.toString();
    }
}
