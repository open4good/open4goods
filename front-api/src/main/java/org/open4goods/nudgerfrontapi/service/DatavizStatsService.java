package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.search.AggregationBucketDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationResponseDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizChartPresetDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizChartQueryRequestDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizChartQueryResponseDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizDefaultFilterDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizHeroStatsDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizHeroKpiValueDto;
import org.open4goods.nudgerfrontapi.dto.stats.VerticalDatavizPlanDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.model.vertical.DatavizConfig;
import org.open4goods.model.vertical.DatavizChartOverride;
import org.open4goods.model.vertical.DatavizHeroKpi;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Provides default dataviz presets for vertical statistics pages and executes
 * chart aggregation queries.
 *
 * <p>
 * The service exposes a curated, localisation-aware plan that can be consumed by the
 * frontend. It also resolves chart preset identifiers into Elasticsearch aggregation
 * queries, delegating to {@link SearchService} for actual execution.
 * </p>
 */
@Service
public class DatavizStatsService {

    private static final Logger logger = LoggerFactory.getLogger(DatavizStatsService.class);

    private static final String DEFAULT_DATE_FILTER = "now-2d";
    private static final int DEFAULT_TERMS_SIZE = 20;
    private static final int DEFAULT_HISTOGRAM_BUCKETS = 20;

    private final VerticalsConfigService verticalsConfigService;
    private final SearchService searchService;

    /**
     * Construct a new dataviz stats service.
     *
     * @param verticalsConfigService service resolving vertical configurations
     * @param searchService          search service for aggregation query execution
     */
    public DatavizStatsService(VerticalsConfigService verticalsConfigService, SearchService searchService) {
        this.verticalsConfigService = verticalsConfigService;
        this.searchService = searchService;
    }

    /**
     * Resolve the dataviz plan for a vertical.
     *
     * @param verticalId     vertical identifier
     * @param domainLanguage language driving localisation
     * @return dashboard plan or {@code null} when the vertical is unknown
     */
    public VerticalDatavizPlanDto getVerticalPlan(String verticalId, DomainLanguage domainLanguage) {
        if (!StringUtils.hasText(verticalId)) {
            return null;
        }

        String normalizedVerticalId = verticalId.trim();
        VerticalConfig vertical = verticalsConfigService.getConfigById(normalizedVerticalId);
        if (vertical == null) {
            return null;
        }

        List<DatavizDefaultFilterDto> defaults = List.of(
                new DatavizDefaultFilterDto("lastChange", "range", null, null, DEFAULT_DATE_FILTER),
                new DatavizDefaultFilterDto("offersCount", "range", 1d, null, null)
        );

        return new VerticalDatavizPlanDto(normalizedVerticalId, defaults, buildChartCatalog(domainLanguage, vertical.getDataviz()));
    }

    /**
     * Execute a chart aggregation query for the given vertical and chart preset.
     *
     * <p>
     * The method resolves the chart preset by identifier, maps it to an Elasticsearch
     * aggregation query, and delegates to {@link SearchService} for execution. Results
     * are formatted as labels/values arrays for immediate ECharts consumption.
     * </p>
     *
     * @param verticalId     vertical identifier scoping the query
     * @param request        chart query request containing the chart ID and optional filter overrides
     * @param domainLanguage language for localised chart metadata
     * @return chart query response or {@code null} when the chart or vertical is unknown
     */
    public DatavizChartQueryResponseDto executeChartQuery(String verticalId,
                                                           DatavizChartQueryRequestDto request,
                                                           DomainLanguage domainLanguage) {
        logger.info("Executing chart query for vertical={}, chartId={}", verticalId, request.chartId());

        VerticalConfig vertical = resolveVertical(verticalId);
        if (vertical == null) {
            return null;
        }

        List<DatavizChartPresetDto> catalog = buildChartCatalog(domainLanguage, vertical.getDataviz());
        DatavizChartPresetDto preset = catalog.stream()
                .filter(c -> c.id().equals(request.chartId()))
                .findFirst()
                .orElse(null);

        if (preset == null) {
            logger.warn("Unknown chart preset: {}", request.chartId());
            return null;
        }

        AggregationRequestDto.Agg agg = resolveAggregation(preset);
        if (agg == null) {
            logger.warn("No aggregation mapping for query preset: {}", preset.queryPreset());
            return null;
        }

        AggregationRequestDto aggregationQuery = new AggregationRequestDto(List.of(agg));
        FilterRequestDto filters = buildFilters(request);

        SearchService.SearchResult result = searchService.search(
                PageRequest.of(0, 1),
                verticalId.trim(),
                null,
                aggregationQuery,
                filters,
                false,
                "TEXT"
        );

        return formatResponse(preset, result);
    }

