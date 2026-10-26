package org.open4goods.services.contribution.model;

import java.util.Locale;

/**
 * Affiliation network of an outbound {@code /contrib/} click, deduced from the host of the target URL.
 * <p>
 * The {@link #key()} is the value stored in the {@code network} field of {@link AffiliationClick} and the network
 * segment of the Metriks event ids ({@code affiliation.<key>.clicks.<window>}).
 */
public enum AffiliationNetwork {

    /** Awin: {@code awin1.com} and its sub-domains. */
    AWIN("awin"),
    /** Effiliation: {@code effiliation.com}, {@code effinity.fr} and their sub-domains. */
    EFFILIATION("effiliation"),
    /** Any other target (direct merchant link, unknown network, unreadable URL). */
    OTHER("other");

    private final String key;

    AffiliationNetwork(String key) {
        this.key = key;
    }

    /**
     * @return the stored and reported value of the network ({@code awin}, {@code effiliation}, {@code other})
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
        return fromHost(hostOf(url));
    }

    /**
     * Deduces the network from a host name.
     *
     * @param host the host name, case-insensitive, possibly {@code null}
     * @return {@link #AWIN} for {@code awin1.com} and its sub-domains, {@link #EFFILIATION} for
     *         {@code effiliation.com}, {@code effinity.fr} and their sub-domains, {@link #OTHER} otherwise
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
