package org.open4goods.nudgerfrontapi.service.opendata;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

/**
 * Health component {@code openDataManifest}: freshness of the published open data files, read from their manifest
 * (GOU-2290).
 * <ul>
 * <li>{@code UNKNOWN}: {@code open-data-config.public-base-url} is empty, the manifest is not read (beta, local).</li>
 * <li>{@code DEGRADED}: the manifest cannot be read (404, network, invalid JSON), the GTIN or ISBN dataset is missing
 * or has no valid date, or a dataset is older than {@code front.opendata.max-age} (8 days).</li>
 * <li>{@code UP} otherwise.</li>
 * </ul>
 * {@code DEGRADED} is a custom status, absent from the status order: like the {@code ALERT} and {@code WARN} of the
 * api's {@code impactScoreGuard}, it never turns the aggregate health of front-api DOWN (which would block its
 * restarts on a stale file). Ops alerts on {@code /actuator/health/openDataManifest} or on
 * {@code opendata_dataset_age_seconds}.
 */
@Component
public class OpenDataManifestHealthIndicator implements HealthIndicator {

    /** Freshness problem of the published files; does not change the aggregate status. */
    public static final Status DEGRADED = new Status("DEGRADED",
            "Open data manifest unreadable or older than front.opendata.max-age");

    private static final List<String> REQUIRED_DATASETS = List.of(OpenDataManifestService.GTIN,
            OpenDataManifestService.ISBN);

    private final OpenDataManifestService manifestService;

    /**
     * @param manifestService reader of the published manifest
     */
    public OpenDataManifestHealthIndicator(OpenDataManifestService manifestService) {
        this.manifestService = manifestService;
    }

    /**
     * Reads the cached manifest (one read per cache TTL at most) and checks every dataset.
     *
     * @return UNKNOWN when not configured, DEGRADED on a problem, UP otherwise
     */
    @Override
    public Health health() {
        OpenDataManifestSnapshot snapshot = manifestService.snapshot();
        if (snapshot.state() == OpenDataManifestSnapshot.State.NOT_CONFIGURED) {
            return Health.unknown()
                    .withDetail("configured", false)
                    .withDetail("reason", "open-data-config.public-base-url is empty: the manifest is not read")
                    .build();
        }

        List<String> problems = new ArrayList<>();
        Map<String, Object> ages = new LinkedHashMap<>();
        Duration maxAge = manifestService.maxAge();

        if (snapshot.state() == OpenDataManifestSnapshot.State.UNAVAILABLE) {
            problems.add("manifest unavailable: " + snapshot.error());
        }
        if (snapshot.manifest() != null) {
            for (String required : REQUIRED_DATASETS) {
                if (snapshot.dataset(required).isEmpty()) {
                    problems.add("dataset " + required + " missing from the manifest");
                }
            }
            snapshot.manifest().datasets().stream().filter(Objects::nonNull).forEach(dataset -> {
                Optional<Duration> age = manifestService.age(dataset);
                if (age.isEmpty()) {
                    problems.add("dataset " + dataset.id() + " has no valid generatedAt");
                    return;
                }
                ages.put(String.valueOf(dataset.id()), age.get().toSeconds());
                if (age.get().compareTo(maxAge) > 0) {
                    problems.add("dataset " + dataset.id() + " generated " + age.get().toDays() + " days ago, more than "
                            + maxAge.toDays());
                }
            });
        }

        Health.Builder builder = problems.isEmpty() ? Health.up() : Health.status(DEGRADED);
        builder.withDetail("configured", true)
                .withDetail("manifestUrl", manifestService.manifestUrl())
                .withDetail("state", snapshot.state().name())
                .withDetail("maxAgeSeconds", maxAge.toSeconds())
                .withDetail("datasetAgeSeconds", ages);
        if (snapshot.manifest() != null && snapshot.manifest().generatedAt() != null) {
            builder.withDetail("generatedAt", snapshot.manifest().generatedAt());
        }
        if (snapshot.readAt() != null) {
            builder.withDetail("readAt", snapshot.readAt().toString());
        }
        if (!problems.isEmpty()) {
            builder.withDetail("problems", problems);
        }
        return builder.build();
    }
}
