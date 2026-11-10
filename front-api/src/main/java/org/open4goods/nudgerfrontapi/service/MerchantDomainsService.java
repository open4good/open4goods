package org.open4goods.nudgerfrontapi.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.price.DatasourceOfferActivity;
import org.open4goods.model.price.DatasourceOfferActivityReport;
import org.open4goods.nudgerfrontapi.config.properties.AffiliationPartnersProperties;
import org.open4goods.nudgerfrontapi.config.properties.MerchantDomainsProperties;
import org.open4goods.nudgerfrontapi.model.stats.ActiveMerchantDomains;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.google.common.net.InternetDomainName;

/**
 * Counts the distinct merchant domains that have at least one active offer (GOU-2037).
 * <p>
 * The figure backs the "compare prices from N merchants" claim and the Google CSS 50-domain threshold, so it is
 * computed conservatively, out of any user request, by {@link #refresh()} (hourly):
 * </p>
 * <ol>
 *   <li>The api gives the date of the last offer of each datasource ({@code GET /merchants/offer-activity}),
 *       recorded as offers are aggregated: {@code price.offers} is not indexed, the product index cannot answer.</li>
 *   <li>A datasource is active when its last offer is fresh under the front-api rule
 *       ({@link OfferFreshnessService#getMaxAge()}, {@code front.offers.max-age}).</li>
 *   <li>Its merchant domain is, in this order: the configured {@code front.merchant-domains.datasource-domains}
 *       entry, the portal URL of its affiliation partner, or its name when the name is a domain
 *       ({@code darty.com}). Offer URLs are never used: they are affiliation links (awin1.com...).</li>
 *   <li>The domain is normalised ({@link #normalizeDomain(String)}): host, lower case, without {@code www.}, reduced
 *       to the registrable domain ({@code fr.aliexpress.com} counts as {@code aliexpress.com}).</li>
 *   <li>Affiliation networks ({@code front.merchant-domains.excluded-domains}) are not merchants. A partner whose
 *       known countries do not include FR is outside France.</li>
 * </ol>
 * <p>
 * A datasource without a resolvable domain is not counted: the count can only be too low, never too high.
 * </p>
 */
