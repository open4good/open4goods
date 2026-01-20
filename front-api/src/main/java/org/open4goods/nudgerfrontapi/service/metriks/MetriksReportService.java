package org.open4goods.nudgerfrontapi.service.metriks;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.open4goods.nudgerfrontapi.dto.metriks.MetriksReportDto;
import org.open4goods.nudgerfrontapi.dto.metriks.MetriksReportRowDto;
import org.open4goods.nudgerfrontapi.dto.metriks.MetriksReportValueDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service that loads metriks history files and computes variations.
 */
@Service
public class MetriksReportService {

    private static final Logger logger = LoggerFactory.getLogger(MetriksReportService.class);

    private final ObjectMapper objectMapper;
    private final Path historyRoot;

    public MetriksReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.historyRoot = Path.of("metriks/history");
    }

    /**
     * Build the metriks report.
     *
     * @param limit max number of columns to include
     * @param includePayload include raw payloads in the response
     * @param domainLanguage domain language (reserved for future localisation)
     * @return aggregated report
     */
    public MetriksReportDto buildReport(int limit, boolean includePayload, DomainLanguage domainLanguage) {
        if (!Files.exists(historyRoot)) {
            return new MetriksReportDto(List.of(), List.of());
        }

        List<MetriksReportRowDto> rows = new ArrayList<>();
        try {
            List<Path> providerFolders = Files.list(historyRoot)
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());

            for (Path providerFolder : providerFolders) {
                List<Path> eventFolders = Files.list(providerFolder)
                        .filter(Files::isDirectory)
                        .collect(Collectors.toList());

                for (Path eventFolder : eventFolders) {
                    List<Path> files = Files.list(eventFolder)
                            .filter(path -> path.getFileName().toString().endsWith(".json"))
                            .filter(path -> !"latest.json".equals(path.getFileName().toString()))
                            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                            .collect(Collectors.toList());

                    if (files.isEmpty()) {
                        continue;
                    }

                    List<String> dateKeys = files.stream()
                            .map(path -> path.getFileName().toString().replace(".json", ""))
                            .sorted()
                            .collect(Collectors.toList());
                    List<String> limitedDateKeys = dateKeys.stream()
                            .skip(Math.max(0, dateKeys.size() - limit))
                            .collect(Collectors.toList());

                    List<MetriksPayloadSnapshot> snapshots = new ArrayList<>();
                    for (Path file : files) {
                        String dateKey = file.getFileName().toString().replace(".json", "");
                        if (!limitedDateKeys.contains(dateKey)) {
                            continue;
                        }
                        snapshots.add(readSnapshot(file, dateKey, includePayload));
                    }

                    snapshots.sort(Comparator.comparing(MetriksPayloadSnapshot::dateKey));
                    rows.add(buildRow(providerFolder.getFileName().toString(),
                            eventFolder.getFileName().toString(),
                            snapshots));
                }
            }
        } catch (IOException ex) {
            logger.error("Failed to read metriks history", ex);
        }

        List<String> columns = rows.stream()
                .flatMap(row -> row.values().stream())
                .map(MetriksReportValueDto::dateKey)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return new MetriksReportDto(columns, rows);
    }

    private MetriksReportRowDto buildRow(String provider, String eventId, List<MetriksPayloadSnapshot> snapshots) {
        String name = null;
        String description = null;
        String unit = null;
        String eventUrl = null;
        List<MetriksReportValueDto> values = new ArrayList<>();

        BigDecimal previousValue = null;
        for (MetriksPayloadSnapshot snapshot : snapshots) {
            MetriksPayload payload = snapshot.payload;
            if (payload != null) {
                name = payload.eventName;
                description = payload.eventDescription;
                unit = payload.eventData != null ? payload.eventData.unit : unit;
                eventUrl = payload.eventUrl != null ? payload.eventUrl : eventUrl;
            }

            BigDecimal currentValue = payload != null && "ok".equalsIgnoreCase(payload.status)
                    && payload.eventData != null ? payload.eventData.value : null;
            BigDecimal variationAbs = null;
            BigDecimal variationPct = null;
            if (currentValue != null && previousValue != null) {
                variationAbs = currentValue.subtract(previousValue);
                if (previousValue.compareTo(BigDecimal.ZERO) != 0) {
                    variationPct = variationAbs
                            .divide(previousValue, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
            }

            values.add(new MetriksReportValueDto(
                    snapshot.dateKey,
                    currentValue,
                    variationAbs,
                    variationPct,
                    snapshot.payloadNode
            ));
            if (currentValue != null) {
                previousValue = currentValue;
            }
        }

        return new MetriksReportRowDto(
                provider,
                eventId,
                Objects.requireNonNullElse(name, eventId),
                Objects.requireNonNullElse(description, ""),
                Objects.requireNonNullElse(unit, "count"),
                eventUrl,
                values
        );
    }

    private MetriksPayloadSnapshot readSnapshot(Path file, String dateKey, boolean includePayload) {
        try {
            JsonNode payloadNode = includePayload ? objectMapper.readTree(file.toFile()) : null;
            MetriksPayload payload = objectMapper.readValue(file.toFile(), MetriksPayload.class);
            return new MetriksPayloadSnapshot(dateKey, payload, payloadNode);
        } catch (IOException ex) {
            logger.warn("Unable to parse metriks payload {}: {}", file, ex.getMessage());
            return new MetriksPayloadSnapshot(dateKey, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class MetriksPayload {

        private String event_provider;
        private String event_id;
        private String eventName;
        private String eventDescription;
        private String status;
        private String eventUrl;
        private EventData eventData;

        public String getEvent_provider() {
            return event_provider;
        }

        public String getEvent_id() {
            return event_id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class EventData {

        private BigDecimal value;
        private String unit;
    }

    private static final class MetriksPayloadSnapshot {

        private final String dateKey;
        private final MetriksPayload payload;
        private final JsonNode payloadNode;
        private MetriksPayloadSnapshot(String dateKey, MetriksPayload payload, JsonNode payloadNode) {
            this.dateKey = dateKey;
            this.payload = payload;
            this.payloadNode = payloadNode;
        }

        public String dateKey() {
            return dateKey;
        }
    }
}
