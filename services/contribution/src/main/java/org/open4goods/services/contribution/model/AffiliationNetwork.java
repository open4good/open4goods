package org.open4goods.services.contribution.model;

import java.net.URI;
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
     *
     * @param url the URL, possibly {@code null} or malformed
     * @return its host, or {@code null} when the URL is blank, malformed or has no host
     */
    public static String hostOf(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            return normaliseHost(URI.create(url.trim()).getHost());
        } catch (IllegalArgumentException e) {
            return null;
        }
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
