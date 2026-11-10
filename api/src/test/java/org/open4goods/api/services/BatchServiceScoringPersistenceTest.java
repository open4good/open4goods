package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.product.EcoScoreRanking;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.feedservice.service.FeedIndexingService;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.VerticalsConfigService;

/**
 * Verifies which scores {@link BatchService} is allowed to publish once a scoring run has ended.
 */
class BatchServiceScoringPersistenceTest {

	@Test
	void excludedProductLosesItsScoresAndRanking() {
		Product product = scoredProduct();
		product.setExcluded(true);

		BatchService.clearObsoleteScores(List.of(product), vertical());

		assertThat(product.getScores()).isEmpty();
		assertThat(product.getRanking().getGlobalPosition()).isZero();
		assertThat(product.getImpactScoreMeta()).isNull();
	}

	@Test
	void productUnmatchedFromTheVerticalLosesItsScores() {
		Product product = scoredProduct();
		product.setVertical(null);

		BatchService.clearObsoleteScores(List.of(product), vertical());

		assertThat(product.getScores()).isEmpty();
		assertThat(product.getImpactScoreMeta()).isNull();
	}

	@Test
	void productWithoutOffersKeepsItsLastScore() {
		Product product = scoredProduct();
		product.setOffersCount(0);

		BatchService.clearObsoleteScores(List.of(product), vertical());

		assertThat(product.ecoscore().getValue()).isEqualTo(12.0);
		assertThat(product.getImpactScoreMeta().getMethodologyVersion()).isEqualTo(1);
		assertThat(product.getRanking().getGlobalCount()).isZero();
		assertThat(product.getRanking().getGlobalPosition()).isZero();
		assertThat(product.getScores().values()).allSatisfy(score -> {
			assertThat(score.getRanking()).isNull();
			assertThat(score.getLowestScoreId()).isNull();
			assertThat(score.getHighestScoreId()).isNull();
		});
		assertThat(product.getScores().get("WARRANTY").getValue()).isEqualTo(2.0);
	}

	@Test
	void productWithNullOffersCountIsTreatedAsWithoutOffers() {
		Product product = scoredProduct();
		product.setOffersCount(null);

		BatchService.clearObsoleteScores(List.of(product), vertical());

		assertThat(product.getRanking().getGlobalCount()).isZero();
		assertThat(product.ecoscore().getRanking()).isNull();
	}

	@Test
	void productWithOffersKeepsItsRanks() {
		Product product = scoredProduct();
		product.ecoscore().setRanking(4);

		BatchService.clearObsoleteScores(List.of(product), vertical());

		assertThat(product.getRanking().getGlobalPosition()).isEqualTo(3);
		assertThat(product.getRanking().getGlobalCount()).isEqualTo(10);
		assertThat(product.ecoscore().getRanking()).isEqualTo(4);
	}

	@Test
	void compositeImpactScoreIsRecomputedFromTheStoredSubScores() {
		Product product = scoredProduct();
		product.setOffersCount(0);
		product.ecoscore().setAggregates(Map.of("REFRIGERANT_GWP", 1.0));
		product.ecoscore().setValue(1.2);

		BatchService.clearObsoleteScores(List.of(product), vertical());

		Score impactScore = product.ecoscore();
		// WARRANTY relative 3.0 / 5, single criterion: 3.0 * 4 = 12 / 20
		assertThat(impactScore.getValue()).isEqualTo(12.0);
		assertThat(impactScore.getAggregates()).isEmpty();
		assertThat(impactScore.getRelativ().getValue()).isEqualTo(3.0);
		// no offers: the rank of the earlier cohort is cleared after the recomputation
		assertThat(impactScore.getRanking()).isNull();
		assertThat(product.getRanking().getGlobalPosition()).isZero();
		assertThat(product.getRanking().getGlobalCount()).isZero();
	}

	@Test
	void compositeImpactScoreIsRemovedWhenItCannotBeRecomputed() {
		Product product = scoredProduct();
		product.setOffersCount(0);
		product.ecoscore().setAggregates(Map.of("REFRIGERANT_GWP", 1.0));

		BatchService.clearObsoleteScores(List.of(product), new VerticalConfig());

		assertThat(product.ecoscore()).isNull();
		assertThat(product.getScores()).containsKey("WARRANTY");
		assertThat(product.getRanking().getGlobalPosition()).isZero();
	}

