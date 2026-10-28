package org.open4goods.services.contribution.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.model.AffiliationClickStats;
import org.open4goods.services.contribution.model.AffiliationClickStats.MerchantClicks;
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

    private static final String AGG_NETWORKS = "networks";
    private static final String AGG_SHORT = "short";
    private static final String AGG_TOP = "top";
    private static final String EPOCH_MILLIS = "epoch_millis";

    private final ElasticsearchOperations elasticsearchOperations;

    public AffiliationClickStatsService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * Counts the clicks of {@code [longStart, end)} and of {@code [shortStart, end)}, per network, and the
     * {@code topMerchants} most clicked (merchant, network) pairs of the long window.
     *
     * @param longStart    start of the long window, epoch milliseconds, inclusive
     * @param shortStart   start of the short window, epoch milliseconds, inclusive ({@code >= longStart})
     * @param end          end of both windows, epoch milliseconds, exclusive
     * @param topMerchants number of top merchant rows
     * @return the aggregated clicks
     * @throws RuntimeException when Elasticsearch cannot answer (unreachable, missing index...)
     */
    public AffiliationClickStats read(long longStart, long shortStart, long end, int topMerchants) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(tsRange(longStart, end))
                .withAggregation(AGG_NETWORKS, networksAggregation())
                .withAggregation(AGG_SHORT, Aggregation.of(a -> a.filter(tsRange(shortStart, end))
                        .aggregations(AGG_NETWORKS, networksAggregation())))
                .withAggregation(AGG_TOP, Aggregation.of(a -> a.multiTerms(m -> m
                        .terms(MultiTermLookup.of(t -> t.field(AffiliationClick.FIELD_MERCHANT)),
                                MultiTermLookup.of(t -> t.field(AffiliationClick.FIELD_NETWORK)))
                        .size(topMerchants))))
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
                List.copyOf(top));
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
