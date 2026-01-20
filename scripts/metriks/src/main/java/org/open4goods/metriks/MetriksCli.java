package org.open4goods.metriks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.metriks.config.ServersConfig;
import org.open4goods.metriks.core.MetricDefinition;
import org.open4goods.metriks.core.MetrikPayload;
import org.open4goods.metriks.core.MetriksWriter;
import org.open4goods.metriks.core.Period;
import org.open4goods.metriks.providers.AwinProvider;
import org.open4goods.metriks.providers.EffiliationProvider;
import org.open4goods.metriks.providers.GithubProvider;
import org.open4goods.metriks.providers.MetriksProvider;
import org.open4goods.metriks.providers.SystemSshDiskProvider;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.feedservice.service.AwinFeedService;
import org.open4goods.services.feedservice.service.EffiliationFeedService;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * CLI entry point for metriks collection.
 */
public final class MetriksCli {

    private static final Logger logger = LoggerFactory.getLogger(MetriksCli.class);

    private static final String DEFAULT_METRICS_CONFIG = "scripts/metriks/config/metrics.json";
    private static final String DEFAULT_SERVERS_CONFIG = "scripts/metriks/config/servers.json";

    private MetriksCli() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> arguments = parseArgs(args);
        boolean dryRun = Boolean.parseBoolean(arguments.getOrDefault("dryRun", "false"));
        int lastPeriodInDays = Integer.parseInt(arguments.getOrDefault("lastPeriodInDays", "7"));
        String metricsConfigPath = arguments.getOrDefault("config", DEFAULT_METRICS_CONFIG);
        String dateOverride = arguments.get("dateOverride");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();

        Period period = Period.from(lastPeriodInDays,
                dateOverride != null ? LocalDate.parse(dateOverride) : null);

        List<MetricDefinition> metrics = loadMetrics(objectMapper, metricsConfigPath);
        ServersConfig serversConfig = loadServersConfig(objectMapper, DEFAULT_SERVERS_CONFIG);

        List<MetriksProvider> providers = new ArrayList<>();
        providers.add(new GithubProvider(objectMapper,
                System.getenv("GITHUB_REPOSITORY"),
                System.getenv("GITHUB_TOKEN")));
        providers.add(new SystemSshDiskProvider(serversConfig));
        providers.add(new AwinProvider(buildAwinService()));
        providers.add(new EffiliationProvider(buildEffiliationService()));

        MetriksWriter writer = new MetriksWriter(objectMapper, Path.of("metriks/history"));
        int success = 0;

        for (MetricDefinition definition : metrics) {
            Optional<MetrikPayload> payload = providers.stream()
                    .map(provider -> provider.collect(definition, period))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();

            MetrikPayload resolved = payload.orElseGet(() -> new MetrikPayload(
                    "1.0",
                    definition.event_id(),
                    definition.event_provider(),
                    definition.eventName(),
                    definition.eventDescription(),
                    new MetrikPayload.PeriodPayload(period.lastPeriodInDays(),
                            period.startDate().toString(),
                            period.endDate().toString()),
                    java.time.Instant.now(),
                    "error",
                    "No provider found for event",
                    new MetrikPayload.EventData(null, "count", null),
                    null,
                    null
            ));

            if (!dryRun) {
                writer.write(resolved, period.dateKey());
            }
            success++;
            logger.info("Processed {}:{}", definition.event_provider(), definition.event_id());
        }

        if (dryRun) {
            logger.info("Dry run enabled. {} metrics processed without writing files.", success);
        } else {
            logger.info("{} metrics written to metriks/history.", success);
        }
    }

    private static List<MetricDefinition> loadMetrics(ObjectMapper mapper, String configPath) throws IOException {
        Path path = Path.of(configPath);
        if (!Files.exists(path)) {
            throw new IOException("metrics.json not found at " + configPath);
        }
        return mapper.readValue(path.toFile(), new TypeReference<>() {
        });
    }

    private static ServersConfig loadServersConfig(ObjectMapper mapper, String configPath) {
        Path path = Path.of(configPath);
        if (!Files.exists(path)) {
            return new ServersConfig(Map.of(), List.of());
        }
        try {
            return mapper.readValue(path.toFile(), ServersConfig.class);
        } catch (IOException ex) {
            logger.warn("Failed to read servers config: {}", ex.getMessage());
            return new ServersConfig(Map.of(), List.of());
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsed = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            String trimmed = arg.substring(2);
            if (trimmed.contains("=")) {
                String[] parts = trimmed.split("=", 2);
                parsed.put(parts[0], parts[1]);
            } else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                parsed.put(trimmed, args[i + 1]);
                i++;
            } else {
                parsed.put(trimmed, "true");
            }
        }
        return parsed;
    }

    private static AwinFeedService buildAwinService() {
        RemoteFileCachingService cachingService = new RemoteFileCachingService("scripts/metriks/.cache");
        SerialisationService serialisationService = new SerialisationService();
        DataSourceConfigService dataSourceConfigService = new DataSourceConfigService("scripts/metriks/.cache");
        FeedConfiguration feedConfiguration = new FeedConfiguration();
        String advertiserId = System.getenv("AWIN_ADVERTISER_ID");
        String accessToken = System.getenv("AWIN_ACCESS_TOKEN");
        return new AwinFeedService(feedConfiguration, cachingService, dataSourceConfigService,
                serialisationService, advertiserId, accessToken);
    }

    private static EffiliationFeedService buildEffiliationService() {
        RemoteFileCachingService cachingService = new RemoteFileCachingService("scripts/metriks/.cache");
        SerialisationService serialisationService = new SerialisationService();
        DataSourceConfigService dataSourceConfigService = new DataSourceConfigService("scripts/metriks/.cache");
        FeedConfiguration feedConfiguration = new FeedConfiguration();
        String apiKey = System.getenv("EFFILIATION_API_KEY");
        return new EffiliationFeedService(feedConfiguration, cachingService, dataSourceConfigService,
                serialisationService, apiKey);
    }
}