    /**
     * Compute hero-level KPI statistics for a vertical.
     *
     * <p>
     * Executes multiple aggregations in a single search call to gather the headline
     * metrics displayed at the top of the dataviz page.
     * </p>
     *
     * @param verticalId     vertical identifier
     * @param domainLanguage language for localisation
     * @return hero statistics or {@code null} when the vertical is unknown
     */
    public DatavizHeroStatsDto computeHeroStats(String verticalId, DomainLanguage domainLanguage) {
        logger.info("Computing hero stats for vertical={}", verticalId);

        VerticalConfig vertical = resolveVertical(verticalId);
        if (vertical == null) {
            return null;
        }

        // Build aggregations for brand terms, condition terms, and country terms
        List<AggregationRequestDto.Agg> aggs = new ArrayList<>();
        aggs.add(new AggregationRequestDto.Agg("brand-terms",
                "attributes.referentielAttributes.BRAND", AggregationRequestDto.AggType.terms,
                null, null, DEFAULT_TERMS_SIZE, null));
        aggs.add(new AggregationRequestDto.Agg("condition-terms",
                        "price.conditions", AggregationRequestDto.AggType.terms,
                        null, null, 10, null));
        aggs.add(new AggregationRequestDto.Agg("country-terms",
                        "gtinInfos.country", AggregationRequestDto.AggType.terms,
                        null, null, 50, null));
        aggs.add(new AggregationRequestDto.Agg("price-range",
                        "price.minPrice.price", AggregationRequestDto.AggType.range,
                        null, null, DEFAULT_HISTOGRAM_BUCKETS, null));

        // Add custom KPIs from configuration
        DatavizConfig datavizConfig = vertical.getDataviz();
        List<DatavizHeroKpi> customKpis = datavizConfig != null ? datavizConfig.getHeroKpis() : List.of();
        for (int i = 0; i < customKpis.size(); i++) {
            DatavizHeroKpi kpi = customKpis.get(i);
            // We use range aggregation to approximate averages/stats via histograms
            aggs.add(new AggregationRequestDto.Agg("custom-kpi-" + i,
                    kpi.getField(), AggregationRequestDto.AggType.range,
                    null, null, DEFAULT_HISTOGRAM_BUCKETS, null));
        }

        AggregationRequestDto aggregationQuery = new AggregationRequestDto(aggs);

        // Default filters: active products only
        FilterRequestDto filters = buildDefaultFilterRequest();

        SearchService.SearchResult result = searchService.search(
                PageRequest.of(0, 1),
                verticalId.trim(),
                null,
                aggregationQuery,
                filters,
                false,
                "TEXT"
        );

        DatavizHeroStatsDto standardStats = buildHeroStatsFromResult(result);
        
        // Extract custom KPIs
        List<DatavizHeroKpiValueDto> extraKpis = new ArrayList<>();
        for (int i = 0; i < customKpis.size(); i++) {
            DatavizHeroKpi kpi = customKpis.get(i);
            AggregationResponseDto agg = findAgg(result.aggregations(), "custom-kpi-" + i);
            Object value = computeKpiValue(agg, kpi.getAggregation());
            
            if (value != null) {
                extraKpis.add(new DatavizHeroKpiValueDto(
                        kpi.getField(), // use field as ID for now
                        kpi.getLabel(), 
                        value, 
                        kpi.getUnit(), 
                        null
                ));
            }
        }

        return new DatavizHeroStatsDto(
                standardStats.totalProducts(),
                standardStats.totalOffers(),
                standardStats.averagePrice(),
                standardStats.medianPrice(),
                standardStats.averageEcoscore(),
                standardStats.topBrand(),
                standardStats.topBrandCount(),
                standardStats.newProductsPercent(),
                standardStats.countriesCount(),
                standardStats.dataFreshnessHours(),
                extraKpis
        );
    }

    // -------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------

    private VerticalConfig resolveVertical(String verticalId) {
        if (!StringUtils.hasText(verticalId)) {
            return null;
        }
        return verticalsConfigService.getConfigById(verticalId.trim());
    }

