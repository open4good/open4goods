package org.open4goods.metriks.providers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.open4goods.metriks.core.MetricDefinition;
import org.open4goods.metriks.core.MetrikPayload;
import org.open4goods.metriks.core.MetrikPayload.EventData;
import org.open4goods.metriks.core.MetrikPayload.PeriodPayload;
import org.open4goods.metriks.core.Period;
import org.open4goods.services.feedservice.dto.AffiliationKpis;
import org.open4goods.services.feedservice.service.EffiliationFeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider mapping Effiliation KPIs to metriks payloads.
 */
public class EffiliationProvider implements MetriksProvider {

    private static final Logger logger = LoggerFactory.getLogger(EffiliationProvider.class);

    private final EffiliationFeedService effiliationFeedService;
    private AffiliationKpis cachedKpis;
    private Period cachedPeriod;

    public EffiliationProvider(EffiliationFeedService effiliationFeedService) {
        this.effiliationFeedService = effiliationFeedService;
    }

    @Override
    public Optional<MetrikPayload> collect(MetricDefinition definition, Period period) {
        if (!"effiliation".equalsIgnoreCase(definition.event_provider())) {
            return Optional.empty();
        }
        try {
            AffiliationKpis kpis = resolveKpis(period);
            BigDecimal value = resolveValue(definition.event_id(), kpis);
            if (value == null) {
                return Optional.of(errorPayload(definition, period, "Unsupported Effiliation metric id"));
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
            logger.error("Failed to fetch Effiliation KPIs", ex);
            return Optional.of(errorPayload(definition, period, ex.getMessage()));
        }
    }

    private AffiliationKpis resolveKpis(Period period) throws Exception {
        if (cachedKpis != null && period.equals(cachedPeriod)) {
            return cachedKpis;
        }
        cachedKpis = effiliationFeedService.getKpis(period.startDate(), period.endDate());
        cachedPeriod = period;
        return cachedKpis;
    }

    private BigDecimal resolveValue(String eventId, AffiliationKpis kpis) {
        return switch (eventId) {
            case "effiliation_clicks" -> BigDecimal.valueOf(kpis.clicks());
            case "effiliation_impressions" -> BigDecimal.valueOf(kpis.impressions());
            case "effiliation_transactions_total" -> BigDecimal.valueOf(kpis.transactionsTotal());
            case "effiliation_transactions_confirmed" -> BigDecimal.valueOf(kpis.transactionsConfirmed());
            case "effiliation_transactions_pending" -> BigDecimal.valueOf(kpis.transactionsPending());
            case "effiliation_commission_total" -> kpis.commissionTotal();
            case "effiliation_turnover_total" -> kpis.turnoverTotal();
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
