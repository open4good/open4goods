package org.open4goods.b2bapi.service.pricehistory;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.pricehistory.model.DailyProviderRollup;
import org.open4goods.pricehistory.model.DailyRollupKey;
import org.open4goods.pricehistory.model.LegacyPriceBackfill;
import org.open4goods.pricehistory.model.OfferAvailability;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.OfferKey;
import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.model.PriceChangeKind;
import org.open4goods.pricehistory.model.PriceHistoryPage;
import org.open4goods.pricehistory.model.PriceHistoryQuery;
import org.open4goods.pricehistory.port.PriceHistoryQueryPort;
import org.open4goods.pricehistory.service.PriceHistoryCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Read-only {@link PriceHistoryQueryPort} adapter over the dedicated price-history Elasticsearch
 * data streams ({@code o4g-price-change-*} and {@code o4g-daily-provider-rollup-*}). Never reads the
 * Product index - every query below is scoped by {@code gtin} directly against a time-series data
 * stream, so serving this facet never scans Product documents (GOU-28 AC8).
 *
 * <p>Cursor stability: pages are ordered by (timestamp, stable id) ascending and paginated with
 * Elasticsearch {@code search_after}, encoded through {@link PriceHistoryCursor}. A fixed query
 * (same filters) therefore always resumes at the same exclusive position.
 *
 * <p>Storage convention for flattened references (no structured mapping exists for these two
 * fields in the time-series index templates): {@code policy_ref} is stored as
 * {@code "<policyId>:<version>"} and {@code content_hash} as {@code "<algorithm>:<hex>"}. A
 * document that does not follow this convention is skipped defensively rather than served with a
 * guessed policy reference.
 */
@Repository
public class EsPriceHistoryQueryAdapter implements PriceHistoryQueryPort {

    /** Data stream backing sparse price-change events (retained 24 months). */
    public static final String PRICE_CHANGE_INDEX = "o4g-price-change";
    /** Data stream backing daily provider rollups (retained 5 years). */
    public static final String DAILY_ROLLUP_INDEX = "o4g-daily-provider-rollup";

    private static final Logger LOGGER = LoggerFactory.getLogger(EsPriceHistoryQueryAdapter.class);

    private final ElasticsearchOperations elasticsearchOperations;

    public EsPriceHistoryQueryAdapter(final ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = Objects.requireNonNull(elasticsearchOperations, "elasticsearchOperations must not be null");
    }

    @Override
    public PriceHistoryPage<PriceChangeEvent> queryChanges(final PriceHistoryQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        final NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(buildFilterQuery(query, "observed_at"))
                .withSort(Sort.by(Sort.Order.asc("observed_at"), Sort.Order.asc("event_id")))
                .withMaxResults(query.pageSize() + 1);
        query.cursor().ifPresent(cursor -> applySearchAfter(builder, cursor));

        final SearchHits<EsPriceChangeEventDocument> hits = elasticsearchOperations.search(
                builder.build(), EsPriceChangeEventDocument.class, IndexCoordinates.of(PRICE_CHANGE_INDEX));
        final List<SearchHit<EsPriceChangeEventDocument>> allHits = hits.getSearchHits();
        final boolean hasMore = allHits.size() > query.pageSize();
        final List<SearchHit<EsPriceChangeEventDocument>> page = hasMore ? allHits.subList(0, query.pageSize()) : allHits;

        final List<PriceChangeEvent> values = new ArrayList<>(page.size());
        for (final SearchHit<EsPriceChangeEventDocument> hit : page) {
            toDomain(hit.getContent()).ifPresent(values::add);
        }

        Optional<String> nextCursor = Optional.empty();
        if (hasMore && !page.isEmpty()) {
            final EsPriceChangeEventDocument last = page.get(page.size() - 1).getContent();
            nextCursor = Optional.of(PriceHistoryCursor.after(last.getObservedAt(), last.getEventId()));
        }
        return new PriceHistoryPage<>(values, nextCursor);
    }

    @Override
    public PriceHistoryPage<DailyProviderRollup> queryDaily(final PriceHistoryQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        final NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(buildFilterQuery(query, "@timestamp"))
                .withSort(Sort.by(Sort.Order.asc("@timestamp"), Sort.Order.asc("provider_id"),
                        Sort.Order.asc("condition"), Sort.Order.asc("currency")))
                .withMaxResults(query.pageSize() + 1);
        query.cursor().ifPresent(cursor -> applySearchAfter(builder, cursor));

        final SearchHits<EsDailyProviderRollupDocument> hits = elasticsearchOperations.search(
                builder.build(), EsDailyProviderRollupDocument.class, IndexCoordinates.of(DAILY_ROLLUP_INDEX));
        final List<SearchHit<EsDailyProviderRollupDocument>> allHits = hits.getSearchHits();
        final boolean hasMore = allHits.size() > query.pageSize();
        final List<SearchHit<EsDailyProviderRollupDocument>> page = hasMore ? allHits.subList(0, query.pageSize()) : allHits;

        final List<DailyProviderRollup> values = new ArrayList<>(page.size());
        for (final SearchHit<EsDailyProviderRollupDocument> hit : page) {
            toDomain(hit.getContent()).ifPresent(values::add);
        }

        Optional<String> nextCursor = Optional.empty();
        if (hasMore && !page.isEmpty()) {
            final EsDailyProviderRollupDocument last = page.get(page.size() - 1).getContent();
            nextCursor = Optional.of(PriceHistoryCursor.after(last.getTimestamp(), rollupStableId(last)));
        }
        return new PriceHistoryPage<>(values, nextCursor);
    }

