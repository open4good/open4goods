package org.open4goods.nudgerfrontapi.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.open4goods.nudgerfrontapi.config.properties.AffiliationClickRefProperties;
import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.model.AffiliationNetwork;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Writes the id of an {@link AffiliationClick} into the publisher reference parameter of the affiliation network of a
 * redirect URL (GOU-2522), so that a sale reported by the network can be joined to its click.
 * <p>
 * Parameters, as read back by the feedservice transaction reports:
 * <ul>
 * <li>Awin: {@code clickref} (reported as {@code clickRef});</li>
 * <li>TradeTracker: {@code r} (reported as {@code reference});</li>
 * <li>Effiliation: {@code effi_id} (reported as {@code effiId});</li>
 * <li>Kwanko: {@code argsite}, only on {@code mclic} links (reported in {@code argsites});</li>
 * <li>Amazon and other targets: none, the URL is never changed.</li>
 * </ul>
 * Each network is switched on by {@link AffiliationClickRefProperties}, all off by default.
 * </p>
 * <p>
 * The rewrite only touches the reference parameter: every other query pair, the fragment and the encoding of the
 * destination parameter ({@code ued}, {@code p}, {@code u}, {@code url}) are kept byte for byte. An existing reference
 * (a feed that sets its own {@code clickref}) is replaced in place. The reference never goes after the destination
 * parameter, because some networks read everything after an unencoded destination as part of it.
 * </p>
 */
@Service
public class AffiliationClickRefService {

    /** Awin publisher click reference. */
    static final String AWIN_PARAMETER = "clickref";
    /** TradeTracker reference. */
    static final String TRADETRACKER_PARAMETER = "r";
    /** Effiliation publisher sub-id. */
    static final String EFFILIATION_PARAMETER = "effi_id";
    /** Kwanko publisher sub-id. */
    static final String KWANKO_PARAMETER = "argsite";

    private final AffiliationClickRefProperties properties;

    public AffiliationClickRefService(AffiliationClickRefProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the redirect URL carrying the click id in the reference parameter of its network.
     *
     * @param url     affiliation target URL carried by the token, possibly {@code null}
     * @param clickId id of the {@link AffiliationClick} built for this redirect
     * @return the rewritten URL, or {@code url} itself when its network has no reference parameter, is switched off,
     *         or when the click id is blank
     */
    public String withClickRef(String url, String clickId) {
        if (url == null || !StringUtils.hasText(clickId)) {
            return url;
        }
        AffiliationNetwork network = AffiliationNetwork.fromUrl(url);
        return switch (network) {
            case AWIN -> properties.isAwinEnabled()
                    ? setParameter(url, AWIN_PARAMETER, clickId, awinDestinations(url))
                    : url;
            case TRADETRACKER -> properties.isTradetrackerEnabled()
                    ? setParameter(url, TRADETRACKER_PARAMETER, clickId, Set.of("u"))
                    : url;
            case EFFILIATION -> properties.isEffiliationEnabled()
                    ? setParameter(url, EFFILIATION_PARAMETER, clickId, Set.of("url"))
                    : url;
            // A Kwanko link on a merchant's own domain is a bare token (?P...V4): no documented place for argsite.
            case KWANKO -> properties.isKwankoEnabled() && hasParameter(url, "mclic")
                    ? setParameter(url, KWANKO_PARAMETER, clickId, Set.of())
                    : url;
            case AMAZON, OTHER -> url;
        };
    }

    /**
     * Destination parameters of an Awin link: {@code ued}, and {@code p} on {@code cread.php} deep links only
     * ({@code p} is the product id of {@code pclick.php} product links).
     */
    private static Set<String> awinDestinations(String url) {
        return pathOf(url).endsWith("/cread.php") ? Set.of("ued", "p") : Set.of("ued");
    }

    /**
     * Sets one query parameter, keeping every other pair as is.
     *
     * @param url          URL to rewrite
     * @param name         parameter to set; its existing occurrences are removed
     * @param value        raw value, URL-encoded here
     * @param destinations parameters carrying the merchant destination; the new pair goes before the first of them
     * @return the rewritten URL
     */
    static String setParameter(String url, String name, String value, Set<String> destinations) {
        int queryStart = queryStart(url);
        int fragmentStart = fragmentStart(url, queryStart);
        int end = fragmentStart < 0 ? url.length() : fragmentStart;
        String base = queryStart < 0 ? url.substring(0, end) : url.substring(0, queryStart);
        String query = queryStart < 0 ? "" : url.substring(queryStart + 1, end);
        String fragment = fragmentStart < 0 ? "" : url.substring(fragmentStart);

        List<String> pairs = new ArrayList<>();
        int position = -1;
        for (String pair : query.isEmpty() ? List.<String>of() : Arrays.asList(query.split("&", -1))) {
            String pairName = nameOf(pair);
            if (pairName.equals(name)) {
                // Every occurrence is dropped; the new pair takes the place of the first one, or of the destination
                // when that comes first.
                position = position < 0 ? pairs.size() : position;
                continue;
            }
            if (position < 0 && destinations.contains(pairName)) {
                position = pairs.size();
            }
            pairs.add(pair);
        }
        pairs.add(position < 0 ? pairs.size() : position, name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        return base + "?" + String.join("&", pairs) + fragment;
    }

    private static boolean hasParameter(String url, String name) {
        int queryStart = queryStart(url);
        if (queryStart < 0) {
            return false;
        }
        int fragmentStart = fragmentStart(url, queryStart);
        String query = url.substring(queryStart + 1, fragmentStart < 0 ? url.length() : fragmentStart);
        return Arrays.stream(query.split("&")).anyMatch(pair -> nameOf(pair).equals(name));
    }

    private static String pathOf(String url) {
        int queryStart = queryStart(url);
        int fragmentStart = fragmentStart(url, queryStart);
        int end = queryStart >= 0 ? queryStart : fragmentStart >= 0 ? fragmentStart : url.length();
        return url.substring(0, end);
    }

    /** Index of the {@code ?} opening the query, {@code -1} when there is none before the fragment. */
    private static int queryStart(String url) {
        int queryStart = url.indexOf('?');
        int hash = url.indexOf('#');
        return queryStart >= 0 && (hash < 0 || queryStart < hash) ? queryStart : -1;
    }

    /** Index of the {@code #} opening the fragment (the first one after the query), {@code -1} when there is none. */
    private static int fragmentStart(String url, int queryStart) {
        return url.indexOf('#', Math.max(queryStart, 0));
    }

    private static String nameOf(String pair) {
        int eq = pair.indexOf('=');
        return eq < 0 ? pair : pair.substring(0, eq);
    }
}