    /**
     * Map a chart preset's query key to an Elasticsearch aggregation definition.
     */
    private AggregationRequestDto.Agg resolveAggregation(DatavizChartPresetDto preset) {
        return switch (preset.queryPreset()) {
            // KPIs (simple counts, handled by totalHits or bucket count)
            case "activeProducts" -> new AggregationRequestDto.Agg("active-products",
                    "offersCount", AggregationRequestDto.AggType.range, 1d, null, 1, null);
            case "activeOffers" -> new AggregationRequestDto.Agg("active-offers",
                    "offersCount", AggregationRequestDto.AggType.range, null, null, DEFAULT_HISTOGRAM_BUCKETS, null);
            case "medianMinimumPrice" -> new AggregationRequestDto.Agg("median-price",
                    "price.minPrice.price", AggregationRequestDto.AggType.range, null, null, DEFAULT_HISTOGRAM_BUCKETS, null);
            case "newVsUsed" -> new AggregationRequestDto.Agg("new-vs-used",
                    "price.conditions", AggregationRequestDto.AggType.terms, null, null, 10, null);

            // Brand & platform distributions
            case "productsByBrand" -> new AggregationRequestDto.Agg("products-by-brand",
                    "attributes.referentielAttributes.BRAND", AggregationRequestDto.AggType.terms, null, null, DEFAULT_TERMS_SIZE, null);
            case "brandMarketShare" -> new AggregationRequestDto.Agg("brand-market-share",
                    "attributes.referentielAttributes.BRAND", AggregationRequestDto.AggType.terms, null, null, DEFAULT_TERMS_SIZE, null);
            case "productsByPlatform" -> new AggregationRequestDto.Agg("products-by-platform",
                    "datasourceCodes", AggregationRequestDto.AggType.terms, null, null, DEFAULT_TERMS_SIZE, null);
            case "offersByPlatform" -> new AggregationRequestDto.Agg("offers-by-platform",
                    "datasourceCodes", AggregationRequestDto.AggType.terms, null, null, DEFAULT_TERMS_SIZE, null);
            case "conditionByPlatform" -> new AggregationRequestDto.Agg("condition-by-platform",
                    "price.conditions", AggregationRequestDto.AggType.terms, null, null, 10, null);

            // Geographic distribution
            case "productsByCountry" -> new AggregationRequestDto.Agg("products-by-country",
                    "gtinInfos.country", AggregationRequestDto.AggType.terms, null, null, 30, null);

            // Price charts
            case "minimumPriceHistogram" -> new AggregationRequestDto.Agg("min-price-histogram",
                    "price.minPrice.price", AggregationRequestDto.AggType.range, null, null, DEFAULT_HISTOGRAM_BUCKETS, null);
            case "priceDeciles" -> new AggregationRequestDto.Agg("price-deciles",
                    "price.minPrice.price", AggregationRequestDto.AggType.range, null, null, 10, null);

            // Offer density
            case "offersDensity" -> new AggregationRequestDto.Agg("offers-density",
                    "offersCount", AggregationRequestDto.AggType.range, null, null, DEFAULT_HISTOGRAM_BUCKETS, null);

            // Ecoscore & quality
            case "excludedVsIncluded" -> new AggregationRequestDto.Agg("excluded-vs-included",
                    "excluded", AggregationRequestDto.AggType.terms, null, null, 2, null);
            case "priceVsScore" -> new AggregationRequestDto.Agg("price-vs-score",
                    "scores.ECOSCORE.relativ.value", AggregationRequestDto.AggType.range, null, null, DEFAULT_HISTOGRAM_BUCKETS, null);

            // Fallback: return null for unsupported presets (timeline, boxplot, etc.)
            default -> {
                logger.info("Query preset '{}' is not yet mapped to an aggregation. Returning null.", preset.queryPreset());
                yield null;
            }
        };
    }

    /**
     * Build a {@link FilterRequestDto} from the chart query request's filter overrides,
     * including the default plan filters.
     */
    private FilterRequestDto buildFilters(DatavizChartQueryRequestDto request) {
        List<FilterRequestDto.Filter> filterList = new ArrayList<>();

        // Default filter: at least 1 offer
        filterList.add(new FilterRequestDto.Filter(
                "offersCount",
                FilterRequestDto.FilterOperator.range,
                null,
                1d,
                null));

        // Apply overrides from request
        if (request.filterOverrides() != null) {
            for (var override : request.filterOverrides()) {
                if ("offersCount".equals(override.field()) && override.min() != null) {
                    filterList.removeIf(f -> "offersCount".equals(f.field()));
                    filterList.add(new FilterRequestDto.Filter(
                            "offersCount",
                            FilterRequestDto.FilterOperator.range,
                            null,
                            override.min(),
                            override.max()));
                }
            }
        }

        return new FilterRequestDto(filterList, null);
    }