    @Override
    public PriceHistoryPage<LegacyPriceBackfill> queryLegacy(final PriceHistoryQuery query) {
        throw new UnsupportedOperationException(
                "the separate legacy representation is not served by the B2B price-history facet; "
                        + "legacy-sourced points are already folded into daily rollups and change events");
    }

    private Query buildFilterQuery(final PriceHistoryQuery query, final String timestampField) {
        return Query.of(q -> q.bool(b -> {
            query.gtin().ifPresent(gtin -> b.filter(f -> f.term(t -> t.field("gtin").value(gtin.value()))));
            query.providerId().ifPresent(providerId -> b.filter(f -> f.term(t -> t.field("provider_id").value(providerId.value()))));
            query.condition().ifPresent(condition -> b.filter(f -> f.term(t -> t.field("condition").value(condition.name()))));
            query.currency().ifPresent(currency -> b.filter(f -> f.term(t -> t.field("currency").value(currency.getCurrencyCode()))));
            b.filter(f -> f.range(r -> r.date(d -> d
                    .field(timestampField)
                    .gte(query.from().toString())
                    .lt(query.to().toString()))));
            return b;
        }));
    }

    private void applySearchAfter(final NativeQueryBuilder builder, final String opaqueCursor) {
        final PriceHistoryCursor.Position position = PriceHistoryCursor.decode(opaqueCursor);
        builder.withSearchAfter(List.of(position.timestamp().toEpochMilli(), position.stableId()));
    }

    private String rollupStableId(final EsDailyProviderRollupDocument document) {
        return document.getProviderId() + "\u0001" + document.getCondition() + "\u0001" + document.getCurrency();
    }

    private Optional<PriceChangeEvent> toDomain(final EsPriceChangeEventDocument document) {
        try {
            final OfferKey key = new OfferKey(
                    new Gtin(document.getGtin()),
                    new SourceId(document.getProviderId()),
                    document.getProviderOfferId());
            return Optional.of(new PriceChangeEvent(
                    document.getEventId(),
                    key,
                    PriceChangeKind.valueOf(document.getEventKind()),
                    OfferCondition.valueOf(document.getCondition()),
                    Currency.getInstance(document.getCurrency()),
                    BigDecimal.valueOf(document.getAmount()),
                    OfferAvailability.valueOf(document.getAvailability()),
                    document.getObservedAt(),
                    document.getTimestamp(),
                    parsePayloadHash(document.getContentHash()),
                    parsePolicyRef(document.getPolicyRef())));
        } catch (final RuntimeException ex) {
            LOGGER.warn("Skipping malformed price-change document eventId={}", document.getEventId(), ex);
            return Optional.empty();
        }
    }

    private Optional<DailyProviderRollup> toDomain(final EsDailyProviderRollupDocument document) {
        try {
            final DailyRollupKey key = new DailyRollupKey(
                    new Gtin(document.getGtin()),
                    new SourceId(document.getProviderId()),
                    OfferCondition.valueOf(document.getCondition()),
                    Currency.getInstance(document.getCurrency()));
            return Optional.of(new DailyProviderRollup(
                    key,
                    document.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate(),
                    BigDecimal.valueOf(document.getMinimumAmount()),
                    BigDecimal.valueOf(document.getMaximumAmount()),
                    BigDecimal.valueOf(document.getCloseAmount()),
                    document.getObservedOfferCount(),
                    document.getFirstObservedAt(),
                    document.getLastObservedAt(),
                    document.getChangeCount()));
        } catch (final RuntimeException ex) {
            LOGGER.warn("Skipping malformed daily rollup document gtin={} providerId={}",
                    document.getGtin(), document.getProviderId(), ex);
            return Optional.empty();
        }
    }

    private PayloadHash parsePayloadHash(final String stored) {
        final int separator = stored == null ? -1 : stored.indexOf(':');
        if (separator < 1) {
            throw new IllegalArgumentException("content_hash must be stored as '<algorithm>:<hex>'");
        }
        return new PayloadHash(stored.substring(0, separator), stored.substring(separator + 1));
    }

    private SourceUsagePolicyRef parsePolicyRef(final String stored) {
        final int separator = stored == null ? -1 : stored.indexOf(':');
        if (separator < 1) {
            throw new IllegalArgumentException("policy_ref must be stored as '<policyId>:<version>'");
        }
        return new SourceUsagePolicyRef(stored.substring(0, separator), stored.substring(separator + 1));
    }
}
