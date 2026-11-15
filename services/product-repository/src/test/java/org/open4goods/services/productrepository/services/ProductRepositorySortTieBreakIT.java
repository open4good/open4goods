package org.open4goods.services.productrepository.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * GOU-1940: sorts a product search on the ImpactScore plus {@link ProductRepository#stableSortTieBreak()} against a
 * real Elasticsearch with the production {@code product-mappings.json}. The former tie-break on {@code id} (mapped
 * without doc values) made every sorted product search fail with a 500. Not picked up by surefire (IT suffix), run it
 * with {@code mvn -pl services/product-repository test -Dtest=ProductRepositorySortTieBreakIT
 * -DargLine="--add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"}
 * (rootless Docker required).
 */
@Testcontainers
class ProductRepositorySortTieBreakIT {

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
        operations.indexOps(Product.class).createWithMapping();

        operations.save(List.of(
                product(3_000_000_000_003L, 16.9, true),
                product(3_000_000_000_002L, 13.7, true),
                product(3_000_000_000_001L, 13.7, true),
                product(3_000_000_000_004L, 13.7, false),
                product(3_000_000_000_005L, 15.7, true)),
                ProductRepository.CURRENT_INDEX);
        operations.indexOps(ProductRepository.CURRENT_INDEX).refresh();
    }

    private static Product product(long id, double score, boolean withNormalizedGtin) {
        Product p = new Product(id);
        p.setVertical("airconditioner");
        p.setScores(Map.of("ECOSCORE", new Score("ECOSCORE", score)));
        if (withNormalizedGtin) {
            p.getGtinInfos().setNormalizedGtin14(String.format("%014d", id));
        }
        return p;
    }

    private static SearchHits<Product> searchSortedBy(Sort sort) {
        return repository.search(new NativeQueryBuilder()
                .withPageable(PageRequest.of(0, 20, sort))
                .build(), ProductRepository.MAIN_INDEX_NAME);
    }

    @Test
    void impactScoreSortWithTieBreakSucceedsAndOrdersTiesByGtin() {
        Sort sort = Sort.by(Sort.Order.desc("scores.ECOSCORE.value")).and(Sort.by(ProductRepository.stableSortTieBreak()));

        List<Long> ids = searchSortedBy(sort).getSearchHits().stream().map(SearchHit::getContent).map(Product::getId).toList();

        // Best score first, then the 13.7 ties by GTIN, the tie without normalized GTIN last.
        assertThat(ids).containsExactly(3_000_000_000_003L, 3_000_000_000_005L, 3_000_000_000_001L,
                3_000_000_000_002L, 3_000_000_000_004L);
    }

    @Test
    void sortingOnTheIdFieldIsRejectedByElasticsearch() {
        // Documents the root cause: "id" is mapped without doc values, Elasticsearch cannot sort on it.
        Sort sort = Sort.by(Sort.Order.desc("scores.ECOSCORE.value")).and(Sort.by(Sort.Order.asc("id")));

        assertThatThrownBy(() -> searchSortedBy(sort)).isInstanceOf(RuntimeException.class)
                .hasStackTraceContaining("search_phase_execution_exception");
    }
}
