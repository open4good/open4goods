package org.open4goods.services.contribution.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Affiliation network of an outbound {@code /contrib/} click, deduced from the host of the target URL.
 * <p>
 * The {@link #key()} is the value stored in the {@code network} field of {@link AffiliationClick} and the network
 * segment of the Metriks event ids ({@code affiliation.<key>.clicks.<window>}).
 */
public enum AffiliationNetwork {

    /** Awin: {@code awin1.com} and its sub-domains. */
    AWIN("awin", "Awin"),
    /** Effiliation: {@code effiliation.com}, {@code effinity.fr} and their sub-domains. */
    EFFILIATION("effiliation", "Effiliation"),
    /** Kwanko (ex Metaffiliation): {@code metaffiliation.com}, {@code kwanko.com} and their sub-domains. */
    KWANKO("kwanko", "Kwanko"),
    /**
     * TradeTracker: {@code tradetracker.net}, {@code tradetracker.com} and their sub-domains, or a merchant's own
     * domain carrying the TradeTracker link signature ({@link #hasTradeTrackerSignature(String)}).
     */
    TRADETRACKER("tradetracker", "TradeTracker"),
    /** Any other target (direct merchant link, unknown network, unreadable URL). Must stay the last one. */
    OTHER("other", "autres");

    private final String key;
    private final String label;

    AffiliationNetwork(String key, String label) {
        this.key = key;
        this.label = label;
    }

    /**
     * @return a human readable name of the network, for Metriks labels
     */
    public String label() {
        return label;
    }

    /**
     * @return the stored and reported value of the network ({@code awin}, {@code effiliation}, {@code kwanko}, {@code tradetracker}, {@code other})
     */
    public String key() {
        return key;
    }

    /**
     * Deduces the network from a target URL.
     *
     * @param url the affiliation target URL, possibly {@code null} or malformed
     * @return the network of its host, {@link #OTHER} when the URL has no readable host
     */
    public static AffiliationNetwork fromUrl(String url) {
        AffiliationNetwork network = fromHost(hostOf(url));
        if (network == OTHER && hostOf(url) != null && hasTradeTrackerSignature(url)) {
            return TRADETRACKER;
        }
        return network;
    }

    /**
     * Tells whether a URL carries the TradeTracker link signature, used by merchants' own tracking domains: query
     * parameters {@code c}, {@code m} and {@code a} numeric, and {@code u} present.
     *
     * @param url the URL, possibly {@code null}
     * @return {@code true} when the signature is present
     */
    static boolean hasTradeTrackerSignature(String url) {
        if (url == null) {
            return false;
        }
        int start = url.indexOf('?');
        if (start < 0) {
            return false;
        }
        int end = url.indexOf('#', start);
        String query = url.substring(start + 1, end < 0 ? url.length() : end);
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String name = eq < 0 ? pair : pair.substring(0, eq);
            params.putIfAbsent(name, eq < 0 ? "" : pair.substring(eq + 1));
        }
        return isDigits(params.get("c")) && isDigits(params.get("m")) && isDigits(params.get("a"))
                && params.containsKey("u");
    }

    private static boolean isDigits(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Deduces the network from a host name.
     *
     * @param host the host name, case-insensitive, possibly {@code null}
     * @return {@link #AWIN} for {@code awin1.com} and its sub-domains, {@link #EFFILIATION} for
     *         {@code effiliation.com}, {@code effinity.fr} and their sub-domains, {@link #KWANKO} and {@link #TRADETRACKER} for
     *         their hosts, {@link #OTHER} otherwise (the TradeTracker signature needs the full URL: {@link #fromUrl})
     */
    public static AffiliationNetwork fromHost(String host) {
        String normalised = normaliseHost(host);
        if (normalised == null) {
            return OTHER;
        }
        if (matches(normalised, "awin1.com")) {
            return AWIN;
        }
        if (matches(normalised, "effiliation.com") || matches(normalised, "effinity.fr")) {
            return EFFILIATION;
        }
        if (matches(normalised, "metaffiliation.com") || matches(normalised, "kwanko.com")) {
            return KWANKO;
        }
        if (matches(normalised, "tradetracker.net") || matches(normalised, "tradetracker.com")) {
            return TRADETRACKER;
        }
        return OTHER;
    }

    /**
     * Extracts the host of a URL, lower-cased and without trailing dot.
     * <p>
     * Only the authority (between {@code ://} and the first {@code /}, backslash, {@code ?} or {@code #}) is read,
     * so a merchant URL with characters that {@link java.net.URI} rejects in its path, query or fragment (space,
     * {@code |}, braces, lone {@code %}, second {@code #}) still yields its host. User info and port are dropped.
     *
     * @param url the URL, possibly {@code null} or malformed
     * @return its host, or {@code null} when the URL is blank, has no {@code scheme://} prefix or no valid host
     */
    public static String hostOf(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        int schemeEnd = trimmed.indexOf("://");
        if (schemeEnd <= 0 || !isScheme(trimmed.substring(0, schemeEnd))) {
            return null;
        }
        int authorityStart = schemeEnd + 3;
        int authorityEnd = authorityStart;
        while (authorityEnd < trimmed.length() && "/\\?#".indexOf(trimmed.charAt(authorityEnd)) < 0) {
            authorityEnd++;
        }
        String authority = trimmed.substring(authorityStart, authorityEnd);
        String hostAndPort = authority.substring(authority.lastIndexOf('@') + 1);
        String host = stripPort(hostAndPort);
        return host != null && isHostName(host) ? normaliseHost(host) : null;
    }

    private static boolean isScheme(String scheme) {
        if (!Character.isLetter(scheme.charAt(0))) {
            return false;
        }
        for (int i = 1; i < scheme.length(); i++) {
            char c = scheme.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '+' && c != '-' && c != '.') {
                return false;
            }
        }
        return true;
    }

    private static String stripPort(String hostAndPort) {
        int colon = hostAndPort.lastIndexOf(':');
        if (colon < 0) {
            return hostAndPort;
        }
        String port = hostAndPort.substring(colon + 1);
        for (int i = 0; i < port.length(); i++) {
            if (!Character.isDigit(port.charAt(i))) {
                return null;
            }
        }
        return hostAndPort.substring(0, colon);
    }

    private static boolean isHostName(String host) {
        if (host.isEmpty()) {
            return false;
        }
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '.' && c != '_') {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    private static String normaliseHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String lower = host.trim().toLowerCase(Locale.ROOT);
        while (lower.endsWith(".")) {
            lower = lower.substring(0, lower.length() - 1);
        }
        return lower.isEmpty() ? null : lower;
    }
}
