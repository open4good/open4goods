package org.open4goods.services.contribution.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.model.AffiliationClickStats;
import org.open4goods.services.contribution.model.AffiliationClickStats.ClickCounts;
import org.open4goods.services.contribution.model.AffiliationClickStats.MerchantClicks;
import org.open4goods.services.contribution.model.AffiliationClickStats.OriginClicks;
import org.open4goods.services.contribution.model.AffiliationNetwork;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.FilterAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermLookup;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Reads the outbound affiliation clicks ({@code affiliation-clicks} index) with a single aggregation request: no
 * document is fetched. Shared so that the reader (api) and the writer (front-api, {@link AffiliationClick}) use the
 * same contract.
 * <p>
 * Failures (Elasticsearch unreachable, missing index...) are propagated: the caller decides how to report them, and
 * must never turn them into zero.
 */
public class AffiliationClickStatsService {

    /** Number of rows of the top merchants. */
    public static final int TOP_MERCHANTS = 20;
    /** Number of rows of the top origins. */
    public static final int TOP_ORIGINS = 50;

    private static final String AGG_NETWORKS = "networks";
    private static final String AGG_SHORT = "short";
    private static final String AGG_TOP = "top";
    private static final String AGG_CONTENT = "content";
    private static final String AGG_NO_CONTENT = "no_content";
    private static final String AGG_ORIGIN = "origin";
    private static final String EPOCH_MILLIS = "epoch_millis";

    private final ElasticsearchOperations elasticsearchOperations;