@Service
public class MerchantDomainsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MerchantDomainsService.class);

    static final String FRANCE = "FR";
    static final String REASON_NETWORK = "affiliation network";
    static final String REASON_OUTSIDE_FRANCE = "outside France";

    private final RestClient restClient;
    private final MerchantDomainsProperties properties;
    private final AffiliationPartnerService affiliationPartnerService;
    private final OfferFreshnessService offerFreshnessService;
    private final Clock clock;
    private final AtomicReference<ActiveMerchantDomains> current = new AtomicReference<>();

    @Autowired
    public MerchantDomainsService(RestClient.Builder restClientBuilder,
                                  AffiliationPartnersProperties partnersProperties,
                                  MerchantDomainsProperties properties,
                                  AffiliationPartnerService affiliationPartnerService,
                                  OfferFreshnessService offerFreshnessService) {
        this(restClientBuilder.baseUrl(partnersProperties.getApiBaseUrl())
                        .defaultHeader(UrlConstants.APIKEY_PARAMETER, partnersProperties.getApiKey())
                        .build(),
                properties, affiliationPartnerService, offerFreshnessService, Clock.systemUTC());
    }

    MerchantDomainsService(RestClient restClient,
                           MerchantDomainsProperties properties,
                           AffiliationPartnerService affiliationPartnerService,
                           OfferFreshnessService offerFreshnessService,
                           Clock clock) {
        this.restClient = restClient;
        this.properties = properties;
        this.affiliationPartnerService = affiliationPartnerService;
        this.offerFreshnessService = offerFreshnessService;
        this.clock = clock;
    }

    /**
     * @return the last computed count, or null when it has never been computed (api unreachable since startup)
     */
    public ActiveMerchantDomains current() {
        return current.get();
    }

    /**
     * Reads the offer activity from the api and recomputes the count. On failure the previous count, with its own
     * date, is kept.
     */
    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "PT1H")
    public void refresh() {
        try {
            DatasourceOfferActivityReport report = restClient.get().uri(properties.getActivityPath()).retrieve()
                    .body(DatasourceOfferActivityReport.class);
            if (report == null) {
                LOGGER.warn("Empty offer activity from the api, keeping the previous merchant domains count");
                return;
            }
            ActiveMerchantDomains computed = compute(report, affiliationPartnerService.getPartners());
            current.set(computed);
            LOGGER.info("{} merchant domains with an active offer (franceOnly={}, tracked since {}): {}",
                    computed.count(), computed.franceOnly(), computed.trackedSince(), computed.domains());
            if (!computed.unresolved().isEmpty() || !computed.excluded().isEmpty()) {
                LOGGER.info("Active datasources not counted: without domain {}, excluded {}",
                        computed.unresolved(), computed.excluded());
            }
            if (computed.trackedSince().plus(offerFreshnessService.getMaxAge()).isAfter(computed.computedAt())) {
                LOGGER.warn("Offer activity tracked since {} only, less than {}: the merchant domains count may be low",
                        computed.trackedSince(), offerFreshnessService.getMaxAge());
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot refresh the merchant domains count, keeping the previous one: {}", e.getMessage());
        }
    }

    /**
     * Counts the distinct merchant domains of the datasources whose last offer is fresh.
     *
     * @param report   last offer date of each datasource
     * @param partners affiliation partners (portal URL, countries), may be null
     * @return the dated count
     */
    ActiveMerchantDomains compute(DatasourceOfferActivityReport report, Collection<AffiliationPartner> partners) {
        Instant now = clock.instant();
        long maxAgeMillis = offerFreshnessService.getMaxAge().toMillis();
        Map<String, AffiliationPartner> partnersByName = partnersByName(partners);
        Map<String, String> configuredDomains = configuredDomains();

        SortedMap<String, Set<String>> domains = new TreeMap<>();
        Set<String> domainsWithUnknownCountry = new TreeSet<>();
        Set<String> unresolved = new TreeSet<>();
        SortedMap<String, String> excluded = new TreeMap<>();

        for (DatasourceOfferActivity activity : report.datasources()) {
            String name = activity.datasourceName();
            // Same rule as OfferFreshnessService.isFresh
            if (name == null || now.toEpochMilli() - activity.lastOfferSeen() > maxAgeMillis) {
                continue;
            }
            AffiliationPartner partner = partnersByName.get(key(name));
            String domain = resolveDomain(name, partner, configuredDomains);
            if (domain == null) {
                unresolved.add(name);
                continue;
            }
            if (isExcludedDomain(domain)) {
                excluded.put(name, REASON_NETWORK + " (" + domain + ")");
                continue;
            }
            Set<String> countries = partner == null || partner.getCountryCodes() == null ? Set.of()
                    : partner.getCountryCodes().stream().filter(Objects::nonNull)
                            .map(c -> c.trim().toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
            if (!countries.isEmpty() && !countries.contains(FRANCE)) {
                excluded.put(name, REASON_OUTSIDE_FRANCE + " " + new TreeSet<>(countries));
                continue;
            }
            if (countries.isEmpty()) {
                domainsWithUnknownCountry.add(domain);
            }
            domains.computeIfAbsent(domain, d -> new TreeSet<>()).add(name);
        }

        SortedMap<String, List<String>> result = new TreeMap<>();
        domains.forEach((domain, names) -> result.put(domain, List.copyOf(names)));
        boolean franceOnly = !result.isEmpty() && domainsWithUnknownCountry.isEmpty();
        return new ActiveMerchantDomains(result, now, franceOnly, Instant.ofEpochMilli(report.trackedSince()),
                new ArrayList<>(unresolved), excluded);
    }

    /**
     * @param partners affiliation partners, may be null
     * @return the partners by matching key ({@link #key(String)})
     */
    private static Map<String, AffiliationPartner> partnersByName(Collection<AffiliationPartner> partners) {
        return partners == null ? Map.of()
                : partners.stream()
                        .filter(p -> p != null && p.getName() != null)
                        .collect(Collectors.toMap(p -> key(p.getName()), p -> p, (a, b) -> a));
    }

    /**
     * @return the configured merchant domain of the datasources by matching key ({@link #key(String)})
     */
    private Map<String, String> configuredDomains() {
        Map<String, String> configuredDomains = new HashMap<>();
        for (MerchantDomainsProperties.DatasourceDomain entry : properties.getDatasourceDomains()) {
            if (entry != null && entry.getDatasource() != null && entry.getDomain() != null) {
                configuredDomains.put(key(entry.getDatasource()), entry.getDomain());
            }
        }
        return configuredDomains;
    }

    /**
     * Merchant domain of a datasource, resolved by the same rules as the active domains count (GOU-2080: the
     * Google CSS eligibility counts the distinct merchants of a product with the rules of the 50 domains
     * threshold, it does not copy them).
     *
     * @param domain  the registrable merchant domain, null when the datasource has none
     * @param network true when the domain is an affiliation network ({@code front.merchant-domains.excluded-domains}),
     *                which is not a merchant
     */
    public record MerchantDomain(String domain, boolean network) {
    }

    /**
     * Builds a resolver of datasource names to merchant domains from the current affiliation partners. The partners
     * and the configuration are read once, so the resolver is meant for a batch (one per run).
     *
     * @return a function giving the merchant domain of a datasource name; its domain is null when unresolved
     */
    public Function<String, MerchantDomain> domainResolver() {
        Map<String, AffiliationPartner> partnersByName = partnersByName(affiliationPartnerService.getPartners());
        Map<String, String> configuredDomains = configuredDomains();
        return name -> {
            if (!StringUtils.hasText(name)) {
                return new MerchantDomain(null, false);
            }
            String domain = resolveDomain(name, partnersByName.get(key(name)), configuredDomains);
            return new MerchantDomain(domain, domain != null && isExcludedDomain(domain));
        };
    }

    /**
     * Resolves the merchant domain of a datasource: configured domain, then partner portal URL, then the datasource
     * name when it is a domain.
     *
     * @return the normalised domain, or null when none is known
     */
    private String resolveDomain(String datasourceName, AffiliationPartner partner, Map<String, String> configuredDomains) {
        String configured = normalizeDomain(configuredDomains.get(key(datasourceName)));
        if (configured != null) {
            return configured;
        }
        String portal = partner == null ? null : normalizeDomain(partner.getPortalUrl());
        if (portal != null) {
            return portal;
        }
        // "darty.com" is a domain, "Carrefour - marketplace" is not
        return datasourceName.trim().contains(" ") ? null : normalizeDomain(datasourceName);
    }

    /**
     * @return true when the domain is a configured affiliation network or one of its subdomains
     */
    private boolean isExcludedDomain(String domain) {
        for (String network : properties.getExcludedDomains()) {
            String excludedDomain = normalizeDomain(network);
            if (excludedDomain != null && (domain.equals(excludedDomain) || domain.endsWith("." + excludedDomain))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalises a URL or host into a merchant domain: host only, lower case, without {@code www.} and reduced to
     * the registrable domain under the public suffix list ({@code https://fr.aliexpress.com/x} gives
     * {@code aliexpress.com}, {@code www.Darty.com} gives {@code darty.com}).
     *
     * @param urlOrHost a URL, with or without scheme, or a host
     * @return the domain, or null when the value holds no public domain name (blank, no dot, IP address...)
     */
    static String normalizeDomain(String urlOrHost) {
        if (!StringUtils.hasText(urlOrHost)) {
            return null;
        }
        String value = urlOrHost.trim();
        String host;
        try {
            host = new URI(value.contains("://") ? value : "http://" + value).getHost();
        } catch (URISyntaxException e) {
            return null;
        }
        if (host == null) {
            return null;
        }
        host = host.toLowerCase(Locale.ROOT);
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        if (!InternetDomainName.isValid(host)) {
            return null;
        }
        InternetDomainName domain = InternetDomainName.from(host);
        if (domain.isUnderPublicSuffix()) {
            return domain.topPrivateDomain().toString();
        }
        // Not under a known public suffix (no dot, unknown TLD, a public suffix itself): not a merchant domain
        return null;
    }

    /**
     * @return the datasource matching key: trimmed, lower case
     */
    private static String key(String datasourceName) {
        return datasourceName.trim().toLowerCase(Locale.ROOT);
    }
}
