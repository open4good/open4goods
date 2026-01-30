package org.open4goods.services.feedservice.service;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.feedservice.model.EffiliationProgram;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Feed service implementation for Effiliation.
 *
 * <p>This service fetches metadata from Effiliation's REST API (JSON), caches it locally
 * using {@link RemoteFileCachingService}, and maps the results to {@link DataSourceProperties}.
 *
 * <h3>Hardening</h3>
 * <ul>
 *   <li>Never logs the API key (masked in logs).</li>
 *   <li>Validates required JSON fields.</li>
 *   <li>Returns an empty datasource set if programs cannot be retrieved (per requirement).</li>
 *   <li>Uses cached API responses (TTL expressed in days).</li>
 * </ul>
 *
 * <h3>API alignment</h3>
 * Uses official fields as documented:
 * <ul>
 *   <li>productfeeds: {@code nom}, {@code code}, {@code url_affilieur}, {@code id_affilieur}</li>
 *   <li>programs: {@code id_affilieur}, {@code url_tracke}, {@code urllo}, {@code pays}</li>
 * </ul>
 */
@Service
public class EffiliationFeedService extends AbstractFeedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EffiliationFeedService.class);

    /**
     * Remote cache TTL in days.
     *
     * <p>Effiliation refreshes data every ~2 hours per documentation, so a 1-day cache is safe
     * and avoids excess calls while still updating regularly. :contentReference[oaicite:1]{index=1}
     */
    /**
     * Remote cache TTL in days.
     *
     * <p>Effiliation refreshes data every ~2 hours per documentation, so a 1-day cache is safe
     * and avoids excess calls while still updating regularly. :contentReference[oaicite:1]{index=1}
     */
    // private static final int CACHE_TTL_DAYS = 1;

    /** Effiliation filter value: "mines" = programs publisher is registered to. :contentReference[oaicite:2]{index=2} */
    private static final String FILTER_MINES = "mines";

    /**
     * Supported Effiliation output languages (lg).
     * Docs mention: fr, en, es, it, pt, de, nl. :contentReference[oaicite:3]{index=3}
     */
    private static final Set<String> SUPPORTED_LG =
            Set.of("fr", "en", "es", "it", "pt", "de", "nl");

    private final ObjectMapper objectMapper;
    private final String effiliationApiKey;

    /**
     * Constructor.
     *
     * @param feedConfig Effiliation feed configuration
     * @param remoteFileCachingService service for caching remote files
     * @param dataSourceConfigService service to retrieve existing datasource properties
     * @param serialisationService service to deep copy datasource properties
     * @param effiliationApiKey the Effiliation API key
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
     * Scheduled refresh of Effiliation datasource properties every day at a random time
     * (with a 30-second offset).
     * TODO : should be configurable from yaml config, through feedConfig.schedule.classname
     * TODO : In config cron expression + doc
     * TODO : Add schedule at 1h43 AM every day in yaml config
     * Should add the random seconds
     */
    /**
     * Scheduled refresh of Effiliation datasource properties according to configuration.
     */
    @Scheduled(cron = "${feed.effiliation.cron}")
    public void scheduledLoad() {
        if (!feedConfig.getEffiliation().isEnabled()) {
            LOGGER.info("Effiliation feed service is disabled. Skipping scheduled load.");
            return;
        }
        LOGGER.info("Scheduled refresh of Effiliation datasources initiated.");
        load();
    }

    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception {
        Set<DataSourceProperties> result = new HashSet<>();

        if (isBlank(effiliationApiKey)) {
            LOGGER.error("Effiliation API key is blank; skipping datasource load.");
            return result;
        }

        // Programs are mandatory to enrich datasources (per your failure policy: "return none").
        Map<String, EffiliationProgram> programs = retrieveEffiliationPrograms(effiliationApiKey, "fr");
        if (programs.isEmpty()) {
            LOGGER.error("Effiliation programs list is empty/unavailable; returning no datasources (per policy).");
            return result;
        }

        /*
         * Language driven by datasource/country:
         * 1) fetch productfeeds in multiple language buckets (lg=fr/en/de/...) to match target feed countries.
         * 2) merge feeds, keeping first occurrence per feedKey (nom).
         *
         * Why: productfeeds endpoint supports lg, but it's a request-level param. :contentReference[oaicite:4]{index=4}
         */
        Map<String, JsonNode> feedsByName = new HashMap<>();
        for (String lg : computeLikelyLanguages(programs)) {
            JsonNode root = retrieveEffiliationFeeds(effiliationApiKey, lg, null);
            JsonNode feedsNode = (root == null) ? null : root.get("feeds");
            if (feedsNode == null || !feedsNode.isArray()) {
                LOGGER.warn("No 'feeds' array found for productfeeds (lg='{}').", lg);
                continue;
            }
            for (JsonNode feedNode : feedsNode) {
                String name = firstNonBlank(
                        textOrNull(feedNode, "nom"),
                        textOrNull(feedNode, "name") // fallback alias (non-breaking)
                );
                if (isBlank(name)) {
                    continue;
                }
                feedsByName.putIfAbsent(name, feedNode);
            }
        }

        if (feedsByName.isEmpty()) {
            LOGGER.warn("Effiliation productfeeds returned no usable feeds.");
            return result;
        }

        // Build datasources from merged feed set.
        for (Map.Entry<String, JsonNode> e : feedsByName.entrySet()) {
            JsonNode feedNode = e.getValue();

            String feedKey = e.getKey();

            // Official: "code" is the product feed URL. :contentReference[oaicite:5]{index=5}
            String feedUrl = firstNonBlank(
                    textOrNull(feedNode, "code"),
                    textOrNull(feedNode, "url"),       // fallback if API variant uses url
                    textOrNull(feedNode, "feed_url")   // fallback alias
            );
            if (isBlank(feedUrl)) {
                LOGGER.warn("Skipping Effiliation feed '{}' due to missing feed URL field (code/url).", feedKey);
                continue;
            }

            String idAffilieur = firstNonBlank(
                    textOrNull(feedNode, "id_affilieur"),
                    textOrNull(feedNode, "idAffilieur") // fallback alias
            );
            if (isBlank(idAffilieur)) {
                LOGGER.warn("Skipping Effiliation feed '{}' due to missing 'id_affilieur'.", feedKey);
                continue;
            }

            EffiliationProgram program = programs.get(idAffilieur);
            if (program == null) {
                // Keep previous behavior: do not emit datasource if program is missing.
                LOGGER.error("Program with affiliation id {} not found (feed='{}').", idAffilieur, feedKey);
                continue;
            }

            DataSourceProperties ds = getVolatileDatasource(feedKey, feedConfig, feedUrl);

            // Official: advertiser url is "url_affilieur". :contentReference[oaicite:6]{index=6}
            String portalUrl = firstNonBlank(
                    textOrNull(feedNode, "url_affilieur"),
                    textOrNull(feedNode, "urlannonceur"), // some APIs name it this way
                    textOrNull(feedNode, "portal_url")    // fallback alias
            );
            ds.setPortalUrl(nullSafe(portalUrl));

            // Program enrichment from programs API fields. :contentReference[oaicite:7]{index=7}
            ds.setAffiliatedPortalUrl(nullSafe(program.getUrlTracke()));
            ds.setLogo(nullSafe(program.getUrlLogo())); // assuming your model maps urllo/url_logo to getUrlLogo()

            // Keep existing behavior: derive name from portal URL.
            ds.setName(extractNameAndTld(ds.getPortalUrl()));

            result.add(ds);
        }

        LOGGER.info("Effiliation datasources loaded: {} entries", result.size());
        return result;
    }

    /**
     * Retrieves Effiliation programs using cached API response.
     *
     * <p>Per requirement: programs are mandatory. Callers treat an empty map as a hard failure.
     *
     * @param apiKey Effiliation API key
     * @param lg output language (fr/en/...)
     * @return map of id_affilieur -> EffiliationProgram
     */
    private Map<String, EffiliationProgram> retrieveEffiliationPrograms(String apiKey, String lg) {
        if (isBlank(apiKey)) {
            LOGGER.error("Effiliation API key is blank; cannot retrieve programs.");
            return Collections.emptyMap();
        }

        String endpoint = buildProgramsEndpoint(apiKey, normalizeLg(lg), FILTER_MINES, null, null);

        try {
            int cacheTtl = feedConfig.getEffiliation().getCacheTtlDays();
            File cachedResponse = remoteFileCachingService.getResource(endpoint, cacheTtl);
            if (cachedResponse == null || !cachedResponse.exists()) {
                LOGGER.error("Effiliation cached programs response file not found (endpoint='{}').",
                        safeEndpointForLogs(endpoint));
                return Collections.emptyMap();
            }

            String jsonResponse = Files.readString(cachedResponse.toPath(), StandardCharsets.UTF_8);
            if (isBlank(jsonResponse)) {
                LOGGER.error("Effiliation cached programs response is empty (endpoint='{}').",
                        safeEndpointForLogs(endpoint));
                return Collections.emptyMap();
            }

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode programsNode = root.path("programs");

            if (!programsNode.isArray()) {
                LOGGER.error("Effiliation programs payload has no 'programs' array (endpoint='{}').",
                        safeEndpointForLogs(endpoint));
                return Collections.emptyMap();
            }

            List<EffiliationProgram> programList =
                    objectMapper.convertValue(programsNode, new TypeReference<List<EffiliationProgram>>() {});

            Map<String, EffiliationProgram> programs = new HashMap<>();
            for (EffiliationProgram p : programList) {
                if (p != null && p.getIdAffilieur() != null) {
                    programs.put(String.valueOf(p.getIdAffilieur()), p);
                }
            }

            return programs;

        } catch (Exception e) {
            LOGGER.error("Failed to retrieve/parse Effiliation programs (endpoint='{}').",
                    safeEndpointForLogs(endpoint), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves Effiliation product feed metadata using cached API response.
     *
     * <p>Official endpoint: /productfeeds.json with params filter, lg, country, etc. :contentReference[oaicite:8]{index=8}
     *
     * @param apiKey Effiliation API key
     * @param lg output language (fr/en/...)
     * @param country optional country filter (comma-separated list accepted by API)
     * @return response JsonNode, or null if retrieval fails
     * @throws Exception on missing cache file or invalid JSON
     */
    public JsonNode retrieveEffiliationFeeds(String apiKey, String lg, String country) throws Exception {
        if (isBlank(apiKey)) {
            throw new IllegalArgumentException("Effiliation API key is blank.");
        }

        String endpoint = buildProductFeedsEndpoint(apiKey, normalizeLg(lg), FILTER_MINES, country, null);

        // Security: do not log the key.
        LOGGER.info("Retrieving Effiliation productfeeds (endpoint='{}').", safeEndpointForLogs(endpoint));

        int cacheTtl = feedConfig.getEffiliation().getCacheTtlDays();
        File cachedResponse = remoteFileCachingService.getResource(endpoint, cacheTtl);
        if (cachedResponse == null || !cachedResponse.exists()) {
            throw new Exception("Effiliation cached response file not found.");
        }

        String jsonResponse = Files.readString(cachedResponse.toPath(), StandardCharsets.UTF_8);
        if (isBlank(jsonResponse)) {
            throw new Exception("Effiliation cached response is empty.");
        }

        return objectMapper.readTree(jsonResponse);
    }

    /* ==========================================================================================
     * Additional Effiliation documented endpoints (cached retrieval)
     * These are implemented as helpers so other services can consume them without reimplementing
     * caching / key masking / URL building.
     * ========================================================================================== */

    /**
     * Links API: banners/text/flash links. :contentReference[oaicite:9]{index=9}
     */
    public JsonNode retrieveEffiliationLinks(String apiKey, String lg, String country) throws Exception {
        String endpoint = buildLinksEndpoint(apiKey, normalizeLg(lg), FILTER_MINES, country, null, null, null, null);
        LOGGER.info("Retrieving Effiliation links (endpoint='{}').", safeEndpointForLogs(endpoint));
        return readCachedJson(endpoint);
    }

    /**
     * Promotional offers (voucher codes / promotions). Listed in Link API methods. :contentReference[oaicite:10]{index=10}
     */
    public JsonNode retrieveEffiliationPromotionalOffers(String apiKey, String lg, String country) throws Exception {
        String endpoint = buildPromotionalOffersEndpoint(apiKey, normalizeLg(lg), FILTER_MINES, country, null, null);
        LOGGER.info("Retrieving Effiliation promotionaloffers (endpoint='{}').", safeEndpointForLogs(endpoint));
        return readCachedJson(endpoint);
    }

    /**
     * Categories API. Listed under "Others". :contentReference[oaicite:11]{index=11}
     */
    public JsonNode retrieveEffiliationCategories(String apiKey, String lg) throws Exception {
        String endpoint = buildCategoriesEndpoint(apiKey, normalizeLg(lg));
        LOGGER.info("Retrieving Effiliation categories (endpoint='{}').", safeEndpointForLogs(endpoint));
        return readCachedJson(endpoint);
    }

    /**
     * Acturss (news/rss) endpoint under "Others". :contentReference[oaicite:12]{index=12}
     */
    public JsonNode retrieveEffiliationActuRss(String apiKey, String lg) throws Exception {
        String endpoint = buildActuRssEndpoint(apiKey, normalizeLg(lg));
        LOGGER.info("Retrieving Effiliation acturss (endpoint='{}').", safeEndpointForLogs(endpoint));
        return readCachedJson(endpoint);
    }

    /**
     * Reporting endpoint. :contentReference[oaicite:13]{index=13}
     */
    public JsonNode retrieveEffiliationReporting(String apiKey, String lg, String type, String start, String end) throws Exception {
        String endpoint = buildReportingEndpoint(apiKey, normalizeLg(lg), type, start, end, null, null);
        LOGGER.info("Retrieving Effiliation reporting (endpoint='{}').", safeEndpointForLogs(endpoint));
        return readCachedJson(endpoint);
    }

    /**
     * Transactions endpoint. :contentReference[oaicite:14]{index=14}
     */
    public JsonNode retrieveEffiliationTransactions(String apiKey, String lg, String start, String end, String dateType) throws Exception {
        String endpoint = buildTransactionsEndpoint(apiKey, normalizeLg(lg), start, end, dateType, null, null);
        LOGGER.info("Retrieving Effiliation transactions (endpoint='{}').", safeEndpointForLogs(endpoint));
        return readCachedJson(endpoint);
    }

    /**
     * UpdatedTransactions endpoint (exists in reporting section navigation). :contentReference[oaicite:15]{index=15}
     *
     * <p>Kept generic because parameters vary depending on how you use it in your project.</p>
     */
    public JsonNode retrieveEffiliationUpdatedTransactions(String apiKey, String lg, String start, String end) throws Exception {
        String endpoint = buildUpdatedTransactionsEndpoint(apiKey, normalizeLg(lg), start, end, null);
        LOGGER.info("Retrieving Effiliation updatedtransactions (endpoint='{}').", safeEndpointForLogs(endpoint));
        return readCachedJson(endpoint);
    }

    /* ==========================================================================================
     * URL builders (official docs + safe encoding)
     * ========================================================================================== */

    private String buildProgramsEndpoint(String apiKey,
                                         String lg,
                                         String filter,
                                         String country,
                                         String categories) {
        // Official: /programs.json supports filter, lg, country, categories... :contentReference[oaicite:16]{index=16}
        return "https://apiv2.effiliation.com/apiv2/programs.json"
                + "?key=" + urlEncode(apiKey)
                + "&filter=" + urlEncode(nullSafe(filter))
                + "&lg=" + urlEncode(lg)
                + (isBlank(country) ? "" : "&country=" + urlEncode(country))
                + (isBlank(categories) ? "" : "&categories=" + urlEncode(categories));
    }

    private String buildProductFeedsEndpoint(String apiKey,
                                            String lg,
                                            String filter,
                                            String country,
                                            String type) {
        // Official: /productfeeds.json supports filter, lg, country, type... :contentReference[oaicite:17]{index=17}
        return "https://apiv2.effiliation.com/apiv2/productfeeds.json"
                + "?key=" + urlEncode(apiKey)
                + "&filter=" + urlEncode(nullSafe(filter))
                + "&lg=" + urlEncode(lg)
                + (isBlank(country) ? "" : "&country=" + urlEncode(country))
                + (isBlank(type) ? "" : "&type=" + urlEncode(type));
    }

    private String buildLinksEndpoint(String apiKey,
                                     String lg,
                                     String filter,
                                     String country,
                                     String categories,
                                     String type,
                                     String sessions,
                                     String mode) {
        // Official: /links.json supports filter, lg, country, categories, type, sessions, mode... :contentReference[oaicite:18]{index=18}
        return "https://apiv2.effiliation.com/apiv2/links.json"
                + "?key=" + urlEncode(apiKey)
                + "&filter=" + urlEncode(nullSafe(filter))
                + "&lg=" + urlEncode(lg)
                + (isBlank(mode) ? "" : "&mode=" + urlEncode(mode))
                + (isBlank(country) ? "" : "&country=" + urlEncode(country))
                + (isBlank(categories) ? "" : "&categories=" + urlEncode(categories))
                + (isBlank(type) ? "" : "&type=" + urlEncode(type))
                + (isBlank(sessions) ? "" : "&sessions=" + urlEncode(sessions));
    }

    private String buildPromotionalOffersEndpoint(String apiKey,
                                                 String lg,
                                                 String filter,
                                                 String country,
                                                 String categories,
                                                 String mode) {
        // Mentioned as Link API method; documentation page not opened here, keep minimal/common params. :contentReference[oaicite:19]{index=19}
        return "https://apiv2.effiliation.com/apiv2/promotionaloffers.json"
                + "?key=" + urlEncode(apiKey)
                + "&filter=" + urlEncode(nullSafe(filter))
                + "&lg=" + urlEncode(lg)
                + (isBlank(mode) ? "" : "&mode=" + urlEncode(mode))
                + (isBlank(country) ? "" : "&country=" + urlEncode(country))
                + (isBlank(categories) ? "" : "&categories=" + urlEncode(categories));
    }

    private String buildCategoriesEndpoint(String apiKey, String lg) {
        // Listed under Others -> Categories.
        return "https://apiv2.effiliation.com/apiv2/categories.json"
                + "?key=" + urlEncode(apiKey)
                + "&lg=" + urlEncode(lg);
    }

    private String buildActuRssEndpoint(String apiKey, String lg) {
        // Listed under Others -> Acturss.
        return "https://apiv2.effiliation.com/apiv2/acturss.json"
                + "?key=" + urlEncode(apiKey)
                + "&lg=" + urlEncode(lg);
    }

    private String buildReportingEndpoint(String apiKey,
                                         String lg,
                                         String type,
                                         String start,
                                         String end,
                                         String fields,
                                         String detail) {
        // Official: /reporting.json supports lg,type,start,end,fields,detail. :contentReference[oaicite:20]{index=20}
        return "https://apiv2.effiliation.com/apiv2/reporting.json"
                + "?key=" + urlEncode(apiKey)
                + "&lg=" + urlEncode(lg)
                + "&type=" + urlEncode(nullSafe(type))
                + "&start=" + urlEncode(nullSafe(start))
                + "&end=" + urlEncode(nullSafe(end))
                + (isBlank(fields) ? "" : "&fields=" + urlEncode(fields))
                + (isBlank(detail) ? "" : "&detail=" + urlEncode(detail));
    }

    private String buildTransactionsEndpoint(String apiKey,
                                            String lg,
                                            String start,
                                            String end,
                                            String type,
                                            String idAffilieur,
                                            String effiId) {
        // Official: /transaction.json exists; parameters include start/end/type and filters like id_affilieur, effi_id. :contentReference[oaicite:21]{index=21}
        return "https://apiv2.effiliation.com/apiv2/transaction.json"
                + "?key=" + urlEncode(apiKey)
                + "&lg=" + urlEncode(lg)
                + "&start=" + urlEncode(nullSafe(start))
                + "&end=" + urlEncode(nullSafe(end))
                + "&type=" + urlEncode(nullSafe(type))
                + (isBlank(idAffilieur) ? "" : "&id_affilieur=" + urlEncode(idAffilieur))
                + (isBlank(effiId) ? "" : "&effi_id=" + urlEncode(effiId));
    }

    private String buildUpdatedTransactionsEndpoint(String apiKey,
                                                   String lg,
                                                   String start,
                                                   String end,
                                                   String fields) {
        // Present in docs nav as UpdatedTransactions. Keep generic.
        return "https://apiv2.effiliation.com/apiv2/updatedtransactions.json"
                + "?key=" + urlEncode(apiKey)
                + "&lg=" + urlEncode(lg)
                + "&start=" + urlEncode(nullSafe(start))
                + "&end=" + urlEncode(nullSafe(end))
                + (isBlank(fields) ? "" : "&fields=" + urlEncode(fields));
    }

    /* ==========================================================================================
     * Cached read helper
     * ========================================================================================== */

    private JsonNode readCachedJson(String endpoint) throws Exception {
        int cacheTtl = feedConfig.getEffiliation().getCacheTtlDays();
        File cachedResponse = remoteFileCachingService.getResource(endpoint, cacheTtl);
        if (cachedResponse == null || !cachedResponse.exists()) {
            throw new Exception("Effiliation cached response file not found.");
        }
        String jsonResponse = Files.readString(cachedResponse.toPath(), StandardCharsets.UTF_8);
        if (isBlank(jsonResponse)) {
            throw new Exception("Effiliation cached response is empty.");
        }
        return objectMapper.readTree(jsonResponse);
    }

    /* ==========================================================================================
     * Language/country heuristics (driven by datasource/country)
     * ========================================================================================== */

    /**
     * Computes a list of likely languages based on program coverage (pays) and common countries.
     *
     * <p>We keep this deterministic & small to avoid extra API calls.</p>
     */
    private List<String> computeLikelyLanguages(Map<String, EffiliationProgram> programs) {
        EnumSet<Lg> langs = EnumSet.noneOf(Lg.class);

        // Attempt to detect from program "pays" / "country"-like fields (model dependent).
        // If model does not provide it, we fall back to always include FR.
        for (EffiliationProgram p : programs.values()) {
            String pays = tryGetPays(p);
            if (!isBlank(pays)) {
                for (String c : splitCountries(pays)) {
                    langs.add(Lg.fromCountry(c));
                }
            }
        }

        // Always include FR to preserve current behavior.
        langs.add(Lg.FR);

        List<String> out = new ArrayList<>();
        for (Lg l : langs) {
            out.add(l.lg);
        }
        return out;
    }

    /**
     * Best-effort extraction of program coverage countries from EffiliationProgram.
     *
     * <p>The official field in programs API is "pays". :contentReference[oaicite:22]{index=22}
     * Your model may expose it as getPays(), getCountry(), etc. We try a few reflective patterns,
     * but avoid throwing (hardening).</p>
     */
    private String tryGetPays(EffiliationProgram p) {
        if (p == null) {
            return null;
        }
        // If your model has getCountry(), you previously referenced it; try it first.
        try {
            //noinspection JavaReflectionMemberAccess
            Object v = p.getClass().getMethod("getCountry").invoke(p);
            return v == null ? null : String.valueOf(v);
        } catch (Exception ignore) {
            // ignore
        }
        // Then try getPays() which matches the documented field name.
        try {
            //noinspection JavaReflectionMemberAccess
            Object v = p.getClass().getMethod("getPays").invoke(p);
            return v == null ? null : String.valueOf(v);
        } catch (Exception ignore) {
            // ignore
        }
        return null;
    }

    private List<String> splitCountries(String pays) {
        if (isBlank(pays)) {
            return List.of();
        }
        String[] parts = pays.split("[,;\\s]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String c = p.trim().toLowerCase(Locale.ROOT);
            if (!c.isEmpty()) {
                out.add(c);
            }
        }
        return out;
    }

    private String normalizeLg(String lg) {
        String v = isBlank(lg) ? "fr" : lg.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_LG.contains(v) ? v : "fr";
    }

    /**
     * Minimal mapping of country -> output language (lg) for Effiliation calls.
     *
     * <p>Countries list is in productfeeds/programs docs. :contentReference[oaicite:23]{index=23}</p>
     */
    private enum Lg {
        FR("fr"), EN("en"), DE("de"), ES("es"), IT("it"), PT("pt"), NL("nl");

        final String lg;

        Lg(String lg) {
            this.lg = lg;
        }

        static Lg fromCountry(String country) {
            if (country == null) {
                return FR;
            }
            switch (country.toLowerCase(Locale.ROOT)) {
                case "uk":
                case "en":
                    return EN;
                case "ge":
                case "de":
                case "at":
                case "ch":
                    return DE;
                case "sp":
                case "es":
                    return ES;
                case "it":
                    return IT;
                case "pt":
                case "br":
                    return PT;
                case "nl":
                case "be":
                    return NL;
                case "fr":
                default:
                    return FR;
            }
        }
    }

    /* ==========================================================================================
     * Low-level helpers
     * ========================================================================================== */

    private String safeEndpointForLogs(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        // Mask key=... (best-effort).
        return endpoint.replaceAll("(key=)([^&]+)", "$1****");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText();
        return isBlank(s) ? null : s;
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
}
