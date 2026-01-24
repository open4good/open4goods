package org.open4goods.metriks.providers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.open4goods.metriks.core.MetricDefinition;
import org.open4goods.metriks.core.MetrikPayload;
import org.open4goods.metriks.core.MetrikPayload.EventData;
import org.open4goods.metriks.core.MetrikPayload.PeriodPayload;
import org.open4goods.metriks.core.Period;
import org.open4goods.services.feedservice.dto.AffiliationKpis;
import org.open4goods.services.feedservice.service.AwinFeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider mapping Awin KPIs to metriks payloads.
 */
public class AwinProvider implements MetriksProvider {

    private static final Logger logger = LoggerFactory.getLogger(AwinProvider.class);

    private final AwinFeedService awinFeedService;
    private AffiliationKpis cachedKpis;
    private Period cachedPeriod;

    public AwinProvider(AwinFeedService awinFeedService) {
        this.awinFeedService = awinFeedService;
    }

    @Override
    public Optional<MetrikPayload> collect(MetricDefinition definition, Period period) {
        if (!"awin".equalsIgnoreCase(definition.event_provider())) {
            return Optional.empty();
        }
        try {
            AffiliationKpis kpis = resolveKpis(period);
            BigDecimal value = resolveValue(definition.event_id(), kpis);
            if (value == null) {
                return Optional.of(errorPayload(definition, period, "Unsupported Awin metric id"));
            }
            String unit = resolveUnit(definition.event_id());
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
            logger.error("Failed to fetch Awin KPIs", ex);
            return Optional.of(errorPayload(definition, period, ex.getMessage()));
        }
    }

    private AffiliationKpis resolveKpis(Period period) throws Exception {
        if (cachedKpis != null && period.equals(cachedPeriod)) {
            return cachedKpis;
        }
        cachedKpis = awinFeedService.getKpis(period.startDate(), period.endDate());
        cachedPeriod = period;
        return cachedKpis;
    }

    private BigDecimal resolveValue(String eventId, AffiliationKpis kpis) {
        return switch (eventId) {
            case "awin_clicks" -> BigDecimal.valueOf(kpis.clicks());
            case "awin_impressions" -> BigDecimal.valueOf(kpis.impressions());
            case "awin_transactions_total" -> BigDecimal.valueOf(kpis.transactionsTotal());
            case "awin_transactions_confirmed" -> BigDecimal.valueOf(kpis.transactionsConfirmed());
            case "awin_transactions_pending" -> BigDecimal.valueOf(kpis.transactionsPending());
            case "awin_commission_total" -> kpis.commissionTotal();
            case "awin_turnover_total" -> kpis.turnoverTotal();
            default -> null;
        };
    }

    private String resolveUnit(String eventId) {
        if (eventId.endsWith("commission_total") || eventId.endsWith("turnover_total")) {
            return "currency";
        }
        return "count";
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
                new EventData(null, "count", null),
                null,
                null
        );
    }
}
