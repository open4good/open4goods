package org.open4goods.services.productrepository.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.product.EcoScoreRanking;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.json.jackson.Jackson3JsonpMapper;

/**
 * GOU-2133: the products holding scores outside the batched verticals are found, cleared, and only them, against a
 * real Elasticsearch with the production {@code product-mappings.json}. Not picked up by surefire (IT suffix), so it
 * is not part of the PR CI. Run it with {@code mvn -pl services/product-repository test
 * -Dtest=ProductRepositoryScoreCleanupIT -DargLine="--add-opens java.base/java.math=ALL-UNNAMED
 * --add-opens java.base/java.util=ALL-UNNAMED"} (rootless Docker required).
 */
@Testcontainers
class ProductRepositoryScoreCleanupIT {

	@Container
	static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
			DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
			.withEnv("xpack.security.enabled", "false")
			.withEnv("discovery.type", "single-node");

	private static final Set<String> BATCHED = Set.of("tv", "washing-machine");

	private static ElasticsearchOperations operations;
	private static ProductRepository repository;

	@BeforeAll
	static void createIndex() {
		ClientConfiguration clientConfiguration = ClientConfiguration.builder()
				.connectedTo(ELASTICSEARCH.getHttpHostAddress())
				.build();
		operations = new ElasticsearchTemplate(ElasticsearchClients.createImperative(
				Rest5Clients.getRest5Client(clientConfiguration), null, new Jackson3JsonpMapper()));
		operations.indexOps(Product.class).createWithMapping();
		repository = new ProductRepository(operations);
	}

	/** A product holding an ImpactScore, its ranking and its assessment metadata. */
	private static Product scored(long id, String vertical, boolean excluded) {
		Product product = new Product(id);
		product.setVertical(vertical);
		product.setExcluded(excluded);
		product.setOffersCount(1);
		Score impactScore = new Score("ECOSCORE", 3.14);
		Score repairability = new Score("REPAIRABILITY_INDEX", 7.5);
		product.getScores().put(impactScore.getName(), impactScore);
		product.getScores().put(repairability.getName(), repairability);
		EcoScoreRanking ranking = new EcoScoreRanking();
		ranking.setGlobalPosition(663);
		ranking.setGlobalCount(2221);
		product.setRanking(ranking);
		product.setImpactScoreMeta(new ImpactScoreMeta());
		return product;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> source(long id) {
		return operations.get(String.valueOf(id), Map.class, ProductRepository.CURRENT_INDEX);
	}

	@Test
	@SuppressWarnings("unchecked")
	void clearsTheScoresOfProductsOutsideTheBatchedVerticalsOnly() {
		Product excludedWithoutVertical = scored(101, null, true);
		Product withoutVertical = scored(102, null, false);
		Product ofAnUnbatchedVertical = scored(103, "old-vertical", true);
		Product rated = scored(104, "tv", false);
		Product excludedOfABatchedVertical = scored(105, "washing-machine", true);
		Product neverScored = new Product(106L);
		operations.save(List.of(excludedWithoutVertical, withoutVertical, ofAnUnbatchedVertical, rated,
				excludedOfABatchedVertical, neverScored), ProductRepository.CURRENT_INDEX);
		operations.indexOps(ProductRepository.CURRENT_INDEX).refresh();

		assertThat(repository.countScoredOutsideVerticals(BATCHED)).isEqualTo(3);
		List<Long> ids;
		try (Stream<Long> stream = repository.exportIdsScoredOutsideVerticals(BATCHED)) {
			ids = stream.toList();
		}
		assertThat(ids).containsExactlyInAnyOrder(101L, 102L, 103L);

		repository.clearScores(ids, BATCHED);
		operations.indexOps(ProductRepository.CURRENT_INDEX).refresh();

		for (long id : ids) {
			Map<String, Object> source = source(id);
			assertThat((Map<String, Object>) source.get("scores")).as("scores of %s", id).isEmpty();
			assertThat((Map<String, Object>) source.get("ranking")).as("ranking of %s", id).isEmpty();
			assertThat(source).as("product %s", id).doesNotContainKey("impactScoreMeta").containsKey("offersCount");
			Product read = operations.get(String.valueOf(id), Product.class, ProductRepository.CURRENT_INDEX);
			assertThat(read.ecoscore()).isNull();
			assertThat(read.getRanking().getGlobalPosition()).isZero();
			assertThat(read.getImpactScoreMeta()).isNull();
		}
		// The products of a batched vertical keep theirs: the vertical batch owns them
		assertThat((Map<String, Object>) source(104).get("scores")).containsKey("ECOSCORE");
		assertThat((Map<String, Object>) source(105).get("scores")).containsKey("ECOSCORE");
		assertThat(source(104)).containsKey("impactScoreMeta");

		// Idempotent: a cleared product no longer matches
		assertThat(repository.countScoredOutsideVerticals(BATCHED)).isZero();
	}

	@Test
	@SuppressWarnings("unchecked")
	void leavesAProductThatJoinedABatchedVerticalSinceItWasRead() {
		Product joined = scored(201, "tv", false);
		operations.save(joined, ProductRepository.CURRENT_INDEX);

		repository.clearScores(List.of(201L), BATCHED);
		operations.indexOps(ProductRepository.CURRENT_INDEX).refresh();

		assertThat((Map<String, Object>) source(201).get("scores")).containsKey("ECOSCORE");
	}
}