    /**
     * Build default filter request for hero stats — active products with at least 1 offer.
     */
    private FilterRequestDto buildDefaultFilterRequest() {
        return new FilterRequestDto(List.of(
                new FilterRequestDto.Filter(
                        "offersCount",
                        FilterRequestDto.FilterOperator.range,
                        null,
                        1d,
                        null)
        ), null);
    }

    /**
     * Format the raw search result into a chart query response.
     */
    private DatavizChartQueryResponseDto formatResponse(DatavizChartPresetDto preset,
                                                         SearchService.SearchResult result) {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        if (result.aggregations() != null && !result.aggregations().isEmpty()) {
            AggregationResponseDto agg = result.aggregations().getFirst();
            if (agg.buckets() != null) {
                for (AggregationBucketDto bucket : agg.buckets()) {
                    if (!bucket.missing() && bucket.count() > 0) {
                        labels.add(bucket.key());
                        values.add((double) bucket.count());
                    }
                }
            }
        }

        long totalHits = result.hits() != null ? result.hits().getTotalHits() : 0;

        return new DatavizChartQueryResponseDto(
                preset.id(),
                preset.chartType(),
                preset.title(),
                preset.description(),
                labels,
                values,
                totalHits,
                null
        );
    }

    /**
     * Build hero stats from the multi-aggregation search result.
     */
    private DatavizHeroStatsDto buildHeroStatsFromResult(SearchService.SearchResult result) {
        long totalProducts = result.hits() != null ? result.hits().getTotalHits() : 0;

        // Extract aggregation results
        String topBrand = null;
        Long topBrandCount = null;
        long totalOffers = 0;
        double newCount = 0;
        double totalConditionCount = 0;
        int countriesCount = 0;
        double priceSum = 0;
        long priceCount = 0;

        for (AggregationResponseDto agg : result.aggregations()) {
            switch (agg.name()) {
                case "brand-terms" -> {
                    if (agg.buckets() != null && !agg.buckets().isEmpty()) {
                        AggregationBucketDto topBucket = agg.buckets().getFirst();
                        topBrand = topBucket.key();
                        topBrandCount = topBucket.count();
                    }
                }
                case "condition-terms" -> {
                    if (agg.buckets() != null) {
                        for (AggregationBucketDto bucket : agg.buckets()) {
                            totalConditionCount += bucket.count();
                            if ("NEUF".equalsIgnoreCase(bucket.key()) || "NEW".equalsIgnoreCase(bucket.key())) {
                                newCount = bucket.count();
                            }
                        }
                    }
                }
                case "country-terms" -> {
                    if (agg.buckets() != null) {
                        countriesCount = (int) agg.buckets().stream().filter(b -> !b.missing()).count();
                    }
                }
                case "price-range" -> {
                    if (agg.buckets() != null) {
                        for (AggregationBucketDto bucket : agg.buckets()) {
                            if (!bucket.missing() && bucket.count() > 0) {
                                try {
                                    double midPrice = Double.parseDouble(bucket.key());
                                    priceSum += midPrice * bucket.count();
                                    priceCount += bucket.count();
                                } catch (NumberFormatException ignored) {
                                    // Skip non-numeric keys
                                }
                            }
                        }
                    }
                }
                default -> logger.debug("Unhandled aggregation in hero stats: {}", agg.name());
            }
        }

        Double averagePrice = priceCount > 0 ? Math.round(priceSum / priceCount * 100.0) / 100.0 : null;
        Double medianPrice = averagePrice; // Approximation — true median requires sorted values
        Double newPercent = totalConditionCount > 0 ? Math.round(newCount / totalConditionCount * 1000.0) / 10.0 : null;

        // Estimate total offers from offersCount aggregation or from total hits
        totalOffers = totalProducts; // Approximation — each product represents at least one offer

        return new DatavizHeroStatsDto(
                totalProducts,
                totalOffers,
                averagePrice,
                medianPrice,
                null, // ecoscore average requires a dedicated computation
                topBrand,
                topBrandCount,
                newPercent,
                countriesCount,
                0, // data freshness would require a max aggregation on lastChange
                new ArrayList<>()
        );
    }