    public AffiliationClickStatsService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * Counts the clicks of {@code [longStart, end)} and of {@code [shortStart, end)}, per network, and, over the
     * long window, the {@code topMerchants} most clicked (merchant, network) pairs and the {@code topOrigins} most
     * clicked origins.
     * <p>
     * The origin of a click is its {@code contentPath} when it has one, otherwise its {@code originPath}, otherwise
     * {@value OriginClicks#NO_ORIGIN}. It is read without script: a {@code terms} aggregation on {@code contentPath},
     * and a {@code terms} aggregation on {@code originPath} ({@code missing} = {@value OriginClicks#NO_ORIGIN})
     * restricted to the clicks without {@code contentPath}. Both sets of clicks are disjoint, so their merged top is
     * exact; an origin present in both (a {@code /blog/} page as {@code originPath} of a click without
     * {@code contentPath}) is summed. On an index whose mapping does not hold {@code contentPath} yet, every click
     * counts by its {@code originPath}.
     *
     * @param longStart    start of the long window, epoch milliseconds, inclusive
     * @param shortStart   start of the short window, epoch milliseconds, inclusive ({@code >= longStart})
     * @param end          end of both windows, epoch milliseconds, exclusive
     * @param topMerchants number of top merchant rows
     * @param topOrigins   number of top origin rows
     * @return the aggregated clicks
     * @throws RuntimeException when Elasticsearch cannot answer (unreachable, missing index...)
     */
    public AffiliationClickStats read(long longStart, long shortStart, long end, int topMerchants, int topOrigins) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(tsRange(longStart, end))
                .withAggregation(AGG_NETWORKS, networksAggregation())
                .withAggregation(AGG_SHORT, Aggregation.of(a -> a.filter(tsRange(shortStart, end))
                        .aggregations(AGG_NETWORKS, networksAggregation())))
                .withAggregation(AGG_TOP, Aggregation.of(a -> a.multiTerms(m -> m
                        .terms(MultiTermLookup.of(t -> t.field(AffiliationClick.FIELD_MERCHANT)),
                                MultiTermLookup.of(t -> t.field(AffiliationClick.FIELD_NETWORK)))
                        .size(topMerchants))))
                .withAggregation(AGG_CONTENT, Aggregation.of(a -> a.terms(t -> t
                        .field(AffiliationClick.FIELD_CONTENT_PATH)
                        .size(topOrigins))))
                .withAggregation(AGG_NO_CONTENT, Aggregation.of(a -> a
                        .filter(f -> f.bool(b -> b.mustNot(n -> n.exists(e -> e.field(
                                AffiliationClick.FIELD_CONTENT_PATH)))))
                        .aggregations(AGG_ORIGIN, Aggregation.of(o -> o.terms(t -> t
                                .field(AffiliationClick.FIELD_ORIGIN_PATH)
                                .missing(OriginClicks.NO_ORIGIN)
                                .size(topOrigins))))))
                .withMaxResults(0)
                .withTrackTotalHits(true)
                .build();

        SearchHits<AffiliationClick> hits = elasticsearchOperations.search(query, AffiliationClick.class,
                IndexCoordinates.of(AffiliationClick.INDEX_NAME));
        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) hits.getAggregations();
        if (aggregations == null) {
            throw new IllegalStateException("Elasticsearch returned no aggregation for " + AffiliationClick.INDEX_NAME);
        }

        FilterAggregate shortWindow = aggregate(aggregations, AGG_SHORT).filter();
        List<MerchantClicks> top = new ArrayList<>();
        for (MultiTermsBucket bucket : aggregate(aggregations, AGG_TOP).multiTerms().buckets().array()) {
            List<FieldValue> key = bucket.key();
            top.add(new MerchantClicks(key.get(0).stringValue(), key.get(1).stringValue(), bucket.docCount()));
        }
        return new AffiliationClickStats(
                perNetwork(shortWindow.aggregations().get(AGG_NETWORKS)),
                perNetwork(aggregate(aggregations, AGG_NETWORKS)),
                shortWindow.docCount(),
                hits.getTotalHits(),
                List.copyOf(top),
                topOrigins(aggregate(aggregations, AGG_CONTENT),
                        aggregate(aggregations, AGG_NO_CONTENT).filter().aggregations().get(AGG_ORIGIN), topOrigins));
    }

    /**
     * Counts the clicks of {@code [start, end)}, per network and in total, with a single aggregation request.
     *
     * @param start start of the window, epoch milliseconds, inclusive
     * @param end   end of the window, epoch milliseconds, exclusive
     * @return the clicks of the window
     * @throws RuntimeException when Elasticsearch cannot answer (unreachable, missing index...)
     */
    public ClickCounts count(long start, long end) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(tsRange(start, end))
                .withAggregation(AGG_NETWORKS, networksAggregation())
                .withMaxResults(0)
                .withTrackTotalHits(true)
                .build();
        SearchHits<AffiliationClick> hits = elasticsearchOperations.search(query, AffiliationClick.class,
                IndexCoordinates.of(AffiliationClick.INDEX_NAME));
        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) hits.getAggregations();
        if (aggregations == null) {
            throw new IllegalStateException("Elasticsearch returned no aggregation for " + AffiliationClick.INDEX_NAME);
        }
        return new ClickCounts(perNetwork(aggregate(aggregations, AGG_NETWORKS)), hits.getTotalHits());
    }

    /**
     * Merges the {@code contentPath} and {@code originPath} buckets: most clicked first, ties ordered by origin,
     * at most {@code size} rows.
     */
    private static List<OriginClicks> topOrigins(Aggregate content, Aggregate origin, int size) {
        if (origin == null) {
            throw new IllegalStateException("Missing aggregation '" + AGG_ORIGIN + "' on " + AffiliationClick.INDEX_NAME);
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Aggregate terms : List.of(content, origin)) {
            if (terms.isUmterms()) {
                // Field not mapped yet (index not updated since GOU-1828): no click carries it.
                continue;
            }
            for (StringTermsBucket bucket : terms.sterms().buckets().array()) {
                counts.merge(bucket.key().stringValue(), bucket.docCount(), Long::sum);
            }
        }
        return counts.entrySet().stream()
                .map(entry -> new OriginClicks(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(OriginClicks::clicks).reversed()
                        .thenComparing(OriginClicks::origin))
                .limit(size)
                .toList();
    }

    private static Query tsRange(long start, long end) {
        return Query.of(q -> q.range(r -> r.date(d -> d.field(AffiliationClick.FIELD_TS)
                .gte(String.valueOf(start))
                .lt(String.valueOf(end))
                .format(EPOCH_MILLIS))));
    }

    private static Aggregation networksAggregation() {
        return Aggregation.of(a -> a.terms(t -> t.field(AffiliationClick.FIELD_NETWORK)
                .size(AffiliationNetwork.values().length + 1)));
    }

    private static Aggregate aggregate(ElasticsearchAggregations aggregations, String name) {
        var aggregation = aggregations.get(name);
        if (aggregation == null) {
            throw new IllegalStateException("Missing aggregation '" + name + "' on " + AffiliationClick.INDEX_NAME);
        }
        return aggregation.aggregation().getAggregate();
    }

    /**
     * Clicks per network key, every known network present (0 when it has no bucket).
     */
    private static Map<String, Long> perNetwork(Aggregate terms) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (AffiliationNetwork network : AffiliationNetwork.values()) {
            counts.put(network.key(), 0L);
        }
        for (StringTermsBucket bucket : terms.sterms().buckets().array()) {
            String key = bucket.key().stringValue();
            if (counts.containsKey(key)) {
                counts.put(key, bucket.docCount());
            }
        }
        return Collections.unmodifiableMap(counts);
    }
}