	/**
	 * GOU-1879: in methodology V2, a product without offers is neither recomputed nor cleared, even when its stored
	 * ImpactScore is the lot 0 composite defect. The front-api hides a score of another version.
	 */
	@Test
	void v2ProductWithoutOffersIsNeitherRecomputedNorCleared() {
		Product product = scoredProduct();
		product.setOffersCount(0);
		product.ecoscore().setAggregates(Map.of("REFRIGERANT_GWP", 1.0));
		product.ecoscore().setValue(1.2);

		BatchService.clearObsoleteScores(List.of(product), v2Vertical());

		assertThat(product.ecoscore().getValue()).isEqualTo(1.2);
		assertThat(product.ecoscore().getAggregates()).containsKey("REFRIGERANT_GWP");
		assertThat(product.getRanking().getGlobalPosition()).isZero();
		assertThat(product.getImpactScoreMeta().getMethodologyVersion()).isEqualTo(1);
	}

	@Test
	void v2ExcludedProductLosesItsScores() {
		Product product = scoredProduct();
		product.setExcluded(true);

		BatchService.clearObsoleteScores(List.of(product), v2Vertical());

		assertThat(product.getScores()).isEmpty();
		assertThat(product.getImpactScoreMeta()).isNull();
	}

	@Test
	void nothingIsIndexedWhenScoringFails() {
		VerticalConfig vertical = vertical();
		Product product = scoredProduct();

		ProductRepository repository = mock(ProductRepository.class);
		when(repository.getProductsMatchingVerticalId(vertical)).thenReturn(Stream.of(product));
		AggregationFacadeService aggregation = mock(AggregationFacadeService.class);
		doThrow(new IllegalStateException("scoring failure")).when(aggregation).score(eq(vertical), anySet());

		new BatchService(aggregation, mock(CompletionFacadeService.class), mock(VerticalsConfigService.class),
				repository, mock(FeedIndexingService.class), mock(FeedService.class),
				mock(SerialisationService.class), mock(ResourceService.class), mock(ApiProperties.class))
				.batch(vertical);

		verify(repository, never()).addToFullindexationQueue(anyCollection());
	}

	private static VerticalConfig vertical() {
		ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
		impactScoreConfig.setCriteriasPonderation(Map.of("WARRANTY", 1.0));
		VerticalConfig vertical = new VerticalConfig();
		vertical.setId("tv");
		vertical.setImpactScoreConfig(impactScoreConfig);
		return vertical;
	}

	private static VerticalConfig v2Vertical() {
		VerticalConfig vertical = vertical();
		ImpactScoreV2Config v2 = new ImpactScoreV2Config();
		v2.setCriteriasPonderation(Map.of("WARRANTY", 1.0));
		v2.setMinRealWeightCoverage(0.5);
		vertical.getImpactScoreConfig().setV2(v2);
		vertical.getImpactScoreConfig().setMethodologyVersion(ImpactScoreConfig.METHODOLOGY_V2);
		return vertical;
	}

	private static Product scoredProduct() {
		Product product = new Product(1234567890123L);
		product.setVertical("tv");
		product.setOffersCount(1);
		product.getScores().put("ECOSCORE", new Score("ECOSCORE", 12.0));
		Score warranty = new Score("WARRANTY", 2.0);
		Cardinality relativ = new Cardinality();
		relativ.setValue(3.0);
		warranty.setRelativ(relativ);
		product.getScores().put("WARRANTY", warranty);
		EcoScoreRanking ranking = new EcoScoreRanking();
		ranking.setGlobalPosition(3);
		ranking.setGlobalCount(10);
		product.setRanking(ranking);
		ImpactScoreMeta meta = new ImpactScoreMeta();
		meta.setMethodologyVersion(1);
		meta.setStatus(ImpactScoreMeta.Status.RATED.name());
		product.setImpactScoreMeta(meta);
		return product;
	}
}
