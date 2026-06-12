package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the in-process MaxMind GeoLite2 City lookup used to compute
 * user-to-factory distances. Disabled by default: when disabled (or when the
 * database is unavailable), {@code UserGeolocationServiceImpl} falls back to the
 * geocode microservice, so enabling this is a pure latency optimisation.
 *
 * <p>City database only (no ASN) to keep the heap footprint minimal on the
 * OOM-sensitive front-api: the reader is memory-mapped and the cache is bounded.
 */
@ConfigurationProperties(prefix = "frontapi.geoip")
public record GeoIpProperties(
        boolean enabled,
        String dbPath,
        String editionId,
        String downloadUrl,
        String licenseKey,
        long refreshMs,
        long initialDelayMs) {

    public GeoIpProperties {
        dbPath = (dbPath == null || dbPath.isBlank()) ? "./.cache/geoip/GeoLite2-City.mmdb" : dbPath;
        editionId = (editionId == null || editionId.isBlank()) ? "GeoLite2-City" : editionId;
        downloadUrl = (downloadUrl == null || downloadUrl.isBlank())
                ? "https://download.maxmind.com/app/geoip_download" : downloadUrl;
        refreshMs = refreshMs <= 0 ? 604_800_000L : refreshMs;
        initialDelayMs = initialDelayMs < 0 ? 10_000L : initialDelayMs;
    }
}
