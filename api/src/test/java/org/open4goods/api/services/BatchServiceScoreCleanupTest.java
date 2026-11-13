package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.feedservice.service.FeedIndexingService;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.VerticalsConfigService;

/**
 * GOU-2133: the end of the batch clears the scores of the products outside the batched verticals, only when
 * enabled, bounded and by bulks. The query and the write itself run against Elasticsearch in
 * {@code ProductRepositoryScoreCleanupIT}.
 */
class BatchServiceScoreCleanupTest {

	private final ProductRepository repository = mock(ProductRepository.class);
	private final VerticalsConfigService verticals = mock(VerticalsConfigService.class);
	private final ApiProperties properties = new ApiProperties();
	private BatchService batch;

	@BeforeEach
	void setUp() {
		VerticalConfig tv = new VerticalConfig();
		tv.setId("tv");
		when(verticals.getConfigsWithoutDefault()).thenReturn(List.of(tv));
		batch = new BatchService(mock(AggregationFacadeService.class), mock(CompletionFacadeService.class), verticals,
				repository, mock(FeedIndexingService.class), mock(FeedService.class), mock(SerialisationService.class),
				mock(ResourceService.class), properties);
	}

	private void found(long count) {
		when(repository.countScoredOutsideVerticals(Set.of("tv"))).thenReturn(count);
		when(repository.exportIdsScoredOutsideVerticals(Set.of("tv")))
				.thenReturn(LongStream.rangeClosed(1, count).boxed());
	}

	@Test
	void disabledByDefaultOnlyCounts() {
		found(3);

		batch.clearScoresOutsideVerticals();

		assertThat(properties.getScoreCleanup().isEnabled()).isFalse();
		verify(repository).countScoredOutsideVerticals(Set.of("tv"));
		verify(repository, never()).exportIdsScoredOutsideVerticals(any());
		verify(repository, never()).clearScores(anyCollection(), any());
	}

	@Test
	void enabledClearsEveryTargetedProductByBulks() {
		properties.getScoreCleanup().setEnabled(true);
		properties.getScoreCleanup().setBulkSize(2);
		List<List<Long>> bulks = new ArrayList<>();
		doAnswer(call -> bulks.add(new ArrayList<>(call.<Collection<Long>>getArgument(0))))
				.when(repository).clearScores(anyCollection(), any());
		found(5);

		batch.clearScoresOutsideVerticals();

		assertThat(bulks).containsExactly(List.of(1L, 2L), List.of(3L, 4L), List.of(5L));
		verify(repository, times(3)).clearScores(anyCollection(), eq(Set.of("tv")));
	}

	@Test
	void enabledStopsAtTheBound() {
		properties.getScoreCleanup().setEnabled(true);
		properties.getScoreCleanup().setBulkSize(10);
		properties.getScoreCleanup().setMaxProducts(3);
		List<List<Long>> bulks = new ArrayList<>();
		doAnswer(call -> bulks.add(new ArrayList<>(call.<Collection<Long>>getArgument(0))))
				.when(repository).clearScores(anyCollection(), any());
		found(5);

		batch.clearScoresOutsideVerticals();

		assertThat(bulks).containsExactly(List.of(1L, 2L, 3L));
	}

	@Test
	void aFailedBulkDoesNotStopTheNextOnes() {
		properties.getScoreCleanup().setEnabled(true);
		properties.getScoreCleanup().setBulkSize(2);
		doThrow(new IllegalStateException("bulk failure")).doNothing()
				.when(repository).clearScores(anyCollection(), any());
		found(3);

		batch.clearScoresOutsideVerticals();

		verify(repository, times(2)).clearScores(anyCollection(), any());
	}

	@Test
	void neverRunsWithoutBatchedVertical() {
		properties.getScoreCleanup().setEnabled(true);
		when(verticals.getConfigsWithoutDefault()).thenReturn(List.of());

		batch.clearScoresOutsideVerticals();

		verify(repository, never()).countScoredOutsideVerticals(any());
		verify(repository, never()).clearScores(anyCollection(), any());
	}

	@Test
	void aRepositoryFailureNeverStopsTheBatch() {
		properties.getScoreCleanup().setEnabled(true);
		when(repository.countScoredOutsideVerticals(any())).thenThrow(new IllegalStateException("es down"));
		when(repository.exportIdsScoredOutsideVerticals(any())).thenReturn(Stream.empty());

		batch.clearScoresOutsideVerticals();

		verify(repository, never()).clearScores(anyCollection(), any());
	}

	@Test
	void runsAtTheEndOfTheBatch() {
		when(repository.getProductsMatchingVerticalId(any())).thenReturn(Stream.empty());
		found(0);

		batch.batch();

		verify(repository).countScoredOutsideVerticals(Set.of("tv"));
	}
}
