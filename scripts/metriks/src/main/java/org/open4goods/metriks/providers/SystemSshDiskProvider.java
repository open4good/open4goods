package org.open4goods.metriks.providers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.open4goods.metriks.config.ServerDefinition;
import org.open4goods.metriks.config.ServersConfig;
import org.open4goods.metriks.core.MetricDefinition;
import org.open4goods.metriks.core.MetrikPayload;
import org.open4goods.metriks.core.MetrikPayload.EventData;
import org.open4goods.metriks.core.MetrikPayload.PeriodPayload;
import org.open4goods.metriks.core.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider collecting disk metrics using SSH and df.
 */
public class SystemSshDiskProvider implements MetriksProvider {

    private static final Logger logger = LoggerFactory.getLogger(SystemSshDiskProvider.class);

    private final ServersConfig serversConfig;
    private final Map<String, DiskStats> cache = new HashMap<>();

    public SystemSshDiskProvider(ServersConfig serversConfig) {
        this.serversConfig = serversConfig;
    }

    @Override
    public Optional<MetrikPayload> collect(MetricDefinition definition, Period period) {
        if (!"system".equalsIgnoreCase(definition.event_provider())) {
            return Optional.empty();
        }
        Map<String, String> params = definition.params();
        if (params == null || !params.containsKey("server") || !params.containsKey("mount")) {
            return Optional.of(errorPayload(definition, period, "Missing server or mount params"));
        }
        String serverKey = params.get("server");
        String mount = params.get("mount");
        String metric = params.getOrDefault("metric", "free_bytes");
        String cacheKey = serverKey + ":" + mount;
        try {
            DiskStats stats = cache.computeIfAbsent(cacheKey, key -> fetchDiskStats(serverKey, mount));
            BigDecimal value;
            String unit;
            switch (metric) {
                case "total_bytes" -> {
                    value = BigDecimal.valueOf(stats.totalBytes());
                    unit = "bytes";
                }
                case "used_percent" -> {
                    value = BigDecimal.valueOf(stats.usedPercent());
                    unit = "percent";
                }
                default -> {
                    value = BigDecimal.valueOf(stats.freeBytes());
                    unit = "bytes";
                }
            }
            return Optional.of(new MetrikPayload(
                    "1.0",
                    definition.event_id(),
                    definition.event_provider(),
                    definition.eventName(),
                    definition.eventDescription(),
                    new PeriodPayload(period.lastPeriodInDays(), period.startDate().toString(), period.endDate().toString()),
                    Instant.now(),
                    "ok",
                    null,
                    new EventData(value, unit, null),
                    null,
                    null
            ));
        } catch (Exception ex) {
            logger.error("Failed to fetch disk stats", ex);
            return Optional.of(errorPayload(definition, period, ex.getMessage()));
        }
    }

    private DiskStats fetchDiskStats(String serverKey, String mount) {
        if (serversConfig == null || serversConfig.servers() == null) {
            throw new IllegalStateException("Servers configuration missing");
        }
        ServerDefinition server = serversConfig.servers().get(serverKey);
        if (server == null) {
            throw new IllegalArgumentException("Server not found: " + serverKey);
        }
        String user = server.user();
        String host = server.host();
        int port = server.port() != null ? server.port() : 22;
        if (user == null || host == null) {
            throw new IllegalArgumentException("Invalid server config for " + serverKey);
        }

        ProcessBuilder builder = new ProcessBuilder(
                "ssh",
                "-p",
                String.valueOf(port),
                user + "@" + host,
                "df",
                "-P",
                "-k",
                mount
        );
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("SSH command timed out");
            }
            if (process.exitValue() != 0) {
                throw new IOException("SSH command failed with exit code " + process.exitValue());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String header = reader.readLine();
                String data = reader.readLine();
                if (data == null) {
                    throw new IOException("Unexpected df output for mount " + mount + " on " + serverKey);
                }
                String[] parts = data.trim().split("\\s+");
                if (parts.length < 6) {
                    throw new IOException("Unable to parse df output: " + data);
                }
                long totalKb = Long.parseLong(parts[1]);
                long usedKb = Long.parseLong(parts[2]);
                long availKb = Long.parseLong(parts[3]);
                String percentRaw = parts[4].replace("%", "");
                int usedPercent = Integer.parseInt(percentRaw);
                return new DiskStats(totalKb * 1024L, availKb * 1024L, usedPercent);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch disk stats via SSH", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to fetch disk stats via SSH", ex);
        }
    }

    private MetrikPayload errorPayload(MetricDefinition definition, Period period, String message) {
        return new MetrikPayload(
                "1.0",
                definition.event_id(),
                definition.event_provider(),
                definition.eventName(),
                definition.eventDescription(),
                new PeriodPayload(period.lastPeriodInDays(), period.startDate().toString(), period.endDate().toString()),
                Instant.now(),
                "error",
                message,
                new EventData(null, "bytes", null),
                null,
                null
        );
    }

    private record DiskStats(long totalBytes, long freeBytes, int usedPercent) {
    }
}
