package org.open4goods.services.productrepository.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * GOU-1744, GOU-1882: runs {@link ProductRepository#exportVerticalForSitemap(String, int)} against a real
 * Elasticsearch with the production {@code product-mappings.json}. Not picked up by surefire
 * (IT suffix) and no failsafe execution is bound, so it is not part of the PR CI. Run it with
 * {@code mvn -pl services/product-repository test -Dtest=ProductRepositorySitemapExportIT
 * -DargLine="--add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"}
 * (rootless Docker required).
 */
@Testcontainers
class ProductRepositorySitemapExportIT {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

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

        IndexOperations indexOps = operations.indexOps(Product.class);
        indexOps.createWithMapping();
    }

    private static Product product(long id, String vertical, boolean recentOffer, Double score, boolean excluded) {
        Product p = new Product(id);
        p.setVertical(vertical);
        p.setExcluded(excluded);
        p.setLastChange(recentOffer ? System.currentTimeMillis() : 1_000L);
        p.setOffersCount(recentOffer ? 2 : 0);
        if (score != null) {
            p.setScores(Map.of("ECOSCORE", new Score("ECOSCORE", score)));
        }
        return p;
    }

    private static Product scoredWith(Product product, int methodologyVersion) {
        ImpactScoreMeta meta = new ImpactScoreMeta();
        meta.setMethodologyVersion(methodologyVersion);
        meta.setMethodologyRevision("0123abcd");
        meta.setScoredAt(System.currentTimeMillis());
        meta.setStatus(ImpactScoreMeta.Status.RATED.name());
        meta.setRealWeightCoverage(0.8);
        meta.setRealCriteriaCount(4);
        meta.setTotalCriteriaCount(5);
        product.setImpactScoreMeta(meta);
        return product;
    }

    @Test
    void keepsOfferOrScoreProductsOrderedByScoreAndDropsExcludedAndOtherVerticals() {
        operations.save(List.of(
                product(1, "tv", true, 3.0, false),     // offer + score
                product(2, "tv", false, 4.0, false),    // no offer + score (GOU-1744)
                product(3, "tv", true, null, false),    // offer, no score
                product(4, "tv", false, null, false),   // neither: out
                product(5, "tv", true, 5.0, true),      // excluded: out
                product(6, "tv", false, 5.0, true),     // excluded without offer: out
                product(7, "phone", true, 5.0, false), // other vertical: out
                scoredWith(product(8, "oven", false, 4.0, false), 1), // no offer, V1 score
                scoredWith(product(9, "oven", false, 3.0, false), 2), // no offer, V2 score
                product(10, "oven", false, 2.0, false),               // no offer, legacy score (no version)
                scoredWith(product(11, "oven", true, 1.0, false), 2)), // offer, V2 score
                ProductRepository.CURRENT_INDEX);
        operations.indexOps(ProductRepository.CURRENT_INDEX).refresh();

        List<Long> ids;
        try (var stream = repository.exportVerticalForSitemap("tv", 1)) {
            ids = stream.map(Product::getId).collect(Collectors.toList());
        }

        // by score desc (2: 4.0, 1: 3.0), product without score last
        assertThat(ids).containsExactly(2L, 1L, 3L);

        // GOU-1882: without a recent offer, only a score of the current methodology keeps a product listed
        try (var stream = repository.exportVerticalForSitemap("oven", 1)) {
            ids = stream.map(Product::getId).collect(Collectors.toList());
        }
        assertThat(ids).containsExactly(8L, 10L, 11L);
        try (var stream = repository.exportVerticalForSitemap("oven", 2)) {
            ids = stream.map(Product::getId).collect(Collectors.toList());
        }
        assertThat(ids).containsExactly(9L, 11L);
    }
}
