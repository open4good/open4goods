package org.open4goods.api.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.api.config.yml.VerticalsGenerationConfig;
import org.open4goods.api.dto.AttributeSuggestionDto;
import org.open4goods.api.dto.CategorySuggestionsDto;
import org.open4goods.api.dto.DatasourceCoverageDto;
import org.open4goods.api.dto.LeakageWarningDto;
import org.open4goods.api.dto.SignificantCategoryDto;
import org.open4goods.api.dto.UnmappedCategoryDto;
import org.open4goods.api.model.AttributesStats;
import org.open4goods.api.model.VerticalAttributesStats;
import org.open4goods.api.model.VerticalCategoryMapping;
import org.open4goods.icecat.services.IcecatFeatureResolver;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.NudgeToolScore;
import org.open4goods.model.vertical.ScoreRange;
import org.open4goods.model.vertical.SubsetCriteria;
import org.open4goods.model.vertical.SubsetCriteriaOperator;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.SignificantStringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;

public class VerticalsGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerticalsGenerationService.class);
    private static final double TARGET_THRESHOLD_RATIO = 1.0 / 3.0;
    private static final double ACCEPTABLE_RATIO_DELTA = 0.05;
    private static final int MAX_THRESHOLD_ITERATIONS = 40;
    private static final String IMPACT_SCORE_NAME = "ECOSCORE";
    private static final String IMPACT_SCORE_FIELD = "scores.ECOSCORE.value";

    private final VerticalsGenerationConfig config;
    private final VerticalsConfigService verticalConfigservice;
    private final ProductRepository repository;
    private final SerialisationService serialisationService;
    private final ResourcePatternResolver resourceResolver;
    private final IcecatFeatureResolver icecatFeatureResolver;

    private Map<String, VerticalCategoryMapping> sortedMappings = new LinkedHashMap<>();
    private final GoogleTaxonomyService googleTaxonomyService;
    private final EvaluationService evalService;
    private final PromptService genAiService;

    public VerticalsGenerationService(VerticalsGenerationConfig config, ProductRepository repository,
            SerialisationService serialisationService, GoogleTaxonomyService googleTaxonomyService,
            VerticalsConfigService verticalsConfigService, ResourcePatternResolver resourceResolver,
            EvaluationService evaluationService, IcecatFeatureResolver icecatFeatureResolver,
            PromptService genAiService) {
        this.config = config;
        this.repository = repository;
        this.serialisationService = serialisationService;
        this.googleTaxonomyService = googleTaxonomyService;
        this.verticalConfigservice = verticalsConfigService;
        this.resourceResolver = resourceResolver;
        this.evalService = evaluationService;
        this.icecatFeatureResolver = icecatFeatureResolver;
        this.genAiService = genAiService;
    }

    /**
     * @return the category-mappings cache
     */
    public Map<String, VerticalCategoryMapping> getMappings() {
        return sortedMappings;
    }

    // -----------------------------------------------------------------------
    // Nudge tool threshold computation (read-only, no disk writes)
    // -----------------------------------------------------------------------

    /**
     * Computes nudge tool score thresholds and impact-score subset boundaries for
     * the given vertical without mutating any files.
     */
    public VerticalConfig computeNudgeToolThresholds(String verticalId) {
        VerticalConfig verticalConfig = verticalConfigservice.getConfigById(verticalId);
        if (verticalConfig == null) {
            return null;
        }

        VerticalConfig result = new VerticalConfig();
        result.setId(verticalId);

        if (verticalConfig.getNudgeToolConfig() != null) {
            List<NudgeToolScore> scores = verticalConfig.getNudgeToolConfig().getScores();
            if (scores != null && !scores.isEmpty()) {
                for (NudgeToolScore score : scores) {
                    if (StringUtils.isNotBlank(score.getScoreName())) {
                        NudgeToolScore newScore = new NudgeToolScore();
                        newScore.setScoreName(score.getScoreName());
                        newScore.setTitle(score.getTitle());
                        newScore.setMdiIcon(score.getMdiIcon());
                        newScore.setDisabled(score.getDisabled());
                        newScore.setDescription(score.getDescription());
                        if (score.getFromPercent() != null) {
                            newScore.setFromPercent(score.getFromPercent());
                            newScore.setToPercent(score.getToPercent());
                        } else {
                            double threshold = computeThresholdForScore(verticalId, score.getScoreName(),
                                    SubsetCriteriaOperator.GREATER_THAN);
                            newScore.setScoreMinValue(threshold);
                        }
                        result.getNudgeToolConfig().getScores().add(newScore);
                    }
                }
            }
        }

        if (verticalConfig.getNudgeToolConfig() != null
                && verticalConfig.getNudgeToolConfig().getSubsets() != null
                && !verticalConfig.getNudgeToolConfig().getSubsets().isEmpty()) {
            List<VerticalSubset> existingSubsets = verticalConfig.getNudgeToolConfig().getSubsets();
            result.getNudgeToolConfig().setSubsets(existingSubsets);
            result.setSubsets(existingSubsets);
        } else {
            ScoreThresholds thresholds = computeImpactScoreThresholds(verticalId);
            String lowerThreshold = formatScoreValue(thresholds.lower());
            String upperThreshold = formatScoreValue(thresholds.upper());
            List<VerticalSubset> subsets = buildImpactScoreSubsetsList(lowerThreshold, upperThreshold);
            result.getNudgeToolConfig().setSubsets(subsets);
            result.setSubsets(subsets);
        }

        return result;
    }

    private List<VerticalSubset> buildImpactScoreSubsetsList(String lowerThreshold, String upperThreshold) {
        List<VerticalSubset> subsets = new ArrayList<>();
        subsets.add(createImpactSubset("impact_high", "LOWER_THAN_OR_EQUAL", lowerThreshold));
        subsets.add(createImpactSubset("impact_medium", List.of(
                new SubsetCriteria(IMPACT_SCORE_FIELD, SubsetCriteriaOperator.GREATER_THAN, lowerThreshold),
                new SubsetCriteria(IMPACT_SCORE_FIELD, SubsetCriteriaOperator.LOWER_THAN_OR_EQUAL, upperThreshold)
        )));
        subsets.add(createImpactSubset("impact_low", "GREATER_THAN", upperThreshold));
        return subsets;
    }

    private VerticalSubset createImpactSubset(String id, String operator, String value) {
        return createImpactSubset(id,
                List.of(new SubsetCriteria(IMPACT_SCORE_FIELD, SubsetCriteriaOperator.valueOf(operator), value)));
    }

    private VerticalSubset createImpactSubset(String id, List<SubsetCriteria> criterias) {
        VerticalSubset subset = new VerticalSubset();
        subset.setId(id);
        subset.setGroup("impactscore");
        subset.setCriterias(new ArrayList<>(criterias));
        return subset;
    }

    private ScoreThresholds computeImpactScoreThresholds(String verticalId) {
        long total = repository.countMainIndexHavingScoreWithFilters(IMPACT_SCORE_NAME, verticalId);
        if (total <= 0) {
            LOGGER.info("No products found for impact score thresholds in {}", verticalId);
            return new ScoreThresholds(2.0, 4.0);
        }
        ScoreRange range = repository.getScoreRange(IMPACT_SCORE_NAME, verticalId, 100);
        double lowerThreshold = computeThresholdForScore(verticalId, IMPACT_SCORE_NAME,
                SubsetCriteriaOperator.LOWER_THAN, range);
        double upperThreshold = computeThresholdForScore(verticalId, IMPACT_SCORE_NAME,
                SubsetCriteriaOperator.GREATER_THAN, range);
        if (lowerThreshold >= upperThreshold) {
            LOGGER.warn("Invalid impact score thresholds for {}: {} >= {}", verticalId, lowerThreshold,
                    upperThreshold);
            return new ScoreThresholds(
                    range.min() + (range.max() - range.min()) / 3.0,
                    range.min() + 2 * (range.max() - range.min()) / 3.0);
        }
        return new ScoreThresholds(lowerThreshold, upperThreshold);
    }

    private double computeThresholdForScore(String verticalId, String scoreName, SubsetCriteriaOperator operator) {
        ScoreRange range = repository.getScoreRange(scoreName, verticalId, 100);
        return computeThresholdForScore(verticalId, scoreName, operator, range);
    }

    private double computeThresholdForScore(String verticalId, String scoreName, SubsetCriteriaOperator operator,
            ScoreRange range) {
        long total = repository.countMainIndexHavingScoreWithFilters(scoreName, verticalId);
        if (total <= 0) {
            LOGGER.info("No products for score {} in {}", scoreName, verticalId);
            return (range.min() + range.max()) / 2.0;
        }
        double low = range.min();
        double high = range.max();
        double bestThreshold = (low + high) / 2.0;
        double bestDelta = Double.MAX_VALUE;
        for (int i = 0; i < MAX_THRESHOLD_ITERATIONS; i++) {
            double mid = (low + high) / 2.0;
            long count = repository.countMainIndexHavingScoreThreshold(scoreName, verticalId, operator, mid);
            double ratio = count / (double) total;
            double delta = Math.abs(ratio - TARGET_THRESHOLD_RATIO);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestThreshold = mid;
            }
            if (delta <= ACCEPTABLE_RATIO_DELTA) {
                break;
            }
            boolean isDirect = (operator == SubsetCriteriaOperator.LOWER_THAN
                    || operator == SubsetCriteriaOperator.LOWER_THAN_OR_EQUAL);
            boolean tooMany = ratio > TARGET_THRESHOLD_RATIO;
            if (isDirect) {
                if (tooMany) high = mid;
                else low = mid;
            } else {
                if (tooMany) low = mid;
                else high = mid;
            }
        }
        return BigDecimal.valueOf(bestThreshold).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatScoreValue(double value) {
        BigDecimal decimal = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return decimal.toPlainString();
    }

    private record ScoreThresholds(double lower, double upper) {}

    // -----------------------------------------------------------------------
    // Attribute coverage stats
    // -----------------------------------------------------------------------

    /**
     * Computes attribute coverage stats for the given vertical by streaming all
     * products with a valid date. Returned map is sorted descending by hit count.
     */
    public VerticalAttributesStats attributesStats(String vertical) {
        VerticalConfig vc = verticalConfigservice.getConfigById(vertical);
        VerticalAttributesStats ret = new VerticalAttributesStats();
        if (vc != null) {
            LOGGER.info("Attributes stats for vertical {} is running", vertical);
            try (java.util.stream.Stream<Product> stream = repository.exportVerticalWithValidDate(vc, true)) {
                stream.forEach(p -> ret.process(p.getAttributes().getAll()));
            }
            ret.clean();
            ret.sort();
        }
        return ret;
    }

    // -----------------------------------------------------------------------
    // Category mapping helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a per-datasource category map from the given product GTINs.
     */
    public Map<String, Set<String>> categoryMappingForGtins(Collection<String> gtins,
            Set<String> excludedDatasources) {
        Map<String, Set<String>> matchingCategories = new HashMap<>();
        matchingCategories.put("all", new HashSet<>());
        for (String gtin : gtins) {
            try {
                if (NumberUtils.isDigits(gtin.trim())) {
                    Product sample = repository.getById(Long.valueOf(gtin.trim()));
                    sample.getCategoriesByDatasources().forEach((ds, cat) -> {
                        if (excludedDatasources != null && excludedDatasources.contains(ds)) {
                            LOGGER.info("Skipping {}, in ignored list", ds);
                        } else {
                            matchingCategories.computeIfAbsent(ds, k -> new HashSet<>()).add(cat);
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.warn("Cannot generate matching categories data: {}", e.getMessage());
            }
        }
        return matchingCategories;
    }

    /**
     * Generates the YAML {@code matchingCategories} fragment for the given vertical.
     */
    public String generateMapping(VerticalConfig vc, Integer minOfferscount) {
        List<String> items;
        try (java.util.stream.Stream<Product> stream =
                repository.exportVerticalWithOffersCountGreater(vc, minOfferscount)) {
            items = stream.map(Product::gtin).toList();
        }
        Map<String, Set<String>> map = categoryMappingForGtins(items,
                vc.getGenerationExcludedFromCategoriesMatching());
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("matchingCategories", map);
        String ret = "";
        try {
            ret = serialisationService.toYaml(retMap);
        } catch (SerialisationException e) {
            LOGGER.error("Serialisation exception", e);
        }
        return ret.replaceFirst("---", "");
    }

    /**
     * Generates the YAML categories mapping fragment from sample GTINs.
     *
     * @deprecated Use {@link #categoryMappingForGtins} for structured output.
     */
    @Deprecated
    public String generateCategoryMappingFragmentForGtin(Collection<String> gtins,
            Set<String> excludedDatasources) {
        Map<String, Set<String>> map = categoryMappingForGtins(gtins, excludedDatasources);
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("matchingCategories", map);
        String ret = "";
        try {
            ret = serialisationService.toYaml(retMap);
        } catch (SerialisationException e) {
            LOGGER.error("Serialisation exception", e);
        }
        return ret.replaceFirst("---", "");
    }

    // -----------------------------------------------------------------------
    // Suggestion endpoints (read-only, structured JSON -- for agent use)
    // -----------------------------------------------------------------------

    /**
     * Returns structured category suggestions for the given vertical without
     * mutating any files. Suitable for agents assembling the
     * {@code matchingCategories} YAML block themselves.
     *
     * @param vc             the vertical configuration
     * @param minOffersCount minimum offer count for a product to be sampled
     * @return DTO with per-datasource category lists
     */
    public CategorySuggestionsDto suggestCategories(VerticalConfig vc, int minOffersCount) {
        List<String> items;
        try (java.util.stream.Stream<Product> stream =
                repository.exportVerticalWithOffersCountGreater(vc, minOffersCount)) {
            items = stream.map(Product::gtin).toList();
        }
        Map<String, Set<String>> raw = categoryMappingForGtins(items,
                vc.getGenerationExcludedFromCategoriesMatching());
        Map<String, List<String>> byDatasource = new LinkedHashMap<>();
        raw.forEach((ds, cats) -> byDatasource.put(ds, new ArrayList<>(cats)));
        return new CategorySuggestionsDto(vc.getId(), items.size(), byDatasource);
    }

    /**
     * Returns observed datasourceCategories coverage for a vertical, grouped by
     * datasource when the datasource can be inferred from the current mapping.
     *
     * @param vc vertical configuration
     * @param minVolume minimum category document count to include
     * @return coverage rows sorted by product volume
     */
    public List<DatasourceCoverageDto> datasourceCoverage(VerticalConfig vc, int minVolume) {
        Set<String> mappedCategories = mappedCategories(vc);
        Map<String, String> categoryToDatasource = categoryToDatasource(vc);
        Map<String, CoverageAccumulator> byDatasource = new LinkedHashMap<>();

        for (StringTermsBucket bucket : categoryBuckets(vc.getId(), minVolume, 200)) {
            String category = bucket.key().stringValue();
            String datasource = categoryToDatasource.getOrDefault(category, "unknown");
            CoverageAccumulator accumulator = byDatasource.computeIfAbsent(datasource, k -> new CoverageAccumulator());
            accumulator.products += bucket.docCount();
            if (!mappedCategories.contains(category)) {
                accumulator.unmappedCategoriesCount++;
                if (accumulator.sampleUnmapped.size() < 5) {
                    accumulator.sampleUnmapped.add(category);
                }
            }
        }

        return byDatasource.entrySet().stream()
                .map(e -> new DatasourceCoverageDto(
                        e.getKey(),
                        e.getValue().products,
                        vc.getMatchingCategories().getOrDefault(e.getKey(), Set.of()).size(),
                        e.getValue().unmappedCategoriesCount,
                        e.getValue().sampleUnmapped))
                .sorted((a, b) -> Long.compare(b.products(), a.products()))
                .toList();
    }

    /**
     * Returns observed category strings for the vertical that are missing from
     * {@code matchingCategories}.
     *
     * @param vc vertical configuration
     * @param minVolume minimum category document count
     * @param limit maximum number of rows
     * @return unmapped categories sorted by volume
     */
    public List<UnmappedCategoryDto> unmappedCategories(VerticalConfig vc, int minVolume, int limit) {
        Set<String> mappedCategories = mappedCategories(vc);
        return categoryBuckets(vc.getId(), minVolume, limit).stream()
                .filter(bucket -> !mappedCategories.contains(bucket.key().stringValue()))
                .map(bucket -> new UnmappedCategoryDto(null, bucket.key().stringValue(), bucket.docCount()))
                .toList();
    }

    /**
     * Returns cross-vertical category leakage rows where the requested vertical is
     * one of the top two verticals for a category.
     *
     * @param vertical vertical identifier
     * @param minVolume minimum category document count
     * @param leakageThreshold runner-up share threshold used to flag a row
     * @return leakage warnings sorted by category volume
     */
    public List<LeakageWarningDto> categoryLeakage(String vertical, int minVolume, double leakageThreshold) {
        NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                .withQuery(q -> q.exists(e -> e.field("vertical")))
                .withMaxResults(0)
                .withSourceFilter(new FetchSourceFilter(false, null, null))
                .withAggregation("cats", Aggregation.of(a -> a
                        .terms(t -> t.field("datasourceCategories").size(2000).minDocCount(minVolume))
                        .aggregations("verts", Aggregation.of(va -> va
                                .terms(t -> t.field("vertical").size(10))))));

        Aggregate aggregate = aggregation(queryBuilder.build(), "cats");
        if (aggregate == null || !aggregate.isSterms()) {
            return List.of();
        }

        List<LeakageWarningDto> warnings = new ArrayList<>();
        for (StringTermsBucket categoryBucket : aggregate.sterms().buckets().array()) {
            Aggregate verticalAggregate = categoryBucket.aggregations().get("verts");
            if (verticalAggregate == null || !verticalAggregate.isSterms()) {
                continue;
            }
            List<StringTermsBucket> verticalBuckets = verticalAggregate.sterms().buckets().array();
            if (verticalBuckets.isEmpty()) {
                continue;
            }
            long total = categoryBucket.docCount();
            StringTermsBucket top = verticalBuckets.get(0);
            StringTermsBucket second = verticalBuckets.size() > 1 ? verticalBuckets.get(1) : null;
            String topVertical = top.key().stringValue();
            String secondVertical = second == null ? null : second.key().stringValue();
            if (!vertical.equals(topVertical) && !vertical.equals(secondVertical)) {
                continue;
            }
            double topShare = share(top.docCount(), total);
            double secondShare = second == null ? 0.0 : share(second.docCount(), total);
            warnings.add(new LeakageWarningDto(
                    categoryBucket.key().stringValue(),
                    total,
                    topVertical,
                    topShare,
                    secondVertical,
                    secondShare,
                    secondShare >= leakageThreshold));
        }
        return warnings;
    }

    /**
     * Returns significant datasourceCategories values for a vertical versus
     * unattached products.
     *
     * @param vertical vertical identifier
     * @param minVolume minimum foreground document count
     * @param limit maximum number of rows
     * @return significant category rows
     */
    public List<SignificantCategoryDto> significantCategories(String vertical, int minVolume, int limit) {
        NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                .withQuery(q -> q.term(t -> t.field("vertical").value(vertical)))
                .withMaxResults(0)
                .withSourceFilter(new FetchSourceFilter(false, null, null))
                .withAggregation("sig", Aggregation.of(a -> a.significantTerms(t -> t
                        .field("datasourceCategories")
                        .size(limit)
                        .minDocCount((long) minVolume)
                        .backgroundFilter(q -> q.bool(b -> b
                                .mustNot(m -> m.exists(e -> e.field("vertical"))))))));

        Aggregate aggregate = aggregation(queryBuilder.build(), "sig");
        if (aggregate == null || !aggregate.isSigsterms()) {
            return List.of();
        }

        List<SignificantCategoryDto> result = new ArrayList<>();
        for (SignificantStringTermsBucket bucket : aggregate.sigsterms().buckets().array()) {
            result.add(new SignificantCategoryDto(
                    bucket.key(),
                    bucket.score(),
                    bucket.docCount(),
                    bucket.bgCount()));
        }
        return result;
    }

    /**
     * Returns structured attribute suggestions for the given vertical without
     * mutating any files. Each entry carries coverage stats and a flag for whether
     * the attribute YAML definition already exists on the classpath. Suitable for
     * agents assembling {@code attributesConfig.configs} themselves.
     *
     * @param vc          the vertical configuration
     * @param minCoverage minimum coverage percentage (0-100)
     * @param containing  optional substring filter on attribute key (case-insensitive)
     * @return list sorted descending by coverage percent
     */
    public List<AttributeSuggestionDto> suggestAttributes(VerticalConfig vc, int minCoverage, String containing) {
        VerticalAttributesStats stats = attributesStats(vc.getId());
        int totalItems = stats.getTotalItems();
        if (totalItems == 0) {
            return List.of();
        }

        Set<String> exclusions = new HashSet<>();
        if (vc.getGenerationExcludedFromAttributesMatching() != null) {
            exclusions.addAll(vc.getGenerationExcludedFromAttributesMatching());
        }
        vc.getAttributesConfig().getConfigs().stream()
                .map(a -> a.getSynonyms().values())
                .forEach(v -> v.forEach(exclusions::addAll));

        Set<String> knownAttributes = getKnownAttributesFromOtherVerticals(vc.getId());

        List<AttributeSuggestionDto> result = new ArrayList<>();
        for (Entry<String, AttributesStats> entry : stats.getStats().entrySet()) {
            String key = entry.getKey();
            if (exclusions.contains(key)) {
                continue;
            }
            if (!StringUtils.isEmpty(containing) && !key.toLowerCase().contains(containing.toLowerCase())) {
                continue;
            }
            int coveragePercent = (int) (entry.getValue().getHits() / (double) totalItems * 100.0);
            boolean isKnown = knownAttributes.contains(key);
            if (!isKnown && coveragePercent < minCoverage) {
                continue;
            }
            boolean ymlExists = resourceResolver.getResource("classpath:attributes/" + key + ".yml").exists();
            Map<String, Integer> topValues = new LinkedHashMap<>();
            entry.getValue().getValues().entrySet().stream().limit(10)
                    .forEach(e -> topValues.put(e.getKey(), e.getValue()));
            result.add(new AttributeSuggestionDto(
                    key,
                    entry.getValue().getHits(),
                    coveragePercent,
                    ymlExists,
                    new ArrayList<>(entry.getValue().getDatasourceNames()),
                    topValues));
        }
        result.sort((a, b) -> Integer.compare(b.coveragePercent(), a.coveragePercent()));
        return result;
    }

    // -----------------------------------------------------------------------
    // Vertical scaffold template
    // -----------------------------------------------------------------------

    /**
     * Returns a scaffolded vertical YAML string from the bundled template.
     */
    public String verticalTemplate(String id, String googleTaxonomyId, String matchingCategories,
            String urlPrefix, String h1Prefix, String verticalHomeUrl, String verticalHomeTitle) {
        String ret = "";
        try {
            Resource r = resourceResolver.getResource("classpath:/templates/vertical-definition.yml");
            String content = r.getContentAsString(java.nio.charset.Charset.defaultCharset());
            Map<String, Object> context = new HashMap<>();
            context.put("id", id);
            context.put("googleTaxonomyId", googleTaxonomyId);
            context.put("matchingCategories",
                    generateCategoryMappingFragmentForGtin(Arrays.asList(matchingCategories.split(",")), null));
            context.put("urlPrefix", urlPrefix);
            context.put("h1Prefix", h1Prefix);
            context.put("verticalHomeUrl", verticalHomeUrl);
            context.put("verticalHomeTitle", verticalHomeTitle);
            ret = evalService.thymeleafEval(context, content);
        } catch (IOException e) {
            LOGGER.error("Error while generating vertical file", e);
        }
        return ret;
    }

    // -----------------------------------------------------------------------
    // Known attributes helper
    // -----------------------------------------------------------------------

    private Set<String> getKnownAttributesFromOtherVerticals(String currentVerticalId) {
        Set<String> knownAttributes = new HashSet<>();
        verticalConfigservice.getConfigsWithoutDefault().stream()
                .filter(vc -> !vc.getId().equals(currentVerticalId))
                .forEach(vc -> {
                    if (vc.getAttributesConfig() != null && vc.getAttributesConfig().getConfigs() != null) {
                        vc.getAttributesConfig().getConfigs().forEach(attr -> {
                            knownAttributes.add(attr.getKey());
                            if (attr.getSynonyms() != null) {
                                attr.getSynonyms().values().forEach(knownAttributes::addAll);
                            }
                        });
                    }
                });
        return knownAttributes;
    }

    // -----------------------------------------------------------------------
    // Attribute mapping (used by /{vertical}/attributes/ endpoint)
    // -----------------------------------------------------------------------

    /**
     * Generates a commented YAML attribute config fragment for the given vertical.
     */
    public String generateAttributesMapping(VerticalConfig verticalConfig, int minCoverage, String containing) {
        LOGGER.info("Generating attributes mapping for {}", verticalConfig);
        VerticalAttributesStats stats = attributesStats(verticalConfig.getId());

        Set<String> exclusions = new HashSet<>();
        if (verticalConfig.getGenerationExcludedFromAttributesMatching() != null) {
            exclusions.addAll(verticalConfig.getGenerationExcludedFromAttributesMatching());
        }
        verticalConfig.getAttributesConfig().getConfigs().stream()
                .map(e -> e.getSynonyms().values())
                .forEach(e -> e.forEach(exclusions::addAll));

        Set<String> knownAttributes = getKnownAttributesFromOtherVerticals(verticalConfig.getId());
        int totalItems = stats.getTotalItems();

        StringBuilder ret = new StringBuilder();
        for (Entry<String, AttributesStats> cat : stats.getStats().entrySet()) {
            if (!exclusions.contains(cat.getKey())) {
                boolean isKnown = knownAttributes.contains(cat.getKey());
                int coveragePercent = (int) (cat.getValue().getHits() / (double) totalItems * 100.0);
                if (isKnown || coveragePercent > minCoverage) {
                    LOGGER.info("Generating template for attribute: {} (Known: {}, Coverage: {}%)",
                            cat.getKey(), isKnown, coveragePercent);
                    if (StringUtils.isEmpty(containing)
                            || cat.getKey().toLowerCase().contains(containing.toLowerCase())) {
                        ret.append(attributeConfigTemplate(cat, totalItems, 10));
                    }
                } else {
                    LOGGER.info("Skipping {}, not enough coverage", cat.getKey());
                }
            }
        }
        return ret.toString();
    }

    // -----------------------------------------------------------------------
    // Impact score criteria fragment
    // -----------------------------------------------------------------------

    /**
     * Generates the {@code availableImpactScoreCriterias} YAML fragment.
     */
    public String generateAvailableImpactScoreCriteriasFragment(VerticalConfig verticalConfig,
            int minCoveragePercent) {
        Objects.requireNonNull(verticalConfig, "verticalConfig is required");
        String verticalId = verticalConfig.getId();
        Set<String> candidates = collectImpactScoreCandidates(verticalId);
        if (candidates.isEmpty()) {
            LOGGER.info("No impact score candidates found for {}", verticalId);
            return "availableImpactScoreCriterias:\n";
        }
        long total = repository.countMainIndexHavingVertical(verticalId);
        if (total <= 0) {
            LOGGER.info("No products found for vertical {}", verticalId);
            return "availableImpactScoreCriterias:\n";
        }
        StringBuilder builder = new StringBuilder("availableImpactScoreCriterias:\n");
        candidates.stream()
                .filter(StringUtils::isNotBlank)
                .sorted()
                .forEach(scoreKey -> {
                    Long count = repository.countMainIndexHavingScore(scoreKey, verticalId);
                    long hits = count == null ? 0L : count;
                    int coveragePercent = (int) (hits / (double) total * 100.0);
                    if (coveragePercent >= minCoveragePercent) {
                        builder.append("  # coverage: ").append(coveragePercent)
                                .append("% (").append(hits).append("/").append(total).append(")\n")
                                .append("  - ").append(scoreKey).append("\n");
                    } else {
                        LOGGER.info("Skipping impact score criteria {} with coverage {}%", scoreKey,
                                coveragePercent);
                    }
                });
        return builder.toString();
    }

    private Set<String> collectImpactScoreCandidates(String targetVerticalId) {
        Set<String> candidates = new HashSet<>();
        List<VerticalConfig> configs = new ArrayList<>(verticalConfigservice.getConfigsWithoutDefault());
        VerticalConfig defaultConfig = verticalConfigservice.getDefaultConfig();
        if (defaultConfig != null) {
            configs.add(defaultConfig);
        }
        for (VerticalConfig cfg : configs) {
            if (cfg == null || StringUtils.equals(targetVerticalId, cfg.getId())) {
                continue;
            }
            addImpactScoreCandidatesFromConfig(cfg, candidates);
        }
        return candidates;
    }

    private void addImpactScoreCandidatesFromConfig(VerticalConfig config, Set<String> candidates) {
        AttributesConfig attributesConfig = config.getAttributesConfig();
        if (attributesConfig != null && attributesConfig.getConfigs() != null) {
            attributesConfig.getConfigs().stream()
                    .filter(AttributeConfig::isAsScore)
                    .map(AttributeConfig::getKey)
                    .filter(StringUtils::isNotBlank)
                    .forEach(candidates::add);
        }
        if (config.getAvailableImpactScoreCriterias() != null) {
            config.getAvailableImpactScoreCriterias().stream()
                    .filter(StringUtils::isNotBlank)
                    .forEach(candidates::add);
        }
        ImpactScoreConfig impactScoreConfig = config.getImpactScoreConfig();
        if (impactScoreConfig != null && impactScoreConfig.getCriteriasPonderation() != null) {
            impactScoreConfig.getCriteriasPonderation().keySet().stream()
                    .filter(StringUtils::isNotBlank)
                    .forEach(candidates::add);
        }
    }

    // -----------------------------------------------------------------------
    // Impact score generation (dry-run + compose -- no disk writes)
    // -----------------------------------------------------------------------

    /**
     * Resolves the impact-score generation prompt without executing the AI call.
     *
     * @param vConf the vertical configuration
     * @return the resolved PromptConfig
     */
    public org.open4goods.services.prompt.config.PromptConfig generateEcoscoreDryRun(VerticalConfig vConf)
            throws Exception {
        Map<String, Object> context = buildEcoscoreContext(vConf);
        return genAiService.resolvePrompt("impactscore-generation", context,
                org.open4goods.model.ai.ImpactScoreAiResult.class);
    }

    /**
     * Runs the impact-score LLM prompt and returns YAML content for the
     * {@code impactscores/{vertical}.yml} file. The caller is responsible for
     * writing the content to disk.
     */
    public String generateEcoscoreYamlConfig(VerticalConfig vConf) {
        try {
            Map<String, Object> context = buildEcoscoreContext(vConf);
            PromptResponse<org.open4goods.model.ai.ImpactScoreAiResult> response =
                    genAiService.objectPrompt("impactscore-generation", context,
                            org.open4goods.model.ai.ImpactScoreAiResult.class);

            ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
            impactScoreConfig.setAiResult(response.getBody());

            impactScoreConfig.setCriteriasPonderation(buildCriteriasPonderation(response.getBody(), vConf));
            String yamlPrompt = serialisationService.toYaml(response.getPrompt());
            impactScoreConfig.setYamlPrompt(yamlPrompt);
            impactScoreConfig.setPromptRevision(shortSha256(yamlPrompt));
            impactScoreConfig.setAiJsonResponse(serialisationService.toJson(response.getBody(), true));
            return serialisationService.toYaml(impactScoreConfig).replaceFirst("^---\\s*", "");
        } catch (Exception e) {
            LOGGER.error("Ecoscore generation failed for {}", vConf, e);
            throw new IllegalStateException("Ecoscore generation failed for vertical " + vConf.getId(), e);
        }
    }

    /**
     * Composes an ImpactScoreConfig from a raw AI JSON response without a new LLM
     * call. The caller writes the result to {@code impactscores/{vertical}.yml}.
     */
    public ImpactScoreConfig generateEcoscoreConfigFromJson(VerticalConfig vConf, String aiJsonResponse)
            throws Exception {
        org.open4goods.model.ai.ImpactScoreAiResult aiResult =
                serialisationService.fromJson(aiJsonResponse, org.open4goods.model.ai.ImpactScoreAiResult.class);

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setAiResult(aiResult);

        impactScoreConfig.setCriteriasPonderation(buildCriteriasPonderation(aiResult, vConf));
        org.open4goods.services.prompt.config.PromptConfig prompt = generateEcoscoreDryRun(vConf);
        String yamlPrompt = serialisationService.toYaml(prompt);
        impactScoreConfig.setYamlPrompt(yamlPrompt);
        impactScoreConfig.setPromptRevision(shortSha256(yamlPrompt));
        impactScoreConfig.setAiJsonResponse(serialisationService.toJson(aiResult, true));
        return impactScoreConfig;
    }

    /**
     * Short (16 hex) SHA-256 fingerprint of the resolved prompt, used as a
     * stable revision marker on {@link ImpactScoreConfig#getPromptRevision()}.
     * A mismatch between this hash and the hash of the currently resolved
     * prompt signals that the file was generated with an outdated template.
     */
    static String shortSha256(String content) {
        if (content == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private Map<String, Object> buildEcoscoreContext(VerticalConfig vConf) throws SerialisationException {
        Map<String, Object> context = new HashMap<>();

        List<String> criteriaKeys = resolveImpactCriteriaKeys(vConf);
        Map<String, Long> coverage = loadImpactCriteriaCoverage(vConf, criteriaKeys);
        if (criteriaKeys.isEmpty() && !coverage.isEmpty()) {
            criteriaKeys = new ArrayList<>(coverage.keySet());
        }
        long totalProducts = repository.countMainIndexTotal(vConf.getId());

        String availableCriterias = formatAvailableCriterias(vConf, criteriaKeys, coverage, totalProducts);
        if (StringUtils.isBlank(availableCriterias)) {
            LOGGER.warn("AVAILABLE_CRITERIAS is empty for vertical {}", vConf.getId());
        }
        context.put("AVAILABLE_CRITERIAS", availableCriterias);
        context.put("AVAILABLE_CRITERIAS_JSON", serialisationService.toJson(criteriaKeys));
        context.put("CURRENT_DATE", LocalDate.now().toString());

        String verticalName = null;
        if (vConf.getI18n() != null && vConf.getI18n().get("fr") != null) {
            verticalName = vConf.getI18n().get("fr").getVerticalHomeTitle();
        }
        if (StringUtils.isBlank(verticalName)) {
            LOGGER.warn("VERTICAL_NAME is missing for vertical {}", vConf.getId());
        }
        context.put("VERTICAL_NAME", verticalName);

        // Grounding stats: lets the LLM weight criteria by real data coverage rather
        // than by a-priori assumptions. coverage > 0 also means the criterion is
        // currently scored on prod ES (post-batch); see ProductRepository.scoresCoverage.
        context.put("TOTAL_PRODUCTS", totalProducts);
        context.put("CRITERIAS_STATS", formatCriteriasStats(criteriaKeys, coverage, totalProducts));

        return context;
    }

    private List<String> resolveImpactCriteriaKeys(VerticalConfig vConf) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (vConf.getAvailableImpactScoreCriterias() != null) {
            vConf.getAvailableImpactScoreCriterias().stream()
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .forEach(keys::add);
        }
        return new ArrayList<>(keys);
    }

    private Map<String, Long> loadImpactCriteriaCoverage(VerticalConfig vConf, List<String> criteriaKeys) {
        Map<String, Long> ret = new LinkedHashMap<>();
        Map<String, Long> indexedCoverage = repository.scoresCoverage(vConf);
        Map<String, Long> safeIndexedCoverage = indexedCoverage == null ? Map.of() : indexedCoverage;
        for (String criteriaKey : criteriaKeys) {
            Long count = safeIndexedCoverage.get(criteriaKey);
            if (count == null) {
                count = repository.countMainIndexHavingScore(criteriaKey, vConf.getId());
            }
            ret.put(criteriaKey, count == null ? 0L : count);
        }
        if (criteriaKeys.isEmpty()) {
            safeIndexedCoverage.entrySet().stream()
                    .filter(entry -> StringUtils.isNotBlank(entry.getKey()))
                    .forEach(entry -> ret.put(entry.getKey(), entry.getValue() == null ? 0L : entry.getValue()));
        }
        return ret;
    }

    private Map<String, Double> buildCriteriasPonderation(org.open4goods.model.ai.ImpactScoreAiResult aiResult,
            VerticalConfig vConf) {
        if (aiResult == null) {
            throw new IllegalArgumentException("Impact-score AI response is empty");
        }

        Map<String, Double> generatedWeights = new LinkedHashMap<>();
        if (aiResult.getCriteriaWeights() != null) {
            for (org.open4goods.model.ai.ImpactScoreAiResult.CriteriaWeight cw : aiResult.getCriteriaWeights()) {
                if (cw == null || StringUtils.isBlank(cw.criterion)) {
                    continue;
                }
                if (!Double.isFinite(cw.weight) || cw.weight < 0.0) {
                    throw new IllegalArgumentException("Invalid impact-score weight for " + cw.criterion + ": "
                            + cw.weight);
                }
                generatedWeights.put(cw.criterion.trim(), cw.weight);
            }
        }
        if (generatedWeights.isEmpty()) {
            throw new IllegalArgumentException("Impact-score AI response does not contain criteria_weights");
        }

        LinkedHashMap<String, Double> ret = new LinkedHashMap<>();
        List<String> expectedCriteria = resolveImpactCriteriaKeys(vConf);
        if (expectedCriteria.isEmpty()) {
            ret.putAll(generatedWeights);
        } else {
            for (String criteria : expectedCriteria) {
                ret.put(criteria, generatedWeights.getOrDefault(criteria, 0.0));
            }
            generatedWeights.forEach((criteria, weight) -> {
                if (!ret.containsKey(criteria)) {
                    LOGGER.warn("AI returned impact-score criterion {} that is not configured for vertical {}",
                            criteria, vConf.getId());
                    ret.put(criteria, weight);
                }
            });
        }

        return normalizeWeights(ret);
    }

    private Map<String, Double> normalizeWeights(Map<String, Double> weights) {
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0.0) {
            throw new IllegalArgumentException("Impact-score criteria weights sum is zero");
        }
        if (Math.abs(sum - 1.0) <= 0.000001) {
            return weights;
        }

        LOGGER.warn("Impact-score criteria weights sum to {}; normalizing to 1.0", sum);
        LinkedHashMap<String, Double> normalized = new LinkedHashMap<>();
        weights.forEach((criteria, weight) -> normalized.put(criteria, roundWeight(weight / sum)));

        double normalizedSum = normalized.values().stream().mapToDouble(Double::doubleValue).sum();
        double delta = roundWeight(1.0 - normalizedSum);
        if (Math.abs(delta) > 0.0) {
            String largestCriteria = normalized.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElseThrow();
            normalized.put(largestCriteria, roundWeight(normalized.get(largestCriteria) + delta));
        }
        return normalized;
    }

    private double roundWeight(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatAvailableCriterias(VerticalConfig vConf, List<String> criteriaKeys,
            Map<String, Long> coverage, long totalProducts) {
        StringBuilder ret = new StringBuilder();
        for (String key : criteriaKeys) {
            long count = coverage.getOrDefault(key, 0L);
            ret.append("  ").append(key).append(" : ");
            Optional.ofNullable(vConf.getAttributesConfig())
                    .map(c -> c.getAttributeConfigByKey(key))
                    .map(AttributeConfig::getScoreDescription)
                    .map(d -> d.get("fr"))
                    .ifPresentOrElse(ret::append, () -> ret.append(key));
            ret.append(" - coverage ").append(formatCoverage(count, totalProducts));
            ret.append("\n");
        }
        return ret.toString();
    }

    /**
     * Markdown table of per-criterion coverage stats, ordered by coverage descending.
     * Injected into the LLM prompt under the {@code CRITERIAS_STATS} variable so the
     * model can ground its weights in actual data density rather than priors.
     */
    private String formatCriteriasStats(List<String> criteriaKeys, Map<String, Long> coverage, long totalProducts) {
        if (criteriaKeys == null || criteriaKeys.isEmpty() || coverage == null || coverage.isEmpty()
                || totalProducts <= 0) {
            return "Aucune statistique disponible (vertical non scoré ou index vide).";
        }
        StringBuilder ret = new StringBuilder();
        ret.append("Total produits indexés pour la verticale : ").append(totalProducts).append("\n");
        ret.append("| criterion | products_with_score | coverage |\n");
        ret.append("|---|---|---|\n");
        criteriaKeys.stream()
                .sorted((a, b) -> Long.compare(coverage.getOrDefault(b, 0L), coverage.getOrDefault(a, 0L)))
                .forEach(criteriaKey -> ret.append("| ")
                        .append(criteriaKey).append(" | ")
                        .append(coverage.getOrDefault(criteriaKey, 0L)).append(" | ")
                        .append(formatCoverage(coverage.getOrDefault(criteriaKey, 0L), totalProducts))
                        .append(" |\n"));
        return ret.toString();
    }

    private static String formatCoverage(long count, long total) {
        if (total <= 0) {
            return count + " / 0";
        }
        double pct = (count * 100.0) / total;
        return String.format(Locale.ROOT, "%d / %d (%.1f%%)", count, total, pct);
    }

    private List<StringTermsBucket> categoryBuckets(String vertical, int minVolume, int limit) {
        NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                .withQuery(q -> q.term(t -> t.field("vertical").value(vertical)))
                .withMaxResults(0)
                .withSourceFilter(new FetchSourceFilter(false, null, null))
                .withAggregation("cats", Aggregation.of(a -> a.terms(t -> t
                        .field("datasourceCategories")
                        .size(limit)
                        .minDocCount(minVolume))));

        Aggregate aggregate = aggregation(queryBuilder.build(), "cats");
        if (aggregate == null || !aggregate.isSterms()) {
            return List.of();
        }
        return aggregate.sterms().buckets().array();
    }

    private Aggregate aggregation(org.springframework.data.elasticsearch.client.elc.NativeQuery query, String name) {
        SearchHits<Product> results = repository.getElasticsearchOperations()
                .search(query, Product.class, ProductRepository.CURRENT_INDEX);
        if (results.getAggregations() == null) {
            return null;
        }
        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) results.getAggregations();
        if (aggregations.get(name) == null) {
            return null;
        }
        return aggregations.get(name).aggregation().getAggregate();
    }

    private Map<String, String> categoryToDatasource(VerticalConfig vc) {
        Map<String, String> result = new HashMap<>();
        vc.getMatchingCategories().forEach((datasource, categories) ->
                categories.forEach(category -> result.put(category, datasource)));
        return result;
    }

    private Set<String> mappedCategories(VerticalConfig vc) {
        Set<String> result = new HashSet<>();
        vc.getMatchingCategories().values().forEach(result::addAll);
        return result;
    }

    private double share(long count, long total) {
        if (total == 0) {
            return 0.0;
        }
        return count / (double) total;
    }

    private static class CoverageAccumulator {
        private long products;
        private int unmappedCategoriesCount;
        private final List<String> sampleUnmapped = new ArrayList<>();
    }

    // -----------------------------------------------------------------------
    // Attribute definition template
    // -----------------------------------------------------------------------

    /**
     * Generates a commented YAML attribute definition template for the given attribute.
     */
    public String attributeConfigTemplate(Entry<String, AttributesStats> category, Integer totalItems,
            Integer maxSample) {
        String ret = "";
        try {
            Resource r = resourceResolver.getResource("classpath:/templates/attribute-definition.yml");
            String content = r.getContentAsString(java.nio.charset.Charset.defaultCharset());

            Map<String, Object> context = new HashMap<>();
            context.put("name", category.getKey());
            context.put("date", DateFormat.getInstance().format(new Date()));
            context.put("coveragePercent", (int) (category.getValue().getHits() / (double) totalItems * 100.0));
            context.put("attrHits", category.getValue().getHits());
            context.put("totalHits", totalItems);
            context.put("faicon", "");

            Set<Integer> icecatIds = icecatFeatureResolver.resolveFeatureName(category.getKey());
            String defaultName = "!!COMPLETE_HERE!!";
            String frName = "!!COMPLETE_HERE!!";

            if (icecatIds != null && !icecatIds.isEmpty()) {
                if (icecatIds.size() == 1) {
                    defaultName = icecatFeatureResolver.getFeatureName(icecatIds.iterator().next(), "default");
                    frName = icecatFeatureResolver.getFeatureName(icecatIds.iterator().next(), "fr");
                    context.put("icecatIds", icecatIds.iterator().next());
                } else {
                    LOGGER.warn("Multiple possibilities to name attribute {}: {}", category.getKey(), icecatIds);
                    defaultName = icecatFeatureResolver.getFeatureName(icecatIds.iterator().next(), "default");
                    frName = icecatFeatureResolver.getFeatureName(icecatIds.iterator().next(), "fr");
                    context.put("icecatIds", "!!!!" + icecatIds);
                }
            } else {
                context.put("icecatIds", "TODO");
            }
            context.put("default_name", defaultName);
            context.put("fr_name", frName);

            boolean onlyNumeric = category.getValue().getValues().keySet().stream()
                    .noneMatch(v -> !NumberUtils.isNumber(v));
            context.put("type", onlyNumeric ? "NUMERIC" : "STRING");

            StringBuilder attrsSamples = new StringBuilder();
            category.getValue().getValues().entrySet().stream().limit(maxSample)
                    .forEach(val -> attrsSamples.append("#     - ").append(val.getKey())
                            .append(" (").append(val.getValue()).append(" items)\n"));
            if (category.getValue().getValues().size() > maxSample) {
                attrsSamples.append("#     + ")
                        .append(category.getValue().getValues().size() - maxSample)
                        .append(" more attributes...\n");
            }
            context.put("attributesSamples", attrsSamples.toString());

            StringBuilder mapping = new StringBuilder();
            category.getValue().getDatasourceNames().forEach(ds -> mapping
                    .append("      ").append(ds).append(":\n")
                    .append("        - \"").append(category.getKey()).append("\"\n"));
            context.put("synonyms", mapping.toString());

            ret = evalService.thymeleafEval(context, content);
        } catch (IOException e) {
            LOGGER.error("Error while generating vertical file", e);
        }

        StringBuilder sb = new StringBuilder();
        for (String line : ret.split("\n")) {
            sb.append("\n");
            if (!line.trim().startsWith("#")) {
                sb.append("#");
            }
            sb.append(line);
        }
        return sb.toString();
    }
}
