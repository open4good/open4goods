package org.open4goods.services.productrepository.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.ImpactScoreVerticalStats;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * GOU-1901: runs {@link ProductRepository#impactScoreVerticalStats(String, int)} against a real Elasticsearch with the
 * production {@code product-mappings.json}. Not part of the PR CI (IT suffix); run it like
 * {@link ProductRepositorySitemapExportIT} (rootless Docker required).
 *
 * <p>It needs {@code -DargLine="--add-opens java.base/java.math=ALL-UNNAMED"}, otherwise Spring Data fails with
 * {@code Cannot create PersistentEntity ... BigDecimal}: {@code mvn -pl services/product-repository test
 * -Dtest=ProductRepositoryImpactScoreStatsIT -DargLine="--add-opens java.base/java.math=ALL-UNNAMED"}.
 */
@Testcontainers
class ProductRepositoryImpactScoreStatsIT {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withStartupTimeout(java.time.Duration.ofMinutes(4));

    static ElasticsearchOperations operations;
    static ProductRepository repository;

    @BeforeAll
    static void connect() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress())
                .build();
        ElasticsearchClient client = ElasticsearchClients.createImperative(Rest5Clients.getRest5Client(clientConfiguration));
        operations = new ElasticsearchTemplate(client);
        repository = new ProductRepository(operations);
        operations.indexOps(Product.class).createWithMapping();
    }

    private static Product product(long id, String vertical, boolean excluded, int version, ImpactScoreMeta.Status status,
            ImpactScoreMeta.NotRatedReason reason, Double score, List<String> realCriteria) {
        Product p = new Product(id);
        p.setVertical(vertical);
        p.setExcluded(excluded);
        p.setLastChange(System.currentTimeMillis());
        p.setOffersCount(2);
        if (score != null) {
            Score s = new Score("ECOSCORE", score);
            Map<String, Score> scores = new HashMap<>();
            scores.put("ECOSCORE", s);
            p.setScores(scores);
        }
        ImpactScoreMeta meta = new ImpactScoreMeta();
        meta.setMethodologyVersion(version);
        meta.setScoredAt(1_700_000_000_000L + id);
        meta.setStatus(status.name());
        meta.setNotRatedReason(reason == null ? null : reason.name());
        meta.setRealCriteria(realCriteria);
        meta.setRealCriteriaCount(realCriteria.size());
        meta.setTotalCriteriaCount(3);
        p.setImpactScoreMeta(meta);
        return p;
    }

    @Test
    void countsOutcomesCoverageAndDistributionOfTheCurrentVersionOnly() {
        ImpactScoreMeta.Status rated = ImpactScoreMeta.Status.RATED;
        ImpactScoreMeta.Status notRated = ImpactScoreMeta.Status.NOT_RATED;
        operations.save(List.of(
                product(1, "tv", false, 2, rated, null, 1.0, List.of("A", "B", "C")),
                product(2, "tv", false, 2, rated, null, 2.75, List.of("A", "B")),
                product(3, "tv", false, 2, rated, null, 4.75, List.of("A")),
                product(4, "tv", false, 2, notRated, ImpactScoreMeta.NotRatedReason.LOW_COVERAGE, null, List.of("C")),
                product(5, "tv", false, 2, notRated, ImpactScoreMeta.NotRatedReason.NO_REAL_CRITERIA, null, List.of()),
                product(6, "tv", false, 1, rated, null, 2.25, List.of("A")),   // previous version: waiting for recalculation
                product(7, "tv", true, 2, rated, null, 3.75, List.of("A")),   // excluded: not counted as rated
                product(8, "oven", false, 2, rated, null, 2.5, List.of("A"))), // other vertical
                ProductRepository.CURRENT_INDEX);
        operations.indexOps(ProductRepository.CURRENT_INDEX).refresh();

        ImpactScoreVerticalStats stats = repository.impactScoreVerticalStats("tv", 2);

        assertThat(stats.ratedCount()).isEqualTo(3);
        assertThat(stats.notRatedLowCoverageCount()).isEqualTo(1);
        assertThat(stats.notRatedNoCriteriaCount()).isEqualTo(1);
        assertThat(stats.outdatedCount()).isEqualTo(1);
        assertThat(stats.coverageByCriterion()).containsEntry("A", 3L).containsEntry("B", 2L).containsEntry("C", 1L);
        assertThat(stats.realCriteriaCountDistribution()).containsEntry(1, 1L).containsEntry(2, 1L).containsEntry(3, 1L);
        assertThat(stats.scoreBandWidth()).isEqualTo(2);
        assertThat(stats.scoreHistogram()).containsExactly(0L, 0L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 1L); // 4.0, 11.0, 19.0
        assertThat(stats.scoreMin()).isEqualTo(4.0);
        assertThat(stats.scoreMax()).isEqualTo(19.0);
        assertThat(stats.scoreMedian()).isEqualTo(11.0);
        assertThat(stats.lastScoredAt()).isEqualTo(1_700_000_000_005L);
    }

    @Test
    void reportsNoMeasureForAnEmptyVertical() {
        ImpactScoreVerticalStats stats = repository.impactScoreVerticalStats("nothing", 2);

        assertThat(stats.ratedCount()).isZero();
        assertThat(stats.scoreHistogram()).isEmpty();
        assertThat(stats.scoreMedian()).isNull();
        assertThat(stats.lastScoredAt()).isNull();
    }
}
