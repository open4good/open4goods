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
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
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

		BatchService.clearObsoleteScores(List.of(product));

		assertThat(product.getScores()).isEmpty();
		assertThat(product.getRanking().getGlobalPosition()).isZero();
	}

	@Test
	void productUnmatchedFromTheVerticalLosesItsScores() {
		Product product = scoredProduct();
		product.setVertical(null);

		BatchService.clearObsoleteScores(List.of(product));

		assertThat(product.getScores()).isEmpty();
	}

	@Test
	void productWithoutOffersKeepsItsLastScore() {
		Product product = scoredProduct();
		product.setOffersCount(0);

		BatchService.clearObsoleteScores(List.of(product));

		assertThat(product.ecoscore()).isNotNull();
		assertThat(product.getRanking().getGlobalPosition()).isEqualTo(3);
	}

	@Test
	void productWithoutOffersLosesAnImpactScoreOverwrittenByAComposite() {
		Product product = scoredProduct();
		product.setOffersCount(0);
		product.ecoscore().setAggregates(Map.of("REFRIGERANT_GWP", 1.0));

		BatchService.clearObsoleteScores(List.of(product));

		assertThat(product.ecoscore()).isNull();
		assertThat(product.getScores()).containsKey("WARRANTY");
		assertThat(product.getRanking().getGlobalPosition()).isZero();
	}

	@Test
	void nothingIsIndexedWhenScoringFails() {
		VerticalConfig vertical = new VerticalConfig();
		vertical.setId("tv");
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

	private static Product scoredProduct() {
		Product product = new Product(1234567890123L);
		product.setVertical("tv");
		product.setOffersCount(1);
		product.getScores().put("ECOSCORE", new Score("ECOSCORE", 12.0));
		product.getScores().put("WARRANTY", new Score("WARRANTY", 3.0));
		EcoScoreRanking ranking = new EcoScoreRanking();
		ranking.setGlobalPosition(3);
		product.setRanking(ranking);
		return product;
	}
}
