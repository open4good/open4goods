package org.open4goods.nudgerfrontapi.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reading of the published open data manifest ({@code {open-data-config.public-base-url}/manifest.json}) by front-api
 * (GOU-2290).
 * <p>
 * The manifest is the source of {@code /opendata/*} (rows, date, size, sha256), of the {@code openDataManifest} health
 * component and of the {@code opendata.dataset.age} gauge. Nothing is read while {@code open-data-config.public-base-url}
 * is empty.
 * </p>
 */
@ConfigurationProperties(prefix = "front.opendata")
public class OpenDataManifestProperties {

    /**
     * How long a read of the manifest, successful or not, is reused before the next read. Matches the
     * {@code Cache-Control: max-age=300} of the manifests on R2.
     */
    private Duration manifestCacheTtl = Duration.ofMinutes(5);

    /**
     * Connection timeout of a manifest read.
     */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /**
     * Read timeout of a manifest read.
     */
    private Duration readTimeout = Duration.ofSeconds(3);

    /**
     * Age of a published dataset (now minus its {@code generatedAt}) above which the {@code openDataManifest} health
     * component turns {@code DEGRADED}. The generation is weekly: 8 days leave one day of margin.
     */
    private Duration maxAge = Duration.ofDays(8);

    public Duration getManifestCacheTtl() {
        return manifestCacheTtl;
    }

    public void setManifestCacheTtl(Duration manifestCacheTtl) {
        this.manifestCacheTtl = manifestCacheTtl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Duration maxAge) {
        this.maxAge = maxAge;
    }
}