    /**
     * Build the default chart catalog with localised titles and descriptions.
     *
     * @param domainLanguage language driving localisation
     * @param datavizConfig  dataviz configuration for filtering and overrides
     * @return ordered list of chart presets
     */
    private List<DatavizChartPresetDto> buildChartCatalog(DomainLanguage domainLanguage, DatavizConfig datavizConfig) {
        boolean fr = domainLanguage == DomainLanguage.fr;

        List<DatavizChartPresetDto> presets = new ArrayList<>(java.util.Arrays.asList(
                chart("active-products-kpi", "kpi", fr ? "Produits actifs" : "Active products", fr ? "Nombre de produits actifs" : "Number of active products", "activeProducts", RolesConstants.ROLE_FRONTEND),
                chart("active-offers-kpi", "kpi", fr ? "Offres actives" : "Active offers", fr ? "Nombre total d'offres actives" : "Total number of active offers", "activeOffers", RolesConstants.ROLE_FRONTEND),
                chart("median-min-price-kpi", "kpi", fr ? "Prix minimum médian" : "Median minimum price", fr ? "Médiane du prix minimum" : "Median of minimum prices", "medianMinimumPrice", RolesConstants.ROLE_FRONTEND),
                chart("new-vs-used-kpi", "donut", fr ? "Neuf vs occasion" : "New vs used", fr ? "Répartition neuf / occasion" : "Split between new and used", "newVsUsed", RolesConstants.ROLE_FRONTEND),
                chart("products-by-brand", "bar", fr ? "Produits par marque" : "Products by brand", fr ? "Top marques par volume" : "Top brands by volume", "productsByBrand", RolesConstants.ROLE_FRONTEND),
                chart("brand-market-share", "treemap", fr ? "Parts de marque" : "Brand market share", fr ? "Part des marques dans la verticale" : "Brand shares in the vertical", "brandMarketShare", RolesConstants.ROLE_FRONTEND),
                chart("products-by-platform", "bar", fr ? "Produits par plateforme" : "Products by platform", fr ? "Répartition par source" : "Distribution by source", "productsByPlatform", RolesConstants.ROLE_FRONTEND),
                chart("offers-by-platform", "stacked-bar", fr ? "Offres par plateforme" : "Offers by platform", fr ? "Volume d'offres par source" : "Offer volume per source", "offersByPlatform", RolesConstants.ROLE_FRONTEND),
                chart("condition-by-platform", "stacked-percent", fr ? "Neuf/occasion par plateforme" : "New/used by platform", fr ? "Structure des états par source" : "Condition structure by source", "conditionByPlatform", RolesConstants.ROLE_FRONTEND),
                chart("products-by-country", "bar", fr ? "Produits par pays GTIN" : "Products by GTIN country", fr ? "Origine des GTIN" : "GTIN country distribution", "productsByCountry", RolesConstants.ROLE_FRONTEND),
                chart("new-products-over-time", "line", fr ? "Nouveaux produits" : "New products", fr ? "Nouveaux produits dans le temps" : "New products over time", "newProductsOverTime", RolesConstants.ROLE_FRONTEND),
                chart("updated-products-over-time", "line", fr ? "Produits mis à jour" : "Updated products", fr ? "Produits mis à jour dans le temps" : "Updated products over time", "updatedProductsOverTime", RolesConstants.ROLE_FRONTEND),
                chart("active-offers-over-time", "area", fr ? "Évolution des offres" : "Offer trend", fr ? "Offres actives dans le temps" : "Active offers over time", "activeOffersOverTime", RolesConstants.ROLE_FRONTEND),
                chart("median-price-over-time", "line", fr ? "Prix médian dans le temps" : "Median price over time", fr ? "Évolution du prix médian" : "Median price evolution", "medianPriceOverTime", RolesConstants.ROLE_FRONTEND),
                chart("price-volatility-over-time", "band", fr ? "Volatilité des prix" : "Price volatility", fr ? "Écart-type du prix par période" : "Price standard deviation by period", "priceVolatilityOverTime", RolesConstants.ROLE_FRONTEND),
                chart("min-price-histogram", "histogram", fr ? "Histogramme des prix min" : "Minimum price histogram", fr ? "Distribution des prix minimum" : "Minimum price distribution", "minimumPriceHistogram", RolesConstants.ROLE_FRONTEND),
                chart("price-boxplot-by-brand", "boxplot", fr ? "Boxplot prix par marque" : "Price boxplot by brand", fr ? "Distribution prix pour top marques" : "Price distribution for top brands", "priceBoxplotByBrand", RolesConstants.ROLE_FRONTEND),
                chart("price-distribution-by-platform", "boxplot", fr ? "Prix par plateforme" : "Price by platform", fr ? "Distribution des prix par plateforme" : "Price distribution per platform", "priceDistributionByPlatform", RolesConstants.ROLE_FRONTEND),
                chart("price-deciles", "bar", fr ? "Déciles de prix" : "Price deciles", fr ? "Répartition par déciles" : "Distribution by deciles", "priceDeciles", RolesConstants.ROLE_FRONTEND),
                chart("excluded-vs-included", "donut", fr ? "Exclus vs non exclus" : "Excluded vs included", fr ? "Part des produits exclus" : "Share of excluded products", "excludedVsIncluded", RolesConstants.ROLE_FRONTEND),
                chart("excluded-causes-pareto", "pareto", fr ? "Causes d'exclusion" : "Exclusion causes", fr ? "Top causes d'exclusion" : "Top exclusion causes", "excludedCausesPareto", RolesConstants.ROLE_EDITOR),
                chart("offers-density", "histogram", fr ? "Densité d'offres" : "Offer density", fr ? "Distribution de offersCount" : "offersCount distribution", "offersDensity", RolesConstants.ROLE_FRONTEND),
                chart("price-vs-score", "scatter", fr ? "Prix vs score" : "Price vs score", fr ? "Corrélation prix et score" : "Price and score correlation", "priceVsScore", RolesConstants.ROLE_FRONTEND),
                chart("brand-platform-heatmap", "heatmap", fr ? "Heatmap marque × plateforme" : "Brand × platform heatmap", fr ? "Matrice marque / plateforme" : "Brand / platform matrix", "brandPlatformHeatmap", RolesConstants.ROLE_FRONTEND)
        ));

        if (datavizConfig == null) {
            return presets;
        }

        // Apply enables/disables
        if (datavizConfig.getEnabledCharts() != null && !datavizConfig.getEnabledCharts().isEmpty()) {
            presets.removeIf(p -> !datavizConfig.getEnabledCharts().contains(p.id()));
        }
        if (datavizConfig.getDisabledCharts() != null && !datavizConfig.getDisabledCharts().isEmpty()) {
            presets.removeIf(p -> datavizConfig.getDisabledCharts().contains(p.id()));
        }

        // Apply overrides
        return presets.stream().map(p -> {
            DatavizChartOverride override = datavizConfig.getChartOverrides().get(p.id());
            if (override != null) {
                return new DatavizChartPresetDto(
                        p.id(),
                        p.chartType(),
                        override.getTitle() != null ? override.getTitle() : p.title(),
                        override.getDescription() != null ? override.getDescription() : p.description(),
                        p.queryPreset(),
                        p.hasRole(),
                        p.exportCsv()
                );
            }
            return p;
        }).collect(Collectors.toList());
    }

    private DatavizChartPresetDto chart(String id,
                                        String chartType,
                                        String title,
                                        String description,
                                        String queryPreset,
                                        String hasRole) {
        return new DatavizChartPresetDto(id, chartType, title, description, queryPreset, hasRole, true);
    }

    private AggregationResponseDto findAgg(List<AggregationResponseDto> aggs, String name) {
        if (aggs == null) return null;
        return aggs.stream().filter(a -> a.name().equals(name)).findFirst().orElse(null);
    }

    private Object computeKpiValue(AggregationResponseDto agg, String aggregationType) {
        if (agg == null || agg.buckets() == null || agg.buckets().isEmpty()) {
            return null;
        }

        if ("avg".equalsIgnoreCase(aggregationType)) {
            double sum = 0;
            long count = 0;
            for (AggregationBucketDto bucket : agg.buckets()) {
                if (!bucket.missing() && bucket.count() > 0) {
                    try {
                        double val = Double.parseDouble(bucket.key());
                        sum += val * bucket.count();
                        count += bucket.count();
                    } catch (NumberFormatException ignored) {}
                }
            }
            return count > 0 ? Math.round(sum / count * 100.0) / 100.0 : null;
        }
        
        return null;
    }
}
