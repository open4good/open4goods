package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.dto.search.AggregationBucketDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.Agg;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.AggType;
import org.open4goods.nudgerfrontapi.dto.search.AggregationResponseDto;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MissingAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;

/**
 * Dedicated search service tailored for the frontend API.
 *
 * <p>
 * The implementation mirrors the behaviour of the historical {@code verticalSearch}
 * method located in the commons module while remaining self contained. It
 * supports vertical scoping, textual queries and aggregation descriptors defined
 * by {@link AggregationRequestDto} so the Nuxt frontend can build charts with a
 * single API call.
 * </p>
 */
@Service
public class SearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);
    private static final String MISSING_BUCKET = "ES-UNKNOWN";
    private static final int DEFAULT_BUCKET_COUNT = 10;
    private static final int DEFAULT_TERMS_SIZE = 50;

    private final ProductRepository repository;
    private final VerticalsConfigService verticalsConfigService;

    public SearchService(ProductRepository repository, VerticalsConfigService verticalsConfigService) {
        this.repository = repository;
        this.verticalsConfigService = verticalsConfigService;
    }

    /**
     * Executes a product search and computes aggregation buckets tailored for the frontend.
     *
     * @param pageable         pagination information requested by the caller
     * @param verticalId       optional vertical identifier used to scope the search
     * @param query            optional free text query applied on offer names
     * @param aggregationQuery optional aggregation definition mirroring the Nuxt contract
     * @return a {@link SearchResult} bundling {@link SearchHits} and aggregation metadata
     */
    public SearchResult search(Pageable pageable, String verticalId, String query, AggregationRequestDto aggregationQuery) {
        Criteria criteria = repository.getRecentPriceQuery();

        if (StringUtils.hasText(verticalId) && verticalsConfigService.getConfigById(verticalId) != null) {
            criteria = criteria.and(new Criteria("vertical").is(verticalId));
        } else if (StringUtils.hasText(verticalId)) {
            LOGGER.warn("Requested vertical {} not found. Returning empty result set.", verticalId);
            criteria = criteria.and(new Criteria("vertical").is("__unknown__"));
        }

        String sanitizedQuery = sanitize(query);
        if (StringUtils.hasText(sanitizedQuery)) {
            criteria = criteria.and(new Criteria("offerNames").matches(sanitizedQuery));
        }

        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
        criteriaQuery.setPageable(pageable);

        List<AggregationDescriptor> descriptors = new ArrayList<>();
        var nativeQueryBuilder = new org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder()
                .withQuery(criteriaQuery)
                .withPageable(pageable);

        if (aggregationQuery != null && aggregationQuery.aggs() != null) {
            for (Agg agg : aggregationQuery.aggs()) {
                if (!isValidAggregation(agg)) {
                    continue;
                }
                switch (agg.type()) {
                case terms -> descriptors.add(configureTermsAggregation(nativeQueryBuilder, agg));
                case range -> descriptors.add(configureRangeAggregation(nativeQueryBuilder, agg));
                default -> LOGGER.warn("Unsupported aggregation type {}", agg.type());
                }
            }
        }

        SearchHits<Product> hits = repository.search(nativeQueryBuilder.build(), ProductRepository.MAIN_INDEX_NAME);
        List<AggregationResponseDto> aggregations = extractAggregationResults(hits, descriptors);
        return new SearchResult(hits, List.copyOf(aggregations));
    }

    private boolean isValidAggregation(Agg agg) {
        return agg != null && StringUtils.hasText(agg.name()) && agg.field() != null && agg.type() != null;
    }

    private AggregationDescriptor configureTermsAggregation(org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder builder,
            Agg agg) {
        int size = agg.buckets() != null && agg.buckets() > 0 ? agg.buckets() : DEFAULT_TERMS_SIZE;
        builder.withAggregation(agg.name(), Aggregation.of(a -> a.terms(t -> t.field(agg.field().getText())
                .size(size)
                .missing(MISSING_BUCKET))));
        return AggregationDescriptor.forTerms(agg);
    }

    private AggregationDescriptor configureRangeAggregation(org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder builder,
            Agg agg) {
        int buckets = agg.buckets() != null && agg.buckets() > 0 ? agg.buckets() : DEFAULT_BUCKET_COUNT;
        double interval = computeInterval(agg.min(), agg.max(), buckets);

        String histogramName = agg.name();
        String missingName = agg.name() + "_missing";
        String statsName = agg.name() + "_stats";

        builder.withAggregation(histogramName,
                Aggregation.of(a -> a.histogram(h -> h.field(agg.field().getText())
                        .interval(interval)
                        .minDocCount(1))))
                .withAggregation(missingName, Aggregation.of(a -> a.missing(m -> m.field(agg.field().getText()))))
                .withAggregation(statsName, Aggregation.of(a -> a.stats(s -> s.field(agg.field().getText()))));

        return AggregationDescriptor.forRange(agg, interval, histogramName, missingName, statsName);
    }

    private double computeInterval(Double min, Double max, int buckets) {
        double effectiveMin = min != null ? min : 0d;
        double effectiveMax = max != null ? max : effectiveMin + buckets;
        if (effectiveMax <= effectiveMin) {
            effectiveMax = effectiveMin + buckets;
        }
        double span = effectiveMax - effectiveMin;
        double interval = span / Math.max(1, buckets);
        if (interval <= 0d) {
            interval = Math.max(1d, Math.abs(effectiveMax));
        }
        return interval;
    }

    private List<AggregationResponseDto> extractAggregationResults(SearchHits<Product> hits, List<AggregationDescriptor> descriptors) {
        if (descriptors.isEmpty() || hits == null || hits.getAggregations() == null) {
            return Collections.emptyList();
        }

        if (!(hits.getAggregations() instanceof ElasticsearchAggregations aggregations)) {
            return Collections.emptyList();
        }

        List<AggregationResponseDto> responses = new ArrayList<>();
        for (AggregationDescriptor descriptor : descriptors) {
            if (descriptor.type == AggType.terms) {
                responses.add(extractTermsAggregation(aggregations, descriptor));
            } else if (descriptor.type == AggType.range) {
                responses.add(extractRangeAggregation(aggregations, descriptor));
            }
        }
        return responses;
    }

    private AggregationResponseDto extractTermsAggregation(ElasticsearchAggregations aggregations, AggregationDescriptor descriptor) {
        var aggregation = aggregations.get(descriptor.histogramName);
        if (aggregation == null) {
            return new AggregationResponseDto(descriptor.request.name(), descriptor.request.field(), descriptor.type, List.of(), null, null);
        }

        var aggregate = aggregation.aggregation().getAggregate();
        List<AggregationBucketDto> buckets = new ArrayList<>();
        if (aggregate.isSterms()) {
            StringTermsAggregate terms = aggregate.sterms();
            for (StringTermsBucket bucket : terms.buckets().array()) {
                buckets.add(new AggregationBucketDto(bucket.key().stringValue(), null, bucket.docCount(), Objects.equals(bucket.key().stringValue(), MISSING_BUCKET)));
            }
        } else if (aggregate.isLterms()) {
            LongTermsAggregate terms = aggregate.lterms();
            for (LongTermsBucket bucket : terms.buckets().array()) {
                String key = Long.toString(bucket.key());
                buckets.add(new AggregationBucketDto(key, null, bucket.docCount(), Objects.equals(key, MISSING_BUCKET)));
            }
        }
        buckets.sort((a, b) -> Long.compare(b.count(), a.count()));
        return new AggregationResponseDto(descriptor.request.name(), descriptor.request.field(), descriptor.type, buckets, null, null);
    }

    private AggregationResponseDto extractRangeAggregation(ElasticsearchAggregations aggregations, AggregationDescriptor descriptor) {
        var histogramAgg = aggregations.get(descriptor.histogramName);
        var missingAgg = aggregations.get(descriptor.missingName);
        var statsAgg = aggregations.get(descriptor.statsName);

        List<AggregationBucketDto> buckets = new ArrayList<>();
        Double min = null;
        Double max = null;
        if (statsAgg != null && statsAgg.aggregation().getAggregate().isStats()) {
            StatsAggregate stats = statsAgg.aggregation().getAggregate().stats();
            min = stats.min();
            max = stats.max();
        }

        if (histogramAgg != null && histogramAgg.aggregation().getAggregate().isHistogram()) {
            HistogramAggregate histogram = histogramAgg.aggregation().getAggregate().histogram();
            for (HistogramBucket bucket : histogram.buckets().array()) {
                double from = bucket.key();
                double to = from + descriptor.interval;
                buckets.add(new AggregationBucketDto(Double.toString(from), to, bucket.docCount(), false));
            }
        }

        if (missingAgg != null && missingAgg.aggregation().getAggregate().isMissing()) {
            MissingAggregate missing = missingAgg.aggregation().getAggregate().missing();
            if (missing.docCount() > 0) {
                buckets.add(new AggregationBucketDto(MISSING_BUCKET, null, missing.docCount(), true));
            }
        }

        return new AggregationResponseDto(descriptor.request.name(), descriptor.request.field(), descriptor.type, buckets, min, max);
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String sanitized = value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        return sanitized;
    }

    /**
     * Wrapper holding the {@link SearchHits} and the aggregation response DTOs.
     */
    public record SearchResult(SearchHits<Product> hits, List<AggregationResponseDto> aggregations) {
    }

    private record AggregationDescriptor(Agg request, AggType type, double interval, String histogramName, String missingName,
            String statsName) {

        private static AggregationDescriptor forTerms(Agg request) {
            return new AggregationDescriptor(request, request.type(), 0d, request.name(), null, null);
        }

        private static AggregationDescriptor forRange(Agg request, double interval, String histogramName, String missingName,
                String statsName) {
            return new AggregationDescriptor(request, request.type(), interval, histogramName, missingName, statsName);
        }
    }
}
