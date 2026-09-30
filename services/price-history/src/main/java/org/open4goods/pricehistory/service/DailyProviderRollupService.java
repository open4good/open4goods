package org.open4goods.pricehistory.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.open4goods.pricehistory.model.DailyOfferPresence;
import org.open4goods.pricehistory.model.DailyProviderRollup;
import org.open4goods.pricehistory.model.DailyRollupKey;
import org.open4goods.pricehistory.model.DailyRollupWindow;
import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.port.DailyProviderRollupStore;

/**
 * Materializes provider-day rollups from presence and sparse change inputs.
 *
 * <p>Presence, including unchanged offers, is the source of amount bounds, close order and
 * observed-offer count. Sparse changes only contribute the change counter. Ordinary ingestion
 * can update the current and previous UTC buckets; callers use {@link #rebuild} for an older
 * date after explicit repair authorization.
 */
public final class DailyProviderRollupService {

    private static final Comparator<PresenceValue> CLOSE_ORDER = Comparator
            .comparing(PresenceValue::observedAt)
            .thenComparing(PresenceValue::stableId);

    private final DailyProviderRollupStore store;
    private final DailyRollupWindow window;

    /** Creates a daily materializer with its durable output and open-bucket policy. */
    public DailyProviderRollupService(DailyProviderRollupStore store, DailyRollupWindow window) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.window = Objects.requireNonNull(window, "window must not be null");
    }

    /**
     * Updates only buckets still open to late observations.
     *
     * @param presences independently persisted offer-presence inputs
     * @param events sparse change events
     * @return number of buckets written
     */
    public int updateOpenBuckets(Stream<DailyOfferPresence> presences, Stream<PriceChangeEvent> events) {
        return materialize(presences, events, false);
    }

    /**
     * Rebuilds buckets regardless of their date after an explicit repair operation.
     *
     * @param presences independently persisted offer-presence inputs
     * @param events sparse change events
     * @return number of buckets written
     */
    public int rebuild(Stream<DailyOfferPresence> presences, Stream<PriceChangeEvent> events) {
        return materialize(presences, events, true);
    }

    private int materialize(Stream<DailyOfferPresence> presences, Stream<PriceChangeEvent> events, boolean repair) {
        Objects.requireNonNull(presences, "presences must not be null");
        Objects.requireNonNull(events, "events must not be null");
        Map<Bucket, List<PresenceValue>> groupedPresence = new HashMap<>();
        presences.forEach(presence -> {
            Bucket bucket = new Bucket(presence.rollupKey(), utcDay(presence.observedAt()));
            groupedPresence.computeIfAbsent(bucket, unused -> new ArrayList<>())
                    .add(new PresenceValue(presence.key().externalForm(), presence.amount(), presence.observedAt()));
        });
        Map<Bucket, Long> changeCounts = new HashMap<>();
        events.forEach(event -> {
            Bucket bucket = new Bucket(new DailyRollupKey(event.key().gtin(), event.key().providerId(), event.condition(),
                    event.currency()), utcDay(event.observedAt()));
            changeCounts.merge(bucket, 1L, Long::sum);
        });
        int writes = 0;
        for (Map.Entry<Bucket, List<PresenceValue>> entry : groupedPresence.entrySet()) {
            Bucket bucket = entry.getKey();
            if (!repair && !window.isOpen(bucket.day())) {
                continue;
            }
            List<PresenceValue> values = entry.getValue();
            PresenceValue close = values.stream().max(CLOSE_ORDER).orElseThrow();
            BigDecimal minimum = values.stream().map(PresenceValue::amount).min(BigDecimal::compareTo).orElseThrow();
            BigDecimal maximum = values.stream().map(PresenceValue::amount).max(BigDecimal::compareTo).orElseThrow();
            long offers = values.stream().map(PresenceValue::stableId).distinct().count();
            Instant first = values.stream().map(PresenceValue::observedAt).min(Instant::compareTo).orElseThrow();
            Instant last = values.stream().map(PresenceValue::observedAt).max(Instant::compareTo).orElseThrow();
            store.upsert(new DailyProviderRollup(bucket.key(), bucket.day(), minimum, maximum, close.amount(), offers,
                    first, last, changeCounts.getOrDefault(bucket, 0L)));
            writes++;
        }
        return writes;
    }

    private static LocalDate utcDay(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private record Bucket(DailyRollupKey key, LocalDate day) { }

    private record PresenceValue(String stableId, BigDecimal amount, Instant observedAt) { }
}
